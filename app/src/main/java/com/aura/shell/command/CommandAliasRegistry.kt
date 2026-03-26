package com.aura.shell.command

/**
 * Maps shorthand / synonyms to semantic tokens or hint phrases for app resolution.
 * Extend by adding entries here — one place for aliases.
 */
object CommandAliasRegistry {

    /**
     * Semantic tokens expand to search phrases against installed app labels.
     */
    enum class SemanticToken {
        MESSAGES,
        CAMERA,
        SETTINGS,
        CONTACTS,
        BROWSER,
        CALCULATOR,
        YOUTUBE,
        PHONE,
        MUSIC,
        CHROME,
        WHATSAPP,
    }

    /**
     * Alias → one or more semantic targets (first match in resolution often wins for single-target UX).
     */
    private val aliasToSemantics: Map<String, List<SemanticToken>> = mapOf(
        // Messages / SMS
        "msg" to listOf(SemanticToken.MESSAGES),
        "msgs" to listOf(SemanticToken.MESSAGES),
        "message" to listOf(SemanticToken.MESSAGES),
        "messages" to listOf(SemanticToken.MESSAGES),
        "text" to listOf(SemanticToken.MESSAGES),
        "texts" to listOf(SemanticToken.MESSAGES),
        "sms" to listOf(SemanticToken.MESSAGES),
        // Camera
        "cam" to listOf(SemanticToken.CAMERA),
        "camera" to listOf(SemanticToken.CAMERA),
        "photo" to listOf(SemanticToken.CAMERA),
        "photos" to listOf(SemanticToken.CAMERA),
        // Settings
        "settings" to listOf(SemanticToken.SETTINGS),
        "setting" to listOf(SemanticToken.SETTINGS),
        "prefs" to listOf(SemanticToken.SETTINGS),
        "preferences" to listOf(SemanticToken.SETTINGS),
        // Contacts
        "contacts" to listOf(SemanticToken.CONTACTS),
        "contact" to listOf(SemanticToken.CONTACTS),
        "phonebook" to listOf(SemanticToken.CONTACTS),
        "phone book" to listOf(SemanticToken.CONTACTS),
        // Browser
        "browser" to listOf(SemanticToken.BROWSER),
        "internet" to listOf(SemanticToken.BROWSER),
        "web" to listOf(SemanticToken.BROWSER),
        // Calculator
        "calc" to listOf(SemanticToken.CALCULATOR),
        "calculator" to listOf(SemanticToken.CALCULATOR),
        // YouTube ecosystem (matches YouTube, YT Music, etc. via keywords)
        "yt" to listOf(SemanticToken.YOUTUBE),
        "youtube" to listOf(SemanticToken.YOUTUBE),
        // WhatsApp
        "whatsapp" to listOf(SemanticToken.WHATSAPP),
        // Phone dialer
        "phone" to listOf(SemanticToken.PHONE),
        "dialer" to listOf(SemanticToken.PHONE),
        "call" to listOf(SemanticToken.PHONE),
        // Music (generic)
        "music" to listOf(SemanticToken.MUSIC),
        // Chrome explicit
        "chrome" to listOf(SemanticToken.CHROME),
        // WhatsApp (typo-friendly)
        "watsap" to listOf(SemanticToken.WHATSAPP),
        "watsapp" to listOf(SemanticToken.WHATSAPP),
    )

    /**
     * Keywords used to score apps when resolving a [SemanticToken].
     */
    fun keywordsFor(token: SemanticToken): Set<String> {
        return when (token) {
            SemanticToken.MESSAGES -> setOf("message", "messages", "messaging", "sms", "text")
            SemanticToken.CAMERA -> setOf("camera", "photo")
            SemanticToken.SETTINGS -> setOf("settings")
            SemanticToken.CONTACTS -> setOf("contact", "contacts", "people")
            SemanticToken.BROWSER -> setOf("browser", "chrome", "internet", "web")
            SemanticToken.CALCULATOR -> setOf("calculator", "calc")
            SemanticToken.YOUTUBE -> setOf("youtube", "yt music", "music", "yt")
            SemanticToken.PHONE -> setOf("phone", "dialer", "call")
            SemanticToken.MUSIC -> setOf("music", "audio", "player", "spotify", "youtube")
            SemanticToken.CHROME -> setOf("chrome")
            SemanticToken.WHATSAPP -> setOf("whatsapp")
        }
    }

    /**
     * Returns semantic tokens for an exact alias hit, or null.
     */
    fun lookupAlias(normalizedToken: String): List<SemanticToken>? {
        val key = normalizedToken.trim().lowercase()
        return aliasToSemantics[key]
    }

    /**
     * Drawer / apps synonyms (handled before app resolution).
     */
    fun matchesAppDrawerIntent(normalizedPhrase: String): Boolean {
        val n = normalizedPhrase.trim()
        return n == "apps" ||
            n == "applications" ||
            n == "app drawer" ||
            n == "all apps" ||
            n == "show apps" ||
            n == "show me apps" ||
            n == "open apps" ||
            n == "open app drawer" ||
            n == "list apps" ||
            n == "open all apps"
    }

    fun matchesHideDrawerIntent(normalizedPhrase: String): Boolean {
        val n = normalizedPhrase.trim()
        return n == "hide apps" ||
            n == "close apps" ||
            n == "dismiss apps" ||
            n == "close app drawer" ||
            n == "close drawer" ||
            n == "hide drawer" ||
            n == "dismiss drawer" ||
            n == "hide the apps" ||
            n == "dismiss drawer apps"
    }

    /**
     * Short phrases: if Aura’s app drawer is expanded, these collapse it; otherwise they mean “show HOME”.
     */
    fun matchesBareCloseOrDismiss(normalizedPhrase: String): Boolean {
        val n = normalizedPhrase.trim()
        return n == "close" ||
            n == "close it" ||
            n == "dismiss" ||
            n == "hide" ||
            n == "dismiss it" ||
            n == "hide it" ||
            n == "go back" ||
            n == "back"
    }

    /** Leave the foreground app and show HOME (launcher). Not force-stop — Android does not allow that here. */
    fun matchesGoHomeIntent(normalizedPhrase: String): Boolean {
        val n = normalizedPhrase.trim()
        return n == "go home" ||
            n == "home" ||
            n == "launcher" ||
            n == "aura home" ||
            n == "back home" ||
            n == "return home" ||
            n == "back to home" ||
            n == "back to launcher" ||
            n == "show home" ||
            n == "close app" ||
            n == "close application" ||
            n == "exit app" ||
            n == "quit app" ||
            n == "leave app" ||
            n == "dismiss app" ||
            n == "exit" ||
            n == "quit"
    }
}
