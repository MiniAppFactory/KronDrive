# Kare zamanlaması ölçümü — 2026-08-17

Cihaz: Samsung SM-G950F (S8), ekran 60 Hz (`dumpsys display` → `fps=60.000004`).
Build: `:app:assembleDebug --offline` (debug, profil kancalı).
Bölüm: Kariyer / Bölüm 1. Tema **CROWD'a sabitlendi** (normalde
`RoadTheme.entries[random.nextInt(...)]`, çizim yükü temalar arası 4 kat
değişiyor → sabitlemeden karşılaştırma gürültü olurdu).

Ölçüm kancaları geçiciydi, koşu sonunda geri alındı (`git diff` temiz).

---

## Kısa cevap

1. **Kırpma hipotezi yanlış.** Oyun ~40 FPS'te değil, **~55–57 FPS**'te dönüyor;
   nominal kare 25 ms değil **16,7 ms**. `deltaSeconds > 0.032` olan karelerin
   oranı %4–5, kaybolan süre saniyede ~9 ms (**wall-clock'un %0,9'u**). Eski
   0,032 sınırı ile 21 saniyede toplam 191 ms sapma birikiyordu — görülebilir
   bir duraksama değil.
2. **Gerçek sorun çizim maliyeti.** `dumpsys gfxinfo` jank oranı **%99,9**,
   toplam kare süresi p50 **35,7 ms** ve karelerin **%96'sında**
   "Slow issue draw commands". Sunum aralığı (vsync→vsync) p50 16,7 ms ama
   karelerin **%10'u çift uzunlukta (33 ms)** — takılma bundan.
3. **Düzeltmenin ölçülebilir bir etkisi yok** ama zararı da yok; koruma amacı
   duruyor. Asıl iş kare başına çizim maliyetini düşürmekte.

---

## 1. Kırpma sayacı

`GameEngine.step` içine geçici sayaç konuldu (kırpılmamış `deltaSeconds`
üzerinden). Çıktı `println` ile logcat'e (`System.out`) basıldı.

```
adb shell am force-stop com.miniappfactory.krondrive
adb logcat -c
adb shell am start -n com.miniappfactory.krondrive/.MainActivity
# 8s -> (540,890) KARIYER -> 3s -> (407,1720) Bolum 1 -> 2s -> (540,1668) BASLA
adb shell input swipe 142 2081 142 2081 20000   # FREN basili, 20 sn
adb logcat -d | grep KDPERF | tail -4
```

Sonuç (fren basılı, düşük yük):

```
KDPERF cap=0.05 theme=CROWD frames=1200 wall=21,05s sim=20,98s drift=67ms
  avgdt=17,5ms fps=57,0
  clip32=56(4,7%)  lost32=191ms(9,1ms/s)
  clipCap=2(0,2%)  lostCap=67ms(3,2ms/s)
  buckets=1144,0,53,0,1,1,1
```

`buckets` = dt histogramı [<20, 20-28, 28-36, 36-45, 45-56, 56-75, >75] ms.

Okunuşu:

- **1144 / 1200 kare = %95,3** tek vsync (16,7 ms) içinde geliyor.
- **53 kare = %4,4** iki vsync (33,3 ms) — atlanmış kare.
- 45 ms üstü **sadece 3 kare** (bölüm açılışı), tamamı `lostCap=67ms`'in kaynağı.
- `clip32 = %4,7`, `lost32 = 9,1 ms/s` → eski 0,032 sınırında simülasyon
  gerçek zamanın **%0,9'u** kadar geride kalıyordu.

Eşik %1'in üzerinde (kare oranı olarak %4,7), ama **kaybolan zaman** olarak
%0,9. Teşhisteki "45 ms'lik karede 13 ms kayboluyor" senaryosu bu cihazda
pratikte gerçekleşmiyor: 45 ms üstü kare 1200'de 3 tane.

## 2. Gerçek kare zamanlaması

```
adb shell dumpsys gfxinfo com.miniappfactory.krondrive reset
# ... 20 sn oynanis ...
adb shell dumpsys gfxinfo com.miniappfactory.krondrive framestats
```

Özet blok (1189 kare):

```
Janky frames: 1188 (99.92%)
50th percentile: 27ms
90th percentile: 40ms
95th percentile: 46ms
99th percentile: 57ms
Number Missed Vsync: 45
Number Slow UI thread: 915
Number Slow issue draw commands: 1138
```

framestats CSV'sinden (son 120 kare) hesaplanan iki ayrı büyüklük:

| | p50 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|
| Toplam kare süresi (`FRAME_COMPLETED - INTENDED_VSYNC`) | 35,7 | 57,6 | 62,4 | 70,2 | 73,3 ms |
| Sunum aralığı (vsync→vsync) | 16,7 | 33,2 | 33,3 | 33,4 | 33,4 ms |

- Sunum aralığı ortalaması **18,34 ms → 54,5 FPS**.
- Kareler: %89,9 tek vsync, **%10,1 çift vsync**, %0 üç+.
- Jank %99,9 çünkü *toplam kare süresi* 16,7 ms bütçesini her karede aşıyor;
  üçlü tamponlama sayesinde sunum cadence'i çoğunlukla 60 Hz'de kalıyor,
  gecikme birikiyor.

Aşama medyanları (ms):

```
input->traversal 1,8 | traversal->draw 0,1 | draw->sync 12,5
sync->issue 0,2 | issue->swap 11,3 | swap->done 4,6
```

**Zaman iki yerde yanıyor:** UI thread'de çizim komutlarının kaydı (12,5 ms) ve
RenderThread'de GPU'ya gönderimi (11,3 ms). Simülasyon (`engine.step`) bu
tabloda görünmüyor — darboğaz Compose Canvas çizimi.
