package com.guardofknight.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.guardofknight.app.service.FloatingEdgeService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Check if the app has permission to draw the floating button
        if (!Settings.canDrawOverlays(this)) {
            // If not, open Android Settings so the user can turn it on
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            // 2. If we have permission, start the floating button service!
            val serviceIntent = Intent(this, FloatingEdgeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            // 3. Close this setup screen so the user just sees their home screen and the new button
            finish()
        }
    }
}
