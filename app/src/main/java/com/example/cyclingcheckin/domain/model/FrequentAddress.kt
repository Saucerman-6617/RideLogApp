package com.example.cyclingcheckin.domain.model

import kotlinx.serialization.Serializable

/** 常用地址图标类型，UI 层映射为具体图标。 */
enum class FrequentAddressIcon {
    HOME,
    WORK,
    SCHOOL,
    GYM,
    PARK,
    OTHER,
    SHOPPING,
    FOOD,
    COFFEE,
    BASKETBALL
}

/** 用户维护的常用地址，点击后填入骑行路线的起点 / 终点。 */
@Serializable
data class FrequentAddress(
    val id: Long = 0,
    val name: String = "",
    val icon: FrequentAddressIcon = FrequentAddressIcon.HOME
)
