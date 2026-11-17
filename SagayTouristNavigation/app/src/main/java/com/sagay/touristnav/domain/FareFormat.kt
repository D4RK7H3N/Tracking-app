package com.sagay.touristnav.domain

import java.util.Locale
import kotlin.math.floor

/**
 * Prints a fare the way it is published: a whole peso amount stays whole,
 * and an amount with centavos keeps them, because the matrix publishes
 * 13.60 pesos rather than 14.
 */
object FareFormat {

    fun peso(amount: Double): String =
        if (amount == floor(amount)) {
            String.format(Locale.US, "\u20B1%.0f", amount)
        } else {
            String.format(Locale.US, "\u20B1%.2f", amount)
        }
}
