package com.rubenmackin.firelogin.ui.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rubenmackin.firelogin.R
import com.rubenmackin.firelogin.databinding.ActivitySignUpBinding
import com.rubenmackin.firelogin.ui.detail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    private val signUpViewModel: SignUpViewModel by viewModels()
    private lateinit var binding: ActivitySignUpBinding
    var context: Context = application.applicationContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()
    }

    private fun initUI() {
        initListeners()
        initUIState()
    }

    private fun initUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                signUpViewModel.isLoading.collect { loginState ->
                    binding.loadingSignUp.isVisible = loginState
                }
            }
        }
    }

    private fun initListeners() {
        binding.btnSignIn.setOnClickListener {
            signUpViewModel.register(
                email = binding.tieUser.text.toString(),
                password = binding.tiePassword.text.toString(),
            ) {
                navigateToDetail()
            }
        }
    }

    private fun navigateToDetail() {
        startActivity(Intent(this, DetailActivity::class.java))
    }
}