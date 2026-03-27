package com.nvv.petber.ui.activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityCreateStoryBinding
import com.nvv.petber.ui.dialog.UploadProgressDialog
import com.nvv.petber.utils.AppEventManager
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadImageUri
import com.nvv.petber.utils.ext.loadMediaCoverWithExtremeGradient
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.CreateContentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateStoryBinding
    private val viewModel: CreateContentViewModel by viewModels()
    private lateinit var uploadDialog: UploadProgressDialog

    private var mediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        mediaUri = intent.getParcelableExtra("media_uri")
        // init dialog
        uploadDialog = UploadProgressDialog(this)

        setupPreview()
        setupClick()
        observeViewModel()
    }

    private fun setupPreview() {

        mediaUri?.let { uri ->

            val type = contentResolver.getType(uri)

            if (type?.startsWith("video") == true) {

                binding.videoStoryPreview.visible()
                binding.ivStoryPreview.gone()

                binding.videoStoryPreview.setVideoURI(uri)
                binding.videoStoryPreview.setOnPreparedListener {
                    it.isLooping = true
                    binding.videoStoryPreview.start()
                }

            } else {
                // process image
                binding.ivStoryPreview.visible()
                binding.videoStoryPreview.gone()

                binding.ivStoryPreview.loadImageUri(uri, centerCrop = false)
            }

            binding.ivStoryPreview.loadMediaCoverWithExtremeGradient(
                uri = uri,
                isVideo = type?.startsWith("video") == true,
                backgroundView = binding.storyBackground,
                scope = lifecycleScope,
                ctx = this
            )
        }
    }

    private fun setupClick() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnUploadStory.setOnClickListener {

            mediaUri?.let {

                viewModel.createStory(it)

            } ?: run {

                toast(getString(R.string.media_not_found))
            }
        }
    }

    private fun observeViewModel() {

        viewModel.isLoading.observe(this) { loading ->
            if (loading) {
                uploadDialog.show()
            } else {
                uploadDialog.dismiss()
            }
        }

        viewModel.uploadProgress.observe(this) { progress ->
            uploadDialog.updateProgress(progress)
        }

        viewModel.storySuccess.observe(this) { success ->
            if (success) {
                toast(getString(R.string.story_posted))
                AppEventManager.triggerRefreshStories()
                viewModel.resetStorySuccess()
                finish()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                toast(it)
                viewModel.clearError()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.videoStoryPreview.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoStoryPreview.start()
    }

}