package com.miniappfactory.krondrive.data

/**
 * Haftalik gorevler: 5 sabit gorev, her birinde 3 kademe. Kademe hedefleri
 * KUMULATIFTIR (ikinci kademe, birincinin ustune degil bastan itibaren sayilir).
 *
 * Hafta siniri sadece ilerlemeyi/odul talebini sifirlar; gorevlerin kendisi
 * sabittir (5 tip = 5 gorev, rotasyona gerek yok).
 */
object WeeklyMissionGenerator {

    /** Tum kademeler tamamlanince acilan haftalik sandigin coin odulu. */
    const val WEEKLY_CHEST_COINS = 750

    /** Sandik ayrica bir guclendirici verir. */
    val WEEKLY_CHEST_BOOSTER = BoosterType.SECOND_CHANCE

    const val CHEST_CLAIM_KEY = "weekly_chest"

    private val MISSIONS = listOf(
        WeeklyMissionDef(
            id = "complete_levels",
            titleTr = "{count} Bölüm Tamamla",
            titleEn = "Complete {count} Levels",
            tiers = listOf(MissionTier(3, 40), MissionTier(8, 60), MissionTier(15, 100)),
            type = MissionType.COMPLETE_LEVELS
        ),
        WeeklyMissionDef(
            id = "drive_distance",
            titleTr = "Toplam {count} m Sür",
            titleEn = "Drive {count} m in Total",
            tiers = listOf(MissionTier(5000, 40), MissionTier(15000, 60), MissionTier(25000, 100)),
            type = MissionType.DRIVE_DISTANCE
        ),
        WeeklyMissionDef(
            id = "pass_vehicles",
            titleTr = "{count} Araç Geç",
            titleEn = "Pass {count} Vehicles",
            tiers = listOf(MissionTier(80, 40), MissionTier(200, 60), MissionTier(400, 100)),
            type = MissionType.PASS_VEHICLES
        ),
        // PERFECT DODGE GOREVI KALDIRILDI (2026-08-17).
        //
        // Ayni gun bu hedef 17 kariyer bolumunden kaldirilmisti; gorev
        // tarafinda kalmis oldugu fark edilmedi. Olcum (LevelCurveTest):
        // temkinli oyun OTUZ BOLUMUN HICBIRINDE tek bir dodge yapmiyor.
        // Yani haftada 60 dodge isteyen bu gorev, temkinli oyuncu icin
        // ulasilamazdi ve haftalik sandigi (haftalik gelirin ~%43'u) ona
        // tamamen kapatiyordu.
        //
        // Yerine mesafe: her oynayis bicimiyle ilerler, beceri kapisi yok.
        WeeklyMissionDef(
            id = "boost_distance",
            titleTr = "Boost'la {count} m Git",
            titleEn = "Travel {count} m on Boost",
            tiers = listOf(MissionTier(2000, 40), MissionTier(5000, 60), MissionTier(9000, 100)),
            type = MissionType.DRIVE_DISTANCE
        ),
        WeeklyMissionDef(
            id = "big_combos",
            titleTr = "{count} Kez 5x Combo Yap",
            titleEn = "Make {count} 5x Combos",
            tiers = listOf(MissionTier(3, 40), MissionTier(7, 60), MissionTier(12, 100)),
            type = MissionType.BIG_COMBOS
        )
    )

    fun forWeek(weekId: String): List<WeeklyMissionDef> {
        require(weekId.isNotBlank())
        return MISSIONS
    }
}
