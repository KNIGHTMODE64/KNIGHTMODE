package com.guardofknight.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.guardofknight.app.DecoyActivity
import com.guardofknight.app.R
import com.guardofknight.app.feature.fakecall.FakeCallActivity

class FloatingEdgeService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var edgeTriggerBar: View
    private var slideMenuView: View? = null
    private var floatingDecoyIcon: View? = null
    private var isMenuVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MUST call startForeground immediately to avoid Android RemoteServiceException crash
        startForeground(1, createPersistentNotification())

        setupEdgeTriggerBar()

        return START_STICKY
    }

    private fun createPersistentNotification(): Notification {
        val channelId = "floating_edge_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Edge Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the edge trigger active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("KnightMode Active")
            .setContentText("Edge trigger is running.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupEdgeTriggerBar() {
        // Prevent re-adding if already initialized
        if (::edgeTriggerBar.isInitialized && edgeTriggerBar.parent != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        edgeTriggerBar = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#993B82F6"))
                cornerRadius = 12f
            }
        }

        val edgeParams = WindowManager.LayoutParams(
            35, 220,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = 0
        }

        var initialY = 0f
        var isDragging = false

        edgeTriggerBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    if (Math.abs(deltaY) > 25) {
                        isDragging = true
                        if (!isMenuVisible) {
                            toggleSlideMenu(layoutFlag)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleSlideMenu(layoutFlag)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(edgeTriggerBar, edgeParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleSlideMenu(layoutFlag: Int) {
        if (isMenuVisible) {
            closeSlideMenu()
        } else {
            val serviceContext = this
            val prefs = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)
            val isDecoyEnabled = prefs.getBoolean("decoy_mode_enabled", true)
            val isFakeCallEnabled = prefs.getBoolean("fake_call_enabled", true)

            val menuLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#F50F172A"))
                    cornerRadius = 24f
                    setStroke(2, Color.parseColor("#334155"))
                }
                setPadding(16, 20, 16, 20)
                gravity = Gravity.CENTER

                // Decoy Option Button
                if (isDecoyEnabled) {
                    val decoyContainer = LinearLayout(serviceContext).apply {
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#1E293B"))
                        }
                        setPadding(22, 22, 22, 22)

                        val decoyIcon = ImageView(serviceContext).apply {
                            setImageResource(android.R.drawable.ic_secure)
                            setColorFilter(Color.parseColor("#38BDF8"))
                        }
                        addView(decoyIcon, LinearLayout.LayoutParams(64, 64))

                        setOnClickListener {
                            closeSlideMenu()
                            spawnMovableDecoyIcon(layoutFlag)
                        }
                    }
                    addView(decoyContainer, LinearLayout.LayoutParams(100, 100).apply { setMargins(0, 0, 0, 16) })
                }

                // Fake Call Option Button
                if (isFakeCallEnabled) {
                    val callContainer = LinearLayout(serviceContext).apply {
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#1E293B"))
                        }
                        setPadding(22, 22, 22, 22)

                        val callIcon = ImageView(serviceContext).apply {
                            setImageResource(android.R.drawable.ic_menu_call)
                            setColorFilter(Color.parseColor("#4ADE80"))
                        }
                        addView(callIcon, LinearLayout.LayoutParams(64, 64))

                        setOnClickListener {
                            closeSlideMenu()
                            val intent = Intent(serviceContext, FakeCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            serviceContext.startActivity(intent)
                        }
                    }
                    addView(callContainer, LinearLayout.LayoutParams(100, 100))
                }
            }

            val menuParams = WindowManager.LayoutParams(
                140,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                x = 24
                y = 0
            }

            try {
                windowManager.addView(menuLayout, menuParams)
                slideMenuView = menuLayout
                isMenuVisible = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeSlideMenu() {
        slideMenuView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
            }
        }
        slideMenuView = null
        isMenuVisible = false
    }

    private fun spawnMovableDecoyIcon(layoutFlag: Int) {
        if (floatingDecoyIcon != null) return
        val serviceContext = this

        val iconView = LinearLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC2563EB"))
                setStroke(3, Color.WHITE)
            }
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER

            val innerIcon = ImageView(serviceContext).apply {
                setImageResource(android.R.drawable.ic_secure)
                setColorFilter(Color.WHITE)
            }
            addView(innerIcon, LinearLayout.LayoutParams(70, 70))
        }

        val iconParams = WindowManager.LayoutParams(
            130, 130,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 250
            y = 400
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        iconView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = iconParams.x
                        initialY = iconParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isMoved = true
                        }
                        iconParams.x = initialX + dx
                        iconParams.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(iconView, iconParams)
                        } catch (e: Exception) {
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            val intent = Intent(serviceContext, DecoyActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            serviceContext.startActivity(intent)
                        }
                        return true
                    }
                    else -> return false
                }
            }
        })

        iconView.setOnLongClickListener {
            try {
                windowManager.removeView(iconView)
            } catch (e: Exception) {
            }
            floatingDecoyIcon = null
            true
        }

        try {
            windowManager.addView(iconView, iconParams)
            floatingDecoyIcon = iconView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::edgeTriggerBar.isInitialized && edgeTriggerBar.parent != null) {
                windowManager.removeView(edgeTriggerBar)
            }
            slideMenuView?.let { windowManager.removeView(it) }
            floatingDecoyIcon?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
