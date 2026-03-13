package com.nvv.petber.ui.fragment.create_post

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentCreatePostBinding
import com.nvv.petber.ui.activity.CreateStoryActivity
import com.nvv.petber.ui.activity.MediaPickerActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->

            val granted = result.all { it.value }

            if (granted) {
                openMediaPicker()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_question),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private val mediaPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                @Suppress("DEPRECATION") val uris =
                    result.data?.getParcelableArrayListExtra<Uri>(MediaPickerActivity.EXTRA_RESULT_URIS)

                if (!uris.isNullOrEmpty()) {
                    openCreateStory(uris.first())
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateStory.setOnClickListener {
            checkMediaPermission()
        }

        binding.btnCreateStory2.setOnClickListener {
            checkMediaPermission()
        }
    }

    private fun checkMediaPermission() {

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

        } else {

            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) {

            // permission granted
            openMediaPicker()

        } else {

            // request permissions
            permissionLauncher.launch(deniedPermissions.toTypedArray())
        }
    }

    private fun openMediaPicker() {

        val intent = Intent(requireContext(), MediaPickerActivity::class.java)

        intent.putExtra(
            MediaPickerActivity.EXTRA_MODE,
            MediaPickerActivity.MODE_SINGLE
        )

        mediaPickerLauncher.launch(intent)
    }

    private fun openCreateStory(uri: Uri) {

        val intent = Intent(requireContext(), CreateStoryActivity::class.java)
        intent.putExtra("media_uri", uri)

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}