package com.guardofknight.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DecoyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = this
            val focusManager = LocalFocusManager.current
            val notePrefs = context.getSharedPreferences("decoy_note_content", Context.MODE_PRIVATE)
            var noteText by remember { mutableStateOf(notePrefs.getString("saved_note", "") ?: "") }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E1E)
            ) {
                // Root Box with a click listener to dismiss the keyboard / close panels on screen tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        // Header Row with Title and Skip Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Quick Notes", fontSize = 26.sp, color = Color.White)
                            
                            // Explicit Skip button to successfully close/bypass the decoy screen
                            TextButton(onClick = { navigateToMain() }) {
                                Text(text = "Skip", fontSize = 16.sp, color = Color(0xFF3B82F6))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fully functional text input field for notes
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { 
                                noteText = it
                                notePrefs.edit().putString("saved_note", it).apply()
                            },
                            placeholder = { Text("Type your notes here...", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }

                    // Discreet settings backdoor button to return to your real app dashboard
                    TextButton(
                        onClick = {
                            navigateToMain()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Text(text = "⚙️", fontSize = 20.sp)
                    }
                }
            }
        }
    }

    // Override the physical/gesture back button so it successfully returns to MainActivity
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        navigateToMain()
    }

    private fun navigateToMain() {
        // Hide the soft keyboard cleanly before switching tasks
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.windowToken?.let { token ->
            imm.hideSoftInputFromWindow(token, 0)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
