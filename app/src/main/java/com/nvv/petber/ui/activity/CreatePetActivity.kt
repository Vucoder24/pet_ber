package com.nvv.petber.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.databinding.ActivityCreatePetBinding
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.CreatePetState
import com.nvv.petber.viewmodel.CreatePetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class CreatePetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePetBinding
    private val viewModel: CreatePetViewModel by viewModels()

    private var avatarUri: Uri? = null
    private var coverUri: Uri? = null
    private lateinit var currentUserId: String
    private var selectedBirthdayDb: String? = null
    private var selectedBirthdayDisplay: String? = null

    private var cropTarget: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                cropTarget?.let { openImagePicker(it) }
            } else {
                toast(R.string.permission_question)
            }
        }

    private val mediaPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

            val items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableArrayListExtra(
                    MediaPickerActivity.EXTRA_RESULT_MEDIAS,
                    com.nvv.petber.ui.adapter.MediaItem::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableArrayListExtra(
                    MediaPickerActivity.EXTRA_RESULT_MEDIAS
                )
            } ?: return@registerForActivityResult

            val firstUri = items.firstOrNull()?.uri ?: return@registerForActivityResult
            val target = cropTarget ?: return@registerForActivityResult

            launchCrop(firstUri, target)
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult

            val uriString = result.data?.getStringExtra(CropImageActivity.EXTRA_RESULT_URI)
                ?: return@registerForActivityResult

            val uri = uriString.toUri()

            when (cropTarget) {
                CropImageActivity.TARGET_AVATAR -> {
                    avatarUri = uri
                    binding.ivAvatar.setImageURI(uri)
                }

                CropImageActivity.TARGET_COVER -> {
                    coverUri = uri
                    binding.ivCover.setImageURI(uri)
                }
            }

            cropTarget = null
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreatePetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentUserId = SharePrefUtils.getCurrentUserId(this)
        updateBirthday(Calendar.getInstance())
        setupHealthDropdown()
        setupClicks()
        observeState()

    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardAvatar.setOnClickListener {
            checkPermissionAndOpenPicker(CropImageActivity.TARGET_AVATAR)
        }

        binding.cardCover.setOnClickListener {
            checkPermissionAndOpenPicker(CropImageActivity.TARGET_COVER)
        }

        binding.edtBirthday.setOnClickListener {
            showDatePicker()
        }
        binding.tilBirthday.setOnClickListener {
            showDatePicker()
        }

        binding.btnCreatePet.setOnClickListener {
            submitForm()
        }
    }

    private fun checkPermissionAndOpenPicker(target: String) {
        cropTarget = target

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val isAllGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (isAllGranted) {
            openImagePicker(target)
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun openImagePicker(target: String) {
        cropTarget = target

        val intent = Intent(this, MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MAX_SELECT, 1)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_IMAGES)
        }

        mediaPickerLauncher.launch(intent)
    }

    private fun showDatePicker() {
        val currentMs = selectedBirthdayDb?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                sdf.parse(it)?.time
            } catch (_: Exception) { null }
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

    private fun launchCrop(sourceUri: Uri, target: String) {
        cropTarget = target

        val intent = Intent(this, CropImageActivity::class.java).apply {
            putExtra(CropImageActivity.EXTRA_SOURCE_URI, sourceUri)
            putExtra(CropImageActivity.EXTRA_TARGET_TYPE, target)
        }

        cropLauncher.launch(intent)
    }

    private fun setupHealthDropdown() {
        setupAdapter(binding.actGender, R.array.gender_options)
        setupAdapter(binding.actPreventiveStatus, R.array.preventive_status_options)
        setupAdapter(binding.actBodyCondition, R.array.body_condition_options)
        setupAdapter(binding.actClinicalStatus, R.array.clinical_status_options)
        setupAdapter(binding.actActivityLevel, R.array.activity_mental_options)
    }

    private fun setupAdapter(view: AutoCompleteTextView, arrayRes: Int) {
        val items = resources.getStringArray(arrayRes)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        view.setAdapter(adapter)
        if (items.isNotEmpty()) view.setText(items.first(), false)
    }

    private fun submitForm() {
        clearErrors()

        val name = binding.edtName.text?.toString()?.trim().orEmpty()
        val breed = binding.edtBreed.text?.toString()?.trim().orEmpty()
        val species = binding.edtSpecies.text?.toString()?.trim().orEmpty()
        val description = binding.edtDescription.text?.toString()?.trim().orEmpty()

        val gender = binding.actGender.text.toString()
        val weight = binding.edtWeight.text?.toString()?.toDoubleOrNull()
        val isNeutered = binding.swNeutered.isChecked

        val preventiveStatus = binding.actPreventiveStatus.text.toString()
        val bodyCondition = binding.actBodyCondition.text.toString()
        val clinicalStatus = binding.actClinicalStatus.text.toString()
        val activityAndMental = binding.actActivityLevel.text.toString()

        if (!validateInput(name, species)) return

        val pet = Pet(
            ownerId = currentUserId,
            name = name,
            breed = breed,
            species = species,
            gender = gender,
            weight = weight,
            isNeutered = isNeutered,
            birthday = selectedBirthdayDb,
            description = description,
            bodyCondition = bodyCondition,
            clinicalStatus = clinicalStatus,
            activityAndMentalState = activityAndMental,
            preventiveStatus = preventiveStatus
        )

        viewModel.createPet(
            pet = pet,
            avatarUri = avatarUri,
            coverUri = coverUri
        )
    }

    private fun validateInput(name: String, species: String): Boolean {
        var valid = true

        if (name.isBlank()) {
            binding.edtName.error = getString(R.string.error_empty_pet_name)
            binding.edtName.requestFocus()
            valid = false
        }

        if (species.isBlank()) {
            binding.edtSpecies.error = getString(R.string.error_empty_pet_species)
            binding.edtSpecies.requestFocus()
            valid = false
        }

        return valid
    }

    private fun clearErrors() {
        binding.edtName.error = null
        binding.edtSpecies.error = null
        binding.edtBirthday.error = null
    }

    private fun updateBirthday(calendar: Calendar) {
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val dateDb = dbFormat.format(calendar.time)
        val dateDisplay = displayFormat.format(calendar.time)

        selectedBirthdayDb = dateDb
        selectedBirthdayDisplay = dateDisplay

        binding.edtBirthday.setText(dateDisplay)

        binding.edtBirthday.error = null

    }

    private fun observeState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreatePetState.Loading -> {
                            binding.apply {
                                layoutLoading.visible()
                                btnCreatePet.isEnabled = false
                            }
                        }

                        is CreatePetState.Success -> {
                            binding.apply {
                                layoutLoading.gone()
                                btnCreatePet.isEnabled = true
                            }
                            Toast.makeText(
                                this@CreatePetActivity,
                                getString(R.string.create_pet_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            setResult(RESULT_OK)
                            viewModel.resetState()
                            finish()
                        }

                        is CreatePetState.Error -> {
                            binding.apply {
                                layoutLoading.gone()
                                btnCreatePet.isEnabled = true
                            }
                            Toast.makeText(this@CreatePetActivity, state.message, Toast.LENGTH_LONG)
                                .show()
                            Log.e("CreatePetActivity", "Error: ${state.message}")
                            viewModel.resetState()
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}