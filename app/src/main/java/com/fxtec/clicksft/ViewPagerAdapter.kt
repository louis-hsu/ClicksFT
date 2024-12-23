package com.fxtec.clicksft

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private val fragments = mutableMapOf<Int, Fragment>()
    private var currentPosition: Int = 0

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> CommandFragment()
            1 -> FirmwareFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
        fragments[position] = fragment
        return fragment
    }

    fun getCurrentFragment(): Fragment? {
        return fragments[currentPosition]
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
    }
}