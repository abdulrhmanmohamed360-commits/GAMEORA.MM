package com.gameora.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gameora.GameoraApp
import com.gameora.R
import com.gameora.ui.common.Nav
import com.gameora.ui.notifications.NotificationsActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يستقبل Push Notifications حقيقية (FCM) جاية من الباكيند،
 * حتى لو التطبيق مقفول أو شغال في الخلفية، ويعرضها كإشعار نظام
 * حقيقي في شريط الإشعارات. الضغط على الإشعار بيفتح شاشة
 * الإشعارات وبيحاول يفتح نفس محادثة الطلب مباشرة لو orderId
 * موجود - بدون إنشاء أي محادثة جديدة.
 */
class GameoraMessagingService : FirebaseMessagingService() {

    private val channelId = "gameora_orders"

    /**
     * بيتنادى كل ما الـ FCM token بتاع الجهاز ده يتغيّر (بيحصل
     * أحيانًا حتى لو المستخدم مسجّل دخول بالفعل). لو المستخدم
     * مسجّل دخول، نبعت التوكن الجديد للباكيند فورًا. لو مش
     * مسجّل دخول، هيتسجل تلقائيًا بعد كده أول ما يسجل دخول
     * (شوف AuthRepository.syncGameoraUser).
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val container = GameoraApp.get().container

        if (container.sessionManager.isLoggedIn.value != true) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.apiService.registerFcmToken(mapOf("token" to token))
            } catch (_: Throwable) {
                // best effort فقط - هيتحاول تاني عند أي login/restore لاحق.
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)

        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val orderId = message.data["orderId"]?.takeIf { it.isNotBlank() }

        showSystemNotification(title, body, orderId)
    }

    private fun showSystemNotification(title: String, body: String, orderId: String?) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
        }

        val intent = Intent(this, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!orderId.isNullOrBlank()) {
                putExtra(Nav.ORDER_ID, orderId)
            }
        }

        val requestCode = (orderId ?: System.currentTimeMillis().toString()).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(requestCode, notification)
    }
}
