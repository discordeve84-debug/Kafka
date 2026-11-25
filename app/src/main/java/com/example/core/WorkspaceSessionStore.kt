package com.example.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory holder for the short-lived Google OAuth2 Workspace access token.
 * Never persisted to SharedPreferences or Firestore, and always cleared on sign-out.
 */
object WorkspaceSessionStore {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setAccessToken(token: String?) {
        _accessToken.value = token?.takeIf { it.isNotBlank() }
    }

    fun clear() {
        _accessToken.value = null
    }
}
