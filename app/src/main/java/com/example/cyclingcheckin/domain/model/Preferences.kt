package com.example.cyclingcheckin.domain.model

/** 主题模式。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** 日历标记样式：圆点或当日里程数字。 */
enum class CalendarMarkStyle {
    DOT,
    DISTANCE
}

/** 应用的通用设置项。 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val calendarMarkStyle: CalendarMarkStyle = CalendarMarkStyle.DOT,
    val autoBackupAfterRecord: Boolean = true,
    val lastBackupAt: Long? = null,
    val webDavServer: String = DEFAULT_WEBDAV_SERVER,
    val webDavAccount: String = "",
    val webDavRemoteDir: String = DEFAULT_WEBDAV_DIR,
    val frequentAddresses: List<FrequentAddress> = emptyList()
) {
    val webDavConfigured: Boolean
        get() = webDavServer.isNotBlank() && webDavAccount.isNotBlank()

    companion object {
        const val DEFAULT_WEBDAV_SERVER = "https://dav.jianguoyun.com/dav/"
        const val DEFAULT_WEBDAV_DIR = "/骑行打卡/"
    }
}
