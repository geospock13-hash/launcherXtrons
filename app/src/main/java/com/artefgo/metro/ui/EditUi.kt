package com.artefgo.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Barra de herramientas que aparece abajo del panel en modo edición. */
@Composable
fun EditToolbar(title: String, enabled: Boolean, content: @Composable RowScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(Color(0xEE1E1E1E))
            .padding(6.dp)
    ) {
        Text(
            title, color = if (enabled) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .widthIn(min = 56.dp)
            .background(Color(0xFF2E2E2E))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1)
    }
}
