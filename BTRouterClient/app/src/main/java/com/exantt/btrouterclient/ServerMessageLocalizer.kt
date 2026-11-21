package com.exantt.btrouterclient

/**
 * Локализует статусные и служебные строки, приходящие от BTRouterServer по Bluetooth,
 * чтобы английский интерфейс Client работал без изменений кода Server.
 */
object ServerMessageLocalizer {

    private val EXACT_MESSAGES_EN = mapOf(
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

    private val SMS_TOO_LONG_REGEX =
        Regex("^Слишком длинное СМС \\(макс\\. (\\d+) частей, получено (\\d+)\\)$")

    private val SEND_ERROR_CODE_REGEX =
        Regex("^Ошибка отправки \\(код (-?\\d+)\\)$")

    fun localizeOperator(raw: String, isEnglish: Boolean): String {
        if (!isEnglish) {
            return raw.ifBlank { "Неизвестно" }
        }
        return when (raw) {
            "", "Неизвестно" -> "Unknown"
            "Нет сети" -> "No service"
            else -> raw
        }
    }

    fun localizeDataStatus(raw: String, isEnglish: Boolean): String {
        if (!isEnglish) {
            return raw.ifBlank { "Неизвестно" }
        }
        return when (raw) {
            "Моб. сеть" -> "Mobile"
            "Моб (нет инета)" -> "Mobile (no net)"
            "В сети" -> "Online"
            "Откл" -> "Off"
            "Неизвестно", "" -> "Unknown"
            else -> raw
        }
    }

    fun isDataStatusOffline(raw: String): Boolean {
        return raw == "Откл" ||
            raw == "Off" ||
            raw.contains("нет инета") ||
            raw.contains("no net", ignoreCase = true)
    }

    fun localizeCallerNumber(raw: String, isEnglish: Boolean): String {
        return if (raw.isBlank() || raw == "Скрытый номер") {
            if (isEnglish) "Hidden number" else "Скрытый номер"
        } else {
            raw
        }
    }

    fun localizeSenderNumber(raw: String, isEnglish: Boolean): String {
        return if (raw.isBlank() || raw == "Неизвестный") {
            if (isEnglish) "Unknown" else "Неизвестный"
        } else {
            raw
        }
    }

    fun localizeServerMessage(raw: String, isEnglish: Boolean): String {
        if (!isEnglish || raw.isBlank()) return raw

        EXACT_MESSAGES_EN[raw]?.let { return it }

        SMS_TOO_LONG_REGEX.matchEntire(raw)?.let { m ->
            return "SMS is too long (max ${m.groupValues[1]} parts, got ${m.groupValues[2]})"
        }

        SEND_ERROR_CODE_REGEX.matchEntire(raw)?.let { m ->
            return "Send error (code ${m.groupValues[1]})"
        }

        return raw
    }
}
