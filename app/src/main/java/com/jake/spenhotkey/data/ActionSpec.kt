package com.jake.spenhotkey.data

/**
 * A single action that can be bound to one S Pen side-button gesture.
 * [code] is the stable string persisted to DataStore; it must never change
 * for an existing object or saved mappings will silently reset to None.
 */
sealed class ActionSpec(val code: String, val label: String) {
    object None : ActionSpec("NONE", "동작 없음")
    object RightClick : ActionSpec("RIGHT_CLICK", "마우스 우클릭 (컨텍스트 메뉴)")
    object Copy : ActionSpec("COPY", "복사")
    object Cut : ActionSpec("CUT", "잘라내기")
    object Paste : ActionSpec("PASTE", "붙여넣기")
    object SelectAll : ActionSpec("SELECT_ALL", "전체 선택")
    object Back : ActionSpec("BACK", "뒤로 가기")
    object Home : ActionSpec("HOME", "홈")
    object Recents : ActionSpec("RECENTS", "최근 앱")
    object Notifications : ActionSpec("NOTIFICATIONS", "알림 패널 열기")
    object QuickSettings : ActionSpec("QUICK_SETTINGS", "빠른 설정 열기")
    object Screenshot : ActionSpec("SCREENSHOT", "스크린샷")
    object LockScreen : ActionSpec("LOCK", "화면 잠금")
    object SplitScreen : ActionSpec("SPLIT_SCREEN", "화면 분할")

    data class LaunchApp(val packageName: String, val appLabel: String) :
        ActionSpec("LAUNCH:$packageName", "앱 실행: $appLabel")

    companion object {
        private val staticChoices = listOf(
            None, RightClick, Copy, Cut, Paste, SelectAll,
            Back, Home, Recents, Notifications, QuickSettings,
            Screenshot, LockScreen, SplitScreen
        )

        /** Choices shown in the action picker dropdown, excluding "launch app" (handled separately). */
        fun defaultChoices(): List<ActionSpec> = staticChoices

        fun fromCode(code: String?, resolveAppLabel: (String) -> String): ActionSpec {
            if (code.isNullOrBlank()) return None
            if (code.startsWith("LAUNCH:")) {
                val pkg = code.removePrefix("LAUNCH:")
                return LaunchApp(pkg, resolveAppLabel(pkg))
            }
            return staticChoices.firstOrNull { it.code == code } ?: None
        }
    }
}
