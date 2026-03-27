package com.nvv.petber.ui.forgot_pw.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nvv.petber.databinding.FragmentForgotPasswordEmailBinding
import com.nvv.petber.utils.ValidationUtils
import com.nvv.petber.utils.ext.hideKeyboard
import com.nvv.petber.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordEmailFragment : Fragment() {
    private var _binding: FragmentForgotPasswordEmailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgotPasswordEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSendOtp.setOnClickListener {
            it.hideKeyboard()
            val email = binding.edtEmail.text.toString().trim()

            val emailError = ValidationUtils.validateEmail(requireContext(), email)
            if (emailError != null) {
                binding.edtEmail.error = emailError
                binding.edtEmail.requestFocus()
                return@setOnClickListener
            }

            viewModel.sendOtp(email)
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}