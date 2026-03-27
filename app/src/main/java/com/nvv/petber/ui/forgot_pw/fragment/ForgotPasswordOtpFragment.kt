package com.nvv.petber.ui.forgot_pw.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentForgotPasswordOtpBinding
import com.nvv.petber.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordOtpFragment : Fragment() {
    private var _binding: FragmentForgotPasswordOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null

    private val email: String by lazy {
        requireArguments().getString(ARG_EMAIL).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.emailDisplayText.text = email

        startResendTimer()

        binding.btnContinue.setOnClickListener {
            val otp = binding.pinView.text.toString()
            if (otp.isEmpty()) {
                binding.pinView.error = getString(R.string.please_enter_otp)
                return@setOnClickListener
            } else if (otp.length < 6) {
                binding.pinView.error = getString(R.string.please_enter_full_otp)
                return@setOnClickListener
            } else {
                viewModel.verifyOtp(email, otp)
            }

        }

        binding.btnResendOtp.setOnClickListener {
            viewModel.sendOtp(email)
            startResendTimer()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //
            }
        })
    }

    private fun startResendTimer() {
        binding.btnResendOtp.isEnabled = false
        countDownTimer?.cancel()

        val totalTime = 300_000L
        val resendLockTime = 60

        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSecondsLeft = millisUntilFinished / 1000
                val secondsPassed = (totalTime / 1000) - totalSecondsLeft

                binding.timeOtp.text = totalSecondsLeft.toString()

                if (secondsPassed < resendLockTime) {
                    val resendWait = resendLockTime - secondsPassed
                    binding.btnResendOtp.isEnabled = false
                    binding.btnResendOtp.text = getString(R.string.resend_otp_second, resendWait)
                } else {
                    if (!binding.btnResendOtp.isEnabled) {
                        binding.btnResendOtp.isEnabled = true
                        binding.btnResendOtp.text = getString(R.string.action_resend_otp)
                    }
                }
            }

            override fun onFinish() {
                binding.timeOtp.text = "0"
                binding.btnResendOtp.isEnabled = true
                binding.btnResendOtp.text = getString(R.string.action_resend_otp)
            }
        }.start()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun newInstance(email: String): ForgotPasswordOtpFragment {
            return ForgotPasswordOtpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                }
            }
        }
    }
}