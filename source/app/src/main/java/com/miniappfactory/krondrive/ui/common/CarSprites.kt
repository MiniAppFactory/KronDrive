package com.miniappfactory.krondrive.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import com.miniappfactory.krondrive.R
import com.miniappfactory.krondrive.game.CarCatalog

/**
 * Arac sprite'lari — iki katmanli, calisma aninda boyanan cizimler.
 *
 * ## Neden iki katman
 *
 * Oyuncu 7 govdeyi 10 boyayla surebiliyor. Hazir renkli PNG kullansaydik 70
 * dosya gerekirdi. Bunun yerine her govde iki dosyaya ayrilmis durumda:
 *
 *  * `car_<id>_body`   — GRI TONLAMALI boyanabilir alan. Yalnizca isik/golge
 *    tasir; cizerken secili boyayla CARPILIR ([androidx.compose.ui.graphics.BlendMode.Modulate]),
 *    boylece tek dosya 10 boyayi da veriyor.
 *  * `car_<id>_detail` — cam, lastik, far, stop lambasi, serit. Renkli ve
 *    oldugu gibi ustune cizilir, boyadan etkilenmez.
 *
 * Dosyalar `tools/build_car_sprites.py` ile `incoming/car_refs/` altindaki
 * referans cizim + maske ciftlerinden uretiliyor. Sprite'i elle DUZENLEME:
 * kaynak referanstir, betik yeniden calistirildiginda uzerine yazilir.
 *
 * ## Hizalama sozlesmesi
 *
 * Sprite'lar [CarCatalog]'un cizim kutusuyla ayni orana (40:76) uretilir ve
 * ciziciye tam o kutu verilir. Yani sprite'a gecmek carpisma kutusuna
 * DOKUNMAZ — vektor cizimde olan hitbox ortusmesi aynen korunur.
 *
 * ## Geri dusus
 *
 * Bir govdenin sprite'i yoksa [CarSpriteSet.of] null doner ve cizici vektor
 * yoluna ([drawCarParts]) duser. Vektor cizim kaldirilmadi: hem guvenlik agi,
 * hem de magaza gorselleri onu kullaniyor.
 */
@Immutable
class CarSprite(val body: ImageBitmap, val detail: ImageBitmap)

/** Govde kimligi -> sprite. Sprite'i olmayan govde icin [of] null doner. */
@Immutable
class CarSpriteSet internal constructor(private val byShapeId: Map<String, CarSprite>) {

    fun of(shapeId: String): CarSprite? = byShapeId[shapeId]

    companion object {
        /** Hicbir sprite yok — cizici tamamen vektor yoluna duser. */
        val Empty = CarSpriteSet(emptyMap())
    }
}

/**
 * Sprite'i olan govdeler. Liste UZUNLUGU sabit olmali: [rememberCarSprites]
 * bunun uzerinde donerek `imageResource` cagiriyor ve Compose'un konumsal
 * hafizasi degisken uzunluklu donguden hoslanmaz.
 *
 * Bir govdeyi buradan cikarmak onu vektor cizime geri dondurur (kirmaz).
 */
private val SPRITE_TABLE: List<Triple<String, Int, Int>> = listOf(
    Triple(CarCatalog.SHAPE_HATCHBACK, R.drawable.car_hatchback_body, R.drawable.car_hatchback_detail),
    Triple(CarCatalog.SHAPE_RACE_SEDAN, R.drawable.car_race_sedan_body, R.drawable.car_race_sedan_detail),
    Triple(CarCatalog.SHAPE_KUS_SLX, R.drawable.car_kus_slx_body, R.drawable.car_kus_slx_detail),
    Triple(CarCatalog.SHAPE_MOUNTAIN_GOAT, R.drawable.car_mountain_goat_body, R.drawable.car_mountain_goat_detail),
    Triple(CarCatalog.SHAPE_MUSCLE, R.drawable.car_muscle_body, R.drawable.car_muscle_detail),
    Triple(CarCatalog.SHAPE_MUSCLE_67, R.drawable.car_muscle_67_body, R.drawable.car_muscle_67_detail),
    Triple(CarCatalog.SHAPE_SUPERCAR, R.drawable.car_supercar_body, R.drawable.car_supercar_detail),
    // Trafik: oyuncunun alamadigi sabit govde. Sprite'a gecmesi ayrica kazanc —
    // sahnede ayni anda onlarca trafik araci var ve her biri vektorde ~20 parca
    // cizdiriyordu.
    Triple(CarCatalog.TRAFFIC_SHAPE_ID, R.drawable.car_traffic_body, R.drawable.car_traffic_detail)
)

/**
 * Surec omru boyunca TEK kopya.
 *
 * Compose'un `@Composable imageResource(id)` bicimi cozulmus bitmap'i CAGRI
 * NOKTASINA gore hatirlar. Garajda ayni anda bir dolu [CarPreview] var
 * (secili arac + her govde icin bir cip) ve her biri ayri bir cagri noktasi
 * oldugundan 16 sprite'i BASTAN cozuyorlardi. Cihazda olculdu (Samsung S8,
 * 2026-08-15): toplam PSS 165 MB -> 302 MB. Tek kopyaya alinca maliyet
 * 16 x 240 x 456 x 4B ~= 7 MB'de sabitleniyor.
 *
 * Bitmap'ler Context tutmuyor (yalnizca piksel), bu yuzden statik tutmak
 * sizinti yaratmaz.
 */
@Volatile
private var cachedSprites: CarSpriteSet? = null

/**
 * Sprite'lari (gerekiyorsa) cozer ve paylasilan kopyayi doner.
 *
 * Composable olmayan `imageResource(res, id)` bicimi bilerek secildi:
 * onbellegi Compose'un konumsal hafizasina degil yukaridaki tek alana
 * baglamak istiyoruz.
 */
@Composable
fun rememberCarSprites(): CarSpriteSet {
    val resources = LocalContext.current.resources
    return cachedSprites ?: remember(resources) {
        val loaded = CarSpriteSet(
            SPRITE_TABLE.associate { (id, bodyRes, detailRes) ->
                id to CarSprite(
                    body = ImageBitmap.imageResource(resources, bodyRes),
                    detail = ImageBitmap.imageResource(resources, detailRes)
                )
            }
        )
        cachedSprites = loaded
        loaded
    }
}
