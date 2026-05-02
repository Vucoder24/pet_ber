package com.nvv.petber.ui.activity

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityMediaPickerBinding
import com.nvv.petber.ui.adapter.ActivityWithResult
import com.nvv.petber.ui.adapter.MediaGridAdapter
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class MediaPickerActivity : BaseActivity(), ActivityWithResult {
    private lateinit var binding: ActivityMediaPickerBinding
    private var mode: String = MODE_SINGLE
    private var maxSelect = DEFAULT_MAX

    private lateinit var adapter: MediaGridAdapter
    private var allItems = mutableListOf<MediaItem>()
    private var preselectedItems: List<MediaItem> = emptyList()

    private var mediaKind: String = MEDIA_KIND_ALL

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.EXTRA_RESULT_URI) ?: return@registerForActivityResult
            val isVideo = result.data?.getBooleanExtra(CameraActivity.EXTRA_IS_VIDEO, false) ?: false
            val uri = uriString.toUri()
            val mediaItem = MediaItem(uri = uri, isVideo = isVideo, duration = 0L)

            if (mode == MODE_SINGLE) {
                val intent = Intent()
                intent.putParcelableArrayListExtra(EXTRA_RESULT_MEDIAS, arrayListOf(mediaItem))
                setResult(RESULT_OK, intent)
                finish()
            } else {
                val mediaItem = MediaItem(uri = uri, isVideo = isVideo, duration = 0L)
                if (allItems.none { it.uri == uri }) {
                    allItems.add(0, mediaItem)
                }
                adapter.addItemToSelection(mediaItem)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMediaPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_SINGLE
        maxSelect = intent?.getIntExtra(EXTRA_MAX_SELECT, DEFAULT_MAX) ?: DEFAULT_MAX
        mediaKind = intent?.getStringExtra(EXTRA_MEDIA_KIND) ?: MEDIA_KIND_ALL

        preselectedItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                EXTRA_PRESELECTED,
                MediaItem::class.java
            ) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_PRESELECTED)
                ?: emptyList()
        }

        setupUi()
        checkPermissionsAndLoad()
    }

    private fun setupUi() {
        adapter = MediaGridAdapter(this, mode == MODE_MULTI, maxSelect) { selectedCount ->
            updateDoneButton(selectedCount)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerView.adapter = adapter

        updateModeUi()

        when (mediaKind) {
            MEDIA_KIND_IMAGES, MEDIA_KIND_VIDEOS -> binding.typeMedia.gone()
            MEDIA_KIND_ALL -> binding.typeMedia.visible()
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCamera.setOnClickListener {
            val cameraMode = when (mediaKind) {
                MEDIA_KIND_IMAGES -> CameraActivity.MODE_PHOTO
                MEDIA_KIND_VIDEOS -> CameraActivity.MODE_VIDEO
                else -> CameraActivity.MODE_ALL
            }
            cameraLauncher.launch(CameraActivity.createIntent(this, cameraMode))
        }

        binding.btnDone.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isEmpty()) {
                toast(getString(R.string.error_select_media))
                return@setOnClickListener
            }
            val result = Intent()
            result.putParcelableArrayListExtra(EXTRA_RESULT_MEDIAS, ArrayList(selected))
            setResult(RESULT_OK, result)
            finish()
        }

        // dropdown media type (Images / Videos / All)
        val dd = binding.dropdownMediaType
        val options =
            resources.getStringArray(R.array.media_types) // assume ["Image","Video","All"]
        dd.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, options))
        dd.setText(options[0], false)
        dd.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            filterByType(options[pos])
        }

//        // multi select toggle button (switch mode at runtime)
//        binding.btnMultiSelect.setOnClickListener {
//
//            mode = MODE_MULTI
//            adapter.setMultiSelect(true)
//
//            updateModeUi()
//            updateDoneButton(adapter.getSelectedUris().size)
//
//        }

        updateDoneButton(0)
    }

    private fun updateDoneButton(selectedCount: Int) {

        if (mode != MODE_MULTI) return

        binding.btnDone.text =
            if (selectedCount == 0) "Next"
            else "Next ($selectedCount)"
    }

    private fun filterByType(choice: String) {
        val selectedUris = adapter.getSelectedUris().toSet()
        val filtered = when (choice.lowercase(Locale.getDefault())) {
            "image" -> allItems.filter { !it.isVideo || it.uri in selectedUris }
            "video" -> allItems.filter { it.isVideo || it.uri in selectedUris }
            else -> allItems
        }
        adapter.submitList(filtered)
    }

    private fun checkPermissionsAndLoad() {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val images = Manifest.permission.READ_MEDIA_IMAGES
            val videos = Manifest.permission.READ_MEDIA_VIDEO
            if (ContextCompat.checkSelfPermission(
                    this,
                    images
                ) != PackageManager.PERMISSION_GRANTED
            ) need.add(images)
            if (ContextCompat.checkSelfPermission(
                    this,
                    videos
                ) != PackageManager.PERMISSION_GRANTED
            ) need.add(videos)
        } else {
            val p = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    p
                ) != PackageManager.PERMISSION_GRANTED
            ) need.add(p)
        }

        if (need.isNotEmpty()) {
            val arr = need.toTypedArray()
            requestPermissions(arr, PERMISSION_REQUEST_CODE)
        } else {
            loadMedia()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
            if (denied) {
                toast(getString(R.string.permission_media_request))
            } else {
                loadMedia()
            }
        }
    }

    private fun loadMedia() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val items = queryMediaStore()
            val preselectedUris = preselectedItems.map { it.uri }.toSet()

            val mapped = items.map { item ->
                item.copy(isSelected = preselectedUris.contains(item.uri))
            }

            allItems.clear()
            allItems.addAll(mapped)

            //  Submit list for DiffUtil
            adapter.submitList(mapped)

            if (preselectedItems.isNotEmpty()) {
                adapter.setInitialSelection(preselectedItems)
            }

            updateDoneButton(adapter.getSelectedUris().size)
            binding.progressBar.gone()
        }
    }

    private suspend fun queryMediaStore(): List<MediaItem> = withContext(Dispatchers.IO) {

        val results = mutableListOf<MediaItem>()

        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )

        val selection = when (mediaKind) {
            MEDIA_KIND_IMAGES -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
            MEDIA_KIND_VIDEOS -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
            else -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        }


        val selectionArgs = when (mediaKind) {
            MEDIA_KIND_IMAGES -> arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            MEDIA_KIND_VIDEOS -> arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            else -> arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val type = cursor.getInt(typeIndex)
                    val uri = ContentUris.withAppendedId(collection, id)

                    if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        results.add(
                            MediaItem(
                                uri = uri,
                                isVideo = false,
                                duration = 0L
                            )
                        )
                    } else if (mediaKind != MEDIA_KIND_IMAGES && type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        val duration = cursor.getLong(durationIndex)
                        results.add(
                            MediaItem(
                                uri = uri,
                                isVideo = true,
                                duration = duration
                            )
                        )
                    }
                }
            }

        results
    }

    private fun updateModeUi() {

//        if (mode == MODE_SINGLE) {
//
//            binding.btnMultiSelect.visibility = View.GONE
//            binding.btnDone.visibility = View.GONE
//
//        } else {
//
//            binding.btnMultiSelect.visibility = View.VISIBLE
//            binding.btnDone.visibility = View.VISIBLE
//
//        }
    }


    override fun returnResult(list: ArrayList<MediaItem>) {
        val result = Intent()
        result.putParcelableArrayListExtra(EXTRA_RESULT_MEDIAS, list)
        setResult(RESULT_OK, result)
        finish()
    }


    companion object {
        const val EXTRA_RESULT_MEDIAS = "extra_result_medias"
        const val EXTRA_PRESELECTED = "extra_preselected"

        const val EXTRA_MODE = "extra_mode"
        const val MODE_SINGLE = "mode_single"
        const val MODE_MULTI = "mode_multi"

        const val EXTRA_MAX_SELECT = "extra_max_select"
        const val DEFAULT_MAX = 10

        const val EXTRA_MEDIA_KIND = "extra_media_kind"
        const val MEDIA_KIND_ALL = "media_kind_all"
        const val MEDIA_KIND_IMAGES = "media_kind_images"
        const val MEDIA_KIND_VIDEOS = "media_kind_videos"

        // Request codes
        private const val PERMISSION_REQUEST_CODE = 99
    }
}