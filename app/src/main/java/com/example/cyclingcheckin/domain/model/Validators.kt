package com.example.cyclingcheckin.domain.model

import java.time.LocalDateTime
import kotlin.math.roundToInt

/** 记录表单的校验错误类型，由 UI 映射为可读文案。 */
enum class RecordFieldError {
    DISTANCE_EMPTY,
    DISTANCE_INVALID,
    DISTANCE_OUT_OF_RANGE,
    TIME_IN_FUTURE,
    NOTE_TOO_LONG
}

data class RecordValidation(
    val distance: Double?,
    val error: RecordFieldError?,
    val noteTooLong: Boolean
) {
    val isValid: Boolean get() = distance != null && error == null && !noteTooLong
}

object Validators {
    const val MIN_DISTANCE = 0.1
    const val MAX_DISTANCE = 9999.9
    const val MAX_NOTE_LENGTH = 200

    /** 输入阶段过滤：只保留数字与一个小数点，小数最多一位。 */
    fun sanitizeDistanceInput(raw: String): String {
        val filtered = buildString {
            var dot = false
            var decimals = 0
            for (ch in raw) {
                when {
                    ch.isDigit() -> {
                        if (dot) {
                            if (decimals < 1) {
                                append(ch)
                                decimals++
                            }
                        } else {
                            append(ch)
                        }
                    }
                    (ch == '.' || ch == ',') && !dot -> {
                        dot = true
                        append('.')
                    }
                }
            }
        }
        // 去掉无意义的前导 0（保留 "0.x" 形式）
        return if (filtered.length > 1 && filtered[0] == '0' && filtered[1] != '.') {
            filtered.dropWhile { it == '0' }.ifEmpty { "0" }
        } else {
            filtered
        }
    }

    fun parseDistance(text: String): Double? = text.trim().toDoubleOrNull()

    /**
     * 校验记录表单。
     * 距离要求 0.1 ≤ d ≤ 9999.9；日期时间不得晚于当前（允许等于）；备注最长 200 字。
     */
    fun validate(
        distanceText: String,
        dateTime: LocalDateTime,
        note: String,
        now: LocalDateTime = LocalDateTime.now()
    ): RecordValidation {
        val noteTooLong = note.length > MAX_NOTE_LENGTH
        if (distanceText.isBlank()) {
            return RecordValidation(null, RecordFieldError.DISTANCE_EMPTY, noteTooLong)
        }
        val distance = parseDistance(distanceText)
            ?: return RecordValidation(null, RecordFieldError.DISTANCE_INVALID, noteTooLong)
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
            return RecordValidation(null, RecordFieldError.DISTANCE_OUT_OF_RANGE, noteTooLong)
        }
        if (dateTime.isAfter(now)) {
            return RecordValidation(distance, RecordFieldError.TIME_IN_FUTURE, noteTooLong)
        }
        return RecordValidation(distance, null, noteTooLong)
    }

    /** 距离保留一位小数（四舍五入）。 */
    fun roundDistance(distance: Double): Double = (distance * 10).roundToInt() / 10.0
}
