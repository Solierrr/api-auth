package com.solaria.auth.validation.utils

import com.solaria.auth.validation.annotation.CNPJ
import com.solaria.auth.validation.utils.CNPJUtils
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CNPJValidator : ConstraintValidator<CNPJ, String> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return false
        return CNPJUtils.isValid(value)
    }
}