package com.solaria.auth.validation.utils

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CNPJValidator : ConstraintValidator<CNPJ, String> {

    override fun isValid(cnpj: String?, context: ConstraintValidatorContext): Boolean {
        if (cnpj.isNullOrBlank()) return false
        return CNPJUtils.isCNPJValid(cnpj)
    }
}