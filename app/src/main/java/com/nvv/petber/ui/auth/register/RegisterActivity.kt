package com.nvv.petber.ui.auth.register

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
import com.nvv.petber.databinding.ActivityRegisterBinding
import com.nvv.petber.ui.auth.login.LoginActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.ValidationUtils
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.AuthState
import com.nvv.petber.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {
    lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
                        is AuthState.Loading -> {
                            showLoading(true)
                        }

                        is AuthState.Success -> {
                            showLoading(false)
                            toast(getString(R.string.register_success))
                            finish() // back to LoginActivity
                        }

                        is AuthState.Error -> {
                            showLoading(false)
                            toast(state.error)
                            authViewModel.resetState()
                        }

                        is AuthState.Idle -> {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun onEvent() {
        binding.apply {
            btnRegister.setOnClickListener {
                val displayName = edtUserName.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val password = edtPw.text.toString().trim()
                val confirmPassword = edtConfirmPw.text.toString().trim()

                val edtUserNameError =
                    ValidationUtils.validateUserName(this@RegisterActivity, displayName)
                if (edtUserNameError != null) {
                    edtUserName.error = edtUserNameError
                    edtUserName.requestFocus()
                    return@setOnClickListener
                }

                val emailError = ValidationUtils.validateEmail(this@RegisterActivity, email)
                if (emailError != null) {
                    edtEmail.error = emailError
                    edtEmail.requestFocus()
                    return@setOnClickListener
                }

                val passwordError =
                    ValidationUtils.validatePassword(this@RegisterActivity, password)
                if (passwordError != null) {
                    edtPw.error = passwordError
                    edtPw.requestFocus()
                    return@setOnClickListener
                }

                val confirmPwError = ValidationUtils.validateConfirmPassword(
                    this@RegisterActivity,
                    password,
                    confirmPassword
                )
                if (confirmPwError != null) {
                    edtConfirmPw.error = confirmPwError
                    edtConfirmPw.requestFocus()
                    return@setOnClickListener
                }

                authViewModel.register(email, password, displayName)
            }

            btnBack.setOnClickListener {
                finish()
            }

            tvLogin.setOnClickListener {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}