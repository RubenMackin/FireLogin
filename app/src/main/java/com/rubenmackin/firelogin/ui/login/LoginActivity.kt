package com.rubenmackin.firelogin.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.rubenmackin.firelogin.R
import com.rubenmackin.firelogin.databinding.ActivityLoginBinding
import com.rubenmackin.firelogin.databinding.DialogPhoneLoginBinding
import com.rubenmackin.firelogin.ui.detail.DetailActivity
import com.rubenmackin.firelogin.ui.login.OathLogin.*
import com.rubenmackin.firelogin.ui.signup.SignUpActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    private lateinit var callbacksManager: CallbackManager

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    loginViewModel.loginWithGoogle(account.idToken!!, { navigateToDetail() })
                } catch (e: ApiException) {
                    Toast.makeText(this, "Ha ocurrido un error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
                loginViewModel.isLoading.collect { loginState ->
                    binding.loading.isVisible = loginState
                }
            }
        }
    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            loginViewModel.login(
                user = binding.tieUser.text.toString(),
                password = binding.tiePassword.text.toString(),
            ) {
                navigateToDetail()
            }
        }

        binding.tvSignUp.setOnClickListener {
            navigateToSignUp()
        }

        binding.btnLoginPhone.setOnClickListener {
            showPhoneLoginDialog()
        }

        binding.btnLoginGoogle.setOnClickListener {
            loginViewModel.onGoogleLoginSelected {
                googleLauncher.launch(it.signInIntent)
            }
        }

        binding.btnLoginGithub.setOnClickListener {
            loginViewModel.onOathLoginSelected(Github, this) { navigateToDetail() }
        }

        binding.btnLoginMicrosoft.setOnClickListener {
            loginViewModel.onOathLoginSelected(Microsoft, this) { navigateToDetail() }
        }

        binding.btnLoginTwitter.setOnClickListener {
            loginViewModel.onOathLoginSelected(Twitter, this) { navigateToDetail() }
        }

        binding.btnLoginYahoo.setOnClickListener {
            loginViewModel.onOathLoginSelected(Yahoo, this) { navigateToDetail() }
        }

        binding.btnLoginAnonymous.setOnClickListener {
            loginViewModel.onAnonymousLoginSelected { navigateToDetail() }
        }

        //Facebook start
        callbacksManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(
            callbacksManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    showToast("Probamos con otra red social")
                }

                override fun onError(error: FacebookException) {
                    showToast("Ha ocurrido un error: $error")
                }

                override fun onSuccess(result: LoginResult) {
                    loginViewModel.loginWithFacebook(result.accessToken) { navigateToDetail() }
                }

            })

        binding.btnLoginFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                callbacksManager,
                listOf("email", "public_profile")
            )
        }

        /*callbacksManager = CallbackManager.Factory.create()
        binding.btnLoginFacebook.setPermissions("email", "public_profile")
        binding.btnLoginFacebook.registerCallback(
            callbacksManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    showToast("Probamos con otra red social")
                }

                override fun onError(error: FacebookException) {
                    showToast("Ha ocurrido un error: $error")
                }

                override fun onSuccess(result: LoginResult) {
                    loginViewModel.loginWithFacebook(result.accessToken) { navigateToDetail() }
                }

            })*/

        //Facebook end
    }

    private fun showPhoneLoginDialog() {
        val phoneBinding = DialogPhoneLoginBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this).apply { setView(phoneBinding.root) }.create()

        phoneBinding.btnPhone.setOnClickListener {
            loginViewModel.loginWithPhone(
                phoneBinding.tiePhone.text.toString(),
                this,
                onVerificationComplete = { onVerificationComplete() },
                onCodeSend = { onCodeSend(phoneBinding) },
                onVerificationFailed = { showToast("Ha habido un error: $it") }
            )
        }

        phoneBinding.pinView.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 6) {
                loginViewModel.verifyCode(text.toString()) { navigateToDetail() }
            }
        }

        alertDialog.show()

    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun onCodeSend(phoneBinding: DialogPhoneLoginBinding) {
        phoneBinding.pinView.isVisible = true
        phoneBinding.tiePhone.isEnabled = false
        phoneBinding.btnPhone.isEnabled = false
        phoneBinding.pinView.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(phoneBinding.tiePhone, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun onVerificationComplete() {
        navigateToDetail()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun navigateToDetail() {
        startActivity(Intent(this, DetailActivity::class.java))
    }
}