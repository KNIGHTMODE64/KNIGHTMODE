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
import android.widget.TextView
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

        // Sleek Translucent Frosted Glass Panel Box (Modern Dark Grey with Border)
        val panelBox = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(180, 220).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                marginEnd = 24
            }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(28f, 28f, 28f, 28f, 28f, 28f, 28f, 28f)
                setColor(0xCC1E293B.toInt()) // Modern translucent dark slate grey
                setStroke(2, 0x44FFFFFF) // Soft glowing border outline
            }
            elevation = 16f
            setPadding(16, 16, 16, 16)
            visibility = View.GONE
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
        }

        // Clean Handle Bar on Bezel
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(22, 130).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(10f, 0f, 0f, 10f, 10f, 0f, 0f, 10f)
                setColor(0x99FFFFFF.toInt()) // Clean semi-transparent white accent bar
            }
        }

        val panelTitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 10
            }
            text = "Quick Actions"
            textSize = 11f
            setTextColor(0xBBFFFFFF.toInt())
        }

        val triggerIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(85, 85).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF2563EB.toInt())
            }
            elevation = 4f
            setPadding(18, 18, 18, 18)
        }

        panelBox.addView(panelTitle)
        panelBox.addView(triggerIcon)

        container.addView(panelBox)
        container.addView(edgeHandle)

        val closePanel: () -> Unit = {
            if (panelBox.visibility == View.VISIBLE) {
                panelBox.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(150)
                    .withEndAction {
                        panelBox.visibility = View.GONE
                        edgeHandle.visibility = View.VISIBLE
                        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        windowManager?.updateViewLayout(container, params)
                    }.start()
            }
        }

        val openPanel: () -> Unit = {
            edgeHandle.visibility = View.GONE
            panelBox.visibility = View.VISIBLE
            // Temporarily allow focus so outside touches can be detected
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            windowManager?.updateViewLayout(container, params)

            panelBox.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(180)
                .start()
        }

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
        val handler = Handler(Looper.getMainLooper())
        
        var isLongPressActive = false
        val longPressRunnable = Runnable {
            isLongPressActive = true
            edgeHandle.animate().scaleY(1.3f).scaleX(1.3f).setDuration(100).start()
        }

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    isDragging = false
                    isLongPressActive = false
                    
                    handler.postDelayed(longPressRunnable, 350)
                    true
                }
                MotionEvent.ACTION_OUTSIDE -> {
                    // Instantly closes when tapping anywhere outside the panel
                    closePanel()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (isLongPressActive && abs(deltaY) > 10) {
                        isDragging = true
                        params.y = initialY + deltaY
                        
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        
                        if (event.rawX < screenWidth / 2) {
                            params.gravity = Gravity.TOP or Gravity.START
                            params.x = 0
                        } else {
                            params.gravity = Gravity.TOP or Gravity.END
                            params.x = 0
                        }
                        
                        windowManager?.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    edgeHandle.animate().scaleY(1.0f).scaleX(1.0f).setDuration(100).start()

                    if (!isDragging && !isLongPressActive) {
                        if (panelBox.visibility == View.GONE) {
                            openPanel()
                        } else {
                            closePanel()
                        }
                    }
                    isDragging = false
                    isLongPressActive = false
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(floatingView, params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.init { }
        super.onConfigurationChanged(newConfig)
        if (windowManager != null && floatingView != null) {
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            if (params.y > screenHeight - 150) {
                params.y = screenHeight - 150
            }
            windowManager?.updateViewLayout(floatingView!!, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}
