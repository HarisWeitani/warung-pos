package com.wfx.warungpos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.wfx.warungpos.core.common.AppPreferences
import com.wfx.warungpos.core.util.applyAppLocale
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WarungPosApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        // DEFECT-015: apply the persisted language preference before any Activity/Compose UI is
        // created, so the very first frame already reflects it — not just after the user
        // revisits Settings. See LocaleHelper.kt for why this calls the platform LocaleManager
        // directly rather than AppCompatDelegate.
        applyAppLocale(this, appPreferences.getLanguage())

        // Must be called before any DatabaseReference is obtained
        FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
            setPersistenceCacheSizeBytes(5L * 1024 * 1024)
        }
        // No PII in crash reports: do not set user email or order content as custom keys.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
