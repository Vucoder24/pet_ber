package com.nvv.petber.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityLoginBinding
import com.nvv.petber.ui.activity.MainActivity
import com.nvv.petber.ui.auth.register.RegisterActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.forgot_pw.ForgotPwActivity
import com.nvv.petber.utils.ValidationUtils
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.AuthState
import com.nvv.petber.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity() {
    lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        onEvent()
        observerState()
    }

    private fun observerState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Idle -> {
                            showLoading(false)
                        }

                        is AuthState.Loading -> {
                            // show loading
                            showLoading(true)
                        }

                        is AuthState.Success -> {
                            // navigate to home screen
                            showLoading(false)
                            toast(getString(R.string.login_success))
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finishAffinity()
                        }

                        is AuthState.Error -> {
                            // show error message
                            showLoading(false)
                            toast(state.error)
                            authViewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun onEvent() {
        binding.apply {
            btnLogin.setOnClickListener {
                val email = edtEmail.text.toString()
                val password = edtPw.text.toString()
                val emailError = ValidationUtils.validateEmail(this@LoginActivity, email)
                if (emailError != null) {
                    edtEmail.error = emailError
                    edtEmail.requestFocus()
                    return@setOnClickListener
                }

                val passwordError = ValidationUtils.validatePassword(this@LoginActivity, password)
                if (passwordError != null) {
                    edtPw.error = passwordError
                    edtPw.requestFocus()
                    return@setOnClickListener
                }

                authViewModel.login(email, password)
            }

            btnForgotPw.setOnClickListener {
                startActivity(Intent(this@LoginActivity, ForgotPwActivity::class.java))
            }

            btnNavigateRegister.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

}