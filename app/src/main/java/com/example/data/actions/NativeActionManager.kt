package com.example.data.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.MainActivity

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Error(val errorMessage: String) : ActionResult()
    data class RequiresConfirmation(val actionTitle: String, val description: String, val execute: () -> ActionResult) : ActionResult()
}

class NativeActionManager(private val context: Context) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YESMIN AI Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and reminders from your AI Assistant"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun openApp(appKey: String): ActionResult {
        return try {
            when (appKey.lowercase()) {
                "youtube" -> launchPackageOrUrl("com.google.android.youtube", "https://youtube.com")
                "whatsapp" -> launchPackageOrUrl("com.whatsapp", "https://web.whatsapp.com")
                "telegram" -> launchPackageOrUrl("org.telegram.messenger", "https://web.telegram.org")
                "maps", "google maps" -> launchPackageOrUrl("com.google.android.apps.maps", "https://maps.google.com")
                "gmail" -> launchPackageOrUrl("com.google.android.gm", "https://mail.google.com")
                "chrome", "browser" -> launchPackageOrUrl("com.android.chrome", "https://google.com")
                "instagram" -> launchPackageOrUrl("com.instagram.android", "https://instagram.com")
                "camera" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult.Success("Opened Camera")
                }
                "calculator" -> {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_CALCULATOR)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        ActionResult.Success("Opened Calculator")
                    } catch (e: Exception) {
                        openUrl("https://www.google.com/search?q=calculator")
                    }
                }
                "calendar" -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.calendar/time/")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        ActionResult.Success("Opened Calendar")
                    } catch (e: Exception) {
                        openUrl("https://calendar.google.com")
                    }
                }
                else -> {
                    openUrl("https://google.com/search?q=${Uri.encode(appKey)}")
                }
            }
        } catch (e: Exception) {
            ActionResult.Error("Unable to open $appKey: ${e.message}")
        }
    }

    private fun launchPackageOrUrl(packageName: String, fallbackUrl: String): ActionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult.Success("Launched $packageName")
        } else {
            openUrl(fallbackUrl)
        }
    }

    fun openUrl(url: String): ActionResult {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened link: $formatted")
        } catch (e: Exception) {
            ActionResult.Error("Could not open URL: ${e.message}")
        }
    }

    fun searchWeb(query: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
            }
            ActionResult.Success("Searching for: $query")
        } catch (e: Exception) {
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun sendNotification(title: String, message: String): ActionResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), builder.build())
            ActionResult.Success("Notification posted: $title")
        } catch (e: Exception) {
            ActionResult.Error("Notification error: ${e.message}")
        }
    }

    fun dialPhone(phoneNumber: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Dialing $phoneNumber")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open dialer: ${e.message}")
        }
    }

    fun calculateMath(expression: String): String {
        return try {
            // Safe mathematical evaluation for simple expressions like 45 * 12, 100 / 4, 25 + 75
            val cleaned = expression.replace(" ", "")
            val result = evalSimpleExpression(cleaned)
            "Result: $expression = $result"
        } catch (e: Exception) {
            "Error evaluating expression: ${e.message}"
        }
    }

    private fun evalSimpleExpression(str: String): Double {
        // Simple recursive descent parser for basic arithmetic (+, -, *, /, ^)
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = Math.pow(x, parseFactor())
                return x
            }
        }.parse()
    }

    companion object {
        const val CHANNEL_ID = "yesmin_ai_channel"
    }
}
