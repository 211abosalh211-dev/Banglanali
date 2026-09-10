package com.example.core.currency

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money value class representing monetary amounts with zero floating-point errors.
 * Internally stores amounts as integer minor units (scale of 2 decimals).
 * Example: 15,000.50 YER is stored internally as 1_500_050L.
 *
 * Default Currency: YER (الريال اليمني)
 */
@ConsistentCopyVisibility
data class Money private constructor(
    val minorUnits: Long,
    val currencyCode: String = DEFAULT_CURRENCY
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies: $currencyCode and ${other.currencyCode}" }
        return Money(minorUnits + other.minorUnits, currencyCode)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies: $currencyCode and ${other.currencyCode}" }
        return Money(minorUnits - other.minorUnits, currencyCode)
    }

    operator fun times(multiplier: Long): Money {
        return Money(minorUnits * multiplier, currencyCode)
    }

    operator fun unaryMinus(): Money {
        return Money(-minorUnits, currencyCode)
    }

    override fun compareTo(other: Money): Int {
        require(currencyCode == other.currencyCode) { "Cannot compare different currencies: $currencyCode and ${other.currencyCode}" }
        return minorUnits.compareTo(other.minorUnits)
    }

    val isZero: Boolean get() = minorUnits == 0L
    val isPositive: Boolean get() = minorUnits > 0L
    val isNegative: Boolean get() = minorUnits < 0L

    /**
     * Formats the amount into an Arabic-friendly or English financial string.
     * e.g., "15,000.50 YER" or "15,000.50 ر.ي"
     */
    fun formatWithCurrency(arabicSymbol: Boolean = true): String {
        val major = minorUnits / 100
        val minor = kotlin.math.abs(minorUnits % 100)
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedMajor = formatter.format(major)
        val decimalPart = String.format(Locale.US, "%02d", minor)
        val symbol = if (arabicSymbol) "ر.ي" else currencyCode
        return "$formattedMajor.$decimalPart $symbol"
    }

    fun toDisplayString(): String {
        val major = minorUnits / 100
        val minor = kotlin.math.abs(minorUnits % 100)
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedMajor = formatter.format(major)
        val decimalPart = String.format(Locale.US, "%02d", minor)
        return "$formattedMajor.$decimalPart"
    }

    val formatted: String get() = formatWithCurrency(true)

    companion object {
        const val DEFAULT_CURRENCY = "YER"

        val ZERO: Money = Money(0L, DEFAULT_CURRENCY)

        /**
         * Creates Money from major currency units (e.g. 1500 -> 1500.00).
         */
        fun fromMajor(amount: Long, currencyCode: String = DEFAULT_CURRENCY): Money {
            return Money(amount * 100L, currencyCode)
        }

        /**
         * Creates Money from minor units directly (e.g. 150000 -> 1500.00).
         */
        fun fromMinor(minorUnits: Long, currencyCode: String = DEFAULT_CURRENCY): Money {
            return Money(minorUnits, currencyCode)
        }

        /**
         * Parses string with decimals safely without floating-point math.
         */
        fun fromString(value: String, currencyCode: String = DEFAULT_CURRENCY): Money {
            val clean = value.trim().replace(",", "")
            if (clean.isEmpty()) return ZERO
            val parts = clean.split(".")
            val major = parts[0].toLongOrNull() ?: 0L
            val minor = if (parts.size > 1) {
                val dec = parts[1].padEnd(2, '0').take(2)
                dec.toLongOrNull() ?: 0L
            } else 0L
            val sign = if (clean.startsWith("-")) -1L else 1L
            val totalMinor = (kotlin.math.abs(major) * 100L + minor) * sign
            return Money(totalMinor, currencyCode)
        }
    }
}
