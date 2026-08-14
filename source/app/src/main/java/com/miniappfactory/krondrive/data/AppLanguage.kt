package com.miniappfactory.krondrive.data

import java.util.Locale

/**
 * Uygulama dili. Boom Blocks'taki desenle ayni: metinler `strings.xml` yerine
 * Kotlin'de [pick] ile secilir — oyun icinde dil degistirince Activity'yi
 * yeniden yaratmaya gerek kalmaz (Compose durumu korunur).
 */
enum class AppLanguage(val code: String, val displayName: String) {
    TR("tr", "Türkçe"),
    EN("en", "English");

    fun pick(tr: String, en: String): String = when (this) {
        TR -> tr
        EN -> en
    }

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN

        /** Ilk kurulumda cihazin diline gore akilli varsayilan. */
        fun fromSystemLocale(): AppLanguage =
            if (Locale.getDefault().language.lowercase() == "tr") TR else EN
    }
}
