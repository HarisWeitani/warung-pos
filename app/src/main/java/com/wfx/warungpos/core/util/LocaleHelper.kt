package com.wfx.warungpos.core.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * DEFECT-015: applies a per-app language preference so it's actually visible in the UI.
 *
 * Two approaches were tried and rejected before this one:
 * 1. A hand-rolled `ContextThemeWrapper` + `CompositionLocalProvider(LocalContext, LocalConfiguration)`
 *    override in `WarungPosApp.kt` — compiled and ran without error but silently never changed
 *    what `stringResource()` resolved to.
 * 2. `androidx.appcompat.app.AppCompatDelegate.setApplicationLocales()` — this is the officially
 *    documented AndroidX mechanism, but it only actually applies when called from a context that
 *    already has a live `AppCompatDelegate` (i.e. an `AppCompatActivity`, or a manually-created
 *    delegate). `MainActivity` here is a plain `ComponentActivity` (its theme descends from a
 *    platform `android:Theme.Material.*`, not an AppCompat/MaterialComponents theme, so switching
 *    it to `AppCompatActivity` would require a much larger theme migration). Confirmed
 *    empirically: calling it from `Application.onCreate()` (before any Activity exists) left
 *    `AppCompatDelegate.getApplicationLocales()` reading back empty immediately afterward — the
 *    call was silently a no-op.
 *
 * This calls the real platform `LocaleManager` API (API 33+) directly instead, which — unlike
 * the AppCompatDelegate wrapper — works with any [Context], including the Application context,
 * and needs no AppCompatActivity: it's a genuine OS-level per-app language setting, the same
 * mechanism Settings > Apps > App languages uses, and the OS handles recomposing/recreating any
 * running Activity to reflect it, the same as any other Configuration change (e.g. rotation).
 *
 * Below API 33 there is no equivalent public framework API without AppCompatActivity, so this is
 * a no-op there — a known, documented limitation (minSdk 26), not a regression: those devices
 * simply keep today's behavior rather than gaining per-app language.
 */
fun applyAppLocale(context: Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    }
}
