package com.solaria.auth.validation.utils

object CPFUtils {

    private val regex = Regex("^\\d{11}$")

    fun sanitize(cpf: String): String {
        return cpf.filter { it.isDigit() }
    }

    fun isCPFValid(cpf: String): Boolean {
        val numbers = sanitize(cpf)
        
        if (!regex.matches(numbers)) return false

        if (numbers.length != 11) return false
        if (numbers.toSet().size == 1) return false

        val dv1 = calculateDigit(numbers, 10, 9)
        val dv2 = calculateDigit(numbers, 11, 10)

        return dv1 == numbers[9].toString().toInt() && dv2 == numbers[10].toString().toInt()
    }

    fun calculateDigit(cpf: String, initialWeight: Int, limit: Int): Int {
        var weight = initialWeight
        var sum = 0

        for (i in 0 until limit) {
            sum += cpf[i].toString().toInt() * weight
            weight--
        }

        val rest = sum % 11
        return if (rest < 2) 0 else 11 - rest
    }
}
