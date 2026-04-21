package com.nvv.petber.utils.ext

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.autoHeight() {
    post {
        val view = (getChildAt(0) as RecyclerView)
        view.addOnScrollListener(object : RecyclerView.OnScrollListener() {})
    }
}