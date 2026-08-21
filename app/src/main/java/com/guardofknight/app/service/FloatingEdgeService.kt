package com.guardofknight.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.guardofknight.app.feature.fakecall.FakeCallActivity

class FloatingEdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(101, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, createNotification())
        }
        createEdgeHandle()
    }

    private fun createNotification(): Notification {
        val channelId = "guardofknight_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GuardOfKnight Guard",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GuardOfKnight Active")
            .setContentText("Edge trigger is active on screen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun createEdgeHandle() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0 
            y = 0  
        }

        val container = FrameLayout(this)

        // 1. Ultra-low visibility edge pull tab (blends smoothly into the screen bezel)
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(28, 200).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(16f, 16f, 0f, 0f, 0f, 0f, 16f, 16f)
                setColor(0x33888888.toInt()) // Very low visibility alpha tint
            }
        }

        // 2. The call icon that pops out when pulled
        val triggerIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                marginEnd = -120 
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xEE2563EB.toInt())
            }
            setPadding(24, 24, 24, 24)
            visibility = View.GONE

            setOnClickListener {
                val callIntent = Intent(this@FloatingEdgeService, FakeCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(callIntent)

                visibility = View.GONE
                triggerIcon.animate().translationX(0f).setDuration(200).start()
            }
        }

        container.addView(edgeHandle)
        container.addView(triggerIcon)

        var initialTouchX = 0f
        var isPulled = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = initialTouchX - event.rawX 
                    if (deltaX > 30 && !isPulled) {
                        isPulled = true
                        triggerIcon.visibility = View.VISIBLE
                        triggerIcon.animate().translationX(-100f).setDuration(150).start()
                    } else if (deltaX < -30 && isPulled) {
                        isPulled = false
                        triggerIcon.animate().translationX(0f).setDuration(150).withEndAction {
                            triggerIcon.visibility = View.GONE
                        }.start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isPulled) {
                        isPulled = true
                        triggerIcon.visibility = View.VISIBLE
                        triggerIcon.animate().translationX(-100f).setDuration(150).start()
                    } else {
                        isPulled = false
                        triggerIcon.animate().translationX(0f).setDuration(150).withEndAction {
                            triggerIcon.visibility = View.GONE
                        }.start()
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}
