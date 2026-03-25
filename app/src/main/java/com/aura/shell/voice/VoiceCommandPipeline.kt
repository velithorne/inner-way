package com.aura.shell.voice

import com.aura.shell.command.CommandDispatch
import com.aura.shell.command.CommandRouter
import com.aura.shell.model.LauncherAppInfo
import com.aura.shell.model.RecentAppEntry
import com.aura.shell.personalization.PersonalResolutionContext

/**
 * Single entry for text from typing or speech — same [CommandRouter.route] as typed input.
 */
object VoiceCommandPipeline {

    fun process(
        transcript: String,
        installedApps: List<LauncherAppInfo>,
        recentApps: List<RecentAppEntry>,
        appDrawerExpanded: Boolean = false,
        router: CommandRouter = CommandRouter(),
        personal: PersonalResolutionContext? = null,
    ): CommandDispatch {
        return router.route(transcript, installedApps, recentApps, appDrawerExpanded, personal)
    }
}
