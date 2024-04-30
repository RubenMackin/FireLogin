package com.rubenmackin.firelogin.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubenmackin.firelogin.data.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val authService: AuthService): ViewModel() {
    fun logout(navigateToLoging: () -> Unit) {
        viewModelScope.launch (Dispatchers.IO){
            authService.logout()
        }
        navigateToLoging()
    }
}