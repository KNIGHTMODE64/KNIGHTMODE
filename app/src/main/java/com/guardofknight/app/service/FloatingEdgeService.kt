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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.guardofknight.app.feature.fakecall.FakeCallActivity

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

        // Keep window layout compact so it never shifts or centers incorrectly
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

        // 1. Invisible touch catcher to dismiss panel on outside taps
        val outsideDismissView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(0x00000000)
        }

        // 2. Premium Frosted Glass Panel Box (Aligned correctly to the edge)
        val panelBox = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(180, 210).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                marginEnd = 16
            }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 24f, 24f, 24f, 24f)
                setColor(0xE61E293B.toInt()) // Modern semi-transparent dark frosted grey
                setStroke(2, 0x55FFFFFF) // Subtle glowing outline border
            }
            elevation = 16f
            setPadding(16, 14, 16, 14)
            visibility = View.GONE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
        }

        // 3. Clean Edge Handle Bar sitting flush on the bezel
        val edgeHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(20, 120).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(8f, 0f, 0f, 8f, 8f, 0f, 0f, 8f)
                setColor(0xB3FFFFFF.toInt()) // Sleek semi-transparent accent bar
            }
        }

        val panelTitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 8
            }
            text = "Quick Actions"
            textSize = 11f
            setTextColor(0xCCFFFFFF.toInt())
        }

        val triggerIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setImageResource(android.R.drawable.ic_menu_call)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF2563EB.toInt())
            }
            elevation = 4f
            setPadding(16, 16, 16, 16)
        }

        panelBox.addView(panelTitle)
        panelBox.addView(triggerIcon)

        container.addView(outsideDismissView)
        container.addView(panelBox)
        container.addView(edgeHandle)

        val closePanel: () -> Unit = {
            if (panelBox.visibility == View.VISIBLE) {
                panelBox.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(150)
                    .withEndAction {
                        panelBox.visibility = View.GONE
                        outsideDismissView.visibility = View.GONE
                        edgeHandle.visibility = View.VISIBLE
                        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        windowManager?.updateViewLayout(container, params)
                    }.start()
            }
        }

        val openPanel: () -> Unit = {
            edgeHandle.visibility = View.GONE
            outsideDismissView.visibility = View.VISIBLE
            panelBox.visibility = View.VISIBLE
            
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            windowManager?.updateViewLayout(container, params)

            panelBox.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(180)
                .start()
        }

        outsideDismissView.setOnClickListener {
            closePanel()
        }

        triggerIcon.setOnClickListener {
            val callIntent = Intent(this@FloatingEdgeService, FakeCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(callIntent)
            closePanel()
        }

        edgeHandle.setOnClickListener {
            if (panelBox.visibility == View.GONE) {
                openPanel()
            } else {
                closePanel()
            }
        }

        floatingView = container
        windowManager?.addView(floatingView, params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
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
