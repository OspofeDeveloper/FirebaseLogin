package com.example.firebaselogin.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.firebaselogin.databinding.ActivityLoginBinding
import com.example.firebaselogin.databinding.DialogPhoneLoginBinding
import com.example.firebaselogin.ui.detail.DetailActivity
import com.example.firebaselogin.ui.signup.SignUpActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        initListeners()
        initUIState()
    }

    private fun initUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.isLoading.collect {
                    binding.pbLoading.isVisible = it
                }
            }
        }
    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            loginViewModel.login(
                user = binding.tieUser.text.toString(),
                password = binding.tiePassword.text.toString()
            ) { navigateToDetail() }
        }

        binding.tvRegister.setOnClickListener {
            navigateToSignUp()
        }

        binding.btnLoginPhone.setOnClickListener {
            showPhoneLogin()
        }
    }

    private fun showPhoneLogin() {
        val phoneBinding = DialogPhoneLoginBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this).apply { setView(phoneBinding.root) }.create()

        phoneBinding.btnPhone.setOnClickListener {
            loginViewModel.loginWithPhone(phoneBinding.tiePhone.text.toString(), this,
                onCodeSent = {
                    phoneBinding.pinView.isVisible = true
                },
                onVerificationComplete = { navigateToDetail() },
                onVerificationFailed = { showToast("An error occurred: $it") }
            )
        }

        /**
         * Una vez introducimos el codigo de verificación completo del SMS llamamos a la función
         * verifyCode
         */
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

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun navigateToDetail() {
        startActivity(Intent(this, DetailActivity::class.java))
    }
}