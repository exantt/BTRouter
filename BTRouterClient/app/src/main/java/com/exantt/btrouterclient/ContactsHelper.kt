package com.exantt.btrouterclient

import java.text.Normalizer
import java.util.Locale

/**
 * Чистые вспомогательные функции для адресной книги:
 * очистка телефонов, дедупликация, сортировка (EN -> RU -> прочее)
 * и динамический поиск с расчётом диапазонов подсветки.
 */
object ContactsHelper {

    data class ContactEntry(
        val name: String,
        val phone: String,
        val normalizedPhone: String,
        val inVisibleGroup: Boolean = true,
        val isPrimaryAccount: Boolean = true,
        val lastUpdatedMs: Long = 0L
    )

    private const val RU_ALPHABET = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
    private val RU_RANK: Map<Char, Int> = RU_ALPHABET.withIndex().associate { it.value to it.index }

    private val SECONDARY_ACCOUNT_MARKERS = listOf(
        "whatsapp", "telegram", "viber", "signal", "skype",
        "imo", "botim", "bip", "wechat", "line", "snapchat"
    )

    private val LATIN_BLOCKS = setOf(
        Character.UnicodeBlock.BASIC_LATIN,
        Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
        Character.UnicodeBlock.LATIN_EXTENDED_A,
        Character.UnicodeBlock.LATIN_EXTENDED_B
    )

    fun cleanDisplayPhone(raw: String): String {
        val sb = StringBuilder(raw.length)
        var prevSpace = false
        for (ch in raw) {
            if (Character.getType(ch) == Character.FORMAT.toInt()) continue
            when {
                ch == '\u00A0' || ch.isWhitespace() -> {
                    if (sb.isNotEmpty() && !prevSpace) {
                        sb.append(' ')
                        prevSpace = true
                    }
                }
                ch == '–' || ch == '—' -> {
                    sb.append('-')
                    prevSpace = false
                }
                else -> {
                    sb.append(ch)
                    prevSpace = false
                }
            }
        }
        return sb.toString().trim().take(40)
    }

    fun canonicalPhoneKey(raw: String): String {
        val sb = StringBuilder(16)
        for (ch in raw) {
            if (Character.getType(ch) == Character.FORMAT.toInt() || ch.isWhitespace()) continue
            if (sb.isEmpty() && ch == '+') {
                sb.append('+')
            } else if (ch in '0'..'9') {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun digitsOnly(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') sb.append(ch)
        }
        return sb.toString()
    }

    fun phoneForInput(contact: ContactEntry): String {
        val cleaned = cleanDisplayPhone(contact.phone)
        return if (BtProtocol.sanitizeAndValidatePhone(cleaned) != null) {
            cleaned
        } else {
            contact.normalizedPhone.ifBlank { canonicalPhoneKey(cleaned) }
        }
    }

    /**
     * Строго 1-to-1 преобразование, чтобы длина и индексы символов
     * совпадали с исходной строкой при подсветке.
     */
    fun normalizeForSearch(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val mapped = when (ch) {
                'İ', 'I', 'ı', 'i' -> 'i'
                'Ş', 'ş' -> 's'
                'Ğ', 'ğ' -> 'g'
                'Ü', 'ü' -> 'u'
                'Ö', 'ö' -> 'o'
                'Ç', 'ç' -> 'c'
                'Ё', 'ё' -> 'е'
                else -> ch.lowercaseChar()
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    fun isPrimaryContactAccount(accountType: String?): Boolean {
        if (accountType.isNullOrBlank()) return true
        val low = accountType.lowercase(Locale.ROOT)
        return SECONDARY_ACCOUNT_MARKERS.none { low.contains(it) }
    }

    private fun hasRealName(entry: ContactEntry): Boolean {
        return entry.name.isNotBlank() &&
            entry.name != entry.phone &&
            canonicalPhoneKey(entry.name) != entry.normalizedPhone
    }

    fun isBetterCandidate(candidate: ContactEntry, existing: ContactEntry): Boolean {
        val candNamed = hasRealName(candidate)
        val existNamed = hasRealName(existing)
        if (candNamed != existNamed) return candNamed

        if (candidate.inVisibleGroup != existing.inVisibleGroup) {
            return candidate.inVisibleGroup
        }
        if (candidate.isPrimaryAccount != existing.isPrimaryAccount) {
            return candidate.isPrimaryAccount
        }
        if (candidate.lastUpdatedMs != existing.lastUpdatedMs) {
            return candidate.lastUpdatedMs > existing.lastUpdatedMs
        }

        val candPlus = candidate.normalizedPhone.startsWith("+")
        val existPlus = existing.normalizedPhone.startsWith("+")
        if (candPlus != existPlus) return candPlus

        return isBetterDisplayPhone(candidate.phone, existing.phone)
    }

    private fun mergeWinnerPlusIfSameDigits(winner: ContactEntry, loser: ContactEntry): ContactEntry {
        if (!winner.normalizedPhone.startsWith("+") &&
            loser.normalizedPhone.startsWith("+") &&
            digitsOnly(winner.normalizedPhone) == digitsOnly(loser.normalizedPhone) &&
            BtProtocol.sanitizeAndValidatePhone(loser.phone) != null
        ) {
            return winner.copy(phone = loser.phone, normalizedPhone = loser.normalizedPhone)
        }
        return winner
    }

    private fun isLocalShortForm(normalizedPhone: String): Boolean {
        if (normalizedPhone.startsWith("+")) return false
        val digits = digitsOnly(normalizedPhone)
        return digits.length == 10 || (digits.length == 11 && (digits.startsWith("0") || digits.startsWith("8")))
    }

    fun deduplicateContacts(raw: List<ContactEntry>): List<ContactEntry> {
        val bestByExactKey = LinkedHashMap<String, ContactEntry>(raw.size)
        for (entry in raw) {
            val cleanPhone = cleanDisplayPhone(entry.phone)
            val canonical = canonicalPhoneKey(entry.normalizedPhone.ifBlank { cleanPhone })
            val digits = digitsOnly(canonical)
            if (digits.length !in 3..15) continue

            val cleanName = entry.name.trim().ifEmpty { cleanPhone }.take(120)
            val candidate = entry.copy(
                name = cleanName,
                phone = cleanPhone.ifEmpty { canonical },
                normalizedPhone = canonical
            )

            val exactKey = if (digits.length >= 10) digits else canonical
            val existing = bestByExactKey[exactKey]
            if (existing == null) {
                bestByExactKey[exactKey] = candidate
            } else {
                bestByExactKey[exactKey] = if (isBetterCandidate(candidate, existing)) {
                    mergeWinnerPlusIfSameDigits(candidate, existing)
                } else {
                    mergeWinnerPlusIfSameDigits(existing, candidate)
                }
            }
        }

        val pass1 = bestByExactKey.values.toMutableList()
        if (pass1.size <= 1) return pass1

        // Проход 2: слияние локального формата (0/8/10 цифр без +) с однозначным международным (+<cc>+last10)
        val intlIndicesByLast10 = HashMap<String, MutableList<Int>>(pass1.size)
        for (i in pass1.indices) {
            val item = pass1[i]
            val digits = digitsOnly(item.normalizedPhone)
            if (item.normalizedPhone.startsWith("+") && digits.length >= 11) {
                intlIndicesByLast10.getOrPut(digits.takeLast(10)) { ArrayList(2) }.add(i)
            }
        }

        val removed = BooleanArray(pass1.size)
        for (i in pass1.indices) {
            if (removed[i]) continue
            val local = pass1[i]
            if (!isLocalShortForm(local.normalizedPhone)) continue

            val last10 = digitsOnly(local.normalizedPhone).takeLast(10)
            val intlMatches = intlIndicesByLast10[last10] ?: continue
            if (intlMatches.size != 1) continue

            val intlIdx = intlMatches[0]
            if (removed[intlIdx]) continue
            val intl = pass1[intlIdx]

            pass1[intlIdx] = if (isBetterCandidate(local, intl)) local else intl
            removed[i] = true
        }

        val finalResult = ArrayList<ContactEntry>(pass1.size)
        for (i in pass1.indices) {
            if (!removed[i]) {
                finalResult.add(pass1[i])
            }
        }
        return finalResult
    }

    fun findLatestNameForPhone(
        normalizedPhone: String,
        namesByExactDigits: Map<String, String>,
        namesByLast10: Map<String, String>
    ): String? {
        val digits = digitsOnly(normalizedPhone)
        if (digits.isEmpty()) return null
        namesByExactDigits[digits]?.let { return it }
        if (digits.length >= 10) {
            namesByLast10[digits.takeLast(10)]?.let { return it }
        }
        return null
    }

    private fun isBetterDisplayPhone(candidate: String, current: String): Boolean {
        val candValid = BtProtocol.sanitizeAndValidatePhone(candidate) != null
        val currValid = BtProtocol.sanitizeAndValidatePhone(current) != null
        if (candValid != currValid) return candValid
        val candFormatted = candidate.any { it == ' ' || it == '-' || it == '(' || it == ')' }
        val currFormatted = current.any { it == ' ' || it == '-' || it == '(' || it == ')' }
        return candFormatted && !currFormatted
    }

    fun sortContactsEnThenRu(contacts: List<ContactEntry>): List<ContactEntry> {
        return contacts.sortedWith { a, b ->
            val cmp = compareContactNames(a.name, b.name)
            if (cmp != 0) cmp else a.normalizedPhone.compareTo(b.normalizedPhone)
        }
    }

    private fun isLatinLetter(ch: Char): Boolean {
        if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in "ÇçĞğİıÖöŞşÜü") return true
        return ch.isLetter() && Character.UnicodeBlock.of(ch) in LATIN_BLOCKS
    }

    private fun isCyrillicLetter(ch: Char): Boolean {
        return ch in '\u0400'..'\u04FF' && ch.isLetter()
    }

    fun sectionLetter(name: String): String {
        val first = name.trim().firstOrNull() ?: return "#"
        return when {
            isLatinLetter(first) -> when (first) {
                'Ç', 'ç' -> "C"
                'Ğ', 'ğ' -> "G"
                'İ', 'I', 'ı', 'i' -> "I"
                'Ö', 'ö' -> "O"
                'Ş', 'ş' -> "S"
                'Ü', 'ü' -> "U"
                in 'A'..'Z', in 'a'..'z' -> first.uppercaseChar().toString()
                else -> Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
                    .firstOrNull { it in 'A'..'Z' || it in 'a'..'z' }
                    ?.uppercaseChar()
                    ?.toString()
                    ?: "#"
            }
            isCyrillicLetter(first) -> first.uppercaseChar().toString()
            else -> "#"
        }
    }

    private fun alphabetGroup(name: String): Int {
        val first = name.trim().firstOrNull() ?: return 2
        return when {
            isLatinLetter(first) -> 0
            isCyrillicLetter(first) -> 1
            else -> 2
        }
    }

    private fun compareContactNames(a: String, b: String): Int {
        val groupA = alphabetGroup(a)
        val groupB = alphabetGroup(b)
        if (groupA != groupB) return groupA.compareTo(groupB)

        val trimA = a.trim()
        val trimB = b.trim()
        return when (groupA) {
            0 -> {
                val secCmp = sectionLetter(trimA).compareTo(sectionLetter(trimB))
                if (secCmp != 0) return secCmp
                val foldCmp = normalizeForSearch(trimA).compareTo(normalizeForSearch(trimB))
                if (foldCmp != 0) foldCmp else trimA.lowercase(Locale.ROOT).compareTo(trimB.lowercase(Locale.ROOT))
            }
            1 -> {
                compareRussianStrings(trimA, trimB)
            }
            else -> trimA.lowercase(Locale.ROOT).compareTo(trimB.lowercase(Locale.ROOT))
        }
    }

    private fun compareRussianStrings(a: String, b: String): Int {
        val len = minOf(a.length, b.length)
        for (i in 0 until len) {
            val ra = charOrderRank(a[i])
            val rb = charOrderRank(b[i])
            if (ra != rb) return ra.compareTo(rb)
        }
        return a.length.compareTo(b.length)
    }

    private fun charOrderRank(ch: Char): Int {
        val low = ch.lowercaseChar()
        val ruIdx = RU_RANK[low]
        if (ruIdx != null) return 1000 + ruIdx
        if (low.isWhitespace()) return 10
        if (low in '0'..'9') return 100 + (low - '0')
        return 2000 + low.code
    }

    private fun isPhoneLikeQuery(trimmedQuery: String): Boolean {
        if (!trimmedQuery.any { it in '0'..'9' }) return false
        return trimmedQuery.all { ch ->
            ch in '0'..'9' || ch == '+' || ch == '-' || ch == '(' || ch == ')' || ch.isWhitespace()
        }
    }

    fun matchesQuery(contact: ContactEntry, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true

        val normQuery = normalizeForSearch(trimmed)
        if (normalizeForSearch(contact.name).contains(normQuery)) {
            return true
        }

        if (contact.phone.lowercase(Locale.ROOT).contains(trimmed.lowercase(Locale.ROOT))) {
            return true
        }

        if (isPhoneLikeQuery(trimmed)) {
            val qDigits = digitsOnly(trimmed)
            if (trimmed.startsWith("+")) {
                return canonicalPhoneKey(contact.phone).contains("+$qDigits")
            }
            if (qDigits.isNotEmpty() && digitsOnly(contact.phone).contains(qDigits)) {
                return true
            }
        }

        return false
    }

    fun filterContacts(contacts: List<ContactEntry>, query: String): List<ContactEntry> {
        if (query.isBlank()) return contacts
        return contacts.filter { matchesQuery(it, query) }
    }

    fun findNameHighlightRange(name: String, query: String): IntRange? {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || name.isEmpty()) return null
        val normName = normalizeForSearch(name)
        val normQuery = normalizeForSearch(trimmed)
        val idx = normName.indexOf(normQuery)
        return if (idx >= 0) idx until (idx + normQuery.length) else null
    }

    fun findPhoneHighlightRange(phone: String, query: String): IntRange? {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || phone.isEmpty()) return null

        val litIdx = phone.lowercase(Locale.ROOT).indexOf(trimmed.lowercase(Locale.ROOT))
        if (litIdx >= 0) {
            return litIdx until (litIdx + trimmed.length)
        }

        if (!isPhoneLikeQuery(trimmed)) return null

        val qDigits = digitsOnly(trimmed)
        if (qDigits.isEmpty()) return null

        val origIndices = ArrayList<Int>(phone.length)
        val condensed = StringBuilder(phone.length)
        for (i in phone.indices) {
            val ch = phone[i]
            if (trimmed.startsWith("+") && condensed.isEmpty() && ch == '+') {
                condensed.append('+')
                origIndices.add(i)
            } else if (ch in '0'..'9') {
                condensed.append(ch)
                origIndices.add(i)
            }
        }

        val target = if (trimmed.startsWith("+")) "+$qDigits" else qDigits
        val matchIdx = condensed.indexOf(target)
        if (matchIdx < 0) return null

        val startOrig = origIndices[matchIdx]
        val endOrig = origIndices[matchIdx + target.length - 1]
        return startOrig..endOrig
    }
}
