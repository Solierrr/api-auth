package com.solaria.auth.validation.utils

import com.example.validation.annotation.CPF
import com.example.validation.utils.CPFUtils
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CPFValidator : ConstraintValidator<CPF, String> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return false
        return CPFUtils.isValid(value)
    }
}