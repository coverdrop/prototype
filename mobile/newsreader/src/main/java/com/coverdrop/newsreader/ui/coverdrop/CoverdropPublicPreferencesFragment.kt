package com.coverdrop.newsreader.ui.coverdrop

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.coverdrop.lib.COVERDROP_NAMESPACE
import com.coverdrop.lib.background.CoverdropBackgroundService
import com.coverdrop.newsreader.R

/**
 * Allows setting custom sync interval for this prototype (one would not allow that in a procution version).
 */
class CoverdropPublicPreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = COVERDROP_NAMESPACE;
        preferenceManager.sharedPreferencesMode = MODE_PRIVATE;
        setPreferencesFromResource(R.xml.coverdrop_public_preferences, rootKey)
    }


    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "coverdrop_sync_interval") {
            CoverdropBackgroundService.schedule(requireContext())
        }
    }

}
