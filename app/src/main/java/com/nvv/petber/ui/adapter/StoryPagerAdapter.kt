package com.nvv.petber.ui.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.ui.view_story.fragment.StoryOwnerFragment
import com.nvv.petber.ui.view_story.fragment.StoryUserFragment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StoryPagerAdapter(
    activity: AppCompatActivity,
    private val groups: List<UserStoryGroup>,
    private val currentUserId: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = groups.size

    override fun createFragment(position: Int): Fragment {
        val group = groups[position]
        val jsonGroup = Json.encodeToString(group)

        return if (group.userId == currentUserId) {
            StoryOwnerFragment.newInstance(jsonGroup)
        } else {
            StoryUserFragment.newInstance(jsonGroup)
        }
    }
}