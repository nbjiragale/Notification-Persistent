package com.nbjiragale.notificationpersistent.oem

import android.os.Build

enum class OemType { GENERIC, XIAOMI, VIVO, OPPO, SAMSUNG, HUAWEI }

object OemDetector {
    fun detect(): OemType = when (Build.MANUFACTURER.uppercase()) {
        "XIAOMI", "REDMI" -> OemType.XIAOMI
        "VIVO" -> OemType.VIVO
        "OPPO", "ONEPLUS", "REALME" -> OemType.OPPO
        "SAMSUNG" -> OemType.SAMSUNG
        "HUAWEI", "HONOR" -> OemType.HUAWEI
        else -> OemType.GENERIC
    }
}
