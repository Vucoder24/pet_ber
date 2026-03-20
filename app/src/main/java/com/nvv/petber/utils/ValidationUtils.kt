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

    fun validateUserName(context: Context, username: String): String? {
        if (username.isBlank()) {
            return context.getString(R.string.username_empty)
        }

        val usernameRegex = "^[a-z0-9_.]+$".toRegex()

        return when {
            username.contains(" ") -> {
                context.getString(R.string.username_cannot_contain_spaces)
            }
            !username.contains(usernameRegex) -> {
                context.getString(R.string.username_invalid_characters)
            }
            username.length < 2 -> {
                context.getString(R.string.username_invalid_characters_length)
            }
            else -> null
        }
    }
}