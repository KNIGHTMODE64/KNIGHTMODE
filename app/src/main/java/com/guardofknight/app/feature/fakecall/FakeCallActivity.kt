package com.guardofknight.app.feature.fakecall

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

class FakeCallActivity : ComponentActivity() {

    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndUnlock()
        startCallVibration()
        startRingtone()

        val sharedPreferences = getSharedPreferences("GuardPrefs", Context.MODE_PRIVATE)
        val savedName = sharedPreferences.getString("caller_name", "Mom") ?: "Mom"
        val savedImage = sharedPreferences.getString("caller_image", "") ?: ""

        setContent {
            DecoyIncomingCallScreen(
                callerName = savedName,
                callerImageUri = savedImage,
                onAccept = { 
                    stopVibration()
                    stopRingtone()
                },
                onDecline = { 
                    stopVibration()
                    stopRingtone()
                    finish() 
                }
            )
        }
    }

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun startCallVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() { vibrator?.cancel() }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopRingtone() { ringtone?.stop() }

    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
        stopRingtone()
    }
}

@Composable
fun DecoyIncomingCallScreen(
    callerName: String,
    callerImageUri: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var callAccepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 72.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                if (callerImageUri.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(callerImageUri)),
                        contentDescription = "Caller Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(75.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = callerName, color = Color.White, fontSize = 34.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (callAccepted) "Connected 00:15" else "Incoming mobile call...",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            if (!callAccepted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            callAccepted = true
                            onAccept()
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
                                )
                            )
                    ) {
                        Icon(
                            Icons.Default.Call, 
                            contentDescription = "Accept", 
                            tint = Color.White, 
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Accept", color = Color.White, fontSize = 14.sp)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF87171), Color(0xFFDC2626))
                            )
                        )
                ) {
                    Icon(
                        Icons.Default.CallEnd, 
                        contentDescription = "Decline", 
                        tint = Color.White, 
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Decline", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
