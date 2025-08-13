

package com.bitchat.android.ui.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.bitchat.android.util.StaffAuth

@Composable
fun StaffBadge(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    Surface(
        color = Color(0xFFFF3B30), // rouge similaire à iOS
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(start = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    StaffAuth.deactivate(context)
                })
            }
    ) {
        Text(
            "STAFF",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
