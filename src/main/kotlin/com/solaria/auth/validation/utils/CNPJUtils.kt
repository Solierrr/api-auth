package com.solaria.auth.validation.utils

object CNPJUtils {

    private val regex = Regex("^\\d{14}$")

    fun sanitize(cnpj: String): String {
        return cnpj.filter { it.isDigit() }
    }

    fun isCNPJValid(cnpj: String): Boolean {
        val numbers = sanitize(cnpj)

        if (!regex.matches(numbers)) return false

        if (numbers.length != 14) return false
        if (numbers.toSet().size == 1) return false

        val dv1 = calculateDigit(numbers.substring(0, 12), FIRST_WEIGHTS)
        val dv2 = calculateDigit(numbers.substring(0, 12) + dv1, SECOND_WEIGHTS)

        return dv1 == numbers[12].digitToInt() &&
                dv2 == numbers[13].digitToInt()
    }

    private fun calculateDigit(cnpj: String, weights: IntArray): Int {
        val sum = cnpj
            .mapIndexed { index, c -> c.digitToInt() * weights[index] }
            .sum()

        val remainder = sum % 11

        return if (remainder < 2) 0 else 11 - remainder
    }

    private val FIRST_WEIGHTS = intArrayOf(
        5, 4, 3, 2,
        9, 8, 7, 6, 5, 4, 3, 2
    )

    private val SECOND_WEIGHTS = intArrayOf(
        6, 5, 4, 3, 2,
        9, 8, 7, 6, 5, 4, 3, 2
    )
}