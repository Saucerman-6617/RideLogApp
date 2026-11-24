package com.example.cyclingcheckin.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.FrequentAddress
import com.example.cyclingcheckin.domain.model.FrequentAddressIcon
import com.example.cyclingcheckin.ui.theme.RideLogTheme

/** 常用地址图标对应的矢量图标，列表与编辑界面共用。 */
internal val FrequentAddressIcon.vector: ImageVector
    get() = when (this) {
        FrequentAddressIcon.HOME -> Icons.Outlined.Home
        FrequentAddressIcon.WORK -> Icons.Outlined.Business
        FrequentAddressIcon.SCHOOL -> Icons.Outlined.School
        FrequentAddressIcon.GYM -> Icons.Outlined.FitnessCenter
        FrequentAddressIcon.PARK -> Icons.Outlined.Park
        FrequentAddressIcon.OTHER -> Icons.Outlined.Add
        FrequentAddressIcon.SHOPPING -> Icons.Outlined.ShoppingBag
        FrequentAddressIcon.FOOD -> Icons.Outlined.Restaurant
        FrequentAddressIcon.COFFEE -> Icons.Outlined.LocalCafe
        FrequentAddressIcon.BASKETBALL -> Icons.Outlined.SportsBasketball
    }

internal const val MAX_FREQUENT_ADDRESS_NAME = 16

/**
 * 常用地址编辑面板：新增或编辑地址名称与图标。
 * 新增时隐藏删除按钮，编辑已有地址时可删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequentAddressSheet(
    existing: FrequentAddress?,
    onDismiss: () -> Unit,
    onSave: (FrequentAddress) -> Unit,
    onDelete: (FrequentAddress) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.name.orEmpty())
    }
    var icon by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.icon ?: FrequentAddressIcon.HOME)
    }
    val canSave = name.isNotBlank()

    fun submit() {
        if (!canSave) return
        onSave(
            FrequentAddress(
                id = existing?.id ?: 0L,
                name = name.trim().take(MAX_FREQUENT_ADDRESS_NAME),
                icon = icon
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Text(
                    text = stringResource(
                        if (existing == null) {
                            R.string.frequent_address_title_add
                        } else {
                            R.string.frequent_address_title_edit
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.frequent_address_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= MAX_FREQUENT_ADDRESS_NAME) name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.frequent_address_hint),
                            color = RideLogTheme.colors.secondaryText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon.vector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                // 两行图标选择区，每行 5 个
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FrequentAddressIcon.entries.chunked(ICON_OPTIONS_PER_ROW).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { option ->
                                AddressIconOption(
                                    option = option,
                                    selected = option == icon,
                                    onClick = { icon = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(ICON_OPTIONS_PER_ROW - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { submit() },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.frequent_address_save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (existing != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onDelete(existing) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            RideLogTheme.colors.danger.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = RideLogTheme.colors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.frequent_address_delete),
                            color = RideLogTheme.colors.danger,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private const val ICON_OPTIONS_PER_ROW = 5

/** 图标选择项：44dp 圆形，选中为蓝色底白图标。 */
@Composable
private fun AddressIconOption(
    option: FrequentAddressIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
                2.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = option.vector,
                    contentDescription = null,
                    tint = if (selected) {
                        Color.White
                    } else {
                        RideLogTheme.colors.secondaryText
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
