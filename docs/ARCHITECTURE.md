# Mimari

Tek modüllü (`:app`) bir Android uygulaması. Kotlin + Jetpack Compose.
Sunucu yok, hesap yok, WebView yok — oyun reklamlar dışında tamamen çevrimdışı.

## Katmanlar

```
ui/          Compose ekranları, navigasyon, oyun döngüsü ve çizim
  game/        GameScreen (döngü + kontroller + overlay'ler), GameRenderer (Canvas çizimi)
  menu/ levels/ garage/ missions/ settings/   meta ekranlar
  common/      paylaşılan bileşenler (KronScreen, KronCard, PrimaryButton, StarRow …)
  theme/       renk paleti ve tipografi
  KronViewModel.kt   tüm ekranların paylaştığı tek ViewModel

game/        SAF KOTLIN simülasyon — Android bağımlılığı yok
  GameEngine.kt      fizik, trafik, çarpışma, perfect dodge, bitiş koşulları
  GameConfig.kt      TÜM denge sabitleri
  LevelCatalog.kt    bölüm tanımları (veri)
  LevelEvaluator.kt  yıldız hesabı
  UpgradeCatalog.kt  yükseltme maliyetleri ve etkileri
  GameModels.kt      LevelDef / Objective / RunStats / RunResult / GameEvent

data/        Kalıcı veri
  GameStateRepository.kt  DataStore Preferences (tek dosya: kron_drive_progress)
  PlayerProgress.kt       oyuncu durumu + BoosterType + görev modelleri
  DailyChallengeGenerator.kt / WeeklyMissionGenerator.kt   tarihten türetilen görevler
  AppLanguage.kt          TR/EN metin seçimi

ads/         AdMob: AdIds (tek kimlik noktası), Banner / Interstitial / Rewarded
audio/       EngineSoundManager (AudioTrack ile sentezlenen motor sesi)
```

## Veri akışı

```
GameScreen ── her karede ──> GameEngine.step(dt) ──> List<GameEvent>
    │                                                     │
    │ çizim                                               ├─ PerfectDodge → HUD banner
    ▼                                                     ├─ BoostStarted → ses
GameRenderer (Compose Canvas)                             └─ Finished → RunResult
                                                                        │
                                          KronViewModel.onRunFinished(result)
                                                                        │
                                              GameStateRepository (DataStore)
                                                                        │
                                        playerProgress / weeklyMissions / dailyChallenge
                                                       (Flow → StateFlow → ekranlar)
```

Kritik kural: **simülasyon durumu Compose durumu değildir.** `GameEngine` düz
alanlar tutar; ekran her karede `withFrameNanos` ile bir adım atar ve tuvali
yeniden çizer. Composable yeniden bestelemesi 60 Hz'de tetiklenmez — HUD ~her 3
karede bir güncellenir, `phase` ise ayrı bir snapshot durumuna yansıtılır.
Bu ayrım olmasaydı her karede tüm ekran ağacı yeniden bestelenirdi.

## Koordinat sistemi

Motor **dp uzayında** çalışır (HTML'in CSS pikseliyle aynı ölçek). `GameRenderer`
sahneyi `density` ile ölçekler. Ham piksel kullanılsaydı yüksek DPI telefonlarda
yol ve araçlar minicik kalırdı.

## Ekran akışı

```
Menü ──> Kariyer (bölüm haritası) ──> Oyun ──> (sonuç) ──> Sonraki bölüm / Tekrar / Menü
  ├──> Sonsuz Mod ──> Oyun
  ├──> Günlük Görev ──> Oyun
  ├──> Garaj (yükseltme + booster satın alma)
  ├──> Görevler (haftalık kademeler + sandık + günlük)
  └──> Ayarlar (ses, dil)
```

Oyun ekranı geri dönerken backstack'te birikmez (`popUpTo`), aksi halde geri
tuşu oyuncuyu eski koşulara götürürdü.

## Reklam sınırları

- **Banner:** yalnızca menü/garaj/görev/ayar ekranlarında, en altta. Oyun
  ekranında banner yok — kontrollerin üstüne reklam gelmemeli.
- **Interstitial:** her bölümde değil, `GameConfig.INTERSTITIAL_EVERY_N_LEVELS`
  eşiğinde ve yalnızca sonuç ekranından çıkarken.
- **Rewarded:** tek kullanım — çarpışma sonrası "devam et"
  (`GameConfig.REVIVE_MAX_PER_RUN` kadar).
- Reklam yüklenemezse akış **asla** bloklanmaz; ödül sadece SDK'nın gerçek
  "kazanıldı" geri çağrısında verilir.

## Kalıcılık

Tek bir DataStore Preferences dosyası. Map/Set alanları `anahtar:değer,…`
string'i olarak kodlanır (Room/serialization bağımlılığı eklememek için).
Coin harcaması, yükseltme ve ödül talebi gibi oku-değiştir-yaz işlemleri tek bir
`edit {}` bloğunda yapılır — DataStore bunu atomik çalıştırır, çift tıklama
coini iki kez harcayamaz.

Yedekleme: Android auto-backup açık; hassas veri yok.
