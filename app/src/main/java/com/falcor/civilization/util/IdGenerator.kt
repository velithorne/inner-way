package com.falcor.civilization.util

import java.util.UUID

object IdGenerator {
    fun generate(): String = UUID.randomUUID().toString().replace("-", "").take(24)
}
