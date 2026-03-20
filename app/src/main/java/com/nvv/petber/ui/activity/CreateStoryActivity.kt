package com.nvv.petber.ui.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityCreateStoryBinding
import com.nvv.petber.ui.dialog.UploadProgressDialog
import com.nvv.petber.utils.AppEventManager
import com.nvv.petber.utils.loadImageUri
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

                binding.videoStoryPreview.visibility = android.view.View.VISIBLE
                binding.ivStoryPreview.visibility = android.view.View.GONE

                binding.videoStoryPreview.setVideoURI(uri)
                binding.videoStoryPreview.setOnPreparedListener {
                    it.isLooping = true
                    binding.videoStoryPreview.start()
                }

            } else {

                binding.ivStoryPreview.visibility = android.view.View.VISIBLE
                binding.videoStoryPreview.visibility = android.view.View.GONE

                binding.ivStoryPreview.loadImageUri(uri)
            }
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

                Toast.makeText(this, "Media not found", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(
                    this,
                    getString(R.string.story_posted),
                    Toast.LENGTH_SHORT
                ).show()
                AppEventManager.triggerRefreshStories()
                viewModel.resetStorySuccess()
                finish()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
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