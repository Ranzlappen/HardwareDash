package dev.ranzlappen.gadget.feature.radios.ir.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class IrLibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val brands: List<IrLibraryBrand> by lazy {
        runCatching {
            val raw = context.assets.open("ir_library.json").bufferedReader().readText()
            json.decodeFromString<List<IrLibraryBrand>>(raw)
        }.getOrDefault(emptyList())
    }
}
