package com.aura.shell.ui.home

/**
 * In-memory hint so [com.aura.shell.command.CommandRouter] can tell “close” apart:
 * collapse the sheet vs bring launcher forward from another app.
 */
object AppDrawerSessionState {
    @Volatile
    var expanded: Boolean = false
}
