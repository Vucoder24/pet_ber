package com.nvv.petber.ui.forgot_pw

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityForgotPwBinding
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.forgot_pw.fragment.ForgotPasswordEmailFragment
import com.nvv.petber.ui.forgot_pw.fragment.ForgotPasswordNewPasswordFragment
import com.nvv.petber.ui.forgot_pw.fragment.ForgotPasswordOtpFragment
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.ForgotPasswordState
import com.nvv.petber.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPwActivity : BaseActivity() {
    private lateinit var binding: ActivityForgotPwBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPwBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            showEmailFragment()
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is ForgotPasswordState.Idle -> showLoading(false)

                        is ForgotPasswordState.Loading -> showLoading(true)

                        is ForgotPasswordState.OtpSent -> {
                            showLoading(false)
                            showOtpFragment(state.email)
                        }

                        is ForgotPasswordState.OtpVerified -> {
                            showLoading(false)
                            showNewPasswordFragment(state.email)
                        }

                        is ForgotPasswordState.PasswordUpdated -> {
                            showLoading(false)
                            toast(R.string.password_updated)
                            finish()
                        }

                        is ForgotPasswordState.Error -> {
                            showLoading(false)
                            toast(state.message)
                            viewModel.reset()
                        }
                    }
                }
            }
        }
    }

    private fun showEmailFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, ForgotPasswordEmailFragment())
        }
    }

    fun showOtpFragment(email: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, ForgotPasswordOtpFragment.newInstance(email))
            addToBackStack("otp")
        }
    }

    fun showNewPasswordFragment(email: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, ForgotPasswordNewPasswordFragment.newInstance(email))
            addToBackStack("new_password")
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarForgotPw.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}