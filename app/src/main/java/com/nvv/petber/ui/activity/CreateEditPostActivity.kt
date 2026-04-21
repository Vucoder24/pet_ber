package com.nvv.petber.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.ActivityCreateEditPostBinding
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.adapter.MediaPreviewAdapter
import com.nvv.petber.ui.dialog.UploadProgressDialog
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.getVideoDuration
import com.nvv.petber.viewmodel.CreateContentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class CreateEditPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateEditPostBinding
    private val viewModel: CreateContentViewModel by viewModels()
    private lateinit var uploadDialog: UploadProgressDialog
    private val remoteMediaItems = mutableListOf<MediaItem>()
    private val localMediaItems = mutableListOf<MediaItem>()
    private lateinit var adapterPreviewMedia: MediaPreviewAdapter
    private var isEditMode = false

    companion object {
        const val EXTRA_POST_JSON = "extra_post_json"
        const val IS_EDIT_MODE = "is_edit_mode"
        const val EXTRA_EDIT_SUCCESS = "extra_edit_success"
    }

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

            items?.let { newLocalItems ->
                localMediaItems.clear()
                localMediaItems.addAll(newLocalItems)

                handleMediaVisibility()
                adapterPreviewMedia.submitList(remoteMediaItems + localMediaItems)
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

    private fun createChipListener(pet: Pet): (CompoundButton, Boolean) -> Unit {
        return { _, isChecked ->
            if (isChecked) {
                viewModel.addPetTag(pet)
            } else {
                viewModel.removePetTag(pet.id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateEditPostBinding.inflate(layoutInflater)
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
        binding.btnClose.bringToFront()
        // init dialog
        uploadDialog = UploadProgressDialog(this)
        binding.rvMediaPreview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapterPreviewMedia = MediaPreviewAdapter(
            items = mutableListOf(),
            onRemove = { pos ->
                val allItems = remoteMediaItems + localMediaItems
                val removedItem = allItems[pos]

                if (removedItem.isFromRemote) {
                    removedItem.remoteId?.let { viewModel.markRemoteMediaAsDeleted(it) }
                    remoteMediaItems.remove(removedItem)
                } else {
                    localMediaItems.remove(removedItem)
                }

                handleMediaVisibility()
                adapterPreviewMedia.submitList(remoteMediaItems + localMediaItems)
            },
            onClick = { pos ->
                val allItems = remoteMediaItems + localMediaItems
                val intent = Intent(this, MediaPreviewActivity::class.java).apply {
                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, allItems[pos])
                }
                startActivity(intent)
            }
        )
        binding.rvMediaPreview.adapter = adapterPreviewMedia

        setupData()
        checkEditMode()
        setupListeners()
        observeViewModel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra(IS_EDIT_MODE, false)
        if (isEditMode) {
            binding.tvTitle.text = getString(R.string.edit_post)
            binding.btnPost.setText(R.string.save_post)
            binding.btnEditMedia.setText(R.string.add_media)
            binding.btnPost.icon = null
            val postJson = intent.getStringExtra(EXTRA_POST_JSON)
            if (postJson != null) {
                try {
                    val post = Json.decodeFromString<Post>(postJson)
                    viewModel.setEditMode(post)

                    binding.etCaption.setText(post.caption)
                    binding.etHashtags.setText(post.hashtags)

                    post.postMedia?.forEach { remoteMedia ->
                        val item = MediaItem(
                            uri = remoteMedia.mediaUrl.toUri(),
                            isVideo = remoteMedia.mediaType == "video",
                            isFromRemote = true,
                            remoteId = remoteMedia.id,
                            remoteUrl = remoteMedia.mediaUrl,
                        )
                        if (remoteMedia.mediaType == "video") {
                            getVideoDuration(this, remoteMedia.mediaUrl.toUri()) { duration ->
                                item.duration = duration
                                adapterPreviewMedia.notifyDataSetChanged()
                            }
                        }
                        remoteMediaItems.add(item)
                    }

                    handleMediaVisibility()
                    adapterPreviewMedia.submitList((remoteMediaItems + localMediaItems).toList())

                } catch (_: Exception) {
                    toast(R.string.error_data_post)
                    finish()
                }
            }
        }
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

        viewModel.isLoadingPets.observe(this){
            binding.layoutLoadingBlock.visibility =
                if (it) View.VISIBLE else View.GONE

            binding.btnPost.isEnabled = !it
        }

        viewModel.uploadProgress.observe(this) { progress ->
            uploadDialog.updateProgress(progress)
        }

        viewModel.postSuccess.observe(this) { success ->
            if (success) {
                if (viewModel.currentEditPostId != null) {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_EDIT_SUCCESS, true)
                    }
                    setResult(RESULT_OK, resultIntent)
                    toast(R.string.post_updated)
                } else {
                    toast(getString(R.string.post_created))
                }
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
            viewModel.submitPost(
                caption = caption,
                location = null,
                hashtags = hashtagsStr,
                allCurrentMedia = remoteMediaItems + localMediaItems,
                petIds = viewModel.selectedPetIds.value?.toList() ?: emptyList()
            )
        }


        binding.btnEditMedia.setOnClickListener {
            val intent = Intent(this, MediaPickerActivity::class.java).apply {
                putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_MULTI)
                putParcelableArrayListExtra(
                    MediaPickerActivity.EXTRA_PRESELECTED,
                    ArrayList(localMediaItems)
                )
            }
            mediaPickerLauncher.launch(intent)
        }
    }

    private fun setupData() {
        viewModel.loadUserPets()

        viewModel.userPets.observe(this) { pets ->
            binding.chipGroupPets.removeAllViews()
            val addChip = layoutInflater.inflate(
                R.layout.item_pet_chip,
                binding.chipGroupPets,
                false
            ) as Chip

            addChip.apply {
                text = getString(R.string.add_pet_tag)
                isCheckable = false
                setTextColor(getColor(R.color.pet_accent))
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@CreateEditPostActivity,
                            CreateEditPetActivity::class.java
                        )
                    )
                }
            }

            binding.chipGroupPets.addView(addChip)

            val selectedIds = viewModel.selectedPetIds.value ?: emptySet()

            pets.forEach { pet ->
                val chip = layoutInflater.inflate(
                    R.layout.item_pet_chip,
                    binding.chipGroupPets,
                    false
                ) as Chip

                chip.apply {
                    text = pet.name
                    isCheckable = true
                    setOnCheckedChangeListener(null)
                    isChecked = selectedIds.contains(pet.id)
                    setOnCheckedChangeListener(createChipListener(pet))
                }
                binding.chipGroupPets.addView(chip)
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
        if (remoteMediaItems.isNotEmpty() || localMediaItems.isNotEmpty()) {
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