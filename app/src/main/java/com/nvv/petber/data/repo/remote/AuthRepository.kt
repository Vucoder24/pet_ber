package com.nvv.petber.data.repo.remote

import android.content.Context
import com.nvv.petber.data.model.User
import com.nvv.petber.utils.SharePrefUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {
    suspend fun loginWithPassword(input: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val email = if (input.contains("@")) {
                    input
                } else {
                    val result = supabaseClient
                        .from("users")
                        .select {
                            filter {
                                eq("username", input)
                            }
                        }
                        .decodeSingle<User>()

                    result.email
                }
                if (email.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("User not found"))
                }

                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                SharePrefUtils.saveCurrentUserId(context, currentUserId ?: "")
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun register(email: String, password: String, userName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.signUpWith(Email, redirectUrl = "petber://verify-email-success") {
                    this.email = email
                    this.password = password
                    this.data = buildJsonObject {
                        put("user_name", userName)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.signOut()
                SharePrefUtils.saveCurrentUserId(context, "")
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendResetPasswordOtp(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.resetPasswordForEmail(
                    email = email
                )
            }
        }

    suspend fun verifyResetPasswordOtp(email: String, otp: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = otp
                )
            }
        }

    suspend fun updatePassword(newPassword: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabaseClient.auth.updateUser {
                    password = newPassword
                }
            }
        }
}