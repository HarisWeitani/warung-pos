package com.wfx.warungpos.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("id", "ID"))

    fun format(amount: Long): String {
        val formatted = DecimalFormat("#,##0", symbols).format(amount)
        return "Rp $formatted"
    }
}
