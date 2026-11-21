package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {

    @Test
    fun normalizeLanguageDefaultsToRussianExceptExplicitEnglish() {
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage(null))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage(""))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage("ru"))
        assertEquals(AppLocale.LANG_RU, AppLocale.normalizeLanguage("tr"))
        assertEquals(AppLocale.LANG_EN, AppLocale.normalizeLanguage("en"))
    }

    @Test
    fun localizeOperatorTranslatesKnownValuesInEnglishAndPreservesCarrierNames() {
        assertEquals("No service", ServerMessageLocalizer.localizeOperator("Нет сети", isEnglish = true))
        assertEquals("Unknown", ServerMessageLocalizer.localizeOperator("Неизвестно", isEnglish = true))
        assertEquals("Unknown", ServerMessageLocalizer.localizeOperator("", isEnglish = true))
        assertEquals("Turkcell", ServerMessageLocalizer.localizeOperator("Turkcell", isEnglish = true))

        assertEquals("Нет сети", ServerMessageLocalizer.localizeOperator("Нет сети", isEnglish = false))
        assertEquals("Неизвестно", ServerMessageLocalizer.localizeOperator("", isEnglish = false))
    }

    @Test
    fun localizeDataStatusAndOfflineDetectionHandleAllServerValues() {
        assertEquals("Mobile", ServerMessageLocalizer.localizeDataStatus("Моб. сеть", isEnglish = true))
        assertEquals("Mobile (no net)", ServerMessageLocalizer.localizeDataStatus("Моб (нет инета)", isEnglish = true))
        assertEquals("Online", ServerMessageLocalizer.localizeDataStatus("В сети", isEnglish = true))
        assertEquals("Off", ServerMessageLocalizer.localizeDataStatus("Откл", isEnglish = true))
        assertEquals("Unknown", ServerMessageLocalizer.localizeDataStatus("Неизвестно", isEnglish = true))
        assertEquals("Unknown", ServerMessageLocalizer.localizeDataStatus("", isEnglish = true))
        assertEquals("Wi-Fi", ServerMessageLocalizer.localizeDataStatus("Wi-Fi", isEnglish = true))
        assertEquals("5G", ServerMessageLocalizer.localizeDataStatus("5G", isEnglish = true))
        assertEquals("4G/LTE", ServerMessageLocalizer.localizeDataStatus("4G/LTE", isEnglish = true))

        assertEquals("Откл", ServerMessageLocalizer.localizeDataStatus("Откл", isEnglish = false))
        assertEquals("Неизвестно", ServerMessageLocalizer.localizeDataStatus("", isEnglish = false))

        assertTrue(ServerMessageLocalizer.isDataStatusOffline("Откл"))
        assertTrue(ServerMessageLocalizer.isDataStatusOffline("Off"))
        assertTrue(ServerMessageLocalizer.isDataStatusOffline("Моб (нет инета)"))
        assertTrue(ServerMessageLocalizer.isDataStatusOffline("Mobile (no net)"))
        assertFalse(ServerMessageLocalizer.isDataStatusOffline("Wi-Fi"))
        assertFalse(ServerMessageLocalizer.isDataStatusOffline("4G/LTE"))
    }

    @Test
    fun localizeCallerAndSenderNumbersTranslateFallbacks() {
        assertEquals("Hidden number", ServerMessageLocalizer.localizeCallerNumber("Скрытый номер", isEnglish = true))
        assertEquals("Hidden number", ServerMessageLocalizer.localizeCallerNumber("", isEnglish = true))
        assertEquals("+90 555 000 00 00", ServerMessageLocalizer.localizeCallerNumber("+90 555 000 00 00", isEnglish = true))
        assertEquals("Скрытый номер", ServerMessageLocalizer.localizeCallerNumber("Скрытый номер", isEnglish = false))

        assertEquals("Unknown", ServerMessageLocalizer.localizeSenderNumber("Неизвестный", isEnglish = true))
        assertEquals("Unknown", ServerMessageLocalizer.localizeSenderNumber("", isEnglish = true))
        assertEquals("QNB", ServerMessageLocalizer.localizeSenderNumber("QNB", isEnglish = true))
        assertEquals("Неизвестный", ServerMessageLocalizer.localizeSenderNumber("Неизвестный", isEnglish = false))
    }

    @Test
    fun localizeServerMessageTranslatesStaticAndParameterizedResponses() {
        val exactSamples = listOf(
            "Входящий звонок уже не активен" to "Incoming call is no longer active",
            "Входящий вызов сброшен" to "Incoming call declined",
            "Не удалось сбросить вызов (проверьте права)" to "Failed to decline call (check permissions)",
            "Некорректный формат номера" to "Invalid phone number format",
            "На Server нет разрешения CALL_PHONE" to "Server lacks CALL_PHONE permission",
            "Звонок запущен на Server" to "Call started on Server",
            "Ошибка запуска звонка на Server" to "Failed to start call on Server",
            "Некорректный номер или пустое сообщение" to "Invalid number or empty message",
            "На Server нет разрешения SEND_SMS" to "Server lacks SEND_SMS permission",
            "SMS-модем недоступен на Server" to "SMS modem unavailable on Server",
            "Не удалось подготовить части СМС" to "Failed to prepare SMS parts",
            "Передано в модем Server" to "Handed off to Server modem",
            "Ошибка модема Server при отправке СМС" to "Server modem error while sending SMS",
            "На Server нет разрешения READ_SMS" to "Server lacks READ_SMS permission",
            "Ошибка чтения СМС на Server" to "Error reading SMS on Server",
            "На Server нет разрешения READ_CONTACTS" to "Server lacks READ_CONTACTS permission",
            "Ошибка чтения контактов на Server" to "Error reading contacts on Server",
            "СМС доставлено в сеть оператора" to "SMS handed off to carrier network",
            "Нет сотовой сети на Server" to "No cellular network on Server",
            "Сотовый модуль Server выключен" to "Server cellular radio is off",
            "Ошибка PDU модема" to "Modem PDU error",
            "Сбой отправки оператором" to "Carrier send failure",
            "Ошибка отправки СМС" to "Failed to send SMS",
            "На Server нет разрешения POST_NOTIFICATIONS" to "Server lacks POST_NOTIFICATIONS permission",
            "Команда включения точки доступа отправлена на Server" to "Hotspot ON command sent to Server",
            "Команда выключения точки доступа отправлена на Server" to "Hotspot OFF command sent to Server",
            "Ошибка отправки уведомления точки доступа на Server" to "Failed to post hotspot trigger notification on Server",
            "Команда включения мобильного интернета отправлена на Server" to "Mobile data ON command sent to Server",
            "Команда выключения мобильного интернета отправлена на Server" to "Mobile data OFF command sent to Server",
            "Ошибка отправки уведомления мобильного интернета на Server" to "Failed to post mobile data trigger notification on Server",
            "Таймаут ответа модема Server" to "Server modem response timeout",
            "Очередь отправки СМС на Server переполнена" to "Server SMS send queue is full"
        )
        for ((ru, en) in exactSamples) {
            assertEquals(en, ServerMessageLocalizer.localizeServerMessage(ru, isEnglish = true))
            assertEquals(ru, ServerMessageLocalizer.localizeServerMessage(ru, isEnglish = false))
        }

        assertEquals(
            "SMS is too long (max 10 parts, got 12)",
            ServerMessageLocalizer.localizeServerMessage(
                "Слишком длинное СМС (макс. 10 частей, получено 12)",
                isEnglish = true
            )
        )
        assertEquals(
            "Send error (code 4)",
            ServerMessageLocalizer.localizeServerMessage("Ошибка отправки (код 4)", isEnglish = true)
        )
        assertEquals(
            "Unrecognized message",
            ServerMessageLocalizer.localizeServerMessage("Unrecognized message", isEnglish = true)
        )
    }
}
