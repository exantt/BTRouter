package com.exantt.btrouterserver

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {

    @Test
    fun appLocaleNormalizesToSupportedLanguages() {
        assertEquals(AppLocale.LANG_EN, AppLocale.normalizeLanguage("en"))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage("ru"))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage("tr"))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage(null))
    }
}
