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

        // 1. Edge Trigger Bar (Quietly sits on the right edge of the screen)
        edgeTriggerBar = View(this).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF")) // Semi-transparent handle
            setOnClickListener {
                toggleSlideMenu(layoutFlag)
            }
        }

        val edgeParams = WindowManager.LayoutParams(
            30, 200,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.`END`
            x = 0
            y = 0
        }

        try {
            windowManager.addView(edgeTriggerBar, edgeParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleSlideMenu(layoutFlag: Int) {
        if (isMenuVisible) {
            // Close menu if already open
            slideMenuView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                }
            }
            slideMenuView = null
            isMenuVisible = false
        } else {
            // Check settings from SharedPreferences
            val prefs = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)
            val isDecoyEnabled = prefs.getBoolean("decoy_mode_enabled", true)
            // You can also add a fake call preference toggle key if desired, defaulting to true here
            val isFakeCallEnabled = prefs.getBoolean("fake_call_service_enabled", true)

            // Create Slide Menu Panel
            val menuLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#DD111827")) // Dark sleek panel background
                setPadding(20, 20, 20, 20)
                gravity = Gravity.CENTER

                // Decoy Option Button (if enabled)
                if (isDecoyEnabled) {
                    val decoyBtn = ImageView(context).apply {
                        setImageResource(android.R.drawable.ic_menu_edit) // Edit/Notes icon for Decoy
                        setColorFilter(Color.WHITE)
                        setPadding(10, 10, 10, 10)
                        setOnClickListener {
                            // Close menu and spawn the movable floating decoy icon onto the screen
                            closeSlideMenu()
                            spawnMovableDecoyIcon(layoutFlag)
                        }
                    }
                    addView(decoyBtn, LinearLayout.LayoutParams(100, 100).apply { setMargins(0, 0, 0, 20) })
                }

                // Fake Call Option Button (if enabled)
                if (isFakeCallEnabled) {
                    val callBtn = ImageView(context).apply {
                        setImageResource(android.R.drawable.ic_menu_call) // Call icon for Fake Call
                        setColorFilter(Color.WHITE)
                        setPadding(10, 10, 10, 10)
                        setOnClickListener {
                            closeSlideMenu()
                            val intent = Intent(context, FakeCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    }
                    addView(callBtn, LinearLayout.LayoutParams(100, 100))
                }
            }

            val menuParams = WindowManager.LayoutParams(
                140,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.`END`
                x = 40
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
        // If already on screen, don't spawn duplicate
        if (floatingDecoyIcon != null) return

        val iconView = LinearLayout(this).apply {
            // Transparent background look that the user can place anywhere
            setBackgroundColor(Color.parseColor("#882563EB")) 
            setPadding(20, 20, 20, 20)
            gravity = Gravity.CENTER

            val innerIcon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setColorFilter(Color.WHITE)
            }
            addView(innerIcon, LinearLayout.LayoutParams(80, 80))
        }

        val iconParams = WindowManager.LayoutParams(
            120, 120,
            layoutFlag,
            // FLAG_NOT_FOCUSABLE allows interaction, but we handle touch manually for dragging
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 300
            y = 500
        }

        // Add touch listener to make the icon freely movable and support long-press to hide
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

        // Tap normally to trigger Decoy Activity
        iconView.setOnClickListener {
            if (!isMoved) {
                val intent = Intent(context, DecoyActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
        }

        // Long press to hide the decoy icon back into the slide tray
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
