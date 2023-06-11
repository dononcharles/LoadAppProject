package com.schaldrack.loadapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.schaldrack.loadapp.R
import com.schaldrack.loadapp.data.models.ButtonState
import com.schaldrack.loadapp.databinding.ActivityMainBinding
import com.schaldrack.loadapp.tools.NOTIFICATION_CHANNEL_ID
import com.schaldrack.loadapp.tools.NOTIFICATION_CHANNEL_NAME
import com.schaldrack.loadapp.tools.createChannel
import com.schaldrack.loadapp.tools.getRandomString
import com.schaldrack.loadapp.tools.sendNotification
import com.schaldrack.loadapp.tools.statusMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var downloadID: Long = 0
    private var downloadStorage: File? = null
    private var downloadFileName: String? = null
    private val downloadManager by lazy { getSystemService(DOWNLOAD_SERVICE) as DownloadManager }
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        askNotificationPermission()
        binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Completed)
        processOnItemClickEvent()
        processMenuItem()
    }

    private fun processOnItemClickEvent() {
        binding.contentMain.downloadBtn.setOnClickListener {
            if (checkIfFileIsSelected()) {
                binding.contentMain.radioGroup.requestFocus()
                Toast.makeText(this, getString(R.string.please_select_file_to_download), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            download(getSelectedFile())
        }
    }

    private fun checkIfFileIsSelected() = binding.contentMain.radioGroup.checkedRadioButtonId == -1

    private fun getSelectedFile(): Pair<String, String> = when (binding.contentMain.radioGroup.checkedRadioButtonId) {
        R.id.rb1 -> Pair(binding.contentMain.rb1.tag.toString(), binding.contentMain.rb1.text.toString())
        R.id.rb2 -> Pair(binding.contentMain.rb2.tag.toString(), binding.contentMain.rb2.text.toString())
        R.id.rb3 -> Pair(binding.contentMain.rb3.tag.toString(), binding.contentMain.rb3.text.toString())
        else -> Pair("", "")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val action = intent?.action

            if (id == downloadID && action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val success = status == DownloadManager.STATUS_SUCCESSFUL

                    if (!success) downloadManager.remove(id)

                    binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Completed)

                    notificationManager.sendNotification(
                        messageBody = downloadStorage?.let { statusMessage(it, status) } ?: getString(R.string.download_failed),
                        applicationContext = applicationContext,
                        downloadedFileName = getSelectedFile().second,
                        status = if (success) getString(R.string.success) else getString(R.string.fail),
                    )
                }
            } else {
                binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Completed)
                Toast.makeText(this@MainActivity, getString(R.string.download_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * first from Pair is the URL
     * second from Pair is the description oder file name
     */
    private fun download(selectedFile: Pair<String, String>) {
        lifecycleScope.launch {
            binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Clicked)
            delay(500) // wait to show Orange background

            // initialize notification manager
            notificationManager.createChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)

            if (selectedFile.first.isEmpty()) {
                binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Completed)
                Toast.makeText(this@MainActivity, getString(R.string.please_select_file_to_download), Toast.LENGTH_LONG).show()
                return@launch
            } else {
                binding.contentMain.downloadBtn.setLoadingButtonState(ButtonState.Loading)

                val directory = File(Environment.DIRECTORY_DOWNLOADS)
                downloadStorage = directory

                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val tempName = if (selectedFile.second.isEmpty()) "loadApp_${getRandomString()}$FILE_EXTENSION" else selectedFile.second.plus(FILE_EXTENSION)

                downloadFileName = tempName

                val request = DownloadManager.Request(Uri.parse(selectedFile.first)).apply {
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(true)
                        .setAllowedOverMetered(true)
                        .setRequiresCharging(false)
                        .setTitle(tempName)
                        .setDescription(tempName.ifEmpty { "" })
                        .setDestinationInExternalPublicDir(directory.toString(), tempName)
                }
                delay(1000)
                downloadID = downloadManager.enqueue(request) // enqueue puts the download request in the queue.
            }
        }
    }

    companion object {
        private const val FILE_EXTENSION = ".zip"
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            println("Notification granted")
        } else {
            Snackbar.make(binding.root, getString(R.string.you_can_not_get_notified), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun processMenuItem() {
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_add_download -> {
                            showDialog()
                            true
                        }

                        else -> false
                    }
                }
            },
            this,
        )
    }

    private fun showDialog() {
        val customAlertDialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_view, null, false)
        val linkEt = customAlertDialogView.findViewById<TextInputEditText>(R.id.linkEt)
        val nameEt = customAlertDialogView.findViewById<TextInputEditText>(R.id.linkNameEt)

        MaterialAlertDialogBuilder(this)
            .setView(customAlertDialogView)
            .setTitle(getString(R.string.add_file_to_download))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dia, _ ->
                val isUrlValid = Patterns.WEB_URL.matcher(linkEt?.text.toString()).matches()
                if (linkEt?.text.toString().isNotBlank() && nameEt?.text.toString().isNotBlank() && isUrlValid) {
                    download(Pair(linkEt?.text.toString(), nameEt?.text.toString()))
                    dia.dismiss()
                } else {
                    showToast(getString(R.string.please_fill_all_fields))
                }
            }
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
