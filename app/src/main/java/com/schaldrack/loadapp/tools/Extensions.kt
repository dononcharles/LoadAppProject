package com.schaldrack.loadapp.tools

import android.app.DownloadManager
import java.io.File

/**
 * @author Komi Donon
 * @since 6/10/2023
 */

fun getRandomString(): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..5).map { allowedChars.random() }.joinToString("")
}

fun statusMessage(directory: File, status: Int) = when (status) {
    DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
    DownloadManager.STATUS_PAUSED -> "Paused"
    DownloadManager.STATUS_PENDING -> "Pending"
    DownloadManager.STATUS_RUNNING -> "Downloading..."
    DownloadManager.STATUS_SUCCESSFUL -> "File downloaded successfully in $directory" + File.separator + " folder"
    else -> "There's nothing to download"
}
