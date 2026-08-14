package com.miniappfactory.krondrive.game

/**
 * Yildiz hesabi tek yerde. Kural:
 *
 * - Bolum tamamlanmadiysa (kaza veya sure limiti) **0 yildiz**.
 * - Yildizlar SIRALI kazanilir: 2. yildiz icin 1. hedef de saglanmis olmali
 *   (UI'daki ★☆☆ / ★★☆ / ★★★ gosterimiyle tutarli).
 * - Sonsuz modda yildiz yoktur.
 */
object LevelEvaluator {

    fun stars(level: LevelDef?, stats: RunStats): Int {
        if (level == null || !level.awardsStars) return 0
        if (!stats.completed) return 0
        return tiersReached(level.stars, stats)
    }

    /**
     * Sirali olarak saglanan hedef sayisi. Yildiz hesabinin ozu ama gunluk
     * gorevin kademeleri de bunu kullanir — tek farkla: gunluk gorevde
     * `completed` sarti YOKTUR, carpsan bile o ana kadar ulastigin kademeler
     * sayilir (yoksa 25 dodge yapip son saniyede carpan oyuncu hicbir sey
     * alamazdi).
     */
    fun tiersReached(objectives: List<Objective>, stats: RunStats): Int {
        var earned = 0
        for (objective in objectives) {
            if (!objective.isMet(stats)) break
            earned++
        }
        return earned
    }

    /** Sonuc ekraninda her hedefin tek tek durumu. */
    fun starBreakdown(level: LevelDef?, stats: RunStats): List<Boolean> {
        if (level == null) return emptyList()
        val earned = stars(level, stats)
        return level.stars.indices.map { it < earned }
    }
}
