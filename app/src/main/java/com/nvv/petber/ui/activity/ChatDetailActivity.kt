package com.nvv.petber.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nvv.petber.R
import com.nvv.petber.data.model.MessageModel
import com.nvv.petber.data.model.PostMedia
import com.nvv.petber.databinding.ActivityChatDetailBinding
import com.nvv.petber.ui.adapter.MediaItemPicker
import com.nvv.petber.ui.adapter.MessageAdapter
import com.nvv.petber.ui.dialog.MediaPickerBottomSheetChat
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.ChatDetailViewModel
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@AndroidEntryPoint
class ChatDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var emojiPopup: EmojiPopup
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null
    private var targetUserId: String? = null
    private var initialConversationId: String? = null
    private var isFirstLoad = true

    companion object {
        const val CONVERSATION_ID = "conversationId"
    }

    private val requestMediaPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val isGranted = result.values.any { it }

        if (isGranted) {
            showMediaPickerBottomSheet()
        } else {
            toast(getString(R.string.error_permission_media))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupUX()
        setupRecyclerView()
        setupInputActions()
        observeViewModel()
    }

    private fun setupUX() {
        otherUserName = intent.getStringExtra("other_name")
        otherUserAvatar = intent.getStringExtra("other_avatar")
        targetUserId = intent.getStringExtra("other_user_id")
        initialConversationId = intent.getStringExtra(CONVERSATION_ID)

        viewModel.setTargetUser(targetUserId, initialConversationId)

        if (targetUserId.isNullOrEmpty() && initialConversationId.isNullOrEmpty()) {
            finish()
        }

        emojiPopup = EmojiPopup(
            binding.root,
            binding.edtMessage,
            onEmojiPopupShownListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_keyboard)
            },
            onEmojiPopupDismissListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_emoji)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.ivAvatarToolbar.loadAvatar(otherUserAvatar)
        binding.tvNameToolbar.text =
            if (otherUserName.isNullOrEmpty()) getString(R.string.petber_user)
            else otherUserName

        adapter = MessageAdapter(
            otherAvatarUrl = otherUserAvatar,
            currentUserId = viewModel.currentUserId,
            onAvatarClick = { UserProfileActivity.start(this, it) },
            onMessageLongClick = { message, anchorView ->
                showMessageOptionsPopup(message, anchorView)
            },
            onMediaClick = { message -> openMediaViewer(message) }
        )

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.edtMessage.setOnClickListener {
            if (adapter.itemCount > 0) {
                binding.rvMessages.postDelayed({
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }, 100)
            }
        }

        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && !recyclerView.canScrollVertically(-1)) {
                    viewModel.loadHistoryMessages()
                }
            }
        })
    }

    private fun setupInputActions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            val text = binding.edtMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendTextMessage(text)
                binding.edtMessage.text?.clear()
            }
        }

        binding.btnEmoji.setOnClickListener { emojiPopup.toggle() }

        binding.ivAvatarToolbar.setOnClickListener {
            UserProfileActivity.start(this, targetUserId!!)
        }
        binding.tvNameToolbar.setOnClickListener {
            UserProfileActivity.start(this, targetUserId!!)
        }

        binding.edtMessage.addTextChangedListener {
            val hasText = !it.isNullOrBlank()
            binding.btnSend.isEnabled = hasText
            binding.btnSend.alpha = if (hasText) 1.0f else 0.5f
        }


        binding.btnMedia.setOnClickListener {
            if (hasMediaPermission()) {
                showMediaPickerBottomSheet()
            } else {
                requestMediaPermission.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                )
            }
        }
    }


    private fun showMediaPickerBottomSheet() {
        if (supportFragmentManager.findFragmentByTag(MediaPickerBottomSheetChat.TAG) != null) return

        val sheet = MediaPickerBottomSheetChat.newInstance()

        sheet.onSendMedia = { mediaItem ->
            sendMediaItem(mediaItem)
        }

        sheet.onPreviewMedia = { mediaItem ->
            previewMediaItem(mediaItem)
        }

        sheet.show(supportFragmentManager, MediaPickerBottomSheetChat.TAG)
    }


    private fun sendMediaItem(item: MediaItemPicker) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val ext = if (item.isVideo) "mp4" else "jpg"
                    val fileName = "${UUID.randomUUID()}.$ext"
                    withContext(Dispatchers.Main) {
                        viewModel.sendMediaMessage(bytes, fileName, item.isVideo)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    toast(getString(R.string.cannot_read_file))
                }
            }
        }
    }


    private fun previewMediaItem(item: MediaItemPicker) {
        val postMedia = PostMedia(
            id = "",
            postId = "",
            mediaUrl = item.uri.toString(),
            mediaType = if (item.isVideo) "video" else "image"
        )
        val json = Gson().toJson(listOf(postMedia))
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_MEDIA_LIST, json)
            putExtra(MediaViewerActivity.EXTRA_START_INDEX, 0)
        }
        startActivity(intent)
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messageList ->
                        val isAtBottom = isAtBottom()
                        val oldSize = adapter.itemCount

                        adapter.submitList(messageList) {
                            val newSize = messageList.size
                            if ((newSize > oldSize && isAtBottom) ||
                                (isFirstLoad && newSize > 0)
                            ) {
                                binding.rvMessages.scrollToPosition(newSize - 1)
                                isFirstLoad = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.error.collect { it?.let { toast(it) } }
                }
            }
        }
    }


    private fun MessageModel.toPostMedia() = PostMedia(
        "", "", mediaUrl = this.mediaUrl ?: "", mediaType = this.mediaType ?: "image"
    )

    private fun openMediaViewer(message: MessageModel) {
        val json = Gson().toJson(listOf(message.toPostMedia()))
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_MEDIA_LIST, json)
            putExtra(MediaViewerActivity.EXTRA_START_INDEX, 0)
        }
        startActivity(intent)
    }

    private fun hasMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isAtBottom(): Boolean {
        val lm = binding.rvMessages.layoutManager as LinearLayoutManager
        return lm.findLastCompletelyVisibleItemPosition() >= adapter.itemCount - 2
    }

    private fun showMessageOptionsPopup(message: MessageModel, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        if (message.senderId == viewModel.currentUserId) {
            popup.menu.add(0, 2, 0, R.string.action_delete_message)
        }
        popup.setOnMenuItemClickListener {
            if (it.itemId == 2) { showConfirmDeleteMessageDialog(message); true }
            else false
        }
        popup.show()
    }

    private fun showConfirmDeleteMessageDialog(message: MessageModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_msg_title)
            .setMessage(R.string.dialog_delete_msg_content)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteMessage(message) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}