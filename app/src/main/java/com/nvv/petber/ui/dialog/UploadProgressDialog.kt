package com.nvv.petber.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.nvv.petber.databinding.DialogUploadProgressBinding

class UploadProgressDialog(context: Context) {

    private val binding = DialogUploadProgressBinding.inflate(LayoutInflater.from(context))

    private val dialog = Dialog(context).apply {
        setContentView(binding.root)
        setCancelable(false)
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            attributes?.apply {
                width = android.view.WindowManager.LayoutParams.MATCH_PARENT
                height = android.view.WindowManager.LayoutParams.MATCH_PARENT
            }
        }
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    @SuppressLint("SetTextI18n")
    fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.tvProgressPercent.text = "$progress%"
    }
}