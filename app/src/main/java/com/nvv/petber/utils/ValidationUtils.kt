package com.nvv.petber.utils

import android.content.Context
import android.util.Patterns
import androidx.core.content.ContextCompat.getString
import com.nvv.petber.R

object ValidationUtils {
    fun validateEmail(context: Context, email: String): String? {
        return when {
            email.isBlank() -> getString(context, R.string.blank_email)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                getString(context, R.string.invalid_email)
            else -> null
        }
    }

    fun validatePassword(context: Context, password: String): String? {
        return when {
            password.isBlank() -> getString(context, R.string.pw_empty)
            password.length < 6 -> getString(context, R.string.invalid_length_password)
            else -> null
        }
    }

    fun validateConfirmPassword(context: Context, password: String, confirmPw: String): String? {
        return when {
            confirmPw.isBlank() -> getString(context, R.string.confirm_pw_empty)
            password != confirmPw -> getString(context, R.string.invalid_confirm_password)
            else -> null
        }
    }

    fun validateDisplayName(context: Context, displayName: String): String? {
        return when {
            displayName.isBlank() -> getString(context, R.string.display_name_empty)
            else -> null
        }
    }
}