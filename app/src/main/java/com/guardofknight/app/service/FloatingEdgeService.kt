package com.guardofknight.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.guardofknight.app.DecoyActivity
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

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Sleek Edge Trigger Handle on the right side
        edgeTriggerBar = View(this).apply {
            setBackgroundColor(Color.parseColor("#553B82F6")) // Modern glowing blue tab
        }

        val edgeParams = WindowManager.LayoutParams(
            35, 250,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = 0
        }

        // Add true slide/swipe gesture recognition to pull out the menu
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
                    if (Math.abs(deltaY) > 30) { // If user slides up/down or pulls inward
                        isDragging = true
                        if (!isMenuVisible) {
                            toggleSlideMenu(layoutFlag)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Regular tap also toggles it if they didn't slide
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
            val isFakeCallEnabled = prefs.getBoolean("fake_call_service_enabled", true)

            // Create a gorgeous, modern floating action tray
            val menuLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#E60F172A")) // Premium dark glassmorphism style
                setPadding(16, 24, 16, 24)
                gravity = Gravity.CENTER

                // Decoy Trigger Option
                if (isDecoyEnabled) {
                    val decoyBtn = ImageView(serviceContext).apply {
                        setImageResource(android.R.drawable.ic_secure) // Security shield/lock icon
                        setColorFilter(Color.parseColor("#38BDF8")) // Bright sky blue accent
                        setPadding(16, 16, 16, 16)
                        setBackgroundColor(Color.parseColor("#331E293B"))
                        setOnClickListener {
                            closeSlideMenu()
                            spawnMovableDecoyIcon(layoutFlag)
                        }
                    }
                    addView(decoyBtn, LinearLayout.LayoutParams(110, 110).apply { setMargins(0, 0, 0, 20) })
                }

                // Fake Call Trigger Option
                if (isFakeCallEnabled) {
                    val callBtn = ImageView(serviceContext).apply {
                        setImageResource(android.R.drawable.ic_menu_call) // Phone call icon
                        setColorFilter(Color.parseColor("#4ADE80")) // Bright green accent
                        setPadding(16, 16, 16, 16)
                        setBackgroundColor(Color.parseColor("#331E293B"))
                        setOnClickListener {
                            closeSlideMenu()
                            val intent = Intent(serviceContext, FakeCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            serviceContext.startActivity(intent)
                        }
                    }
                    addView(callBtn, LinearLayout.LayoutParams(110, 110))
                }
            }

            val menuParams = WindowManager.LayoutParams(
                150,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                x = 30
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

        // Floating movable panic icon
        val iconView = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#992563EB")) // Semi-transparent professional blue
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER

            val innerIcon = ImageView(serviceContext).apply {
                setImageResource(android.R.drawable.ic_secure)
                setColorFilter(Color.WHITE)
            }
            addView(innerIcon, LinearLayout.LayoutParams(80, 80))
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
                        return false
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
                    else -> return false
                }
            }
        })

        // Tap normally to trigger the Decoy Activity
        iconView.setOnClickListener {
            if (!isMoved) {
                val intent = Intent(serviceContext, DecoyActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                serviceContext.startActivity(intent)
            }
        }

        // Long press to dismiss the floating icon back into the slide menu
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
            if (::edgeTriggerBar.isInitialized) windowManager.removeView(edgeTriggerBar)
            slideMenuView?.let { windowManager.removeView(it) }
            floatingDecoyIcon?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
