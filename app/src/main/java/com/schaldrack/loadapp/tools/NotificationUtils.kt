/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.schaldrack.loadapp.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.schaldrack.loadapp.R
import com.schaldrack.loadapp.ui.DetailActivity
import com.schaldrack.loadapp.ui.DetailActivity.Companion.DOWNLOAD_FILE_NAME
import com.schaldrack.loadapp.ui.DetailActivity.Companion.DOWNLOAD_STATUS

private const val NOTIFICATION_ID = 0

/**
 * Builds and delivers the notification.
 */
fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context, downloadedFileName: String, status: String) {
    val contentIntent = Intent(applicationContext, DetailActivity::class.java).apply {
        putExtra(DOWNLOAD_FILE_NAME, downloadedFileName)
        putExtra(DOWNLOAD_STATUS, status)
    }

    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // Build the notification
    val build = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo32)
        .setContentTitle(applicationContext.getString(R.string.notification_title))
        .setContentText(messageBody)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)

    build.addAction(R.drawable.baseline_cloud_download_24, "Check the status", contentPendingIntent)

    build.priority = NotificationCompat.PRIORITY_HIGH

    notify(NOTIFICATION_ID, build.build())
}

fun NotificationManager.cancelNotifications() {
    cancelAll()
}

fun NotificationManager.createChannel(channelId: String, channelName: String) {
    val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
        setShowBadge(false)
    }

    notificationChannel.enableLights(true)
    notificationChannel.lightColor = Color.RED
    notificationChannel.enableVibration(true)
    notificationChannel.description = "File download status"
    createNotificationChannel(notificationChannel)
}
