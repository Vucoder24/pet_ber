package com.nvv.petber.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nvv.petber.ui.fragment.pet_profile.AllPetPostsFragment
import com.nvv.petber.ui.fragment.pet_profile.PetDiaryFragment

class PetProfilePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllPetPostsFragment()
            1 -> PetDiaryFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}