package com.example.caster

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

object AuthManager {

	private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

	fun signInAnonymouslyIfNeeded(onReady: () -> Unit, onError: (Exception) -> Unit) {
		val current = auth.currentUser
		if (current != null) {
			onReady()
			return
		}
		auth.signInAnonymously()
			.addOnSuccessListener { onReady() }
			.addOnFailureListener { e ->
				Log.e("AuthManager", "Anonymous sign-in failed: ${e.message}")
				onError(e)
			}
	}
}


