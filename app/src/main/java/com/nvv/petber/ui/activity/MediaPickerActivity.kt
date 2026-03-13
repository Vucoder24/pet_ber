package com.nvv.petber.ui.activity

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityMediaPickerBinding
import com.nvv.petber.ui.adapter.ActivityWithResult
import com.nvv.petber.ui.adapter.MediaGridAdapter
import com.nvv.petber.ui.adapter.MediaItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class MediaPickerActivity : AppCompatActivity(), ActivityWithResult {
    private lateinit var binding: ActivityMediaPickerBinding
    private var mode: String = MODE_SINGLE
    private var maxSelect = DEFAULT_MAX

    private lateinit var adapter: MediaGridAdapter
    private var allItems = mutableListOf<MediaItem>()

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

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnDone.setOnClickListener {
            val selected = adapter.getSelectedUris()
            if (selected.isEmpty()) {
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = Intent()
            result.putParcelableArrayListExtra(EXTRA_RESULT_URIS, ArrayList(selected))
            setResult(Activity.RESULT_OK, result)
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

        // multi select toggle button (switch mode at runtime)
        binding.btnMultiSelect.setOnClickListener {

            mode = MODE_MULTI
            adapter.setMultiSelect(true)

            updateModeUi()
            updateDoneButton(adapter.getSelectedUris().size)

        }

        updateDoneButton(0)
    }

    private fun updateDoneButton(selectedCount: Int) {

        if (mode != MODE_MULTI) return

        binding.btnDone.text =
            if (selectedCount == 0) "Next"
            else "Next ($selectedCount)"
    }

    private fun filterByType(choice: String) {
        val filtered = when (choice.lowercase(Locale.getDefault())) {
            "image" -> allItems.filter { !it.isVideo }
            "video" -> allItems.filter { it.isVideo }
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
                Toast.makeText(this, "Cần quyền truy cập media để tiếp tục", Toast.LENGTH_LONG)
                    .show()
            } else {
                loadMedia()
            }
        }
    }

    private fun loadMedia() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val items = queryMediaStore()
            allItems.clear()
            allItems.addAll(items)
            adapter.submitList(allItems)
            binding.progressBar.visibility = View.GONE
        }
    }

    private suspend fun queryMediaStore(): List<MediaItem>
    = withContext(Dispatchers.IO) {

        val results = mutableListOf<MediaItem>()

        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )

        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"

        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
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

                } else if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {

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

        if (mode == MODE_SINGLE) {

            binding.btnMultiSelect.visibility = View.GONE
            binding.btnDone.visibility = View.GONE

        } else {

            binding.btnMultiSelect.visibility = View.VISIBLE
            binding.btnDone.visibility = View.VISIBLE

        }
    }


    override fun returnResult(list: ArrayList<Uri>) {
        val result = Intent()
        result.putParcelableArrayListExtra(EXTRA_RESULT_URIS, list)
        setResult(Activity.RESULT_OK, result)
        finish()
    }


    companion object {
        const val EXTRA_RESULT_URIS = "extra_result_uris"

        const val EXTRA_MODE = "extra_mode"
        const val MODE_SINGLE = "mode_single"
        const val MODE_MULTI = "mode_multi"

        const val EXTRA_MAX_SELECT = "extra_max_select"
        const val DEFAULT_MAX = 10

        // Request codes
        private const val PERMISSION_REQUEST_CODE = 99
    }
}