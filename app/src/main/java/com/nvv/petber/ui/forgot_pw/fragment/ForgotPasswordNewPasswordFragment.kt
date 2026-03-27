package com.nvv.petber.ui.forgot_pw.fragment

import com.nvv.petber.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nvv.petber.databinding.FragmentForgotPasswordNewPasswordBinding
import com.nvv.petber.utils.ValidationUtils
import com.nvv.petber.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordNewPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordNewPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordNewPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnSend.setOnClickListener {
            val newPassword = binding.edtPw.text.toString()
            val confirmPassword = binding.edtConfirmPw.text.toString()

            val passError = ValidationUtils.validatePassword(requireContext(), newPassword)
            if (passError != null) {
                binding.edtPw.error = passError
                binding.edtPw.requestFocus()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.edtConfirmPw.error = requireContext().getString(R.string.invalid_confirm_password)
                binding.edtConfirmPw.requestFocus()
                return@setOnClickListener
            }

            viewModel.updatePassword(newPassword)
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun newInstance(email: String): ForgotPasswordNewPasswordFragment {
            return ForgotPasswordNewPasswordFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                }
            }
        }
    }
}