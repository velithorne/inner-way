package com.aura.shell.ui.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple clock for UI; recreated on recomposition via remember in HomeHeader.
 */
class ClockFormatter(
    locale: Locale = Locale.getDefault(),
) {
    private val timeFormat = SimpleDateFormat("HH:mm", locale)
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", locale)

    fun timeLine(nowMillis: Long): String = timeFormat.format(Date(nowMillis))

    fun dateLine(nowMillis: Long): String = dateFormat.format(Date(nowMillis))
}
