package com.coverdrop.lib.crypto

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid

/** Singleton pattern for LibSodium (otherwise JNA will crash some method reference tables) */
internal object CoverdropSodiumProvider {
    internal val instance: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid())
}
