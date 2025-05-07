package com.eriknivar.firebasedatabase.view.utility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSubItem: Boolean = false,
    trailingIcon: ImageVector? = null // 🟢 AGREGA ESTE PARÁMETRO OPCIONAL
) {
    val textStyle = TextStyle(
        fontSize = if (isSubItem) 14.sp else 16.sp,
        fontStyle = if (isSubItem) FontStyle.Italic else FontStyle.Normal,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF003366)
    )

    val paddingStart = if (isSubItem) 32.dp else 0.dp

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFE8F1FB),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color(0xFF003366),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = label,
                    style = textStyle
                )
            }

            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = "Expandir",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
