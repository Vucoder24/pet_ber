package com.nvv.petber.data.repo.remote

import android.content.Context
import com.nvv.petber.utils.SharePrefUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {
    suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
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
                supabaseClient.auth.signUpWith(Email) {
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
}