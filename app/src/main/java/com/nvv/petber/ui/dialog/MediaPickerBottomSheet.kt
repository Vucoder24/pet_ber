package com.nvv.petber.ui.dialog

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.databinding.BottomSheetMediaPickerBinding
import com.nvv.petber.ui.adapter.MediaModel
import com.nvv.petber.ui.adapter.MediaPickerAdapter
import com.nvv.petber.utils.ext.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPickerBottomSheet(
    private val onMediaSelected: (MediaModel) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MediaPickerAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            loadMedia()
        } else {
            requireContext().toast(R.string.error_permission_media)
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMediaPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        checkPermissionsAndLoadMedia()
    }

    private fun setupRecyclerView() {
        adapter = MediaPickerAdapter { selectedMedia ->
            onMediaSelected(selectedMedia)
            dismiss()
        }
        binding.rvMedia.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvMedia.adapter = adapter
    }

    private fun checkPermissionsAndLoadMedia() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasAllPermissions = permissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            loadMedia()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun loadMedia() {
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaList = mutableListOf<MediaModel>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            val queryUri = MediaStore.Files.getContentUri("external")

            requireContext().contentResolver.query(
                queryUri, projection, selection, null, sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val type = cursor.getInt(typeColumn)
                    val uri = ContentUris.withAppendedId(queryUri, id)
                    val isVideo = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                    mediaList.add(MediaModel(uri, isVideo))
                }
            }

            withContext(Dispatchers.Main) {
                adapter.submitList(mediaList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}