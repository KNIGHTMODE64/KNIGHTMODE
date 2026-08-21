package com.guardofknight.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardofknight.app.feature.fakecall.FakeCallActivity
import com.guardofknight.app.service.FloatingEdgeService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)

        setContent {
            var callerName by remember { 
                mutableStateOf(sharedPrefs.getString("caller_name", "Mom") ?: "Mom") 
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
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // Input to change caller name
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Button to activate the invisible floating edge bar
                    Button(
                        onClick = { startEdgeService() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(text = "Activate Edge Bar Service", color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test button to check fake call screen immediately
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
}
