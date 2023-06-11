package com.schaldrack.loadapp.ui

import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.schaldrack.loadapp.R
import com.schaldrack.loadapp.databinding.ActivityDetailBinding
import com.schaldrack.loadapp.tools.cancelNotifications

class DetailActivity : AppCompatActivity() {

    companion object {
        const val DOWNLOAD_FILE_NAME = "downloadedFileName"
        const val DOWNLOAD_STATUS = "status"
    }

    private lateinit var binding: ActivityDetailBinding
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.downloaded_file_details)
        processIntentExtra()
    }

    private fun processIntentExtra() {
        val downloadedFileName = intent.getStringExtra(DOWNLOAD_FILE_NAME)
        val status = intent.getStringExtra(DOWNLOAD_STATUS)

        binding.cd.fileNameValue.text = downloadedFileName

        if (status == getString(R.string.success)) {
            binding.cd.statusValue.text = getString(R.string.success)
            binding.cd.statusValue.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.cd.statusValue.text = getString(R.string.fail)
            binding.cd.statusValue.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        notificationManager.cancelNotifications()

        binding.cd.goBack.setOnClickListener {
            finish()
        }
    }
}
