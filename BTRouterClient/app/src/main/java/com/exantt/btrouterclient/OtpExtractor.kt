package com.exantt.btrouterclient

/**
 * Извлекает одноразовые коды подтверждения (OTP / 2FA) из SMS,
 * включая инвертированный турецкий синтаксис ("482910 şifresi ile..."),
 * русскую и английскую банковскую терминологию,
 * игнорируя даты (например, 2026), суммы, номера карт, телефоны и коды B001/M001.
 */
object OtpExtractor {

    private const val MAX_BODY_LENGTH = 1200

    private val SHORT_ONLY_CODE_REGEX = Regex("^(?:[A-Za-z]-?)?(\\d{4,8})$")

    // Границы слов, корректно работающие и для кириллицы, и для латиницы
    private val TR_GENERIC_KOD_REGEX = Regex("(?<![\\p{L}\\p{N}])kod(?:u|unuz|unu)?(?![\\p{L}\\p{N}])")
    private val TR_STOP_BEFORE_KOD = Regex("(?:kampanya|indirim|kupon|promosyon|kargo|takip|fatura|abone|musteri)\\s+$")

    private val RU_KOD_REGEX = Regex("(?<![\\p{L}\\p{N}])код(?:а|у|ом|е|ы)?(?![\\p{L}\\p{N}])")
    private val RU_STOP_BEFORE_KOD = Regex("(?:почтов\\p{L}*|трек|акцион\\p{L}*|скидоч\\p{L}*)[\\s-]+$")
    private val RU_STOP_AFTER_KOD = Regex("^\\s+(?:товар\\p{L}*|посыл\\p{L}*|отслеживан\\p{L}*|акци\\p{L}*|скидк\\p{L}*|домофон\\p{L}*)(?![\\p{L}\\p{N}])")

    private val EN_CODE_REGEX = Regex("(?<![\\p{L}\\p{N}])code(?![\\p{L}\\p{N}])")
    private val EN_STOP_BEFORE_CODE = Regex("(?:promo|promotional|coupon|discount|zip|postal|tracking|area|qr|bar|invite|referral|post)[\\s-]+$")

    private val DIRECT_CONTEXT_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "sifre(?!siz)\\p{L}*|onay\\s+kod\\p{L}*|dogrulama\\p{L}*|tek\\s+kullanim\\p{L}*|" +
            "giris\\s+kod\\p{L}*|guvenlik\\s+kod\\p{L}*|islem\\s+kod\\p{L}*|sms\\s+kod\\p{L}*|" +
            "akilli\\s+sms|parola\\p{L}*|3d\\s*-?\\s*secure|3ds|kimseyle\\s+paylasma\\p{L}*|kimseye\\s+soyleme\\p{L}*|" +
            "парол\\p{L}*|одноразов\\p{L}*|подтвержд\\p{L}*|пин-?код\\p{L}*|" +
            "никому\\s+не\\s+сообщай\\p{L}*|никому\\s+не\\s+говори\\p{L}*|не\\s+передавайте|" +
            "otp|2fa|mfa|passcode|one-?time|onetime|verification|verify|authenticat(?:e|ion)|" +
            "confirmation|security\\s+code|login\\s+code|access\\s+code|auth\\s+code|" +
            "activation\\s+code|secret\\s+code|pin\\s+code|sms\\s+code|" +
            "do\\s+not\\s+share|don't\\s+share|never\\s+share" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val STRONG_WARNING_OR_OTP_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "sifre(?!siz)\\p{L}*|onay\\s+kod\\p{L}*|dogrulama\\p{L}*|tek\\s+kullanim\\p{L}*|" +
            "3d\\s*-?\\s*secure|kimseyle\\s+paylasma\\p{L}*|" +
            "код\\s+подтвержд\\p{L}*|одноразов\\p{L}*|никому\\s+не\\s+сообщай\\p{L}*|никому\\s+не\\s+говори\\p{L}*|" +
            "otp|passcode|verification|do\\s+not\\s+share|don't\\s+share" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val MASK_REGEXES = listOf(
        // URL и email
        Regex("https?://\\S+"),
        Regex("www\\.\\S+"),
        Regex("[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}"),

        // Даты (dd.mm.yyyy, yyyy-mm-dd, dd.mm.yy, словесные месяцы, годы с контекстом)
        Regex("(?<![\\p{L}\\p{N}])\\d{1,2}[./-]\\d{1,2}[./-](?:19|20)\\d{2}(?![\\p{L}\\p{N}])"),
        Regex("(?<![\\p{L}\\p{N}])(?:19|20)\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}(?![\\p{L}\\p{N}])"),
        Regex("(?<![\\p{L}\\p{N}])\\d{1,2}[./-]\\d{1,2}[./-]\\d{2}(?![\\p{L}\\p{N}])"),
        Regex(
            "(?<![\\p{L}\\p{N}])(?:\\d{1,2}\\s+)?(?:" +
                "ocak|subat|mart|nisan|mayis|haziran|temmuz|agustos|eylul|ekim|kasim|aralik|" +
                "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|" +
                "aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?|" +
                "янв(?:ар[яь])?|фев(?:рал[яь])?|мар(?:та?)?|апр(?:ел[яь])?|ма[яй]|" +
                "июн[яь]?|июл[яь]?|авг(?:уста?)?|сен(?:тябр[яь])?|окт(?:ябр[яь])?|ноя(?:бр[яь])?|дек(?:абр[яь])?" +
                ")\\s+20\\d{2}(?![\\p{L}\\p{N}])"
        ),
        Regex("(?<![\\p{L}\\p{N}])20[2-3]\\d\\s*(?:tarih\\p{L}*|yil\\p{L}*|год\\p{L}*)(?![\\p{L}\\p{N}])"),

        // Время
        Regex("(?<!\\d)\\d{1,2}:\\d{2}(?::\\d{2})?(?!\\d)"),
        Regex("(?<![\\p{L}\\p{N}])saat\\s+\\d{1,2}[.:]\\d{2}(?!\\d)"),

        // Турецкие служебные коды операторов в конце SMS (B001, B016, M001 и т.п.)
        Regex("(?<![\\p{L}\\p{N}])[bm]\\d{3,4}(?![\\p{L}\\p{N}])"),

        // Суммы с валютой (буквенной и символьной; после символов ₺€$£₽ граница слова не ставится)
        Regex(
            "(?<![\\p{L}\\p{N}])\\d+(?:[.,]\\d+)*\\s*(?:" +
                "tl|try|usd|eur|gbp|rub|rur|kzt|azn|uah|chf|kurus|kr|" +
                "руб\\p{L}*|коп\\p{L}*|р\\.?(?=\\s|$|[,!?;:)])" +
                ")(?![\\p{L}\\p{N}])"
        ),
        Regex("(?<![\\p{L}\\p{N}])\\d+(?:[.,]\\d+)*\\s*[₺€\$£₽]"),
        Regex("(?:(?<![\\p{L}\\p{N}])(?:tl|try|usd|eur|gbp|rub|rur|kzt|azn|uah|chf)|[₺€\$£₽])\\s*\\d+(?:[.,]\\d+)*(?![\\p{L}\\p{N}])"),
        Regex(
            "(?<![\\p{L}\\p{N}])(?:" +
                "tutar\\p{L}*|bakiye\\p{L}*|limit\\p{L}*|harcama\\p{L}*|fatura\\p{L}*|" +
                "баланс\\p{L}*|сумм\\p{L}*|доступно|остаток|покупк\\p{L}*|оплат\\p{L}*|списан\\p{L}*" +
                ")\\s*[:=]?\\s*\\d+(?:[.,]\\d+)*(?![\\p{L}\\p{N}])"
        ),

        // Числа с разделителем тысяч или дробной частью (1.250,00 / 450.00 / 1.500 / 299,90)
        Regex("(?<![\\p{L}\\p{N}])\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?(?![\\p{L}\\p{N}])"),
        Regex("(?<![\\p{L}\\p{N}])\\d+[.,]\\d{2}(?![\\p{L}\\p{N}])"),

        // Номера карт (включая маску с пробелами и турецкую инверсию)
        Regex("(?:\\d{4}[\\s-]+)?(?:\\d{0,2}[*x]{2,4}[\\s-]+){1,3}\\d{4}(?!\\d)"),
        Regex("(?:\\d{4,6}[*x]+|(?<![\\p{L}\\p{N}])[*x]+)\\d{4}(?!\\d)"),
        Regex("(?<![\\p{L}\\p{N}])\\d{4}\\s*(?:ile\\s+biten|nolu|no\\.?lu|numarali|sonlu|son\\s+4)(?![\\p{L}\\p{N}])"),
        Regex(
            "(?<![\\p{L}\\p{N}])(?:ecmc|visa|mir|mc|mastercard|карт\\p{L}*|kart\\p{L}*|card|acct|account)" +
                "\\s*(?:ending(?:\\s+in)?|no\\.?|numara\\p{L}*|son\\s*4(?:\\s*hanesi)?|#|[*№])?\\s*[:\\-*#№]?\\s*\\d{4}(?!\\d)"
        ),
        Regex("(?<![\\p{L}\\p{N}])ending\\s+(?:in\\s+|with\\s+)?[*#]?\\d{4}(?!\\d)"),
        Regex("(?<!\\d)\\d{4}[\\s-]+\\d{4}[\\s-]+\\d{4}[\\s-]+\\d{4}(?!\\d)"),

        // Телефоны и колл-центры
        Regex("(?<!\\d)444(?:\\s*-?\\s*\\d){4}(?!\\d)"),
        Regex("(?<!\\d)0?850(?:\\s*-?\\s*\\d){7}(?!\\d)"),
        Regex("(?<!\\d)(?:0?800|[78][\\s-]?\\(?800\\)?)(?:[\\s-]?\\d){7}(?!\\d)"),
        Regex("\\+\\d{1,3}(?:[\\s()-]*\\d){7,13}(?!\\d)"),
        Regex("(?<!\\d)0[2-5]\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}(?!\\d)"),

        // IBAN и явные не-OTP идентификаторы
        Regex("(?<![\\p{L}\\p{N}])tr\\d{2}(?:[\\s-]?\\d){16,28}(?![\\p{L}\\p{N}])"),
        Regex(
            "(?<![\\p{L}\\p{N}])(?:" +
                "musteri\\s*no|hesap\\s*no|siparis\\s*no|fatura\\s*no|abone\\s*no|belge\\s*no|" +
                "tc\\s*kimlik|tckn|order\\s*#|order\\s*no|customer\\s*id|account\\s*no|" +
                "заказ\\p{L}*\\s*[#№]?|договор\\p{L}*\\s*[#№]?|счет\\p{L}*\\s*[#№]?|лицевой\\s*счет\\p{L}*\\s*[#№]?|" +
                "промокод\\p{L}*|трек-?код\\p{L}*|takip\\s*kod\\p{L}*|kampanya\\s*kod\\p{L}*|indirim\\s*kod\\p{L}*|kupon\\s*kod\\p{L}*|" +
                "promo\\s*code|tracking\\s*code" +
                ")\\s*[:.#№-]?\\s*\\d+(?![\\p{L}\\p{N}])"
        ),

        // Длинные числа (счета, TCKN, телефоны)
        Regex("(?<![\\p{L}\\p{N}])\\d{9,}(?![\\p{L}\\p{N}])"),

        // Длительности (время жизни кода и т.п.)
        Regex(
            "(?<!\\d)\\d+\\s*(?:" +
                "saniye\\p{L}*|dakika\\p{L}*|dk|sn|saat\\p{L}*|gun\\p{L}*|" +
                "seconds?|secs?|minutes?|mins?|hours?|days?|" +
                "секунд\\p{L}*|сек\\.?|минут\\p{L}*|мин\\.?|час\\p{L}*|дн\\p{L}*" +
                ")(?![\\p{L}\\p{N}])"
        )
    )

    private val SENTENCE_BREAK_REGEX = Regex("[.!?](?:\\s|$)|\\n\\s*\\n")

    private val KEYWORD_BEFORE_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "onay\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "dogrulama\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "tek\\s+kullanimlik(?:\\s+[\\p{L}\\p{N}]+){0,2}|" +
            "giris\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "guvenlik\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "islem\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "sms\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "akilli\\s+sms(?:\\s+(?:kod\\p{L}*|sifre\\p{L}*))?|" +
            "3d\\s*-?\\s*secure(?:\\s+(?:kod\\p{L}*|sifre\\p{L}*|pin))?|" +
            "sifre(?!siz)\\p{L}*|parola\\p{L}*|kodunuz|kodunu|kodu|kod|" +
            "код\\s+подтверждения|одноразов\\p{L}*\\s+(?:код\\p{L}*|парол\\p{L}*)|парол\\p{L}*|пин-?код\\p{L}*|код|" +
            "(?:verification|security|confirmation|login|access|auth|one-?time|3d\\s*-?\\s*secure|sms)\\s+(?:code|password|passcode|pin)|" +
            "otp|passcode|pin|code" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val KEYWORD_AFTER_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "onay\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "dogrulama\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "tek\\s+kullanimlik(?:\\s+[\\p{L}\\p{N}]+){0,2}|" +
            "giris\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "guvenlik\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "islem\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "sms\\s+(?:kod\\p{L}*|sifre\\p{L}*)|" +
            "3d\\s*-?\\s*secure(?:\\s+(?:kod\\p{L}*|sifre\\p{L}*|pin))?|" +
            "sifre(?!siz)\\p{L}*|parola\\p{L}*|kodunuz\\p{L}*|kodunu\\p{L}*|kodu\\p{L}*|kod\\p{L}*|" +
            "(?:ваш\\s+)?(?:одноразов\\p{L}*\\s+)?код\\s+подтверждения|одноразов\\p{L}*\\s+(?:код\\p{L}*|парол\\p{L}*)|" +
            "(?:ваш\\s+)?(?:код|парол\\p{L}*|пин-?код\\p{L}*)|" +
            "(?:verification|security|confirmation|login|access|auth|one-?time|sms)\\s+(?:code|password|passcode|pin)|" +
            "otp|passcode|pin|code" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val WARNING_FOOTER_AFTER_REGEX = Regex(
        "^(?:sifre\\p{L}*|kod\\p{L}*|код\\p{L}*|code)(?:\\s+lutfen)?\\s+(?:" +
            "kimse(?:yle|ye)\\s+(?:paylasma\\p{L}*|soyleme\\p{L}*)|" +
            "никому\\s+не|" +
            "with\\s+anyone" +
            ")"
    )
    private val WARNING_PHRASE_IN_HEAD_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "kimse(?:yle|ye)\\s+(?:paylasma\\p{L}*|soyleme\\p{L}*)|" +
            "никому\\s+не\\s+(?:сообщай\\p{L}*|говори\\p{L}*)|не\\s+передавайте|" +
            "do\\s+not\\s+share|don't\\s+share|never\\s+share" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val REF_REGEX = Regex("(?<![\\p{L}\\p{N}])(?:referans\\p{L}*|ref)(?![\\p{L}\\p{N}])")
    private val NON_OTP_ADJACENT_REGEX = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            "kart\\p{L}*|card|hesap\\p{L}*|account|tutar\\p{L}*|amount|bakiye\\p{L}*|balance|" +
            "limit\\p{L}*|fatura\\p{L}*|musteri\\p{L}*|siparis\\p{L}*|order|" +
            "карт\\p{L}*|баланс\\p{L}*|сумм\\p{L}*|заказ\\p{L}*" +
            ")(?![\\p{L}\\p{N}])"
    )

    private val SPLIT_6_REGEX = Regex("(?<![\\p{L}\\p{N}])(\\d{3})[-\\s](\\d{3})(?![\\p{L}\\p{N}])")
    private val OTP_DIGITS_REGEX = Regex("(?<![\\p{L}\\p{N}])(\\d{4,8})(?![\\p{L}\\p{N}])")
    private val ANY_4_TO_8_DIGITS_REGEX = Regex("\\d{4,8}")
    private val CLAUSE_STOP_PUNCT = charArrayOf(',', ';', '(')

    private data class Candidate(
        val code: String,
        val start: Int,
        val end: Int
    )

    fun extract(body: String, sender: String = ""): String? {
        if (body.isBlank() || body.length > MAX_BODY_LENGTH || !body.any { it.isDigit() }) {
            return null
        }

        val trimmed = body.trim()
        val shortOnly = SHORT_ONLY_CODE_REGEX.matchEntire(trimmed)
        if (shortOnly != null) {
            return if (isServiceOrBankSender(sender)) shortOnly.groupValues[1] else null
        }

        val norm = normalizeTurkishAscii(body)
        if (!hasOtpContext(norm)) {
            return null
        }

        val hasStrongWarningOrOtpWord = STRONG_WARNING_OR_OTP_REGEX.containsMatchIn(norm)
        val maskedNorm = maskNonOtpSpans(norm)

        val candidates = collectCandidates(norm, maskedNorm)
        if (candidates.isEmpty()) {
            return null
        }

        var bestCode: String? = null
        var bestScore = Int.MIN_VALUE

        for (candidate in candidates) {
            val score = scoreCandidate(candidate, norm, maskedNorm, hasStrongWarningOrOtpWord)
            if (score > bestScore) {
                bestScore = score
                bestCode = candidate.code
            }
        }

        return if (bestScore >= 30) bestCode else null
    }

    /**
     * Строго 1-to-1 преобразование, чтобы `normalized.length == input.length`
     * и индексы символов в `norm` и `body` совпадали один в один.
     */
    private fun normalizeTurkishAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
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

    private fun isServiceOrBankSender(sender: String): Boolean {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.any { it.isLetter() }) return true
        if (trimmed.startsWith("+")) return false
        val digitsCount = trimmed.count { it.isDigit() }
        return digitsCount in 1..6
    }

    private fun hasOtpContext(norm: String): Boolean {
        if (DIRECT_CONTEXT_REGEX.containsMatchIn(norm)) return true

        for (m in TR_GENERIC_KOD_REGEX.findAll(norm)) {
            val prefix = norm.substring(maxOf(0, m.range.first - 24), m.range.first)
            if (!TR_STOP_BEFORE_KOD.containsMatchIn(prefix)) {
                return true
            }
        }

        for (m in RU_KOD_REGEX.findAll(norm)) {
            val prefix = norm.substring(maxOf(0, m.range.first - 24), m.range.first)
            val suffix = norm.substring(m.range.last + 1, minOf(norm.length, m.range.last + 1 + 24))
            if (!RU_STOP_BEFORE_KOD.containsMatchIn(prefix) && !RU_STOP_AFTER_KOD.containsMatchIn(suffix)) {
                return true
            }
        }

        for (m in EN_CODE_REGEX.findAll(norm)) {
            val prefix = norm.substring(maxOf(0, m.range.first - 24), m.range.first)
            if (!EN_STOP_BEFORE_CODE.containsMatchIn(prefix)) {
                return true
            }
        }

        return false
    }

    private fun maskNonOtpSpans(norm: String): String {
        val chars = norm.toCharArray()
        for (regex in MASK_REGEXES) {
            for (match in regex.findAll(norm)) {
                for (idx in match.range) {
                    if (chars[idx] != '\n') {
                        chars[idx] = ' '
                    }
                }
            }
        }
        return String(chars)
    }

    private fun extractClauseBefore(text: String, pos: Int): Pair<Int, String> {
        val windowStart = maxOf(0, pos - 60)
        val raw = text.substring(windowStart, pos)
        val lastBreak = SENTENCE_BREAK_REGEX.findAll(raw).lastOrNull()
        val cutOffset = if (lastBreak != null) lastBreak.range.last + 1 else 0
        return Pair(windowStart + cutOffset, raw.substring(cutOffset))
    }

    private fun extractClauseAfter(text: String, pos: Int): String {
        val windowEnd = minOf(text.length, pos + 60)
        val raw = text.substring(pos, windowEnd)
        val firstBreak = SENTENCE_BREAK_REGEX.find(raw)
        return if (firstBreak != null) raw.substring(0, firstBreak.range.first) else raw
    }

    private fun collectCandidates(norm: String, maskedNorm: String): List<Candidate> {
        val splitCandidates = mutableListOf<Candidate>()

        for (match in SPLIT_6_REGEX.findAll(maskedNorm)) {
            val start = match.range.first
            val end = match.range.last + 1
            val (_, clauseBefore) = extractClauseBefore(norm, start)
            val clauseAfter = extractClauseAfter(norm, end)
            val hasSameClauseKeyword =
                KEYWORD_BEFORE_REGEX.containsMatchIn(clauseBefore) ||
                    KEYWORD_AFTER_REGEX.containsMatchIn(clauseAfter)

            if (hasSameClauseKeyword) {
                val merged = match.groupValues[1] + match.groupValues[2]
                splitCandidates.add(Candidate(code = merged, start = start, end = end))
            }
        }

        val result = ArrayList<Candidate>(splitCandidates.size + 4)
        result.addAll(splitCandidates)

        for (match in OTP_DIGITS_REGEX.findAll(maskedNorm)) {
            val start = match.range.first
            val end = match.range.last + 1
            val overlapsSplit = splitCandidates.any { sc -> start < sc.end && end > sc.start }
            if (!overlapsSplit) {
                result.add(Candidate(code = match.groupValues[1], start = start, end = end))
            }
        }

        return result
    }

    private fun scoreCandidate(
        candidate: Candidate,
        norm: String,
        maskedNorm: String,
        hasStrongWarningOrOtpWord: Boolean
    ): Int {
        val (clauseBeforeStart, clauseBefore) = extractClauseBefore(norm, candidate.start)
        val clauseAfter = extractClauseAfter(norm, candidate.end)

        var beforeScore = 0
        for (beforeMatch in KEYWORD_BEFORE_REGEX.findAll(clauseBefore).toList().asReversed()) {
            val kwEndGlobal = clauseBeforeStart + beforeMatch.range.last + 1
            val tail = clauseBefore.substring(beforeMatch.range.last + 1)
            val maskedTail = maskedNorm.substring(kwEndGlobal, candidate.start)
            val anotherCodeInBetween = ANY_4_TO_8_DIGITS_REGEX.containsMatchIn(maskedTail)
            if (anotherCodeInBetween) {
                break
            }
            if (isStoppedGenericKeyword(clauseBefore, beforeMatch)) {
                continue
            }
            val beforeKwPrefix = clauseBefore.substring(maxOf(0, beforeMatch.range.first - 28), beforeMatch.range.first)
            val isWarningBefore =
                WARNING_PHRASE_IN_HEAD_REGEX.containsMatchIn(tail) ||
                    (tail.length > 5 && WARNING_PHRASE_IN_HEAD_REGEX.containsMatchIn(beforeKwPrefix))
            if (isWarningBefore) {
                continue
            }
            beforeScore = if (tail.length <= 15) {
                if (tail.contains(':') || tail.contains('=') || tail.contains('-') || tail.contains('—') || tail.contains('–')) {
                    115
                } else {
                    100
                }
            } else if (clauseBefore.length - beforeMatch.range.first <= 55) {
                70
            } else {
                0
            }
            if (beforeScore > 0) break
        }

        var afterScore = 0
        for (afterMatch in KEYWORD_AFTER_REGEX.findAll(clauseAfter)) {
            val head = clauseAfter.substring(0, afterMatch.range.first)
            val maskedHead = maskedNorm.substring(candidate.end, candidate.end + afterMatch.range.first)
            val anotherCodeInBetween = ANY_4_TO_8_DIGITS_REGEX.containsMatchIn(maskedHead)
            if (anotherCodeInBetween) {
                break
            }
            if (isWarningFooterLabel(clauseAfter, afterMatch, head)) {
                continue
            }
            if (isStoppedGenericKeyword(clauseAfter, afterMatch)) {
                continue
            }
            afterScore = if (afterMatch.range.first <= 20) {
                105
            } else if (afterMatch.range.first <= 55) {
                70
            } else {
                0
            }
            if (afterScore > 0) break
        }

        val directKeywordScore = maxOf(beforeScore, afterScore)
        var score = directKeywordScore

        // Предпочтение по длине (турецкие банки почти всегда присылают 6 цифр)
        score += when (candidate.code.length) {
            6 -> 25
            5 -> 15
            7, 8 -> 10
            else -> 5
        }

        // Если в предложении нет прямой метки, но во всём СМС есть явная фраза про OTP/безопасность
        if (directKeywordScore == 0 && hasStrongWarningOrOtpWord) {
            score += 15
        }

        // Защита от года 2020..2035 без непосредственной метки (e.g. "2026")
        if (candidate.code.length == 4) {
            val year = candidate.code.toIntOrNull()
            if (year != null && year in 2020..2035 && directKeywordScore < 100) {
                score -= 120
            }
        }

        // Проверяем только текущую микро-клаузу (до запятой/скобки/точки с запятой),
        // чтобы метка соседнего поля ("Код 4829, заказ 9981") не штрафовала настоящий OTP.
        val nearBefore = clauseBefore.substringAfterLastAny(CLAUSE_STOP_PUNCT).takeLast(15)
        val nearAfter = clauseAfter.substringBeforeFirstAny(CLAUSE_STOP_PUNCT).take(15)

        if ((REF_REGEX.containsMatchIn(nearBefore) || REF_REGEX.containsMatchIn(nearAfter)) && directKeywordScore < 100) {
            score -= 60
        }

        if (NON_OTP_ADJACENT_REGEX.containsMatchIn(nearBefore) || NON_OTP_ADJACENT_REGEX.containsMatchIn(nearAfter)) {
            score -= 80
        }

        return score
    }

    private fun isStoppedGenericKeyword(clause: String, match: MatchResult): Boolean {
        val kw = match.value
        val prefix = clause.substring(maxOf(0, match.range.first - 20), match.range.first)
        return when {
            kw.startsWith("kod") -> TR_STOP_BEFORE_KOD.containsMatchIn(prefix)
            kw == "код" -> {
                val suffix = clause.substring(match.range.last + 1, minOf(clause.length, match.range.last + 1 + 20))
                RU_STOP_BEFORE_KOD.containsMatchIn(prefix) || RU_STOP_AFTER_KOD.containsMatchIn(suffix)
            }
            kw == "code" -> EN_STOP_BEFORE_CODE.containsMatchIn(prefix)
            else -> false
        }
    }

    private fun isWarningFooterLabel(clauseAfter: String, match: MatchResult, head: String): Boolean {
        val fromMatch = clauseAfter.substring(match.range.first)
        if (WARNING_PHRASE_IN_HEAD_REGEX.containsMatchIn(head)) {
            return true
        }
        val directNounModifier = head.all { it.isWhitespace() } && fromMatch.startsWith("kod")
        if (!directNounModifier && WARNING_FOOTER_AFTER_REGEX.containsMatchIn(fromMatch)) {
            return true
        }
        return false
    }

    private fun String.substringAfterLastAny(delims: CharArray): String {
        val idx = lastIndexOfAny(delims)
        return if (idx >= 0) substring(idx + 1) else this
    }

    private fun String.substringBeforeFirstAny(delims: CharArray): String {
        val idx = indexOfAny(delims)
        return if (idx >= 0) substring(0, idx) else this
    }
}
