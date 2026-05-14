package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moneymanager.app.ui.util.MaterialIconProvider

@Composable
fun CategoryIcon(
    emoji: String,
    iconType: String = "emoji",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    tint: Color = Color.Unspecified
) {
    when (iconType) {
        "material" -> {
            val imageVector = remember(emoji) { MaterialIconProvider.getIcon(emoji) }
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = modifier,
                    tint = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "□",
                    modifier = modifier,
                    fontSize = fontSize,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        "image" -> {
            AsyncImage(
                model = emoji,
                contentDescription = null,
                modifier = modifier
            )
        }
        else -> {
            Text(
                text = emoji,
                modifier = modifier,
                fontSize = fontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategoryIconDisplay(
    emoji: String,
    iconType: String = "emoji",
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 36.dp
) {
    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        CategoryIcon(
            emoji = emoji,
            iconType = iconType,
            fontSize = (iconSize.value * 0.5).sp,
            modifier = Modifier.size(iconSize * 0.6f)
        )
    }
}
