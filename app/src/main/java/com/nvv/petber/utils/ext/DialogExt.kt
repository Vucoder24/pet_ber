package com.nvv.petber.utils.ext

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nvv.petber.R

fun Context.showLogoutConfirmDialog(
    onConfirm: () -> Unit
) {
    val dialog = BottomSheetDialog(this)
    val view = LayoutInflater.from(this).inflate(R.layout.layout_dialog_logout, null)

    val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
    val btnLogout = view.findViewById<TextView>(R.id.btnLogout)

    btnCancel.setOnClickListener {
        dialog.dismiss()
    }

    btnLogout.setOnClickListener {
        onConfirm()
        dialog.dismiss()
    }

    dialog.setContentView(view)
    dialog.show()
}

fun Context.showAvatarOptionDialog(
    onViewAvatar:() -> Unit,
    onChooseAvatar: () -> Unit
){
    val dialog = BottomSheetDialog(this)
    val view = LayoutInflater.from(this).inflate(R.layout.layout_dialog_option_avatar, null)

    val tvView = view.findViewById<LinearLayout>(R.id.itemViewAvatar)
    val tvSelect = view.findViewById<LinearLayout>(R.id.itemChooseAvatar)


    tvView.setOnClickListener {
        onViewAvatar()
        dialog.dismiss()
    }

    tvSelect.setOnClickListener {
        onChooseAvatar()
        dialog.dismiss()
    }

    dialog.setContentView(view)
    dialog.show()
}

fun Context.showCoverOptionDialog(
    onViewCover:() -> Unit,
    onChooseCover: () -> Unit
){
    val dialog = BottomSheetDialog(this)
    val view = LayoutInflater.from(this).inflate(R.layout.layout_dialog_option_cover, null)

    val tvView = view.findViewById<LinearLayout>(R.id.itemViewCover)
    val tvSelect = view.findViewById<LinearLayout>(R.id.itemChooseCover)


    tvView.setOnClickListener {
        onViewCover()
        dialog.dismiss()
    }

    tvSelect.setOnClickListener {
        onChooseCover()
        dialog.dismiss()
    }

    dialog.setContentView(view)
    dialog.show()
}