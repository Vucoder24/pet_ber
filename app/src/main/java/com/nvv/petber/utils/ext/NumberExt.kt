package com.nvv.petber.utils.ext

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

fun Long.formatSocialCount(): String {
    if (this < 1000) return this.toString()

    val suffixes = arrayOf("", "K", "M", "B", "T")
    val exp = (log10(this.toDouble()) / 3).toInt()
    val value = this / 1000.0.pow(exp.toDouble())

    return if (value >= 10 || value % 1 == 0.0) {
        String.format(Locale.US, "%d%s", value.toInt(), suffixes[exp])
    } else {
        String.format(Locale.US, "%.1f%s", value, suffixes[exp])
    }
}

fun Int.formatSocialCount(): String = this.toLong().formatSocialCount()