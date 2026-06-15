package dev.ranzlappen.gadget.feature.radios.nfc

import android.content.Context
import android.nfc.NfcAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcAdapterWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isAvailable(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true
}
