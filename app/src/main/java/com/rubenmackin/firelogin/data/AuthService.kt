package com.rubenmackin.firelogin.data

import android.app.Activity
import android.content.Context
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rubenmackin.firelogin.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    suspend fun login(username: String, password: String): FirebaseUser? {
        return firebaseAuth.signInWithEmailAndPassword(username, password).await().user
    }

    suspend fun anonymousLogin(): FirebaseUser? {
        return firebaseAuth.signInAnonymously().await().user
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun register(email: String, password: String): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val user = it.user
                    cancellableContinuation.resume(user)
                }
                .addOnFailureListener {
                    cancellableContinuation.resumeWithException(it)
                }
        }
    }

    fun isUserLogged(): Boolean {
        return getCurrentUser() != null
    }

    private fun getCurrentUser() = firebaseAuth.currentUser

    fun logout() {
        firebaseAuth.signOut()
        LoginManager.getInstance().logOut()
    }

    fun loginWithPhone(
        phoneNumber: String,
        activity: Activity,
        callback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        //firebaseAuth.firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber("+52 1234567890", "123456")

        val options = PhoneAuthOptions
            .newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callback)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyCode(verificationCode: String, phoneCode: String): FirebaseUser? {
        val credentials = PhoneAuthProvider.getCredential(verificationCode, phoneCode)
        return completeRegisterWithCredencials(credentials)
    }

    private suspend fun completeRegisterWithCredencials(
        credential: AuthCredential
    ): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.signInWithCredential(credential).addOnSuccessListener {
                cancellableContinuation.resume(it.user)
            }
                .addOnFailureListener {
                    cancellableContinuation.resumeWithException(it)
                }
        }
    }

    fun getGoogleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun loginWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return completeRegisterWithCredencials(credential)
    }

    suspend fun completeRegisterWithPhoneVerification(credentials: PhoneAuthCredential) =
        completeRegisterWithCredencials(credentials)

    suspend fun loginWithFacebook(accessToken: AccessToken): FirebaseUser? {
        val credential = FacebookAuthProvider.getCredential(accessToken.token)
        return completeRegisterWithCredencials(credential)
    }

    suspend fun loginWithGithub(activity: Activity): FirebaseUser? {

        val provider = OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email")
        }.build()

        return initRegisterWithProvider(activity, provider)

    }

    private suspend fun initRegisterWithProvider(
        activity: Activity,
        provider: OAuthProvider
    ): FirebaseUser? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            firebaseAuth.pendingAuthResult?.addOnSuccessListener {
                cancellableContinuation.resume(it.user)
            }?.addOnFailureListener {
                cancellableContinuation.resumeWithException(it)
            } ?: completeRegisterWithProvider(activity, provider, cancellableContinuation)
        }
    }

    suspend fun loginWithMicrosoft(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("microsoft.com").apply {
            scopes = listOf("mail.read", "calendars.read")
        }.build()

        return initRegisterWithProvider(activity, provider)
    }

    private fun completeRegisterWithProvider(
        activity: Activity,
        provider: OAuthProvider,
        cancellableContinuation: CancellableContinuation<FirebaseUser?>
    ) {
        firebaseAuth.startActivityForSignInWithProvider(activity, provider).addOnSuccessListener {
            cancellableContinuation.resume((it.user))
        }.addOnFailureListener {
            cancellableContinuation.resumeWithException(it)
        }
    }

    suspend fun loginWithTwitter(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("twitter.com").build()

        return initRegisterWithProvider(activity, provider)
    }

    suspend fun loginWithYahoo(activity: Activity): FirebaseUser? {
        val provider = OAuthProvider.newBuilder("yahoo.com").build()
        return initRegisterWithProvider(activity, provider)
    }

}