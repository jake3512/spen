package com.jake.spenhotkey.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.jake.spenhotkey.data.Mapping
import com.jake.spenhotkey.data.MappingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Detects S Pen side-button presses (Android 14+ reports them as
 * [KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY]), classifies them into single click,
 * double click or long press, and runs the mapped [com.jake.spenhotkey.data.ActionSpec]
 * for the app currently in the foreground.
 */
class SPenAccessibilityService : AccessibilityService() {

    private lateinit var repository: MappingRepository
    private lateinit var executor: ActionExecutor
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private val currentPackageFlow = MutableStateFlow<String?>(null)
    private var activeMapping: Mapping = Mapping()
    private var longPressMs: Long = MappingRepository.DEFAULT_LONG_PRESS_MS
    private var doubleClickMs: Long = MappingRepository.DEFAULT_DOUBLE_CLICK_MS

    private var longPressFired = false
    private var pendingClicks = 0

    private val longPressRunnable = Runnable {
        longPressFired = true
        pendingClicks = 0
        handler.removeCallbacks(clickRunnable)
        executor.perform(activeMapping.longPress)
    }

    private val clickRunnable = Runnable {
        val count = pendingClicks
        pendingClicks = 0
        when {
            count >= 2 -> executor.perform(activeMapping.doubleClick)
            count == 1 -> executor.perform(activeMapping.singleClick)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = MappingRepository(applicationContext)
        executor = ActionExecutor(this)

        serviceScope.launch {
            combine(
                currentPackageFlow,
                repository.overrideAppsFlow,
                repository.defaultMappingFlow(::resolveAppLabel)
            ) { pkg, overrides, defaultMapping -> Triple(pkg, overrides, defaultMapping) }
                .collectLatest { (pkg, overrides, defaultMapping) ->
                    activeMapping = if (pkg != null && pkg in overrides) {
                        repository.appMappingFlow(pkg, ::resolveAppLabel).first()
                    } else {
                        defaultMapping
                    }
                }
        }
        serviceScope.launch { repository.longPressMsFlow.collectLatest { longPressMs = it } }
        serviceScope.launch { repository.doubleClickMsFlow.collectLatest { doubleClickMs = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentPackageFlow.value = event.packageName?.toString()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    longPressFired = false
                    handler.removeCallbacks(longPressRunnable)
                    handler.postDelayed(longPressRunnable, longPressMs)
                }
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!longPressFired) {
                    pendingClicks++
                    handler.removeCallbacks(clickRunnable)
                    handler.postDelayed(clickRunnable, doubleClickMs)
                }
            }
        }
        // Consume the event so Samsung's own S Pen button handling (Air command, etc.)
        // doesn't also fire for the same press.
        return true
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun resolveAppLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }
}
