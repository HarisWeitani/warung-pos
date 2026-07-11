package com.wfx.warungpos.core.util

/**
 * Filters free-text "unit" input (e.g. the Stock screen's "kg"/"pcs"/"liter" field) down to
 * non-digit characters.
 *
 * Reported UX bug: the unit field previously accepted anything, including digits, with the only
 * server-side check being "not blank". A user typing e.g. "20 kg" into Unit (instead of the
 * quantity field it was easy to mistake it for) silently produced a stock item whose every
 * on-screen quantity string became "<qty> 20 kg" — read as two jammed-together numbers. Unit
 * names don't need digits, so stripping them at entry time closes the mistake off entirely
 * rather than relying on the user typing correctly.
 */
fun filterUnitInput(value: String): String = value.filterNot { it.isDigit() }
