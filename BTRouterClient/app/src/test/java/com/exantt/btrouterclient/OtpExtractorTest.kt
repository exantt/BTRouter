package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpExtractorTest {

    // --- Турецкие банки (классический порядок, инверсия, даты, суммы, карты, Bxxx) ---

    @Test
    fun turkishBanks_ziraatStandardAnd3DSecure_withDateAmountCardAndB001() {
        val sms1 = "Internet/Mobil Sube girisiniz icin onay sifreniz: 482910. Sifrenizi kimseyle paylasmayiniz. B001"
        assertEquals("482910", OtpExtractor.extract(sms1, "ZIRAAT"))

        val sms2 = "20.11.2026 tarihinde 5432 ile biten kartinizla 1.499,90 TL tutarindaki " +
            "internet alisverisiniz icin 3D Secure onay kodunuz: 739201. " +
            "Bize 444 0 333 veya 0850 220 0 000 dan ulasabilirsiniz. B001"
        assertEquals("739201", OtpExtractor.extract(sms2, "ZIRAATBANK"))
    }

    @Test
    fun turkishBanks_garantiInvertedOrderWithUnicodeAndDate2026() {
        val sms1 = "20.11.2026 14:35 tarihinde *5432 nolu kartınızla yapacağınız 2.450,00 TL " +
            "işlemi onaylamak için 918234 şifresini kullanabilirsiniz. B016"
        assertEquals("918234", OtpExtractor.extract(sms1, "GarantiBBVA"))

        val sms2 = "19/11/2026 tarihli Garanti BBVA Mobil girişiniz için 561289 tek kullanımlık " +
            "doğrulama kodunu kullanınız. Güvenliğiniz için paylaşmayınız. B016"
        assertEquals("561289", OtpExtractor.extract(sms2, "GarantiBBVA"))
    }

    @Test
    fun turkishBanks_isbankApostropheInvertedAndUpperTurkishI() {
        val sms1 = "İŞCEP GİRİŞ ONAY KODUNUZ: 304921. 20 Kasım 2026 tarihinde geçerlidir. " +
            "Lütfen kimseyle paylaşmayınız. B018"
        assertEquals("304921", OtpExtractor.extract(sms1, "ISBANK"))

        val sms2 = "İşCep girişiniz için 821034'i şifre alanına giriniz. " +
            "Kart: 5432 12** **** 9876, Tutar: 5.000,00 TL. B018"
        assertEquals("821034", OtpExtractor.extract(sms2, "ISBANK"))
    }

    @Test
    fun turkishBanks_yapiKrediAkbankDenizBankQnbEnparaPapara() {
        val yapi = "Yapı Kredi Internet/Mobil Şube işleminiz için Akıllı SMS şifreniz: 619283. B002"
        assertEquals("619283", OtpExtractor.extract(yapi, "YAPIKREDI"))

        val akbank = "129485 onay kodu ile Akbank Mobil'e giriş yapabilirsiniz. " +
            "Ref: 998123. 0850 222 0 724. B003"
        assertEquals("129485", OtpExtractor.extract(akbank, "AKBANK"))

        val deniz = "DenizBank tek kullanımlık şifreniz 502931 ile 20.11.2026 tarihli işlemi onaylayabilirsiniz. B010"
        assertEquals("502931", OtpExtractor.extract(deniz, "DENIZBANK"))

        val qnb = "QNB Mobil giriş doğrulama kodunuz: 382910 (180 saniye geçerlidir). B011"
        assertEquals("382910", OtpExtractor.extract(qnb, "QNB"))

        val enpara = "Enpara.com Internet Şubesi işlem onay şifreniz: 729104. " +
            "Şifrenizi kimseyle paylaşmayınız. B021"
        assertEquals("729104", OtpExtractor.extract(enpara, "ENPARA"))

        val papara = "Papara giriş onay kodunuz: 492018. Kodun geçerlilik süresi 3 dakikadır. " +
            "Tutar: 250,00 TRY. M001"
        assertEquals("492018", OtpExtractor.extract(papara, "PAPARA"))
    }

    @Test
    fun turkishServices_edevletIyzicoTrendyolAndCompetingRef() {
        val edevlet = "e-Devlet Kapısı giriş işleminiz için doğrulama kodunuz: 847291. " +
            "Kimlik no ve şifrenizi kimseyle paylaşmayınız. B001"
        assertEquals("847291", OtpExtractor.extract(edevlet, "E-DEVLET"))

        val iyzico = "iyzico ile ödemeniz için tek kullanımlık onay şifreniz: 192837. " +
            "Tutar: 890 TL, Ref: 665544. B040"
        assertEquals("192837", OtpExtractor.extract(iyzico, "IYZICO"))

        val trendyol = "Trendyol hesabınıza giriş için doğrulama kodunuz: 519204. " +
            "https://ty.gl/abc123456 B002"
        assertEquals("519204", OtpExtractor.extract(trendyol, "TRENDYOL"))

        val refBeforeOtp = "Ref: 998123 nolu işleminiz için 3D Secure şifreniz: 482910. B001"
        assertEquals("482910", OtpExtractor.extract(refBeforeOtp, "GARANTI"))
    }

    // --- Крайние случаи (2026 как реальный код, ведущие нули, переносы строк) ---

    @Test
    fun edgeCases_year2026AsActualOtp_leadingZeros_multiline_warningOnly() {
        val actual2026 = "Onay sifreniz: 2026. Sifrenizi kimseyle paylasmayiniz. B001"
        assertEquals("2026", OtpExtractor.extract(actual2026, "ZIRAAT"))

        val leadingZeros = "Doğrulama kodunuz: 004829. B016"
        assertEquals("004829", OtpExtractor.extract(leadingZeros, "GARANTI"))

        val multiline = "Onay kodunuz:\n482910\nŞifrenizi kimseyle paylaşmayınız."
        assertEquals("482910", OtpExtractor.extract(multiline, "AKBANK"))

        val minimalWarning = "482910. Kimseyle paylaşmayınız."
        assertEquals("482910", OtpExtractor.extract(minimalWarning, "BANK"))

        val directKodWarning = "482910 kodunu kimseyle paylaşmayınız."
        assertEquals("482910", OtpExtractor.extract(directKodWarning, "BANK"))
    }

    // --- Российские и международные сервисы/банки ---

    @Test
    fun russianBanksAndServices_extractCorrectCode() {
        val sber = "Никому не говорите код 58492. СберБанк Онлайн: вход, *5432, сумма 1500р."
        assertEquals("58492", OtpExtractor.extract(sber, "900"))

        val sber4 = "Код подтверждения: 4910. Покупка 1 250,00 руб. Никому не сообщайте код."
        assertEquals("4910", OtpExtractor.extract(sber4, "900"))

        val tbank = "Код подтверждения входа 748291. Не передавайте его никому. Баланс 15000 руб."
        assertEquals("748291", OtpExtractor.extract(tbank, "T-Bank"))

        val alfa = "Пароль для входа: 839201, заказ №998123, карта *4321."
        assertEquals("839201", OtpExtractor.extract(alfa, "Alfa-Bank"))

        val gosuslugi = "Код подтверждения для входа на Госуслуги: 604819. Никому не сообщайте."
        assertEquals("604819", OtpExtractor.extract(gosuslugi, "gosuslugi"))
    }

    @Test
    fun internationalAndSplitCodes_extractCleanDigits() {
        val google = "G-849201 is your Google verification code."
        assertEquals("849201", OtpExtractor.extract(google, "Google"))

        val apple = "Your Apple Account code is: 593018. Do not share it with anyone."
        assertEquals("593018", OtpExtractor.extract(apple, "Apple"))

        val telegram = "Telegram code: 48291. You can also tap on this link: https://t.me/login/123456"
        assertEquals("48291", OtpExtractor.extract(telegram, "Telegram"))

        val hyphenSplit = "Verification code: 482-910. Valid for 5 minutes."
        assertEquals("482910", OtpExtractor.extract(hyphenSplit, "Auth"))

        val spaceSplit = "Одноразовый код: 482 910 для подтверждения операции."
        assertEquals("482910", OtpExtractor.extract(spaceSplit, "Bank"))
    }

    // --- Короткое СМС только с кодом ---

    @Test
    fun shortOnlyCodeSms_acceptsServiceSender_rejectsRegularPhone() {
        assertEquals("482910", OtpExtractor.extract("482910", "ZIRAAT"))
        assertEquals("0042", OtpExtractor.extract("0042", "900"))
        assertEquals("849201", OtpExtractor.extract("G-849201", "Google"))

        assertNull(OtpExtractor.extract("482910", "+905551234567"))
        assertNull(OtpExtractor.extract("482910", "905551234567"))
        assertNull(OtpExtractor.extract("2026", ""))
    }

    // --- Отрицательные тесты (ложные срабатывания запрещены) ---

    @Test
    fun nonOtpMessages_receiptsBalancesPromosRegularTexts_returnNull() {
        val turkishReceiptWithWarning = "20.11.2026 tarihinde 5432 ile biten kartinizla 1.250,00 TL " +
            "harcama yapilmistir. Guvenliginiz icin sifrenizi kimseyle paylasmayiniz. B001"
        assertNull(OtpExtractor.extract(turkishReceiptWithWarning, "ZIRAAT"))

        val turkishReceiptWithRef = "20.11.2026 tarihinde *5432 kartinizla 450,00 TL odeme alindi. " +
            "Ref: 998123, sifrenizi kimseyle paylasmayiniz. B016"
        assertNull(OtpExtractor.extract(turkishReceiptWithRef, "GarantiBBVA"))

        val turkishBill = "2026 Kasim faturaniz 850,50 TL olarak duzenlenmistir. " +
            "Musteri No: 12345678, son odeme 20.11.2026. B002"
        assertNull(OtpExtractor.extract(turkishBill, "TURKCELL"))

        val promoTr = "Size ozel %20 indirim! Kampanya kodu: 2026. Detaylar https://shop.tr/123456 B001"
        assertNull(OtpExtractor.extract(promoTr, "SHOP"))

        val cargoTr = "Kargo takip kodunuz: 74829102. Teslimat tarihi 20.11.2026."
        assertNull(OtpExtractor.extract(cargoTr, "YURTICI"))

        val promoRu = "Ваш промокод: 4829 на скидку 500 руб. в заказе 9911."
        assertNull(OtpExtractor.extract(promoRu, "Ozon"))

        val discountRu = "Скидочный код: 4829 для заказа 9911 (доставка 500 рублей)."
        assertNull(OtpExtractor.extract(discountRu, "Shop"))

        val postalRu = "Почтовый код: 123456 для посылки."
        assertNull(OtpExtractor.extract(postalRu, "Post"))

        val ruReceipt = "MIR-5432 20.11.2026 14:20 покупка 1490р SUPERMARKET Баланс: 25400.50р"
        assertNull(OtpExtractor.extract(ruReceipt, "900"))

        val regularChat = "Привет, встретимся в 2026 году возле дома 1450 или в офисе 4829?"
        assertNull(OtpExtractor.extract(regularChat, "+905550000000"))

        assertNull(OtpExtractor.extract("", "ZIRAAT"))
        assertNull(OtpExtractor.extract("   ", "900"))
    }
}
