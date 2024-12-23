package com.fxtec.clicksft

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var xmlFileUri: Uri? = null
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            xmlFileUri = it
            viewPagerAdapter.getCurrentFragment()?.let { fragment ->
                if (fragment is CommandFragment) {
                    fragment.updateXmlFile(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPagerAdapter.setCurrentPosition(position)
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Command"
                1 -> "Firmware"
                else -> ""
            }
        }.attach()

        checkPersistedXmlFile()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPager.unregisterOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
    }

    private fun checkPersistedXmlFile() {
        contentResolver.persistedUriPermissions.firstOrNull()?.uri?.let {
            xmlFileUri = it
        }
    }

    fun openXmlFilePicker() {
        openDocument.launch(arrayOf("text/xml"))
    }

    fun getXmlFileUri(): Uri? = xmlFileUri
}