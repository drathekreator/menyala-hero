package org.heroism.herocompass.children

import android.app.DatePickerDialog
import android.content.Context
import java.time.LocalDate
import java.util.Calendar

fun interface BirthDatePicker {
    fun show(currentDate: LocalDate?, onDateSelected: (LocalDate) -> Unit)
}

class AndroidBirthDatePicker(private val context: Context) : BirthDatePicker {
    override fun show(currentDate: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
        val today = LocalDate.now()
        val initial = currentDate ?: today
        DatePickerDialog(context, { _, year, month, day ->
            onDateSelected(LocalDate.of(year, month + 1, day))
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
            datePicker.maxDate = Calendar.getInstance().timeInMillis
        }.show()
    }
}
