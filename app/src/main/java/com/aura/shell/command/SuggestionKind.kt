package com.aura.shell.command

enum class SuggestionKind {
    /** Several plausible apps */
    DISAMBIGUATION,

    /** Best guess is uncertain — pick to confirm */
    DID_YOU_MEAN,

    /** Alias maps to multiple product lines (e.g. YouTube vs YT Music) */
    ALIAS_SPLIT,
}
