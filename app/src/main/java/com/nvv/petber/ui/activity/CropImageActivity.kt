package com.nvv.petber.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityCropImageBinding
import com.nvv.petber.ui.base.BaseActivity
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CropImageActivity : BaseActivity() {
    private lateinit var binding: ActivityCropImageBinding
    private var sourceUri: Uri? = null
    private var targetType: String = TARGET_AVATAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sourceUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SOURCE_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SOURCE_URI)
        }

        targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: TARGET_AVATAR

        binding.btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        startCrop()

    }

    private fun startCrop() {
        val uri = sourceUri ?: return

        val destinationUri = Uri.fromFile(java.io.File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        val uCrop = UCrop.of(uri, destinationUri)

        if (targetType == TARGET_AVATAR) {
            uCrop.withAspectRatio(1f, 1f)
        } else {
            uCrop.withAspectRatio(16f, 9f)
        }

        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setToolbarColor(getColorCompat(R.color.pet_primary))
            setStatusBarColor(getColorCompat(R.color.pet_primary))
            setActiveControlsWidgetColor(getColorCompat(R.color.pet_primary))
        }

        uCrop.withOptions(options)
        uCrop.start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            UCrop.REQUEST_CROP -> {
                when (resultCode) {
                    RESULT_OK -> {
                        val resultUri = UCrop.getOutput(data!!)
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_RESULT_URI, resultUri?.toString())
                        }
                        setResult(RESULT_OK, resultIntent)
                    }
                    UCrop.RESULT_ERROR -> {
                        setResult(RESULT_CANCELED)
                    }
                    else -> {
                        setResult(RESULT_CANCELED)
                    }
                }
                finish()
            }
        }
    }

    private fun getColorCompat(colorRes: Int): Int = resources.getColor(colorRes, theme)

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_TARGET_TYPE = "extra_target_type"
        const val EXTRA_RESULT_URI = "extra_result_uri"

        const val TARGET_AVATAR = "target_avatar"
        const val TARGET_COVER = "target_cover"
    }
}