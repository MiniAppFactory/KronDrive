---
name: resume
description: Yeni bir oturumun basinda kaldigi yerden devam etmesi icin kullan. HANDOVER.md'yi, aktif projelerin git durumunu ve acik TODO maddelerini okuyup kisa bir "su an neredeyiz / sirada ne var" ozeti cikarir.
---

# Resume — kaldigin yerden devam et

Bu skill, oturum sifirlandiktan sonra baglami hizlica geri kurmak icindir.
Amac: kullaniciya dosya dokumu degil, **karar verilebilir bir ozet** sunmak.

## Adimlar

1. **Master handover'i oku:** `C:\Users\bhdre\APPDeveloper\HANDOVER.md`
   Bu dosya her iki aktif projenin (Kaboom Blocks, Kron Drive) durumunu,
   bekleyen kararlari ve oncelik sirasini tutar. Once bunu oku.

2. **Aktif projenin git durumunu dogrula.** Handover eskimis olabilir — diske guven:
   ```
   cd C:/Users/bhdre/APPDeveloper/projects/Boom-Blocks/source
   git log --oneline -3 && git status --short
   ```
   Commit edilmemis degisiklik varsa bunu ozetin BASINDA belirt.

3. **Proje bazli detay dosyalarini oku** (sadece calisilacak proje icin):
   - Kaboom Blocks: `projects/Boom-Blocks/docs/CHANGELOG.md` (en ustteki 1-2 Faz)
     ve `projects/Boom-Blocks/docs/TODO.md` (acik maddeler)
   - Kron Drive: `projects/Kron-Drive/docs/CHANGELOG.md` (en ustteki faz) ve
     `projects/Kron-Drive/docs/RELEASE_CHECKLIST.md` (acik maddeler)

4. **Ozeti su formatta ver:**
   - Son tamamlanan is (tek cumle)
   - Commit/push durumu (temiz mi, bekleyen var mi)
   - Yayindaki surum vs. koddaki surum farki (testcilerde eksik duzeltme var mi)
   - Bekleyen kararlar (kullanicidan onay gerektirenler, madde madde)
   - Onerilen sonraki adim (tek bir net oneri, secenek listesi degil)

## Kurallar

- Handover ile disk celisirse **disk dogrudur** — handover'i guncelle.
- Ozet kisa olsun; kullanici dosya icerigi degil durum istiyor.
- Is bitince (veya anlamli bir asama tamamlaninca) `HANDOVER.md`'yi guncelle ki
  bir sonraki oturum yine temiz baslasin.
