package com.guardofknight.app.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.guardofknight.app.DecoyActivity

class FloatingEdgeService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var edgeTriggerBar: View
    private var floatingPanicIcon: View? = null
    private var isIconVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Create the Edge Slide Bar (sits quietly on the right edge)
        edgeTriggerBar = View(this).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF")) // Semi-transparent edge tab
            setOnClickListener {
                // Tapping the edge bar toggles the floating icon on/off the screen
                toggleFloatingIcon(layoutFlag)
            }
        }

        val edgeParams = WindowManager.LayoutParams(
            30, // Thin width
            200, // Height of the slide bar
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = 0
        }

        try {
            windowManager.addView(edgeTriggerBar, edgeParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFloatingIcon(layoutFlag: Int) {
        if (isIconVisible) {
            // If the icon is already outside, tapping the edge bar again makes it DISAPPEAR
            floatingPanicIcon?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            floatingPanicIcon = null
            isIconVisible = false
        } else {
            // If hidden, tapping the edge bar PLACES THE ICON OUTSIDE near the middle-right area (away from camera)
            val iconView = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#CC2563EB")) // Blue panic button style
                setPadding(20, 20, 20, 20)
                
                // Clicking the outside floating icon triggers the Decoy Notes page instantly!
                setOnClickListener {
                    val intent = Intent(context, DecoyActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
            }

            val iconParams = WindowManager.LayoutParams(
                130, // Comfortable size of the floating icon
                130,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                // Positioned cleanly on the middle-right area, offset away from the edge bar and top camera
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                x = 180 // Pushes it inward from the right edge so it's easy to tap
                y = 0   // Perfectly centered vertically
            }

            try {
                windowManager.addView(iconView, iconParams)
                floatingPanicIcon = iconView
                isIconVisible = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::edgeTriggerBar.isInitialized) {
                windowManager.removeView(edgeTriggerBar)
            }
            floatingPanicIcon?.let {
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
