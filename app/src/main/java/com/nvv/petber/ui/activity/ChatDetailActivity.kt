package com.nvv.petber.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
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
import com.nvv.petber.ui.adapter.MessageAdapter
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

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val isVideo = contentResolver.getType(it)?.startsWith("video") == true
            handleMediaUri(it, isVideo = isVideo)
        }
    }

    private val pickFromGalleryLegacy = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val isVideo = contentResolver.getType(it)?.startsWith("video") == true
            handleMediaUri(it, isVideo)
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchMediaPicker()
        else toast(getString(R.string.error_permission_media))
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

        if (targetUserId.isNullOrEmpty() && initialConversationId.isNullOrEmpty()){
            finish()
        }
        // 1. Setup Emoji
        emojiPopup = EmojiPopup(
            binding.root,
            binding.edtMessage,
            onEmojiPopupShownListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_keyboard)
            },
            onEmojiPopupDismissListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_emoji)
            },
            onEmojiBackspaceClickListener = {
                //
            }
        )
    }

    private fun setupRecyclerView() {
        binding.ivAvatarToolbar.loadAvatar(otherUserAvatar)
        binding.tvNameToolbar.text = if (otherUserName.isNullOrEmpty()) {
            getString(R.string.petber_user)
        } else otherUserName
        adapter = MessageAdapter(
            otherAvatarUrl = otherUserAvatar,
            currentUserId = viewModel.currentUserId,
            onAvatarClick = {
                UserProfileActivity.start(this, it)
            },
            onMessageLongClick = { message, anchorView ->
                showMessageOptionsPopup(message, anchorView)
            },
            onMediaClick = { message ->
                openMediaViewer(message)
            }
        )

        val layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = false
            stackFromEnd = false
        }

        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && !recyclerView.canScrollVertically(-1)) {
                    viewModel.loadHistoryMessages()
                }
            }
        })
    }

    private fun MessageModel.toPostMedia(): PostMedia {
        return PostMedia(
            "", "",
            mediaUrl = this.mediaUrl ?: "",
            mediaType = this.mediaType ?: "image"
        )
    }

    private fun openMediaViewer(message: MessageModel) {
        val mediaList = listOf(message.toPostMedia())
        val json = Gson().toJson(mediaList)
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_MEDIA_LIST, json)
            putExtra(MediaViewerActivity.EXTRA_START_INDEX, 0)
        }
        startActivity(intent)
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
        binding.btnEmoji.setOnClickListener {
            emojiPopup.toggle()
        }

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
            checkAndLaunchMediaPicker()
        }
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

                            val hasNewMessage = newSize > oldSize

                            if (hasNewMessage && isAtBottom) {
                                binding.rvMessages.post {
                                    binding.rvMessages.scrollToPosition(newSize - 1)
                                }
                            }

                            if (isFirstLoad && newSize > 0) {
                                binding.rvMessages.scrollToPosition(newSize - 1)
                                isFirstLoad = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            toast(it)
                        }
                    }
                }
            }
        }
    }


    private fun checkAndLaunchMediaPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchMediaPicker()
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchMediaPicker()
            } else {
                requestStoragePermission.launch(permission)
            }
        }
    }

    private fun launchMediaPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        } else {
            pickFromGalleryLegacy.launch("image/* video/*")
        }
    }

    private fun handleMediaUri(uri: Uri, isVideo: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val byteArray = inputStream?.readBytes()
                inputStream?.close()

                if (byteArray != null) {
                    val extension = if (isVideo) "mp4" else "jpg"
                    val fileName = "${UUID.randomUUID()}.$extension"
                    withContext(Dispatchers.Main) {
                        viewModel.sendMediaMessage(byteArray, fileName, isVideo)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    toast(getString(R.string.cannot_read_file))
                }
            }
        }
    }

    private fun isAtBottom(): Boolean {
        val lm = binding.rvMessages.layoutManager as LinearLayoutManager
        val lastVisible = lm.findLastCompletelyVisibleItemPosition()
         return lastVisible >= adapter.itemCount - 2
    }


    private fun showMessageOptionsPopup(message: MessageModel, anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)
        if (message.senderId == viewModel.currentUserId) {
            popupMenu.menu.add(0, 2, 0, R.string.action_delete_message)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                2 -> {
                    showConfirmDeleteMessageDialog(message.id)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showConfirmDeleteMessageDialog(messageId: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_msg_title)
            .setMessage(R.string.dialog_delete_msg_content)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteMessage(messageId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}