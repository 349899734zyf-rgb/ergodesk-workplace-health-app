package com.fit5046.ergodesk.ui.screens

/*
 * FIT5046 Assessment 2 — ErgoDesk
 * Shared branded header + palette constants, matching the header used on
 * ErgoFormScreen and AnalyticsScreen so every main screen looks the same.
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.ui.theme.ErgoGreen

// Shared palette for Member 2 screens (mirrors the constants in ErgoForm/Analytics).
val ErgoHeaderBg = Color(0xFFF7FBF2)   // pale green header strip
val ErgoSoftGreen = Color(0xFFF1F8EE)  // light green card fill / progress track
val ErgoHairline = Color(0xFFD9DDD9)   // 1dp card outline
val ErgoMutedText = Color(0xFF666B66)  // secondary text

/** App bar: logo, tagline and a profile avatar. Same on every main screen. */
@Composable
fun ErgoDeskHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ErgoHeaderBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = null,
                tint = ErgoGreen,
                modifier = Modifier.size(31.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text("ErgoDesk", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Move Better, Work Smarter", fontSize = 9.sp, color = ErgoMutedText)
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ErgoHairline),
                color = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonOutline,
                    contentDescription = "Profile",
                    tint = ErgoMutedText,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(22.dp)
                )
            }
        }
    }
}
