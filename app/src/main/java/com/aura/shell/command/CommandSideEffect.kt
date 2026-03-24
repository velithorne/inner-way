package com.aura.shell.command

sealed class CommandSideEffect {
    data object OpenAppDrawerAndFinish : CommandSideEffect()
    data object CloseAppDrawerAndFinish : CommandSideEffect()
}
