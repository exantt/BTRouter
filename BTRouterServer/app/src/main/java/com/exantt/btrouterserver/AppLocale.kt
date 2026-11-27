package com.exantt.btrouterserver

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Управляет языком интерфейса BTRouterServer (RU / EN) для Activity и Service.
 */
object AppLocale {

    const val KEY_APP_LANGUAGE = "app_language"
    const val LANG_RU = "ru"
    const val LANG_EN = "en"

    fun normalizeLanguage(raw: String?): String = if (raw == LANG_EN) LANG_EN else LANG_RU

    fun getLanguage(context: Context): String {
        return try {
            val saved = context
                .getSharedPreferences(BTRouterServerService.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_APP_LANGUAGE, LANG_RU)
            normalizeLanguage(saved)
        } catch (_: Exception) {
            LANG_RU
        }
    }

    fun isEnglish(context: Context): Boolean = getLanguage(context) == LANG_EN

    fun setLanguage(context: Context, language: String): String {
        val resolved = normalizeLanguage(language)
        try {
            context
                .getSharedPreferences(BTRouterServerService.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APP_LANGUAGE, resolved)
                .apply()
        } catch (_: Exception) {
        }
        Locale.setDefault(Locale.forLanguageTag(resolved))
        return resolved
    }

    fun wrap(base: Context): Context {
        val lang = getLanguage(base)
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
