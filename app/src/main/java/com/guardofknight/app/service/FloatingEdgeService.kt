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
import kotlin.math.abs

class FloatingEdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(101, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, createNotification())
        }
        createFloatingHandle()
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

    private fun createFloatingHandle() {
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
            gravity = Gravity.TOP or Gravity.START
            x = 1000 
            y = 500  
        }

        val container = FrameLayout(this)

        // 1. The visible grey slide bar handle
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(48, 260).apply {
                gravity = Gravity.CENTER
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24f
                // Clear, visible grey color mixed into the screen
                setColor(0xAA777777.toInt()) 
            }
        }

        // 2. The trigger button icon (appears when tapped)
        val triggerIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(130, 130).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xEE2563EB.toInt()) // Solid blue button
            }
            setPadding(28, 28, 28, 28)
            visibility = View.GONE

            setOnClickListener {
                val callIntent = Intent(this@FloatingEdgeService, FakeCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(callIntent)

                // Hide the icon back to normal bar
                visibility = View.GONE
                edgeHandle.visibility = View.VISIBLE
                isExpanded = false
            }
        }

        container.addView(edgeHandle)
        container.addView(triggerIcon)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    if (abs(deltaX) > 8 || abs(deltaY) > 8) {
                        isDragging = true
                    }

                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Toggle between slide bar and call button on tap
                        if (!isExpanded) {
                            edgeHandle.visibility = View.GONE
                            triggerIcon.visibility = View.VISIBLE
                            isExpanded = true
                        } else {
                            triggerIcon.visibility = View.GONE
                            edgeHandle.visibility = View.VISIBLE
                            isExpanded = false
                        }
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
