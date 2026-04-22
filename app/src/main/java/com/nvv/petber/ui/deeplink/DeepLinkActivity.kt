package com.nvv.petber.ui.deeplink

import android.content.Intent
import android.os.Bundle
import com.nvv.petber.ui.activity.PostDetailActivity
import com.nvv.petber.ui.base.BaseActivity

class DeepLinkActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data

        val postId = when {
            data?.scheme == "https" -> data.lastPathSegment
            data?.scheme == "petber" -> data.lastPathSegment
            else -> null
        }

        if (postId != null) {
            val intent = Intent(this, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST_ID, postId)
            }
            startActivity(intent)
        }

        finish()
    }
}