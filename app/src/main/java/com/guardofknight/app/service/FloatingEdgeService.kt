package com.guardofknight.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.guardofknight.app.feature.fakecall.FakeCallActivity
import kotlin.math.abs

class FloatingEdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private lateinit var params: WindowManager.LayoutParams

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

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // Full width window when open to catch outside taps
            WindowManager.LayoutParams.MATCH_PARENT, // Full height window to allow free vertical dragging
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0 
            y = 0 
        }

        val container = FrameLayout(this)

        // 1. Transparent Backdrop to catch outside clicks when panel is open
        val backdrop = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(0x00000000)
        }

        // 2. The Interactive Edge Bar Container (Positioned on the right bezel)
        val edgeContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(220, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 400
            }
        }

        // The Panel Box (The dark box that appears on pull)
        val panelBox = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(180, 260).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(24f, 0f, 0f, 24f, 24f, 0f, 0f, 24f)
                setColor(0xDD111827.toInt()) 
            }
            elevation = 12f
            setPadding(16, 20, 16, 20)
            visibility = View.GONE
            alpha = 0f
        }

        // Icon inside the Panel Box
        val triggerIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(110, 110).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF2563EB.toInt())
            }
            setPadding(22, 22, 22, 22)
        }

        panelBox.addView(triggerIcon)

        // The Edge Handle bar sitting on the bezel
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(28, 160).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(12f, 12f, 0f, 0f, 0f, 0f, 12f, 12f)
                setColor(0x66FFFFFF.toInt()) 
            }
        }

        edgeContainer.addView(panelBox)
        edgeContainer.addView(edgeHandle)

        val closePanel: () -> Unit = {
            panelBox.animate().alpha(0f).setDuration(150).withEndAction {
                panelBox.visibility = View.GONE
                backdrop.visibility = View.GONE
                edgeHandle.visibility = View.VISIBLE
                
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager?.updateViewLayout(container, params)
            }.start()
        }

        val openPanel: () -> Unit = {
            edgeHandle.visibility = View.GONE
            backdrop.visibility = View.VISIBLE
            panelBox.visibility = View.VISIBLE
            panelBox.alpha = 0f
            panelBox.animate().alpha(1f).setDuration(150).start()

            params.flags = 0 // Allow touch outside to capture backdrop clicks
            windowManager?.updateViewLayout(container, params)
        }

        backdrop.setOnClickListener { closePanel() }

        triggerIcon.setOnClickListener {
            val callIntent = Intent(this@FloatingEdgeService, FakeCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(callIntent)
            closePanel()
        }

        var initialY = 0
        var initialTouchY = 0f
        var isDragging = false
        var isPulledOpen = false
        val handler = Handler(Looper.getMainLooper())
        
        var isLongPressReady = false
        val longPressRunnable = Runnable {
            isLongPressReady = true
            edgeHandle.animate().scaleY(1.2f).scaleX(1.2f).setDuration(100).start()
        }

        edgeContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val lp = edgeContainer.layoutParams as FrameLayout.LayoutParams
                    initialY = lp.topMargin
                    initialTouchY = event.rawY
                    isDragging = false
                    isLongPressReady = false
                    
                    handler.postDelayed(longPressRunnable, 400)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (isLongPressedActive(isLongPressReady, deltaY)) {
                        isDragging = true
                        val lp = edgeContainer.layoutParams as FrameLayout.LayoutParams
                        lp.topMargin = initialY + deltaY
                        edgeContainer.layoutParams = lp
                        windowManager?.updateViewLayout(container, params)
                    } 
                    else if (!isPulledOpen && !isDragging && event.rawX < event.rawX - 30) {
                        handler.removeCallbacks(longPressRunnable)
                        isPulledOpen = true
                        openPanel()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    edgeHandle.animate().scaleY(1.0f).scaleX(1.0f).setDuration(100).start()

                    if (!isDragging && !isLongPressReady && !isPulledOpen) {
                        isPulledOpen = true
                        openPanel()
                    } else if (isDragging) {
                        isDragging = false
                    }
                    isLongPressReady = false
                    true
                }
                else -> false
            }
        }

        container.addView(backdrop)
        container.addView(edgeContainer)

        floatingView = container
        windowManager?.addView(floatingView, params)
    }

    private fun isLongPressedActive(ready: Boolean, deltaY: Int): Boolean {
        return ready || abs(deltaY) > 30
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Configuration adjustments handled natively by MATCH_PARENT layout parameters
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}
