package com.nvv.petber.ui.dialog

import android.app.Dialog
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.ui.adapter.MediaItemPicker
import com.nvv.petber.ui.adapter.MediaPickerSheetAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPickerBottomSheetChat : BottomSheetDialogFragment() {

    var onSendMedia: ((MediaItemPicker) -> Unit)? = null
    var onPreviewMedia: ((MediaItemPicker) -> Unit)? = null

    private lateinit var rvMedia: RecyclerView
    private lateinit var btnPreview: Button
    private lateinit var btnSend: Button
    private lateinit var layoutButtons: LinearLayout

    private lateinit var adapter: MediaPickerSheetAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_media_picker, container, false)


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupButtons()
        loadMedia()
        setupBottomSheetBehavior()
    }


    private fun bindViews(view: View) {
        rvMedia = view.findViewById(R.id.rvMediaPicker)
        btnPreview = view.findViewById(R.id.btnPreview)
        btnSend = view.findViewById(R.id.btnSend)
        layoutButtons = view.findViewById(R.id.layoutButtons)
    }

    private fun toggleActionButtons(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        layoutButtons.visibility = visibility
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun setupBottomSheetBehavior() {
        val d = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val sheet = d?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(sheet)

        val keyboardHeight = getEstimatedKeyboardHeight()

        behavior.apply {
            peekHeight = keyboardHeight
            state = BottomSheetBehavior.STATE_COLLAPSED
            isHideable = true
            skipCollapsed = false
            isFitToContents = false
            expandedOffset = 0
        }

        rvMedia.isNestedScrollingEnabled = true
    }

    private fun getEstimatedKeyboardHeight(): Int {
        val density = resources.displayMetrics.density
        return (320 * density).toInt()
    }

    private fun setupRecyclerView() {
        adapter = MediaPickerSheetAdapter { _ ->
            val hasSelection = adapter.getSelectedItem() != null
            toggleActionButtons(hasSelection)
        }
        rvMedia.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@MediaPickerBottomSheetChat.adapter
        }
    }

    private fun setupButtons() {

        btnPreview.setOnClickListener {
            val item = adapter.getSelectedItem() ?: return@setOnClickListener
            onPreviewMedia?.invoke(item)
        }

        btnSend.setOnClickListener {
            val item = adapter.getSelectedItem() ?: return@setOnClickListener
            onSendMedia?.invoke(item)
            dismissAllowingStateLoss()
        }
    }


    private fun loadMedia() {
        viewLifecycleOwner.lifecycleScope.launch {
            val mediaList = withContext(Dispatchers.IO) {
                queryMediaStore()
            }
            adapter.submitList(mediaList)
            rvMedia.visibility = View.VISIBLE
        }
    }


    private fun queryMediaStore(): List<MediaItemPicker> {
        val results = mutableListOf<MediaItemPicker>()
        val context = requireContext()

        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED
            ),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )
        )

        collections.forEach { (uri, projection) ->
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val durCol = cursor.queryColumnExists(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val duration = if (durCol != -1) cursor.getLong(durCol) else 0L

                    results.add(
                        MediaItemPicker(
                            id = id,
                            uri = contentUri,
                            name = cursor.getString(nameCol) ?: "",
                            mimeType = cursor.getString(mimeCol) ?: "",
                            duration = duration,
                            dateAdded = cursor.getLong(dateCol)
                        )
                    )
                }
            }
        }

        return results.sortedByDescending { it.dateAdded }
    }

    private fun android.database.Cursor.queryColumnExists(columnName: String): Int {
        return try { getColumnIndexOrThrow(columnName) } catch (_: Exception) { -1 }
    }

    companion object {
        const val TAG = "MediaPickerBottomSheet"

        fun newInstance(): MediaPickerBottomSheetChat = MediaPickerBottomSheetChat()
    }
}