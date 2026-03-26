package com.aura.shell.command

/**
 * Intents sent from [com.aura.shell.CommandLayerActivity] to [com.aura.shell.MainActivity]
 * to coordinate bottom sheet open/close without global singletons.
 */
object LauncherDrawerIntent {
    const val ACTION_OPEN_APP_DRAWER = "com.aura.shell.action.OPEN_APP_DRAWER"
    const val ACTION_CLOSE_APP_DRAWER = "com.aura.shell.action.CLOSE_APP_DRAWER"
}
