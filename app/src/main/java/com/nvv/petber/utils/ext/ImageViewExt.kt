package com.nvv.petber.utils.ext

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.nvv.petber.R
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.get
import com.bumptech.glide.load.DecodeFormat

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
        .override(600, 600)
        .format(DecodeFormat.PREFER_RGB_565)

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


fun ImageView.loadMediaCoverWithExtremeGradient(
    ctx: Context,
    uri: Uri,
    isVideo: Boolean,
    backgroundView: View,
    scope: LifecycleCoroutineScope,
) {
    if (isVideo) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                extractVideoFrame(ctx, uri)
            }

            bitmap?.let {
                setImageBitmap(it)
                applyExtremeGradient(it, backgroundView)
            }
        }
    } else {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .override(300, 300)
            .format(DecodeFormat.PREFER_RGB_565)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    setImageBitmap(resource)
                    applyExtremeGradient(resource, backgroundView)
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
    }
}

private fun extractVideoFrame(cxt: Context, uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(cxt, uri)

        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {}
    }
}

private fun applyExtremeGradient(bitmap: Bitmap, backgroundView: View) {
    val (darkColor, lightColor) = extractBetterGradientColors(bitmap)

    val gradient = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(darkColor, lightColor)
    ).apply {
        cornerRadius = 0f
    }

    backgroundView.background = gradient
}

private fun extractBetterGradientColors(bitmap: Bitmap): Pair<Int, Int> {
    val width = bitmap.width
    val height = bitmap.height

    val stepX = maxOf(1, width / 50)
    val stepY = maxOf(1, height / 50)

    val colors = mutableListOf<Pair<Int, Double>>() // color + luminance

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val color = bitmap[x, y]
            val lum = luminance(color)

            if (lum in 0.05..0.95) {
                colors.add(color to lum)
            }

            x += stepX
        }
        y += stepY
    }

    if (colors.isEmpty()) {
        return Color.BLACK to Color.GRAY
    }

    // sort theo độ sáng
    val sorted = colors.sortedBy { it.second }

    val dark = adjustColor(sorted.first().first, 0.8f)
    val light = adjustColor(sorted.last().first, 1.2f)

    return dark to light
}

private fun luminance(color: Int): Double {
    val r = Color.red(color) / 255.0
    val g = Color.green(color) / 255.0
    val b = Color.blue(color) / 255.0

    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun adjustColor(color: Int, factor: Float): Int {
    val r = (Color.red(color) * factor).coerceIn(0f, 255f).toInt()
    val g = (Color.green(color) * factor).coerceIn(0f, 255f).toInt()
    val b = (Color.blue(color) * factor).coerceIn(0f, 255f).toInt()
    return Color.rgb(r, g, b)
}