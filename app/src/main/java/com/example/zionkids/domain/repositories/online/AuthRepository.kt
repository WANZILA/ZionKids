package com.example.zionkids.domain.repositories.online

import com.example.zionkids.data.model.AuthUser

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(email: String, password: String): Result<AuthUser>
    fun currentUser(): AuthUser?
    suspend fun signOut()
}
//interface AuthRepository {
//    val currentUserFlow: kotlinx.coroutines.flow.Flow<com.google.firebase.auth.FirebaseUser?>
//    suspend fun signIn(email: String, password: String): Result<FirebaseUser?>
//    suspend fun signOut()
//}

//class AuthRepositoryImpl @Inject constructor(
//    private val auth: FirebaseAuth
//) : AuthRepository {

//    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
//        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
//        auth.addAuthStateListener(listener)
//        trySend(auth.currentUser)
//        awaitClose { auth.removeAuthStateListener(listener) }
//    }

//    override suspend fun signIn(email: String, password: String): Result<FirebaseUser?> =
//        runCatching {
//            auth.signInWithEmailAndPassword(email, password).await()
//            auth.currentUser
//        }
//
//    override suspend fun signOut() {
//        auth.signOut()
//    }
//}
//
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        val user = auth.currentUser ?: error("No user")
        AuthUser(uid = user.uid, email = user.email)
    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await()
        val user = auth.currentUser ?: error("No user")
        AuthUser(uid = user.uid, email = user.email)
    }

    override fun currentUser(): AuthUser? =
        auth.currentUser?.let { AuthUser(uid = it.uid, email = it.email) }

    override suspend fun signOut() {
        auth.signOut()
    }
}
