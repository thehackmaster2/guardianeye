package com.example.managers

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object SupabaseManager {
    private const val TAG = "SupabaseManager"
    private const val SUPABASE_URL = "https://regccnnrfvfzuozkmeod.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJlZ2Njbm5yZnZmenVvemttZW9kIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODEzNTQzNzEsImV4cCI6MjA5NjkzMDM3MX0.hzV3GaAuNECCEqJiWUC0yp7H8f5c7UNEI1kV62QyFeo"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}", e)
            false
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            false
        }
    }

    suspend fun getCurrentUserEmail(): String? = withContext(Dispatchers.IO) {
        try {
            client.auth.retrieveUserForCurrentSession().email
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve user email: ${e.message}")
            null
        }
    }

    suspend fun getCurrentUserId(): String? = withContext(Dispatchers.IO) {
        try {
            client.auth.retrieveUserForCurrentSession().id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve user ID: ${e.message}")
            null
        }
    }

    suspend fun saveUserRole(userId: String, email: String, role: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = Profile(id = userId, email = email, role = role)
            client.postgrest["profiles"].upsert(profile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Save profile failed: ${e.message}", e)
            false
        }
    }

    suspend fun createPairingRoom(userId: String, roomCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val room = Room(code = roomCode, parent_id = userId, child_id = null, status = "waiting")
            client.postgrest["rooms"].upsert(room)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Create pairing room failed: ${e.message}", e)
            false
        }
    }

    suspend fun joinPairingRoom(roomCode: String, childId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.postgrest["rooms"].update(mapOf("child_id" to childId, "status" to "joined")) {
                filter {
                    eq("code", roomCode)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Join pairing room failed: ${e.message}", e)
            false
        }
    }

    suspend fun checkRoomStatus(roomCode: String): String? = withContext(Dispatchers.IO) {
        try {
            val results = client.postgrest["rooms"].select(columns = Columns.list("status")) {
                filter {
                    eq("code", roomCode)
                }
            }.decodeList<RoomStatusResponse>()
            results.firstOrNull()?.status
        } catch (e: Exception) {
            Log.e(TAG, "Check room status failed: ${e.message}", e)
            null
        }
    }
}

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val role: String
)

@Serializable
data class Room(
    val code: String,
    val parent_id: String,
    val child_id: String?,
    val status: String
)

@Serializable
data class RoomStatusResponse(
    val status: String
)
