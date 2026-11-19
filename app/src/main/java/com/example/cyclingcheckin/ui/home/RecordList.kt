package com.example.cyclingcheckin.ui.home

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val REVEAL_WIDTH_DP = 120f

/** 松手后判定为"粘住"的比例阈值，达到即停靠在 -120dp，否则回弹到 0。 */
private const val REVEAL_THRESHOLD = 0.45f

/** 轻扫判定速度阈值（px/s），超过则不看距离直接按方向停靠。 */
private const val FLING_VELOCITY = 800f

/**
 * 左滑露出删除按钮的记录行。
 *
 * 手势实现要点（修复卡顿 / 滑不动）：
 * - 不再使用 animateFloatAsState + LaunchedEffect 回写 offset 的写法，
 *   那种写法在拖动过程中动画目标恒为 0，会把手指位移每帧拉回去，导致滑不动并抖动。
 * - 改用单一可变状态 offset 作为唯一数据源：拖动期间直接累加（完全跟手，无动画干扰），
 *   仅在松手时用协程动画停靠到 -120dp 或回弹到 0。
 * - 使用 Modifier.draggable(Orientation.Horizontal)：只处理横向手势，
 *   纵向滑动仍交给外层 LazyColumn 滚动，两者互不抢夺。
 */
@Composable
fun RecordRow(
    record: Record,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    collapseTrigger: Int = 0
) {
    val description = stringResource(
        R.string.cd_record_item,
        "${TimeUtils.dayLabel(record.date)} ${TimeUtils.formatTime(record.time)}",
        TimeUtils.formatDistance(record.distance)
    )
    val revealWidthPx = with(LocalDensity.current) { REVEAL_WIDTH_DP.dp.toPx() }

    var offset by remember { mutableFloatStateOf(0f) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // 平滑停靠到目标位置
    fun settleTo(target: Float) {
        animJob?.cancel()
        animJob = scope.launch {
            animate(
                initialValue = offset,
                targetValue = target,
                animationSpec = tween(durationMillis = 200)
            ) { value, _ -> offset = value }
        }
    }

    // 外部请求收起（如确认删除对话框关闭后）
    LaunchedEffect(collapseTrigger) {
        if (offset != 0f) settleTo(0f)
    }

    Box(
        modifier = modifier
            .heightIn(min = 68.dp)
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    // 拖动期间取消未完成的停靠动画，避免与手指争夺 offset
                    animJob?.cancel()
                    animJob = null
                    offset = (offset + delta).coerceIn(-revealWidthPx, 0f)
                },
                onDragStopped = { velocity ->
                    // 轻扫也能生效：快速左滑直接停靠，快速右滑直接收回
                    val target = when {
                        velocity < -FLING_VELOCITY -> -revealWidthPx
                        velocity > FLING_VELOCITY -> 0f
                        offset <= -revealWidthPx * REVEAL_THRESHOLD -> -revealWidthPx
                        else -> 0f
                    }
                    settleTo(target)
                }
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart
    ) {
        // —— 背景：红色删除按钮区（被前景卡片遮盖，滑出后可见） ——
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(RideLogTheme.colors.danger)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        settleTo(0f)
                        onRequestDelete()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // —— 前景：记录卡（随 offset 平移） ——
        Surface(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .fillMaxWidth()
                .heightIn(min = 68.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 已露出时点击卡片 = 收回；未露出时点击 = 编辑
                        if (offset < 0f) settleTo(0f) else onEdit()
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TimeUtils.formatTime(record.time),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.heightIn(min = 2.dp))
                    Text(
                        text = record.note.ifBlank { stringResource(R.string.home_note_empty) },
                        style = MaterialTheme.typography.bodySmall,
                        color = RideLogTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = TimeUtils.formatDistance(record.distance),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = RideLogTheme.colors.accent
                    )
                    Text(
                        text = " km",
                        style = MaterialTheme.typography.bodySmall,
                        color = RideLogTheme.colors.secondaryText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
