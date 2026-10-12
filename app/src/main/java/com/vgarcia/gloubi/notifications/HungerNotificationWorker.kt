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

class HungerNotificationWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "gloubi_hunger"
        const val NOTIF_ID_THRESHOLD = 1
        const val NOTIF_ID_EMPTY = 2
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
            currentSatiety <= 0.0 -> notify(
                ctx,
                NOTIF_ID_EMPTY,
                "Gloubi a faim ! 😢",
                "Sa jauge est vide, revenez vite le nourrir !"
            )
            currentSatiety <= GameConfig.GAIN_THRESHOLD -> notify(
                ctx,
                NOTIF_ID_THRESHOLD,
                "Gloubi commence à avoir faim 🍖",
                "Sa jauge est sous ${GameConfig.GAIN_THRESHOLD.toInt()} %, il ne gagne plus d'XP."
            )
        }

        return Result.success()
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, notif)
    }
}