package com.streaming.sunne.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Secure token storage using EncryptedSharedPreferences (or plain for simplicity).
 * Call TokenManager.init(context) once from Application.onCreate().
 */
object TokenManager {

    private const val PREFS_NAME   = "sune_auth_prefs"
    private const val KEY_ACCESS   = "access_token"
    private const val KEY_REFRESH  = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_IS_ADMIN = "is_admin"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Tokens ────────────────────────────────────────────────────────────────

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit { putString(KEY_ACCESS, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit { putString(KEY_REFRESH, value) }

    // ── User info ─────────────────────────────────────────────────────────────

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_ADMIN, value) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    val isLoggedIn: Boolean get() = accessToken != null

    fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken  = accessToken
        this.refreshToken = refreshToken
    }

    fun clear() {
        prefs.edit { clear() }
    }
}