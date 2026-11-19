package com.example.cyclingcheckin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.data.remote.WebDavError
import com.example.cyclingcheckin.ui.theme.RideLogTheme

@Composable
fun RideLogCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun BikeIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_bike),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

/** 空状态插画：简单的骑行路线 + 自行车轮，无需图片资源。 */
@Composable
fun EmptyRideIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = RideLogTheme.colors.accent
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.size(width = 170.dp, height = 96.dp)) {
        val centerY = size.height * 0.58f
        drawLine(
            color = track,
            start = Offset(8.dp.toPx(), centerY),
            end = Offset(size.width - 8.dp.toPx(), centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val wheelRadius = 18.dp.toPx()
        drawCircle(
            color = primary,
            radius = wheelRadius,
            center = Offset(size.width * 0.30f, centerY),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = primary,
            radius = wheelRadius,
            center = Offset(size.width * 0.68f, centerY),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = accent,
            radius = 4.dp.toPx(),
            center = Offset(size.width * 0.49f, centerY - 8.dp.toPx())
        )
        drawLine(
            color = primary,
            start = Offset(size.width * 0.30f, centerY),
            end = Offset(size.width * 0.49f, centerY - 8.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary,
            start = Offset(size.width * 0.49f, centerY - 8.dp.toPx()),
            end = Offset(size.width * 0.68f, centerY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    illustration: @Composable () -> Unit = {
        Icon(
            painter = painterResource(id = R.drawable.ic_notebook),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            modifier = Modifier.size(96.dp)
        )
    }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        illustration()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = RideLogTheme.colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(6.dp))
            action()
        }
    }
}

/** 把 WebDAV 错误映射为可操作的中文提示（可在非 Composable 上下文使用）。 */
fun webDavErrorMessage(context: android.content.Context, kind: WebDavError, httpCode: Int?): String =
    when (kind) {
        WebDavError.NOT_CONFIGURED -> context.getString(R.string.webdav_required)
        WebDavError.INSECURE -> context.getString(R.string.error_https)
        WebDavError.UNAUTHORIZED -> context.getString(R.string.error_unauthorized)
        WebDavError.FORBIDDEN -> context.getString(R.string.error_forbidden)
        WebDavError.NOT_FOUND -> context.getString(R.string.error_not_found)
        WebDavError.SERVER -> context.getString(R.string.error_server, httpCode ?: 500)
        WebDavError.HTTP -> context.getString(R.string.error_http, httpCode ?: 0)
        WebDavError.TIMEOUT -> context.getString(R.string.error_timeout)
        WebDavError.NETWORK -> context.getString(R.string.error_network)
        WebDavError.INVALID_RESPONSE -> context.getString(R.string.error_invalid_response)
        WebDavError.INVALID_DATA -> context.getString(R.string.error_invalid_data)
    }

/** 把 WebDAV 错误映射为可操作的中文提示。 */
@Composable
fun webDavErrorMessage(kind: WebDavError, httpCode: Int?): String =
    webDavErrorMessage(androidx.compose.ui.platform.LocalContext.current, kind, httpCode)

@Composable
fun ValueWithUnit(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = RideLogTheme.colors.secondaryText,
            modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
        )
    }
}

@Composable
fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 6.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(0.dp)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(color = color, radius = this.size.minDimension / 2f)
        }
    }
}
