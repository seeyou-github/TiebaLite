package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material.fade
import com.eygraber.compose.placeholder.material.placeholder
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.DisplayRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlin.math.abs

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@Composable
fun AvatarIcon(
    icon: ImageVector,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarIcon(
    @DrawableRes
    resId: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = ImageVector.vectorResource(id = resId),
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarPlaceholder(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Avatar(
        data = ImageUtil.getPlaceHolder(LocalContext.current, 0),
        size = size,
        contentDescription = null,
        modifier = modifier.placeholder(
            visible = true,
            color = MaterialTheme.colors.surface,
            highlight = PlaceholderHighlight.fade(),
            shape = CircleShape
        )
    )
}

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    username: String? = null,
    // Only user avatars should be affected by the "do not load avatar" preference.
    isUserAvatar: Boolean = true,
) {
    val context = LocalContext.current
    if (isUserAvatar && context.appPreferences.doNotLoadAvatar) {
        val displayName = username ?: data ?: ""
        val firstChar = displayName.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
        val colorsLight = listOf(
            Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
            Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
            Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFD54F),
            Color(0xFFFFB74D), Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE)
        )
        val colorsDark = listOf(
            Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4527A0),
            Color(0xFF283593), Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F),
            Color(0xFF00695C), Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFFF9A825),
            Color(0xFFEF6C00), Color(0xFFD84315), Color(0xFF4E342E), Color(0xFF37474F)
        )
        val hash = abs(displayName.hashCode())
        val isNight = ThemeUtil.isNightMode(ThemeUtil.themeState.value)
        val bgColorList = if (isNight) colorsDark else colorsLight
        val bgColor = bgColorList[hash % bgColorList.size]

        Box(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(bgColor)
        ) {
            Text(
                text = firstChar,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Avatar(
            data = data,
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            shape = shape,
        )
    }
}

@Composable
fun Avatar(
    data: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    val context = LocalContext.current

    AsyncImage(
        request = DisplayRequest(LocalContext.current, data) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
        },
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}

@Composable
fun Avatar(
    data: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AsyncImage(
        request = DisplayRequest(LocalContext.current, newResourceUri(data)) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
        },
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}
