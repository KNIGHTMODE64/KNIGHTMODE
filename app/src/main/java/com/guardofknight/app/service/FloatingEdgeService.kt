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

        // Locked to the right edge, allowing vertical up/down dragging along the bezel
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
            gravity = Gravity.TOP or Gravity.END
            x = 0 
            y = 400 
        }

        val container = FrameLayout(this)

        // 1. The edge handle bar (hugs the bezel)
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(32, 200).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(16f, 16f, 0f, 0f, 0f, 0f, 16f, 16f)
                setColor(0x44888888.toInt()) 
            }
        }

        // 2. The call icon that slides OUT onto the screen when pulled
        val triggerIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                marginEnd = 16
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xEE2563EB.toInt())
            }
            setPadding(24, 24, 24, 24)
            translationX = 150f // Hidden off-screen to the right initially
            visibility = View.GONE

            setOnClickListener {
                val callIntent = Intent(this@FloatingEdgeService, FakeCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(callIntent)

                // Tuck back away after triggering call
                animate().translationX(150f).setDuration(200).withEndAction {
                    visibility = View.GONE
                    edgeHandle.visibility = View.VISIBLE
                }.start()
            }
        }

        container.addView(edgeHandle)
        container.addView(triggerIcon)

        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var isPulledOpen = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = initialTouchX - event.rawX
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    // If moving vertically, drag the bar UP or DOWN along the edge
                    if (abs(deltaY) > 12 && !isPulledOpen) {
                        isDragging = true
                        params.y = initialY + deltaY
                        windowManager?.updateViewLayout(container, params)
                    } 
                    // If pulled horizontally inward from the right edge
                    else if (deltaX > 35 && !isPulledOpen && !isDragging) {
                        isPulledOpen = true
                        edgeHandle.visibility = View.GONE
                        triggerIcon.visibility = View.VISIBLE
                        triggerIcon.animate().translationX(-30f).setDuration(150).start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // If it wasn't dragged vertically, support a tap toggle to open/close
                    if (!isDragging && abs(initialTouchX - event.rawX) < 15) {
                        if (!isPulledOpen) {
                            isPulledOpen = true
                            edgeHandle.visibility = View.GONE
                            triggerIcon.visibility = View.VISIBLE
                            triggerIcon.animate().translationX(-30f).setDuration(150).start()
                        } else {
                            isPulledOpen = false
                            triggerIcon.animate().translationX(150f).setDuration(150).withEndAction {
                                triggerIcon.visibility = View.GONE
                                edgeHandle.visibility = View.VISIBLE
                            }.start()
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
