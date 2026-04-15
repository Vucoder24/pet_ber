package com.nvv.petber.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nvv.petber.ui.fragment.follow.FollowListFragment

class FollowPagerAdapter(
    activity: FragmentActivity,
    private val userId: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return FollowListFragment.newInstance(userId, position)
    }
}