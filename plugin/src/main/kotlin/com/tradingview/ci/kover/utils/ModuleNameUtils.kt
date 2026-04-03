package com.tradingview.ci.kover.utils

fun String.toSafeModuleSegment(): String =
    trimStart(':')
        .replace("_", "__")
        .replace("-", "_dash_")
        .replace(':', '_')

