package dev.matheus.fluviapp.extensions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")
private val timeStampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

fun LocalDate.formatarDataBarrasBr(): String {
    return format(dateFormatter)
}

fun LocalDateTime.formatarDataHoraBarrasBr(): String {
    return format(dateTimeFormatter)
}

fun LocalDateTime.formatarTimeStamp(): String {
    return format(timeStampFormatter)
}

fun Long.convertMillisToLocalDateToString(): String {
    val localDate = LocalDate.ofEpochDay(Duration.ofMillis(this).toDays())
    return localDate.format(dateFormatter)
}

@OptIn(ExperimentalMaterial3Api::class)
fun TimePickerState.formatarTimeState(): String {
    return "$hour:${if (minute == 0) "00" else minute}"
}

fun String.converterParaLocalDate(): LocalDate {
    return LocalDate.parse(this, dateFormatter)
}