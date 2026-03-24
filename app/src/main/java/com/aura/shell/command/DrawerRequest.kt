package com.aura.shell.command

/**
 * One-shot signal from [com.aura.shell.MainActivity] to [com.aura.shell.ui.home.HomeScreen]
 * to expand or collapse the app drawer after a command.
 */
data class DrawerRequest(
    val nonce: Long,
    val expand: Boolean,
)
