package com.nvv.petber.utils.ext

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.nvv.petber.R

@SuppressLint("CheckResult")
fun ImageView.loadImage(
    url: String?,
    @DrawableRes placeholder: Int = R.color.colorImagePlaceholder,
    centerCrop: Boolean = true
) {
    val request = Glide.with(context)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)

    if (centerCrop) request.centerCrop()

    request.into(this)
}

@SuppressLint("CheckResult")
fun ImageView.loadImageUri(
    uri: Uri?,
    @DrawableRes placeholder: Int = R.color.colorImagePlaceholder,
    centerCrop: Boolean = true
) {
    val request = Glide.with(context)
        .load(uri)
        .placeholder(placeholder)
        .error(placeholder)

    if (centerCrop) request.centerCrop()

    request.into(this)
}

fun ImageView.loadAvatar(
    url: String?,
    @DrawableRes placeholder: Int = R.drawable.ic_default_avatar
) {
    Glide.with(context)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .transform(CircleCrop())
        .into(this)
}