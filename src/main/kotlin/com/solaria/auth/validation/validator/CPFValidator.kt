package com.solaria.auth.validation.utils

import com.solaria.auth.validation.annotation.CPF
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CPFValidator : ConstraintValidator<CPF, String> {

    override fun isValid(cpf: String?, context: ConstraintValidatorContext): Boolean {
        if (cpf.isNullOrBlank()) return false
        return CPFUtils.isCPFValid(cpf)
    }
}