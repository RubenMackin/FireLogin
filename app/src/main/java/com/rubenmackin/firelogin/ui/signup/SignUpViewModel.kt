package com.rubenmackin.firelogin.ui.signup

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubenmackin.firelogin.data.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(private val authService: AuthService) : ViewModel() {

    private var _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun register(
        email: String,
        password: String,
        navigateToDetail: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = withContext(Dispatchers.IO) {
                    authService.register(email, password)
                }

                if (result != null) {
                    navigateToDetail()
                } else {
                    //error
                }
            } catch (e: Exception) {
            }

            _isLoading.value = false
        }
    }
}