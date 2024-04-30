package com.rubenmackin.firelogin.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.rubenmackin.firelogin.data.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val authService: AuthService) : ViewModel() {

    private var _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private lateinit var verificationCode: String


    fun login(user: String, password: String, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                authService.login(user, password)
            }

            if (result != null) {
                navigateToDetail()
            } else {
                //error
            }

            _isLoading.value = false
        }
    }

    fun onAnonymousLoginSelected(navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                authService.anonymousLogin()
            }

            if (result!= null) {
                navigateToDetail()
            } else {
                //error
            }

            _isLoading.value = false
        }
    }

    fun loginWithPhone(
        phoneNumber: String,
        activity: Activity,
        onVerificationComplete: () -> Unit,
        onCodeSend: () -> Unit,
        onVerificationFailed: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val callback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credentials: PhoneAuthCredential) {
                    viewModelScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            authService.completeRegisterWithPhoneVerification(credentials)
                        }

                        if (result != null) {
                            onVerificationComplete()
                        }
                    }
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    _isLoading.value = false
                    onVerificationFailed(p0.message.orEmpty())
                }

                override fun onCodeSent(
                    verificationCode: String,
                    p1: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@LoginViewModel.verificationCode = verificationCode
                    _isLoading.value = false
                    onCodeSend()
                }

            }

            withContext(Dispatchers.IO) {
                authService.loginWithPhone(phoneNumber, activity, callback)
            }


            _isLoading.value = false
        }
    }

    fun verifyCode(phoneCode: String, onSuccessVerification: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                authService.verifyCode(verificationCode, phoneCode)
            }

            if (result != null) {
                onSuccessVerification()
            }

            _isLoading.value = false
        }
    }

    fun onGoogleLoginSelected(googleLauncherLogin: (GoogleSignInClient) -> Unit) {
        val gsc = authService.getGoogleClient()
        googleLauncherLogin(gsc)
    }

    fun loginWithGoogle(idToken: String, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithGoogle(idToken)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun loginWithFacebook(accessToken: AccessToken, navigateToDetail: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authService.loginWithFacebook(accessToken)
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }

    fun onOathLoginSelected(
        oath: OathLogin,
        activity: Activity,
        navigateToDetail: () -> Unit
    ) {

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {

                when (oath) {
                    OathLogin.Github -> authService.loginWithGithub(activity)
                    OathLogin.Microsoft -> authService.loginWithMicrosoft(activity)
                    OathLogin.Twitter -> authService.loginWithTwitter(activity)
                    OathLogin.Yahoo -> authService.loginWithYahoo(activity)
                }
            }

            if (result != null) {
                navigateToDetail()
            }
        }
    }
}

sealed class OathLogin() {
    object Github : OathLogin()
    object Twitter : OathLogin()
    object Microsoft : OathLogin()
    object Yahoo : OathLogin()
}