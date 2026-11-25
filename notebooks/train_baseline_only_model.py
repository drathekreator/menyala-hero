#!/usr/bin/env python3
"""Train and export the experimental pre-program HERO outcome model.

This candidate intentionally uses only values available before intervention:
age, Identity Foreclosure pre-test, Future Orientation pre-test, gender, and
scavenging involvement. Labels remain the existing proxy outcome (post-test
score below 30); this export is for a field-test prototype, not an approved
operational recommendation model.
"""

from __future__ import annotations

import json
import re
import statistics
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

import numpy as np
import pandas as pd
from iterstrat.ml_stratifiers import MultilabelStratifiedKFold
from sklearn.base import clone
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, hamming_loss, precision_score, recall_score
from sklearn.multioutput import MultiOutputClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "Dataset_Heroism.xlsx"
OUTPUT_ASSET = ROOT / "hero-compass-android/app/src/main/assets/heroism_baseline_model.json"
REPORT_DIR = ROOT / "reports"

FEATURES_NUMERIC = ["Usia", "IF_Pre", "OMD_Pre"]
FEATURES_CATEGORICAL = ["Jenis_Kelamin", "Terlibat_Memulung"]
FEATURES = FEATURES_NUMERIC + FEATURES_CATEGORICAL
TARGETS = ["Y_Potensi", "Y_Eksplorasi", "Y_Refleksi", "Y_Perencanaan"]
TARGET_PROGRAMS = ["HERO Asa", "HERO Aksi", "HERO Rasa", "HERO Karsa"]
LABEL_MAPPING = {"Tidak Perlu": 0, "Perlu": 1}


def _column_number(reference: str) -> int:
    letters = re.match(r"([A-Z]+)", reference).group(1)
    result = 0
    for letter in letters:
        result = result * 26 + ord(letter) - ord("A") + 1
    return result - 1


def read_xlsx_sheet(path: Path, sheet_name: str) -> pd.DataFrame:
    """Read a plain-value worksheet without requiring an Excel engine."""
    main_ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    rel_ns = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    ns = {"m": main_ns}
    with zipfile.ZipFile(path) as archive:
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        relationships = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        targets = {item.attrib["Id"]: item.attrib["Target"] for item in relationships}
        sheet = next(item for item in workbook.findall("m:sheets/m:sheet", ns) if item.attrib["name"] == sheet_name)
        target = targets[sheet.attrib[f"{{{rel_ns}}}id"]]
        worksheet_path = target.lstrip("/") if target.startswith("/") else f"xl/{target}"
        if not worksheet_path.startswith("xl/"):
            worksheet_path = f"xl/{worksheet_path}"

        shared_strings: list[str] = []
        if "xl/sharedStrings.xml" in archive.namelist():
            strings = ET.fromstring(archive.read("xl/sharedStrings.xml"))
            shared_strings = [
                "".join(node.text or "" for node in item.findall(".//m:t", ns))
                for item in strings.findall("m:si", ns)
            ]

        worksheet = ET.fromstring(archive.read(worksheet_path))
        matrix: list[list[object | None]] = []
        for row in worksheet.findall(".//m:sheetData/m:row", ns):
            values: list[object | None] = []
            for cell in row.findall("m:c", ns):
                index = _column_number(cell.attrib["r"])
                while len(values) <= index:
                    values.append(None)
                value = cell.find("m:v", ns)
                raw = None if value is None else value.text
                if raw is not None and cell.attrib.get("t") == "s":
                    values[index] = shared_strings[int(raw)]
                elif raw is not None:
                    values[index] = raw
            matrix.append(values)

    if not matrix:
        raise ValueError(f"Worksheet {sheet_name!r} is empty")
    headers = [str(value) if value is not None else "" for value in matrix[0]]
    return pd.DataFrame(matrix[1:], columns=headers).dropna(how="all")


def build_pipeline() -> Pipeline:
    preprocessor = ColumnTransformer([
        (
            "numeric",
            Pipeline([
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
            ]),
            FEATURES_NUMERIC,
        ),
        (
            "categorical",
            Pipeline([
                ("imputer", SimpleImputer(strategy="most_frequent")),
                ("onehot", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
            ]),
            FEATURES_CATEGORICAL,
        ),
    ])
    return Pipeline([
        ("preprocessor", preprocessor),
        ("model", MultiOutputClassifier(LogisticRegression(
            max_iter=3000,
            class_weight="balanced",
            random_state=42,
        ))),
    ])


def evaluate(y_true: np.ndarray, y_pred: np.ndarray) -> dict[str, float]:
    return {
        "macro_f1": float(f1_score(y_true, y_pred, average="macro", zero_division=0)),
        "macro_precision": float(precision_score(y_true, y_pred, average="macro", zero_division=0)),
        "macro_recall": float(recall_score(y_true, y_pred, average="macro", zero_division=0)),
        "hamming_loss": float(hamming_loss(y_true, y_pred)),
        "subset_accuracy": float(accuracy_score(y_true, y_pred)),
    }


def main() -> None:
    frame = read_xlsx_sheet(DATASET, "Dataset_Model")
    missing = sorted(set(FEATURES + TARGETS) - set(frame.columns))
    if missing:
        raise ValueError(f"Dataset is missing expected columns: {missing}")

    x = frame[FEATURES].copy()
    for feature in FEATURES_NUMERIC:
        x[feature] = pd.to_numeric(x[feature], errors="raise")
    y = frame[TARGETS].replace(LABEL_MAPPING).astype(int).to_numpy()
    pipeline = build_pipeline()

    fold_rows: list[dict[str, object]] = []
    for seed in range(5):
        splitter = MultilabelStratifiedKFold(n_splits=5, shuffle=True, random_state=seed)
        for fold, (train_indices, test_indices) in enumerate(splitter.split(x, y), start=1):
            fitted = clone(pipeline).fit(x.iloc[train_indices], y[train_indices])
            metrics = evaluate(y[test_indices], fitted.predict(x.iloc[test_indices]))
            fold_rows.append({"seed": seed, "fold": fold, **metrics})

    cv_summary = {
        key: {
            "mean": float(statistics.mean(float(row[key]) for row in fold_rows)),
            "std": float(statistics.stdev(float(row[key]) for row in fold_rows)),
        }
        for key in evaluate(y[:1], np.zeros_like(y[:1]))
    }

    final = pipeline.fit(x, y)
    preprocessor = final.named_steps["preprocessor"]
    numeric_pipeline = preprocessor.named_transformers_["numeric"]
    imputer = numeric_pipeline.named_steps["imputer"]
    scaler = numeric_pipeline.named_steps["scaler"]
    categorical_pipeline = preprocessor.named_transformers_["categorical"]
    categories = categorical_pipeline.named_steps["onehot"].categories_
    feature_names = preprocessor.get_feature_names_out().tolist()
    estimators = final.named_steps["model"].estimators_

    targets = []
    for name, program, estimator in zip(TARGETS, TARGET_PROGRAMS, estimators, strict=True):
        positive_class_index = int(np.where(estimator.classes_ == 1)[0][0])
        if len(estimator.classes_) != 2 or estimator.classes_[positive_class_index] != 1:
            raise ValueError(f"Unexpected binary classes for target {name}: {estimator.classes_}")
        targets.append({
            "id": name,
            "program": program,
            "positive_label": "Perlu",
            "class_order": estimator.classes_.astype(int).tolist(),
            "coefficients": estimator.coef_[0].astype(float).tolist(),
            "intercept": float(estimator.intercept_[0]),
        })

    asset = {
        "schema_version": 1,
        "model_name": "Baseline-only Logistic Regression",
        "model_version": "0.1.0-prototype",
        "trained_from": "Dataset_Heroism.xlsx / Dataset_Model",
        "training_rows": int(len(frame)),
        "features": {
            "numeric": FEATURES_NUMERIC,
            "categorical": FEATURES_CATEGORICAL,
            "transformed_order": feature_names,
            "numeric_imputation_median": imputer.statistics_.astype(float).tolist(),
            "numeric_mean": scaler.mean_.astype(float).tolist(),
            "numeric_scale": scaler.scale_.astype(float).tolist(),
            "categorical_categories": [category.tolist() for category in categories],
        },
        "decision_threshold": 0.5,
        "targets": targets,
        "target_definition": "Perlu = historical post-test score below 30; proxy label, not an approved support-need diagnosis.",
        "cross_validation": {
            "method": "MultilabelStratifiedKFold",
            "folds": 5,
            "repeats": 5,
            "summary": cv_summary,
        },
        "field_validation": "Not yet field-validated on a new cohort.",
        "use_limit": "Experimental risk estimate for human review. It does not estimate causal benefit or select the best intervention.",
    }

    OUTPUT_ASSET.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_ASSET.write_text(json.dumps(asset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    (REPORT_DIR / "logistic_regression_baseline_only_summary.json").write_text(
        json.dumps(asset["cross_validation"], ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    pd.DataFrame(fold_rows).to_csv(REPORT_DIR / "logistic_regression_baseline_only_cv.csv", index=False)
    print(json.dumps({"n": len(frame), "positive_counts": y.sum(axis=0).tolist(), "summary": cv_summary, "asset": str(OUTPUT_ASSET)}, indent=2))


if __name__ == "__main__":
    main()
