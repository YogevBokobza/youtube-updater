package com.yogev.youtubeupdater

import android.app.Application
import com.yogev.youtubeupdater.notify.Notifications
import com.yogev.youtubeupdater.work.WorkScheduler

class YouTubeUpdaterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        WorkScheduler.schedule(this)
    }
}
