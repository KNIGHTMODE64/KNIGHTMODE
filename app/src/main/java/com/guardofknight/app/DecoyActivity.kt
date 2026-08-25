package com.guardofknight.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DecoyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF121212)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Standard Decoy Notes View Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Quick Notes", fontSize = 28.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No notes available.", fontSize = 14.sp, color = Color.Gray)
                    }

                    // DISCREET BACKDOOR / SKIP BUTTON: 
                    // Placing a tiny, invisible or subtle button at the bottom-right corner 
                    // lets you jump back to your real app dashboard to change options anytime!
                    TextButton(
                        onClick = {
                            // Temporarily clear the decoy lock or jump back to MainActivity with a bypass extra
                            val prefs = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("decoy_mode_enabled", false).apply() // Turns off decoy so you can access settings

                            val intent = Intent(this@DecoyActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Text(text = "⚙️", fontSize = 18.sp) // Tiny settings icon hidden in plain sight
                    }
                }
            }
        }
    }
}
