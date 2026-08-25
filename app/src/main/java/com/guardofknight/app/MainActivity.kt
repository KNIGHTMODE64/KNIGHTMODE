package com.guardofknight.app

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardofknight.app.feature.fakecall.FakeCallActivity
import com.guardofknight.app.service.FloatingEdgeService
import java.io.InputStream

class MainActivity : ComponentActivity() {

    // Receiver to listen for the power button (screen off) panic trigger
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                triggerPanicPurge()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)
        
        // Check if Decoy Stealth Disguise mode is turned ON
        val isDecoyEnabled = sharedPrefs.getBoolean("decoy_mode_enabled", false)
        if (isDecoyEnabled) {
            val intent = Intent(this, DecoyActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        // Block screenshots and recent apps window previews for security
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            var callerName by remember { 
                mutableStateOf(sharedPrefs.getString("caller_name", "Mom") ?: "Mom") 
            }
            var callerImageUri by remember {
                mutableStateOf(sharedPrefs.getString("caller_image", "") ?: "")
            }
            var isDecoyModeEnabled by remember {
                mutableStateOf(sharedPrefs.getBoolean("decoy_mode_enabled", false))
            }

            var isServiceRunning by remember {
                mutableStateOf(isMyServiceRunning(FloatingEdgeService::class.java))
            }

            var loadedBitmap by remember(callerImageUri) {
                mutableStateOf<Bitmap?>(null)
            }

            LaunchedEffect(callerImageUri) {
                if (callerImageUri.isNotBlank()) {
                    try {
                        val inputStream: InputStream? = contentResolver.openInputStream(Uri.parse(callerImageUri))
                        loadedBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        loadedBitmap = null
                    }
                } else {
                    loadedBitmap = null
                }
            }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    callerImageUri = it.toString()
                    sharedPrefs.edit().putString("caller_image", it.toString()).apply()
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF111827)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "GuardOfKnight", fontSize = 32.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Stealth Edge Trigger Controller", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                    
                    Spacer(modifier = Modifier.height(28.dp))

                    // Caller Photo Preview Box
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF374151)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = loadedBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Caller Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                    ) {
                        Text(text = "Choose Caller Photo", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = callerName,
                        onValueChange = { 
                            callerName = it
                            sharedPrefs.edit().putString("caller_name", it).apply()
                        },
                        label = { Text("Fake Caller Name", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Decoy Disguise Mode Switch Toggle Row with Visual Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Decoy Icon",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Enable Decoy Disguise", color = Color.White, fontSize = 16.sp)
                        }
                        Switch(
                            checked = isDecoyModeEnabled,
                            onCheckedChange = { enabled ->
                                isDecoyModeEnabled = enabled
                                sharedPrefs.edit().putBoolean("decoy_mode_enabled", enabled).apply()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { 
                            if (isServiceRunning) {
                                stopEdgeService()
                                isServiceRunning = false
                            } else {
                                startEdgeService()
                                isServiceRunning = isMyServiceRunning(FloatingEdgeService::class.java)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) Color(0xFFDC2626) else Color(0xFF2563EB)
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            text = if (isServiceRunning) "Deactivate Edge Bar Service" else "Activate Edge Bar Service", 
                            color = Color.White, 
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(this@MainActivity, FakeCallActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(text = "Test Fake Call Now", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the screen-off listener dynamically
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered
        }
    }

    private fun triggerPanicPurge() {
        // Launch the decoy interface and clear task stack instantly on power button press
        val intent = Intent(this, DecoyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun startEdgeService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            val serviceIntent = Intent(this, FloatingEdgeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun stopEdgeService() {
        val serviceIntent = Intent(this, FloatingEdgeService::class.java)
        stopService(serviceIntent)
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
