package com.miniappfactory.krondrive.data

/**
 * Kayit dosyasinin metin kodlamasi — SAF KOTLIN, Android importu YOK.
 *
 * ## Neden ayri bir dosya
 *
 * Bu fonksiyonlar 2026-08-19'a kadar [GameStateRepository]'nin companion
 * nesnesinde `private` idi. Mantiklari Android'e hic bagli degil ama
 * BULUNDUKLARI SINIF `Context` istedigi icin JVM testinden erisilemiyorlardi
 * — yani oyunun kalici verisini yazip okuyan kod tamamen test disindaydi.
 * Ayni gerekce `game/` paketinin saf tutulmasinin da sebebi.
 *
 * ## Formatin bildigi tek kural
 *
 * Girdiler `anahtar:deger,anahtar:deger` seklinde saklanir ve **KACIS
 * KARAKTERI YOKTUR**. Dolayisiyla iceriye giren hicbir kimlik `,` ya da `:`
 * icermemelidir. Iceren tek bir arac/boya/booster kimligi, cozumleme
 * sirasinda o girdiyi sessizce DUSURUR: oyuncu bir daha acildiginda sahip
 * oldugu boyalari kaybeder, hicbir hata da gorunmez.
 *
 * Bu kural [SaveCodecTest] tarafindan hem bicim hem de KATALOG tarafinda
 * dogrulanir (gercek kimlikler taranir).
 */
internal object SaveCodec {

    fun encodeIntMap(map: Map<Int, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decodeIntMap(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0].toIntOrNull()?.let { k -> parts[1].toIntOrNull()?.let { v -> k to v } }
            } else {
                null
            }
        }.toMap()
    }

    /** Sifir ve altindaki sayilar YAZILMAZ: bos booster satiri saklamanin anlami yok. */
    fun encodeBoosterMap(map: Map<BoosterType, Int>): String =
        map.entries.filter { it.value > 0 }.joinToString(",") { "${it.key.name}:${it.value}" }

    fun decodeBoosterMap(raw: String?): Map<BoosterType, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val type = runCatching { BoosterType.valueOf(parts[0]) }.getOrNull()
                val count = parts[1].toIntOrNull()
                if (type != null && count != null) type to count else null
            } else {
                null
            }
        }.toMap()
    }

    fun encodeStringIntMap(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decodeStringIntMap(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[1].toIntOrNull()?.let { v -> parts[0] to v } else null
        }.toMap()
    }

    fun decodeStringSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun encodeStringMap(map: Map<String, String>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decodeStringMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                parts[0] to parts[1]
            } else {
                null
            }
        }.toMap()
    }
}
