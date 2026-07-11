package com.wfx.warungpos.core.util

/**
 * Filters free-text numeric input down to digits and at most one decimal point.
 *
 * DEFECT-011: the naive `value.filter { it.isDigit() || it == '.' }` pattern (previously
 * duplicated across the reorder-threshold, batch-quantity, and opname counted-quantity fields)
 * lets a second `.` through untouched — e.g. typing "2.5.1" produces the literal string
 * `"2.5.1"`, which `toDoubleOrNull()` can't parse. Each caller then silently fell back to some
 * default (`0.0`, or "leave unchanged") on save instead of rejecting the bad input. Keeping only
 * the *first* decimal point and dropping any subsequent ones means the field always holds a
 * value `toDoubleOrNull()` can actually parse.
 */
fun filterDecimalInput(value: String): String {
    var seenDot = false
    return value.filter { c ->
        when {
            c.isDigit() -> true
            c == '.' && !seenDot -> {
                seenDot = true
                true
            }
            else -> false
        }
    }
}
