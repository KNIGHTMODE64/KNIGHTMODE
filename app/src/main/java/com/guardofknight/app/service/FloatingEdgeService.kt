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
import android.widget.ImageView
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
                toggleFloatingIcon(layoutFlag)
            }
        }

        val edgeParams = WindowManager.LayoutParams(
            30,
            200,
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
            // Create a styled floating panic button layout containing an image icon
            val iconContainer = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#CC1E293B")) // Sleek dark slate background
                setPadding(24, 24, 24, 24)
                gravity = Gravity.CENTER
                
                // Add a visible inner icon so it doesn't look like an empty box
                val iconView = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_edit) // Clean built-in notepad/edit icon
                    setColorFilter(Color.WHITE)
                }
                addView(iconView, LinearLayout.LayoutParams(70, 70))

                // Clicking the floating icon triggers the Decoy Notes page instantly!
                setOnClickListener {
                    val intent = Intent(context, DecoyActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
            }

            val iconParams = WindowManager.LayoutParams(
                130,
                130,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                x = 180
                y = 0
            }

            try {
                windowManager.addView(iconContainer, iconParams)
                floatingPanicIcon = iconContainer
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
