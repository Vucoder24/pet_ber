package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.databinding.ActivityCreatePostBinding
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.adapter.MediaPreviewAdapter
import com.nvv.petber.ui.dialog.UploadProgressDialog
import com.nvv.petber.ui.mention.MentionEditText
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.CreateContentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePostBinding
    private val viewModel: CreateContentViewModel by viewModels()
    private lateinit var uploadDialog: UploadProgressDialog
    private val selectedMediaItems = mutableListOf<MediaItem>()
    private lateinit var adapterPreviewMedia: MediaPreviewAdapter

    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION") val items =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableArrayListExtra(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS,
                        MediaItem::class.java
                    )
                } else {
                    result.data?.getParcelableArrayListExtra(MediaPickerActivity.EXTRA_RESULT_MEDIAS)
                }

            items?.let {
                selectedMediaItems.clear()
                selectedMediaItems.addAll(it)
                handleMediaVisibility()
                adapterPreviewMedia.submitList(selectedMediaItems.toList())
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openMediaPicker()
        } else {
            toast(getString(R.string.permission_question))
        }
    }

    private fun createChipListener(pet: Pet): (android.widget.CompoundButton, Boolean) -> Unit {
        return { _, isChecked ->
            if (isChecked) {
                if (!binding.etCaption.hasMention(pet.id)) {
                    binding.etCaption.insertMention(pet.id, pet.name)
                    viewModel.togglePetTag(pet)
                }
            } else {
                binding.etCaption.removeMention(pet.id)
                viewModel.removePetTag(pet.id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        // init dialog
        uploadDialog = UploadProgressDialog(this)
        binding.rvMediaPreview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapterPreviewMedia = MediaPreviewAdapter(
            items = mutableListOf(),
            onRemove = { pos ->
                selectedMediaItems.removeAt(pos)
                handleMediaVisibility()
                adapterPreviewMedia.submitList(selectedMediaItems.toList())
            },
            onClick = { pos ->
                val intent = Intent(this, MediaPreviewActivity::class.java).apply {
                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, selectedMediaItems[pos])
                }
                startActivity(intent)
            }
        )
        binding.rvMediaPreview.adapter = adapterPreviewMedia

        setupData()
        setupListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.btnPost.isEnabled = !loading
            if (loading) {
                uploadDialog.show()
            } else {
                uploadDialog.dismiss()
            }
        }

        viewModel.uploadProgress.observe(this) { progress ->
            uploadDialog.updateProgress(progress)
        }

        viewModel.postSuccess.observe(this) { success ->
            if (success) {
                toast(getString(R.string.story_posted))
                finish()
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                toast(it)
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { finish() }

        binding.btnAddMedia.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString().trim()
            val hashtagsStr = binding.etHashtags.text.toString().trim()

            if (caption.isEmpty()) {
                binding.etCaption.error = getString(R.string.caption_empty)
                return@setOnClickListener
            }

            //call ViewModel
            viewModel.createPost(
                caption = caption, location = null, mediaUris = selectedMediaItems.map { it.uri },
                hashtags = hashtagsStr,
                petIds = binding.etCaption.getMentions()
            )
        }

        binding.etCaption.mentionRemovedListener =
            object : MentionEditText.OnMentionRemovedListener {
                override fun onMentionRemoved(petId: String) {
                    viewModel.removePetTag(petId)
                    unselectChip(petId)
                }
            }

        binding.btnEditMedia.setOnClickListener {
            val intent = Intent(this, MediaPickerActivity::class.java).apply {
                putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_MULTI)
                putParcelableArrayListExtra(
                    MediaPickerActivity.EXTRA_PRESELECTED,
                    ArrayList(selectedMediaItems)
                )
            }
            mediaPickerLauncher.launch(intent)
        }
    }

    private fun setupData() {
        viewModel.loadUserPets()

        viewModel.userPets.observe(this) { pets ->
            binding.chipGroupPets.removeAllViews()
            pets.forEach { pet ->
                val chip = Chip(this).apply {
                    text = pet.name
                    isCheckable = true
                }

                chip.setOnCheckedChangeListener(createChipListener(pet))

                binding.chipGroupPets.addView(chip)
            }
        }
    }


    private fun unselectChip(petId: String) {
        val pets = viewModel.userPets.value ?: return

        for (i in 0 until binding.chipGroupPets.childCount) {
            val chip = binding.chipGroupPets.getChildAt(i) as Chip
            val pet = pets[i]

            if (pet.id == petId) {

                chip.setOnCheckedChangeListener(null)

                chip.isChecked = false

                chip.setOnCheckedChangeListener(createChipListener(pet))

                break
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            openMediaPicker()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun openMediaPicker() {
        val intent = Intent(this, MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_MULTI)
        }
        mediaPickerLauncher.launch(intent)
    }

    private fun handleMediaVisibility() {
        if (selectedMediaItems.isNotEmpty()) {
            binding.rvMediaPreview.visibility = View.VISIBLE
            binding.btnAddMedia.visibility = View.GONE
            binding.btnEditMedia.visibility = View.VISIBLE
        } else {
            binding.rvMediaPreview.visibility = View.GONE
            binding.btnAddMedia.visibility = View.VISIBLE
            binding.btnEditMedia.visibility = View.GONE
        }
    }
}