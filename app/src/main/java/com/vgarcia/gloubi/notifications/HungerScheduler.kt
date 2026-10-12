package com.vgarcia.gloubi.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HungerScheduler {

    private const val WORK_NAME = "hunger_check"

    fun schedule(ctx: Context) {
        val request = PeriodicWorkRequestBuilder<HungerNotificationWorker>(
            15, TimeUnit.MINUTES   // minimum autorisé par WorkManager
        ).build()

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }
}