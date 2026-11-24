package org.heroism.herocompass.ml

import android.content.Context
import org.json.JSONObject
import kotlin.math.exp

data class BaselineModelInput(
    val ageAtPretest: Int,
    val identityForeclosurePretest: Int,
    val futureOrientationPretest: Int,
    val genderCode: String,
    val scavengingInvolved: Boolean,
)

data class DomainRiskEstimate(
    val targetId: String,
    val programName: String,
    val probabilityBelowTarget: Double,
    val decisionThreshold: Double,
) {
    val isAbovePrototypeThreshold: Boolean get() = probabilityBelowTarget >= decisionThreshold
}

data class ExperimentalBaselineForecast(
    val modelVersion: String,
    val trainingRows: Int,
    val macroF1Mean: Double,
    val macroF1Std: Double,
    val inputAgeOutsideTrainingRange: Boolean,
    val estimates: List<DomainRiskEstimate>,
)

/** On-device inference for a prototype baseline-only multi-output logistic regression. */
class ExperimentalBaselineModel private constructor(private val definition: JSONObject) {
    fun predict(input: BaselineModelInput): ExperimentalBaselineForecast? {
        if (input.ageAtPretest <= 0 || input.identityForeclosurePretest !in 15..60 ||
            input.futureOrientationPretest !in 15..60 || input.genderCode !in setOf("L", "P")
        ) return null

        val features = definition.getJSONObject("features")
        val medians = features.getJSONArray("numeric_imputation_median")
        val means = features.getJSONArray("numeric_mean")
        val scales = features.getJSONArray("numeric_scale")
        val rawValues = listOf(
            input.ageAtPretest.toDouble(),
            input.identityForeclosurePretest.toDouble(),
            input.futureOrientationPretest.toDouble(),
        )
        val transformed = rawValues.indices.map { index ->
            val value = rawValues[index].takeIf { it.isFinite() } ?: medians.getDouble(index)
            (value - means.getDouble(index)) / scales.getDouble(index)
        }.toMutableList()

        val categorical = features.getJSONArray("categorical_categories")
        val genderCategories = categorical.getJSONArray(0)
        for (index in 0 until genderCategories.length()) {
            transformed += if (genderCategories.getString(index) == input.genderCode) 1.0 else 0.0
        }
        val scavengingCategories = categorical.getJSONArray(1)
        val scavengingCode = if (input.scavengingInvolved) "Ya" else "Tidak"
        for (index in 0 until scavengingCategories.length()) {
            transformed += if (scavengingCategories.getString(index) == scavengingCode) 1.0 else 0.0
        }

        val threshold = definition.getDouble("decision_threshold")
        val targets = definition.getJSONArray("targets")
        val estimates = (0 until targets.length()).map { index ->
            val target = targets.getJSONObject(index)
            val coefficients = target.getJSONArray("coefficients")
            require(coefficients.length() == transformed.size) { "Model feature dimension mismatch" }
            var logit = target.getDouble("intercept")
            transformed.forEachIndexed { featureIndex, value ->
                logit += coefficients.getDouble(featureIndex) * value
            }
            DomainRiskEstimate(
                targetId = target.getString("id"),
                programName = target.getString("program"),
                probabilityBelowTarget = sigmoid(logit),
                decisionThreshold = threshold,
            )
        }

        val summary = definition.getJSONObject("cross_validation").getJSONObject("summary").getJSONObject("macro_f1")
        return ExperimentalBaselineForecast(
            modelVersion = definition.getString("model_version"),
            trainingRows = definition.getInt("training_rows"),
            macroF1Mean = summary.getDouble("mean"),
            macroF1Std = summary.getDouble("std"),
            inputAgeOutsideTrainingRange = input.ageAtPretest !in 8..13,
            estimates = estimates,
        )
    }

    private fun sigmoid(value: Double): Double = when {
        value >= 0 -> 1.0 / (1.0 + exp(-value))
        else -> exp(value) / (1.0 + exp(value))
    }

    companion object {
        fun load(context: Context): ExperimentalBaselineModel =
            context.applicationContext.assets.open("heroism_baseline_model.json").bufferedReader().use { reader ->
                ExperimentalBaselineModel(JSONObject(reader.readText()))
            }
    }
}
