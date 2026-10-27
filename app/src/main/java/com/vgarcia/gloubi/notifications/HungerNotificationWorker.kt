package com.vgarcia.gloubi.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vgarcia.gloubi.MainActivity
import com.vgarcia.gloubi.data.AppDb
import com.vgarcia.gloubi.data.GameConfig
import androidx.core.content.edit
import com.vgarcia.gloubi.R

class HungerNotificationWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "gloubi_hunger"
        const val NOTIF_ID_THRESHOLD = 1
        const val NOTIF_ID_EMPTY = 2
        const val PREFS_NAME = "notif_prefs"
        const val KEY_THRESHOLD_PENDING = "threshold_pending"
        const val KEY_EMPTY_PENDING = "empty_pending"
    }

    override suspend fun doWork(): Result {
        val player = AppDb.get(ctx).dao().get() ?: return Result.success()
        val now = System.currentTimeMillis()

        // Calcule la faim actuelle (elle a baissé depuis lastSeen)
        val elapsed = (now - player.lastSeen).coerceAtLeast(0L)
        val autoAdd = if (player.autoFeedUntil > now)
            GameConfig.AUTO_FEED_PER_SEC * elapsed / 1000.0 else 0.0
        val currentSatiety = (player.satiety
                - GameConfig.DECAY_PER_SEC * elapsed / 1000.0
                + autoAdd).coerceIn(0.0, 100.0)

        createChannel(ctx)

        when {
            currentSatiety <= 0.0 -> notifyIfNotPending(ctx, NOTIF_ID_EMPTY, KEY_EMPTY_PENDING,
                "Gloubi a faim ! 😢",
                "Sa jauge est vide, revenez vite le nourrir !")
            currentSatiety <= GameConfig.GAIN_THRESHOLD -> notifyIfNotPending(ctx, NOTIF_ID_THRESHOLD, KEY_THRESHOLD_PENDING,
                "Gloubi commence à avoir faim 🍖",
                "Sa jauge est sous ${GameConfig.GAIN_THRESHOLD.toInt()} %, il ne gagne plus d'XP."
            )
        }

        return Result.success()
    }

    private fun notifyIfNotPending(ctx: Context, id: Int, key: String, title: String, text: String) {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return  // déjà envoyée, pas ouverte
        notify(ctx, id, title, text)
        prefs.edit { putBoolean(key, true) }
    }

    private fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Faim de Gloubi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alertes sur la faim de Gloubi" }
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, notif)
    }
}