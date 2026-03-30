package com.nvv.petber.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.nvv.petber.R

class MediaFullscreenDialog : DialogFragment() {

    private var exoPlayer: ExoPlayer? = null
    private var imageUrl: String? = null
    var onDismissCallback: (() -> Unit)? = null

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_IMAGE_URL = "image_url"
        private const val TYPE_VIDEO = "video"
        private const val TYPE_IMAGE = "image"

        fun newVideoInstance(player: ExoPlayer): MediaFullscreenDialog {
            return MediaFullscreenDialog().apply {
                exoPlayer = player
                arguments = Bundle().apply {
                    putString(ARG_TYPE, TYPE_VIDEO)
                }
            }
        }

        fun newImageInstance(imageUrl: String): MediaFullscreenDialog {
            return MediaFullscreenDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, TYPE_IMAGE)
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val type = arguments?.getString(ARG_TYPE)
        return if (type == TYPE_VIDEO) {
            inflater.inflate(R.layout.dialog_fullscreen_video, container, false)
        } else {
            inflater.inflate(R.layout.dialog_fullscreen_image, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString(ARG_TYPE)

        if (type == TYPE_VIDEO) {
            // Attach the preloaded player to the dialog's PlayerView
            val playerView = view.findViewById<PlayerView>(R.id.playerViewFullscreen)
            val pbLoading = view.findViewById<ProgressBar>(R.id.pbLoadingFullscreen)
            playerView.player = exoPlayer
            exoPlayer?.play()

            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        pbLoading.visibility = View.VISIBLE
                    } else {
                        pbLoading.visibility = View.GONE
                    }
                }
            })
            view.findViewById<View>(R.id.btnClose).setOnClickListener {
                dismiss()
            }
        } else {
            imageUrl = arguments?.getString(ARG_IMAGE_URL)
            val photoView = view.findViewById<PhotoView>(R.id.photoViewFullscreen)
            Glide.with(this)
                .load(imageUrl)
                .dontAnimate()
                .into(photoView)

            view.findViewById<View>(R.id.btnClose).setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Fullscreen
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        val type = arguments?.getString(ARG_TYPE)
        if (type == TYPE_VIDEO) {
            exoPlayer?.pause()
            // Detach from the dialog's playerView to return it to ViewHolder
            val playerView = view?.findViewById<PlayerView>(R.id.playerViewFullscreen)
            playerView?.player = null
        }
        onDismissCallback?.invoke()
    }
}