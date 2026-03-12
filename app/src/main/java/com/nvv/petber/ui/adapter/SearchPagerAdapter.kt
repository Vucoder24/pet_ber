package com.nvv.petber.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nvv.petber.ui.fragment.search.tab_fragment.HashtagSearchFragment
import com.nvv.petber.ui.fragment.search.tab_fragment.PetSearchFragment
import com.nvv.petber.ui.fragment.search.tab_fragment.UserSearchFragment

class SearchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserSearchFragment()
            1 -> PetSearchFragment()
            else -> HashtagSearchFragment()
        }
    }
}