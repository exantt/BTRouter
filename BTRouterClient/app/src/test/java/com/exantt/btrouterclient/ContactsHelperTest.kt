package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsHelperTest {

    @Test
    fun sortContactsEnThenRu_ordersLatinThenCyrillicWithYoAfterYeThenDigits() {
        val input = listOf(
            ContactsHelper.ContactEntry("112", "112", "112"),
            ContactsHelper.ContactEntry("Ярослав", "+79991112233", "+79991112233"),
            ContactsHelper.ContactEntry("Женя", "+79991112234", "+79991112234"),
            ContactsHelper.ContactEntry("Ёжик", "+79991112235", "+79991112235"),
            ContactsHelper.ContactEntry("Елена", "+79991112236", "+79991112236"),
            ContactsHelper.ContactEntry("Александр", "+79991112237", "+79991112237"),
            ContactsHelper.ContactEntry("Zara", "+905551112233", "+905551112233"),
            ContactsHelper.ContactEntry("Şahin", "+905551112234", "+905551112234"),
            ContactsHelper.ContactEntry("Çağrı", "+905551112235", "+905551112235"),
            ContactsHelper.ContactEntry("Alice", "+905551112236", "+905551112236"),
            ContactsHelper.ContactEntry("#Техподдержка", "4440333", "4440333")
        )

        val sortedNames = ContactsHelper.sortContactsEnThenRu(input).map { it.name }
        assertEquals(
            listOf(
                "Alice",
                "Çağrı",
                "Şahin",
                "Zara",
                "Александр",
                "Елена",
                "Ёжик",
                "Женя",
                "Ярослав",
                "#Техподдержка",
                "112"
            ),
            sortedNames
        )
    }

    @Test
    fun deduplicateContacts_mergesFormattingDupes_keepsDifferentNumbersSameName() {
        val raw = listOf(
            ContactsHelper.ContactEntry("Ahmet", "+905551234567", "+905551234567"),
            ContactsHelper.ContactEntry("Ahmet", "+90\u00A0555\u200E 123-45-67", "+905551234567"),
            ContactsHelper.ContactEntry("Ahmet", "+90 532 000 11 22", "+905320001122"),
            ContactsHelper.ContactEntry("BadShort", "12", "12")
        )

        val deduped = ContactsHelper.deduplicateContacts(raw)
        assertEquals(2, deduped.size)
        assertEquals("+90 555 123-45-67", deduped[0].phone)
        assertEquals("+90 532 000 11 22", deduped[1].phone)
        assertEquals("+90 555 123-45-67", ContactsHelper.phoneForInput(deduped[0]))
        assertNotNull(BtProtocol.sanitizeAndValidatePhone(ContactsHelper.phoneForInput(deduped[0])))
    }

    @Test
    fun cleanDisplayPhone_removesBidiAndNbspAndWeirdDashes() {
        val messy = "\u202A+90\u00A0555\u200E — 123–45\u00A067\u202C"
        val cleaned = ContactsHelper.cleanDisplayPhone(messy)
        assertEquals("+90 555 - 123-45 67", cleaned)
        assertEquals("+905551234567", ContactsHelper.canonicalPhoneKey(cleaned))
        assertEquals("+905551234567", BtProtocol.sanitizeAndValidatePhone(cleaned))
    }

    @Test
    fun dynamicSearch_matchesPartialNamesTurkishCyrillicYoAndPartialFormattedPhone() {
        val c1 = ContactsHelper.ContactEntry("Çağrı Yılmaz", "+90 (555) 123-45-67", "+905551234567")
        val c2 = ContactsHelper.ContactEntry("Артём Иванов", "+7 999 888-77-66", "+79998887766")
        val list = listOf(c1, c2)

        assertEquals(2, ContactsHelper.filterContacts(list, "").size)
        assertEquals(listOf(c1), ContactsHelper.filterContacts(list, "cagri"))
        assertEquals(listOf(c1), ContactsHelper.filterContacts(list, "YIL"))
        assertEquals(listOf(c2), ContactsHelper.filterContacts(list, "артем"))
        assertEquals(listOf(c2), ContactsHelper.filterContacts(list, "ванов"))
        assertEquals(listOf(c1), ContactsHelper.filterContacts(list, "555123"))
        assertEquals(listOf(c1), ContactsHelper.filterContacts(list, "+90555"))
        assertEquals(listOf(c2), ContactsHelper.filterContacts(list, "888 77"))
        assertTrue(ContactsHelper.filterContacts(list, "неизвестно").isEmpty())

        assertFalse(ContactsHelper.matchesQuery(c1, "+7999"))
    }

    @Test
    fun highlightRanges_returnExactSpanForNameAndFormattedPhone() {
        val nameRange = ContactsHelper.findNameHighlightRange("Çağrı Yılmaz", "yilm")
        assertEquals(6..9, nameRange)

        val ruRange = ContactsHelper.findNameHighlightRange("Артём", "тем")
        assertEquals(2..4, ruRange)

        val formattedPhone = "+90 (555) 123-45-67"
        val phoneRange = ContactsHelper.findPhoneHighlightRange(formattedPhone, "555123")
        assertNotNull(phoneRange)
        assertEquals("555) 123", formattedPhone.substring(phoneRange!!.first, phoneRange.last + 1))

        val plusPhoneRange = ContactsHelper.findPhoneHighlightRange(formattedPhone, "+90555")
        assertNotNull(plusPhoneRange)
        assertEquals("+90 (555", formattedPhone.substring(plusPhoneRange!!.first, plusPhoneRange.last + 1))

        assertNull(ContactsHelper.findNameHighlightRange("Alice", "zzz"))
        assertNull(ContactsHelper.findPhoneHighlightRange(formattedPhone, "999"))
    }

    @Test
    fun sectionLetter_mapsTurkishLatinCyrillicAndSymbols_andStaysContiguousAfterSort() {
        assertEquals("C", ContactsHelper.sectionLetter("Çağlar"))
        assertEquals("S", ContactsHelper.sectionLetter("Şahin"))
        assertEquals("I", ContactsHelper.sectionLetter("İstanbul"))
        assertEquals("I", ContactsHelper.sectionLetter("ısparta"))
        assertEquals("A", ContactsHelper.sectionLetter("Alice"))
        assertEquals("E", ContactsHelper.sectionLetter("Élodie"))
        assertEquals("Е", ContactsHelper.sectionLetter("Елена"))
        assertEquals("Ё", ContactsHelper.sectionLetter("Ёжик"))
        assertEquals("Я", ContactsHelper.sectionLetter("Ярослав"))
        assertEquals("#", ContactsHelper.sectionLetter("112"))
        assertEquals("#", ContactsHelper.sectionLetter("+905550000000"))
        assertEquals("#", ContactsHelper.sectionLetter("#Техподдержка"))

        val contacts = listOf(
            ContactsHelper.ContactEntry("Ярослав", "+79991112233", "+79991112233"),
            ContactsHelper.ContactEntry("Élodie", "+33123456789", "+33123456789"),
            ContactsHelper.ContactEntry("112", "112", "112"),
            ContactsHelper.ContactEntry("Çağlar", "+905551112235", "+905551112235"),
            ContactsHelper.ContactEntry("Batuhan", "+905551112236", "+905551112236"),
            ContactsHelper.ContactEntry("Carlos", "+905551112237", "+905551112237"),
            ContactsHelper.ContactEntry("Елена", "+79991112238", "+79991112238"),
            ContactsHelper.ContactEntry("Ёжик", "+79991112239", "+79991112239"),
            ContactsHelper.ContactEntry("Женя", "+79991112240", "+79991112240"),
            ContactsHelper.ContactEntry("Alice", "+905551112241", "+905551112241"),
            ContactsHelper.ContactEntry("Şahin", "+905551112242", "+905551112242"),
            ContactsHelper.ContactEntry("#Техподдержка", "4440333", "4440333")
        )
        val sorted = ContactsHelper.sortContactsEnThenRu(contacts)
        val seenClosedLetters = HashSet<String>()
        var currentLetter: String? = null
        for (entry in sorted) {
            val letter = ContactsHelper.sectionLetter(entry.name)
            if (letter != currentLetter) {
                if (currentLetter != null) {
                    seenClosedLetters.add(currentLetter)
                }
                assertFalse("Буква $letter разорвана при сортировке", seenClosedLetters.contains(letter))
                currentLetter = letter
            }
        }
    }

    @Test
    fun deduplicateContacts_afterRename_prefersPrimaryAndFresherAndMergesLocalInternational() {
        // 1. Старая запись из WhatsApp/Telegram + новая в основном аккаунте (Google) с тем же номером
        val whatsappOld = ContactsHelper.ContactEntry(
            name = "Старое Имя",
            phone = "+90 555 123-45-67",
            normalizedPhone = "+905551234567",
            inVisibleGroup = true,
            isPrimaryAccount = false,
            lastUpdatedMs = 2000L
        )
        val googleNew = ContactsHelper.ContactEntry(
            name = "Новое Имя",
            phone = "+905551234567",
            normalizedPhone = "+905551234567",
            inVisibleGroup = true,
            isPrimaryAccount = true,
            lastUpdatedMs = 1000L
        )
        val res1 = ContactsHelper.deduplicateContacts(listOf(whatsappOld, googleNew))
        assertEquals(1, res1.size)
        assertEquals("Новое Имя", res1[0].name)

        // 2. Два основных аккаунта с разным временем обновления — побеждает более свежий переименованный
        val accOld = ContactsHelper.ContactEntry(
            name = "Старый Вася",
            phone = "+7 999 111-22-33",
            normalizedPhone = "+79991112233",
            inVisibleGroup = true,
            isPrimaryAccount = true,
            lastUpdatedMs = 1000L
        )
        val accFresh = ContactsHelper.ContactEntry(
            name = "Василий Новый",
            phone = "+7 999 111-22-33",
            normalizedPhone = "+79991112233",
            inVisibleGroup = true,
            isPrimaryAccount = true,
            lastUpdatedMs = 5000L
        )
        val res2 = ContactsHelper.deduplicateContacts(listOf(accOld, accFresh))
        assertEquals(1, res2.size)
        assertEquals("Василий Новый", res2[0].name)

        // 3. Скрытый дубль (inVisibleGroup = false) против видимого контакта
        val hiddenOld = ContactsHelper.ContactEntry(
            name = "Скрытый Старый",
            phone = "+90 532 000 11 22",
            normalizedPhone = "+905320001122",
            inVisibleGroup = false,
            isPrimaryAccount = true,
            lastUpdatedMs = 9999L
        )
        val visibleNew = ContactsHelper.ContactEntry(
            name = "Видимый Новый",
            phone = "+90 532 000 11 22",
            normalizedPhone = "+905320001122",
            inVisibleGroup = true,
            isPrimaryAccount = true,
            lastUpdatedMs = 1000L
        )
        val res3 = ContactsHelper.deduplicateContacts(listOf(hiddenOld, visibleNew))
        assertEquals(1, res3.size)
        assertEquals("Видимый Новый", res3[0].name)

        // 4. Слияние локального (0555...) свежего и международного (+90555...) старого из мессенджера
        val localFresh = ContactsHelper.ContactEntry(
            name = "Иван Обновлённый",
            phone = "0555 123 45 67",
            normalizedPhone = "05551234567",
            inVisibleGroup = true,
            isPrimaryAccount = true,
            lastUpdatedMs = 5000L
        )
        val intlMessenger = ContactsHelper.ContactEntry(
            name = "Иван Старый",
            phone = "+90 555 123 45 67",
            normalizedPhone = "+905551234567",
            inVisibleGroup = true,
            isPrimaryAccount = false,
            lastUpdatedMs = 9000L
        )
        val res4 = ContactsHelper.deduplicateContacts(listOf(intlMessenger, localFresh))
        assertEquals(1, res4.size)
        assertEquals("Иван Обновлённый", res4[0].name)
        assertEquals("0555 123 45 67", res4[0].phone)

        // 5. Разные международные страны с общими последними 10 цифрами НЕ сливаются,
        // а один контакт с двумя разными номерами сохраняет обе записи
        val countryUs = ContactsHelper.ContactEntry("John US", "+1 555 123 45 67", "+15551234567")
        val countryTr = ContactsHelper.ContactEntry("John TR", "+90 555 123 45 67", "+905551234567")
        val res5 = ContactsHelper.deduplicateContacts(listOf(countryUs, countryTr))
        assertEquals(2, res5.size)
    }
}
