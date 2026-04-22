package com.nvv.petber.ui.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.ActivityEditProfileBinding
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.DateTimeUtils
import com.nvv.petber.utils.ValidationUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.ProfileViewModel
import com.nvv.petber.viewmodel.UpdateUserState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class EditProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private var selectedBirthdayDb: String? = null
    private var selectedBirthdayDisplay: String? = null
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var userData: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getArg()
        setupDropdown()
        setupListeners()
        observerUiState()
    }

    private fun observerUiState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UpdateUserState.Loading -> {
                            binding.apply {
                                layoutLoading.visible()
                                btnSave.isEnabled = false
                            }
                        }

                        UpdateUserState.Success -> {
                            binding.apply {
                                layoutLoading.gone()
                                btnSave.isEnabled = true
                            }
                            toast(getString(R.string.update_user_success))
                            viewModel.resetState()
                            finish()
                        }

                        is UpdateUserState.Error -> {
                            binding.apply {
                                layoutLoading.gone()
                                btnSave.isEnabled = true
                            }
                            toast(state.message)
                            viewModel.resetState()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun getArg() {
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<User>(USER_DATA)
        if (data != null){
            userData = data
            setupUserdata()
        }else{
            toast(R.string.error_get_arg)
            finish()
        }
    }

    private fun setupUserdata() {
        binding.apply {
            edtBio.setText(userData.bio ?: "")
            edtFullName.setText(userData.fullName ?: "")
            edtUserName.setText(userData.username ?: "")
            edtAddress.setText(userData.address ?: "")
            edtPhoneNumber.setText(userData.phone ?: "")

            if (!userData.gender.isNullOrEmpty()) {
                actGender.setText(userData.gender, false)
            }

            edtHobbies.setText(userData.hobbies ?: "")

            userData.birthday?.let { dbDate ->
                selectedBirthdayDb = dbDate
                selectedBirthdayDisplay = DateTimeUtils.formatToDisplay(dbDate)
                edtBirthday.setText(selectedBirthdayDisplay)
            }
        }
    }

    private fun setupDropdown() {
        val genders = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        binding.actGender.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            edtBirthday.setOnClickListener {
                showDatePicker()
            }

            tilBirthday.setEndIconOnClickListener {
                showDatePicker()
            }

            btnSave.setOnClickListener {
                submitForm()
            }
        }
    }

    private fun showDatePicker() {
        val currentMs = selectedBirthdayDb?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                sdf.parse(it)?.time
            } catch (_: Exception) {
                null
            }
        } ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.birthday))
            .setSelection(currentMs)
            .setTheme(R.style.PetBerDatePickerTheme)
            .build()

        picker.addOnPositiveButtonClickListener { ms ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = ms
            updateBirthday(calendar)
        }

        picker.show(supportFragmentManager, "date_picker")
    }

    private fun updateBirthday(calendar: Calendar) {
        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        selectedBirthdayDisplay = displayFormat.format(calendar.time)

        selectedBirthdayDb = DateTimeUtils.formatToSupabase(selectedBirthdayDisplay)

        binding.edtBirthday.setText(selectedBirthdayDisplay)
        binding.tilBirthday.error = null
    }

    private fun submitForm() {
        clearErrors()

        val fullName = binding.edtFullName.text.toString().trim()
        val userName = binding.edtUserName.text.toString().trim()
        val bio = binding.edtBio.text.toString().trim()
        val address = binding.edtAddress.text.toString().trim()
        val phoneNumber = binding.edtPhoneNumber.text.toString().trim()
        val gender = binding.actGender.text.toString()
        val hobbies = binding.edtHobbies.text.toString().trim()

        if (!validateInput(fullName, userName)) return
        userData = userData.copy(
            fullName = fullName,
            username = userName,
            bio = bio,
            address = address,
            phone = phoneNumber,
            gender = gender,
            hobbies = hobbies,
            birthday = selectedBirthdayDb
        )

        viewModel.updateProfile(userData)
    }

    private fun validateInput(fullName: String, userName: String): Boolean {
        var isValid = true

        if (fullName.isBlank()) {
            binding.edtFullName.error = getString(R.string.please_enter_full_name)
            binding.edtFullName.requestFocus()
            isValid = false
        }

        val userNameError = ValidationUtils.validateUserName(this, userName)
        if (userNameError != null) {
            binding.edtUserName.error = userNameError
            if (isValid) binding.edtUserName.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        binding.edtFullName.error = null
        binding.edtUserName.error = null
    }

    companion object{
        const val USER_DATA = "user_data"
    }
}