(function () {
  const translations = {
    ru: {
      nav_use_cases: 'Для кого',
      nav_features: 'Возможности',
      nav_setup: 'Как настроить',
      nav_download: 'Скачать',
      nav_tech: 'Под капотом',
      hero_pill: 'Прямая связь по Bluetooth · Без интернета и облаков · Бесплатно',
      hero_title: 'Второй телефон в рюкзаке — все СМС и звонки у вас в руках',
      hero_subtitle:
        'BTRouter соединяет ваш основной смартфон со вторым телефоном, где стоит нужная SIM-карта. Читайте и отправляйте СМС, копируйте коды из банков в одно касание, сбрасывайте лишние звонки и включайте раздачу Wi-Fi удалённо — даже без интернета.',
      hero_cta_download: 'Скачать приложение',
      hero_cta_demo: 'Посмотреть в действии',
      hero_chip_1: 'Для Android 10 и новее',
      hero_chip_2: 'Надёжное шифрование',
      hero_chip_3: 'Работает без интернета',
      role_server: 'Телефон с SIM',
      role_client: 'Основной телефон',
      srv_title: 'Телефон с SIM',
      srv_tab_main: 'ГЛАВНАЯ',
      srv_tab_settings: 'НАСТРОЙКИ',
      srv_gateway_status: 'СОСТОЯНИЕ',
      srv_connected: '● ПОДКЛЮЧЁН',
      srv_state_idle: 'Готов передавать СМС и звонки',
      srv_security_label: 'ДОСТУП ТОЛЬКО ДЛЯ ВАС',
      srv_trusted_phone: 'Подключён ваш смартфон',
      srv_trusted_badge: 'СВОЙ',
      srv_psk_label: 'СЕКРЕТНЫЙ КОД ДОСТУПА',
      srv_btn_show: 'Показать',
      srv_btn_hide: 'Скрыть',
      srv_log_label: 'ПОСЛЕДНИЕ СОБЫТИЯ',
      log_init_1: 'Заряд 84%, сеть и интернет в норме',
      log_init_2: 'Защищённая связь с телефоном открыта',
      tunnel_title: 'Bluetooth',
      tunnel_sub: 'Зашифровано',
      tunnel_idle: 'Прямая связь',
      cli_gw_label: 'ТЕЛЕФОН С SIM',
      cli_online: '● В СЕТИ',
      cli_tile_data: 'Интернет',
      cli_tile_hotspot: 'Точка доступа',
      cli_status_ready: 'Соединение активно · Всё защищено',
      cli_call_title: 'Входящий вызов на второй телефон',
      cli_btn_decline: 'СБРОСИТЬ',
      cli_call_hint: 'Ответить на звонок можно только на втором телефоне',
      demo_label: 'Попробуйте сами:',
      demo_btn_otp: 'Код из банка',
      demo_btn_call: 'Входящий звонок',
      demo_btn_hotspot: 'Раздать интернет',
      val_1_title: 'Без облаков',
      val_1_desc: 'Сообщения идут напрямую между телефонами и нигде не хранятся в сети',
      val_2_title: 'Коды за секунду',
      val_2_desc: 'Сам находит код из банковского СМС и сразу копирует его в буфер',
      val_3_title: 'Всё на виду',
      val_3_desc: 'Заряд батареи, уровень сигнала и звонки второго телефона в одном окне',
      val_4_title: 'Бесплатно',
      val_4_desc: 'Никаких подписок, рекламы, регистраций и скрытых ограничений',
      cases_eyebrow: 'ДЛЯ ЧЕГО ЭТО НУЖНО',
      cases_title: 'Удобно каждый день и в поездках',
      cases_desc:
        'Не носите два телефона в руках и не переставляйте постоянно SIM-карты — пусть второй смартфон спокойно лежит в рюкзаке, кармане или дома.',
      case_1_title: 'В поездках и за границей',
      case_1_desc:
        'Оставьте домашнюю или зарубежную SIM-карту во втором телефоне в сумке и получайте все коды подтверждения от банков и сервисов прямо на основной экран.',
      case_2_title: 'Рабочий и личный номер',
      case_2_desc:
        'Не доставайте второй смартфон ради каждого уведомления: переписывайтесь по СМС, сбрасывайте спам-звонки и смотрите пропущенные с одного устройства.',
      case_3_title: 'Интернет и раздача Wi-Fi',
      case_3_desc:
        'Если второй телефон — Samsung, включайте на нём мобильный интернет и точку доступа Wi-Fi в одно касание с основного телефона, не доставая его из рюкзака.',
      features_eyebrow: 'ВОЗМОЖНОСТИ',
      features_title: 'Всё, к чему вы привыкли — на главном телефоне',
      features_desc:
        'Простой и понятный интерфейс с чатами, быстрым копированием кодов и контролем состояния второго телефона.',
      feat_sms_title: 'Удобные СМС-чаты и быстрые коды из банков',
      feat_sms_desc:
        'Вся переписка в одном списке: важные чаты можно закрепить сверху, лишние — убрать в архив без звука. Одноразовые коды сразу появляются в уведомлении с кнопкой «Скопировать» или копируются автоматически.',
      otp_tester_title: 'Проверьте, как приложение находит код в тексте:',
      preset_ru: 'Российский банк',
      preset_en: 'Зарубежный банк',
      preset_tr: 'Турецкий банк',
      otp_found_label: 'Найденный код:',
      otp_ignored_hint: 'Не путает код с номером карты (*4412), датой или суммой покупки',
      feat_telemetry_title: 'Заряд и связь всегда перед глазами',
      feat_telemetry_desc:
        'В шапке приложения и в шторке уведомлений всегда видно, сколько заряда осталось на втором телефоне, ловит ли он сеть и включён ли мобильный интернет.',
      feat_calls_title: 'Звонки и пропущенные',
      feat_calls_desc:
        'Узнавайте о входящем звонке сразу и сбрасывайте ненужные вызовы прямо с основного телефона. Пропущенные сохраняются в отдельный список со счётчиком.',
      feat_contacts_title: 'Контакты с обоих телефонов',
      feat_contacts_desc:
        'Пишите людям из записной книжки любого из двух смартфонов — переключение между адресными книгами и недавними номерами работает в один тап.',
      feat_samsung_title: 'Точка доступа в один клик (Samsung)',
      feat_samsung_desc:
        'Удобные круглые кнопки прямо на главном экране позволяют удалённо включать интернет и раздачу Wi-Fi на втором телефоне Samsung.',
      feat_sync_title: 'Ни одно сообщение не потеряется, даже если вы отошли',
      feat_sync_desc:
        'Если телефоны временно потеряли связь, всё накопившееся автоматически догрузится при возвращении в радиус Bluetooth. А после перезагрузки приложение само запустится заново без лишних напоминаний.',
      priv_eyebrow: 'ПРИВАТНОСТЬ И ЗАЩИТА',
      priv_title: 'Ваши сообщения остаются только между вами и вашими телефонами',
      priv_1_tag: 'ТОЛЬКО НАПРЯМУЮ',
      priv_1_title: 'Никаких серверов и аккаунтов',
      priv_1_desc:
        'Вам не нужно регистрироваться, привязывать почту или доверять личные СМС чужим облакам — данные никуда не уходят с ваших устройств.',
      priv_2_tag: 'ТОЛЬКО СВОИ',
      priv_2_title: 'Доступ закрыт для посторонних',
      priv_2_desc:
        'Второй телефон принимает команды только от выбранного вами смартфона и дополнительно проверяет секретный код доступа при каждой встрече.',
      priv_3_tag: 'ШИФРОВАНИЕ',
      priv_3_title: 'Переписка под надёжным замком',
      priv_3_desc:
        'Каждое сообщение и событие звонка надёжно зашифровано в пути, а секретные ключи хранятся в защищённой области памяти смартфона.',
      setup_eyebrow: 'ПРОСТОЙ СТАРТ',
      setup_title: 'Настройка за пару минут',
      step1_title: 'Установите приложение на оба смартфона',
      step1_desc: 'На телефон с SIM-картой поставьте версию Server, а на ваш основной телефон — версию Client.',
      step2_title: 'Свяжите телефоны по Bluetooth',
      step2_desc: 'Как обычные наушники — откройте системные настройки Bluetooth и соедините два телефона в пару.',
      step3_title: 'Выберите основной телефон на Server',
      step3_desc:
        'Выдайте приложению нужные разрешения (и отключите экономию батареи для стабильной работы), укажите свой телефон и скопируйте секретный ключ.',
      step4_title: 'Введите ключ на основном телефоне',
      step4_desc:
        'Откройте настройки в Client, выберите телефон с SIM, вставьте скопированный ключ и нажмите «Сохранить» — связь сразу заработает.',
      samsung_acc_title: 'По желанию: как настроить кнопки «Точка доступа» и «Интернет» (для Samsung)',
      samsung_acc_sub: 'Работает через встроенные сценарии Samsung One UI 5+',
      samsung_acc_intro:
        'На телефоне с SIM (Samsung с One UI 5+) откройте «Настройки → Режимы и сценарии → Сценарии → +», выберите условие «Уведомление получено» от BTRouterServer и добавьте 4 простых сценария по ключевым словам (отключив автовозврат):',
      btn_copy: 'Копировать',
      kw_hotspot_on: 'Мобильная точка доступа → Вкл.',
      kw_hotspot_off: 'Мобильная точка доступа → Выкл.',
      kw_data_on: 'Мобильные данные → Вкл.',
      kw_data_off: 'Мобильные данные → Выкл.',
      dl_eyebrow: 'СКАЧАТЬ v1.1.0',
      dl_title: 'Выберите приложение для каждого телефона',
      dl_desc:
        'Оба установочных файла бесплатны и доступны на странице релизов проекта на GitHub. Подходит для любых смартфонов на Android 10 и выше.',
      dl_client_role: 'На ваш основной телефон',
      dl_client_sub: 'Приём СМС, кодов и звонков',
      dl_client_desc:
        'Устанавливается на смартфон, которым вы пользуетесь каждый день: показывает СМС-чаты, автоматически копирует коды и выводит пропущенные звонки.',
      dl_server_role: 'На второй телефон с SIM-картой',
      dl_server_sub: 'Работает в фоне и передаёт сигнал',
      dl_server_desc:
        'Устанавливается на смартфон с нужной SIM-картой: тихо работает в фоне, пересылает СМС и звонки только вашему телефону и следит за зарядом.',
      dl_ver_badge: 'Версия 1.1.0',
      dl_btn_unavailable: 'Релиз пока недоступен',
      dl_notice_text: 'Готовые APK-файлы ещё не опубликованы — первый публичный релиз скоро появится на GitHub.',
      dl_notice_link: 'Открыть страницу Releases →',
      tech_eyebrow: 'ПОД КАПОТОМ · ДЛЯ СПЕЦИАЛИСТОВ',
      tech_title: 'Архитектура, криптография и сборка из исходного кода',
      tech_desc:
        'Этот раздел предназначен для разработчиков и тех, кто хочет детально изучить внутреннее устройство протокола BtProtocol, схему шифрования и команды сборки.',
      tech_btn_open: 'Открыть технические подробности',
      tech_btn_close: 'Скрыть технические подробности',
      arch_title: 'Архитектура транспорта (Bluetooth Classic RFCOMM)',
      arch_c_1: 'Foreground-служба BTRouterClientService',
      arch_c_2: 'SQLite-история (btrouter_chats.db) и очередь без связи (до 20 СМС, TTL 30 мин)',
      arch_c_3: 'Парсер OTP (EN/RU/TR), sync-команды SYNC_5MIN / SYNC_DISCONNECT и журнал пропущенных',
      arch_bridge_meta: 'Взаимное HMAC-SHA256 рукопожатие · AES-256-GCM кадры',
      arch_s_1: 'Foreground-служба connectedDevice + BootReceiver автозапуск',
      arch_s_2: 'Сериализованный SMS-модем (до 10 сегментов, SMS_SENT), буфер 30 СМС (TTL 24 ч) с EVENT_ACK',
      arch_s_3: 'TelecomManager (DECLINE / DIAL), выгрузка до 5000 контактов, HotspotDetector и ContentObserver',
      proto_t1: 'UUID и MAC-фильтр',
      proto_t2: 'PSK + HMAC-SHA256',
      proto_t3: 'AES-256-GCM кадры',
      proto_t4: 'AndroidKeyStore',
      sec_frame_title: 'Структура зашифрованного кадра BtProtocol и защита хранилища',
      sec_frame_note: 'Нажмите на сегмент для деталей',
      sec_c1_title: 'Взаимная проверка без передачи секрета',
      sec_c1_desc:
        'Обе стороны вычисляют SHA-256("BTRouter-PSK-v2:" + secret) из 16-символьного ключа, обмениваются 16-байтными SecureRandom nonce и сверяют HMAC-SHA256 через MessageDigest.isEqual.',
      sec_c2_title: 'Аппаратная обёртка ключей',
      sec_c2_desc:
        'PSK хранится зашифрованным ключом btrouter_psk_wrap_key (AES-256-GCM) в AndroidKeyStore. Облачные бэкапы и миграция sharedpref отключены (android:allowBackup="false").',
      sec_c3_title: 'Усиление release-сборки',
      sec_c3_desc:
        'Включены R8-минификация, сжатие ресурсов, удаление Log.v / Log.d / Log.i, проверка подписи apksigner и публикация контрольных сумм SHA256SUMS.txt в GitHub Actions.',
      build_title: 'Сборка из исходного кода (JDK 21 · Android SDK 36.1)',
      build_sub: 'Запуск testDebugUnitTest, lintDebug и assembleDebug (Client versionCode 19 / Server versionCode 15)',
      footer_tech: 'Техническая документация',
    },
    en: {
      nav_use_cases: 'Use Cases',
      nav_features: 'Features',
      nav_setup: 'Setup',
      nav_download: 'Download',
      nav_tech: 'Under the Hood',
      hero_pill: 'Direct Bluetooth Link · No Internet or Cloud · Free & Open',
      hero_title: 'Your second phone stays in your bag — all texts and calls right in your hand',
      hero_subtitle:
        'BTRouter links your everyday smartphone to your secondary phone holding the SIM card. Read and reply to SMS, copy bank verification codes in one tap, dismiss unwanted calls, and toggle Wi-Fi hotspot remotely — even offline.',
      hero_cta_download: 'Download App',
      hero_cta_demo: 'See It in Action',
      hero_chip_1: 'Android 10 and newer',
      hero_chip_2: 'Private encrypted link',
      hero_chip_3: 'Works without internet',
      role_server: 'SIM Phone',
      role_client: 'Primary Phone',
      srv_title: 'SIM Phone',
      srv_tab_main: 'HOME',
      srv_tab_settings: 'SETTINGS',
      srv_gateway_status: 'STATUS',
      srv_connected: '● CONNECTED',
      srv_state_idle: 'Ready to forward SMS & calls',
      srv_security_label: 'YOUR DEVICE ONLY',
      srv_trusted_phone: 'Your main phone is linked',
      srv_trusted_badge: 'TRUSTED',
      srv_psk_label: 'SECRET PAIRING KEY',
      srv_btn_show: 'Show',
      srv_btn_hide: 'Hide',
      srv_log_label: 'RECENT ACTIVITY',
      log_init_1: 'Battery 84%, cellular & data ready',
      log_init_2: 'Private Bluetooth link established',
      tunnel_title: 'Bluetooth',
      tunnel_sub: 'Encrypted',
      tunnel_idle: 'Direct link',
      cli_gw_label: 'SIM PHONE',
      cli_online: '● ONLINE',
      cli_tile_data: 'Mobile Data',
      cli_tile_hotspot: 'Hotspot',
      cli_status_ready: 'Connected · Link protected',
      cli_call_title: 'Incoming call on your SIM phone',
      cli_btn_decline: 'DECLINE',
      cli_call_hint: 'You can only answer calls on the SIM phone itself',
      demo_label: 'Try it yourself:',
      demo_btn_otp: 'Bank Code',
      demo_btn_call: 'Incoming Call',
      demo_btn_hotspot: 'Share Wi-Fi',
      val_1_title: 'Zero Cloud',
      val_1_desc: 'Messages travel straight between your phones and are never uploaded online',
      val_2_title: 'Instant Codes',
      val_2_desc: 'Automatically picks out bank verification codes and copies them for you',
      val_3_title: 'Always Informed',
      val_3_desc: 'Battery level, signal strength, and calls from your SIM phone at a glance',
      val_4_title: 'Completely Free',
      val_4_desc: 'Open-source with no ads, no accounts, and no subscriptions',
      cases_eyebrow: 'WHO IT HELPS',
      cases_title: 'Built for travel, work, and everyday convenience',
      cases_desc:
        'Stop juggling two phones in your hands or swapping SIM trays — keep your second phone tucked safely in your bag, pocket, or at home.',
      case_1_title: 'Travel & roaming abroad',
      case_1_desc:
        'Keep your home or local SIM card inside your secondary phone in a bag and receive every bank SMS and login code directly on your main screen.',
      case_2_title: 'Work & personal numbers',
      case_2_desc:
        'No need to pull out your work phone for every ping: answer texts, dismiss spam calls, and review missed callers right from your daily phone.',
      case_3_title: 'Remote internet & Wi-Fi hotspot',
      case_3_desc:
        'If your SIM phone is a Samsung, switch mobile data and Wi-Fi hotspot on or off remotely in one tap without ever taking it out of your backpack.',
      features_eyebrow: 'FEATURES',
      features_title: 'Everything you expect — right on your main phone',
      features_desc:
        'A clean, familiar messaging experience with smart code copying and live status from your second device.',
      feat_sms_title: 'Clean SMS chats & automatic bank code copying',
      feat_sms_desc:
        'All conversations organized in one inbox: pin important threads to the top, mute/archive noisy ones, and copy one-time codes straight from notifications or automatically.',
      otp_tester_title: 'Try how the app finds codes inside real SMS texts:',
      preset_ru: 'Russian Bank',
      preset_en: 'International Bank',
      preset_tr: 'Turkish Bank',
      otp_found_label: 'Detected code:',
      otp_ignored_hint: 'Ignores card numbers (*4412), dates, and purchase amounts',
      feat_telemetry_title: 'Battery & signal always in sight',
      feat_telemetry_desc:
        'See your second phone’s battery percentage, charging status, carrier signal, and internet state right inside the app header and notification shade.',
      feat_calls_title: 'Incoming calls & missed log',
      feat_calls_desc:
        'Get instant alerts when your second phone rings and decline unwanted calls remotely. Missed calls are saved neatly with an unread counter.',
      feat_contacts_title: 'Contacts from both phones',
      feat_contacts_desc:
        'Message people saved on either smartphone — switch between your primary contacts, SIM phone contacts, and recent recipients in one tap.',
      feat_samsung_title: 'One-tap Wi-Fi hotspot (Samsung)',
      feat_samsung_desc:
        'Handy round buttons right on the main screen let you toggle mobile data and Wi-Fi hotspot on a Samsung SIM phone.',
      feat_sync_title: 'Never miss a text, even if you step away',
      feat_sync_desc:
        'If your phones briefly move out of Bluetooth range, queued messages catch up automatically once you reconnect — and everything restarts smoothly after a reboot.',
      priv_eyebrow: 'PRIVACY FIRST',
      priv_title: 'Your messages stay strictly between your two phones',
      priv_1_tag: 'DIRECT ONLY',
      priv_1_title: 'No servers or sign-ups',
      priv_1_desc:
        'No email, no account creation, and no third-party cloud servers handling your private messages — nothing leaves your personal devices.',
      priv_2_tag: 'PRIVATE ACCESS',
      priv_2_title: 'Locked to your phone alone',
      priv_2_desc:
        'Your SIM phone refuses connections from unfamiliar devices and verifies your personal secret key every single time.',
      priv_3_tag: 'ENCRYPTED',
      priv_3_title: 'Protected from start to finish',
      priv_3_desc:
        'Every message and call alert is encrypted in transit, and your pairing secret is kept inside protected device storage.',
      setup_eyebrow: 'EASY SETUP',
      setup_title: 'Ready in a couple of minutes',
      step1_title: 'Install the apps on both phones',
      step1_desc: 'Put the Server app on your SIM phone and the Client app on your main phone.',
      step2_title: 'Pair them in Bluetooth settings',
      step2_desc: 'Just like wireless headphones — open Android Bluetooth settings and pair the two smartphones.',
      step3_title: 'Pick your main phone on Server',
      step3_desc:
        'Allow permissions (and disable battery restrictions for reliable background delivery), choose your primary phone, and copy the secret key.',
      step4_title: 'Paste the key on your main phone',
      step4_desc:
        'Open Settings in Client, pick your SIM phone, paste the pairing key, and tap Save — both devices connect in seconds.',
      samsung_acc_title: 'Optional: how to enable Remote Hotspot & Mobile Data buttons (Samsung)',
      samsung_acc_sub: 'Powered by built-in Samsung One UI 5+ Modes & Routines',
      samsung_acc_intro:
        'On a Samsung SIM phone (One UI 5+), open Settings → Modes and Routines → Routines → "+", trigger on "Notification received" from BTRouterServer, and add 4 simple routines using these keywords (disable auto-revert):',
      btn_copy: 'Copy',
      kw_hotspot_on: 'Mobile Hotspot → On',
      kw_hotspot_off: 'Mobile Hotspot → Off',
      kw_data_on: 'Mobile Data → On',
      kw_data_off: 'Mobile Data → Off',
      dl_eyebrow: 'DOWNLOAD v1.1.0',
      dl_title: 'Get the right app for each phone',
      dl_desc:
        'Both installers are completely free on GitHub Releases and work on any smartphone running Android 10 or newer.',
      dl_client_role: 'For your everyday phone',
      dl_client_sub: 'Reads SMS, copies codes & shows calls',
      dl_client_desc:
        'Install on the smartphone you hold in your hand: shows SMS threads, copies verification codes, and notifies you about calls.',
      dl_server_role: 'For your second phone with SIM',
      dl_server_sub: 'Runs quietly in the background',
      dl_server_desc:
        'Install on the phone carrying your SIM card: forwards texts and calls exclusively to your main phone and reports battery & signal.',
      dl_ver_badge: 'Version 1.1.0',
      dl_btn_unavailable: 'Release not available yet',
      dl_notice_text: 'Prebuilt APK files are not published yet — the first public release will appear on GitHub soon.',
      dl_notice_link: 'View Releases page →',
      tech_eyebrow: 'UNDER THE HOOD · FOR DEVELOPERS',
      tech_title: 'Architecture, cryptography & building from source',
      tech_desc:
        'Everything technical lives down here for engineers and power users: transport architecture, BtProtocol frame security, and Gradle build commands.',
      tech_btn_open: 'Show technical details',
      tech_btn_close: 'Hide technical details',
      arch_title: 'Transport Architecture (Bluetooth Classic RFCOMM)',
      arch_c_1: 'Foreground service BTRouterClientService',
      arch_c_2: 'SQLite chat store (btrouter_chats.db) & offline send queue (up to 20 SMS, 30 min TTL)',
      arch_c_3: 'EN/RU/TR OTP extractor, SYNC_5MIN / SYNC_DISCONNECT recovery & missed calls log',
      arch_bridge_meta: 'Mutual HMAC-SHA256 handshake · AES-256-GCM frames',
      arch_s_1: 'Foreground connectedDevice service + BootReceiver auto-start',
      arch_s_2: 'Serialized SMS modem (up to 10 parts, SMS_SENT), 30-SMS buffer (24h TTL) with EVENT_ACK',
      arch_s_3: 'TelecomManager (DECLINE / DIAL), chunked 5,000-contact export, HotspotDetector & ContentObserver',
      proto_t1: 'UUID & MAC Filter',
      proto_t2: 'PSK + HMAC-SHA256',
      proto_t3: 'AES-256-GCM Frames',
      proto_t4: 'AndroidKeyStore',
      sec_frame_title: 'BtProtocol Encrypted Frame Anatomy & Key Storage',
      sec_frame_note: 'Click a segment to inspect details',
      sec_c1_title: 'Mutual verification without key leakage',
      sec_c1_desc:
        'Both peers derive SHA-256("BTRouter-PSK-v2:" + secret) from a 16-char pairing key, exchange 16-byte SecureRandom nonces, and compare HMAC-SHA256 via MessageDigest.isEqual.',
      sec_c2_title: 'Hardware-backed secret wrapping',
      sec_c2_desc:
        'The pairing key is wrapped with btrouter_psk_wrap_key (AES-256-GCM) in AndroidKeyStore. Cloud backups and sharedpref transfers are disabled (android:allowBackup="false").',
      sec_c3_title: 'Hardened release builds',
      sec_c3_desc:
        'Release APKs enable R8 minification, resource shrinking, stripping of Log.v / Log.d / Log.i, apksigner verification, and SHA256SUMS.txt checksums in GitHub Actions.',
      build_title: 'Build from source (JDK 21 · Android SDK 36.1)',
      build_sub: 'Run testDebugUnitTest, lintDebug, and assembleDebug (Client versionCode 19 / Server versionCode 15)',
      footer_tech: 'Technical Docs',
    },
  };

  const protoSteps = {
    ru: [
      {
        title: 'Выделенный UUID и fail-closed MAC-фильтрация',
        desc: 'Вместо стандартного SPP используется собственный RFCOMM UUID 7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90. Server немедленно закрывает сокеты от любых устройств, чей MAC-адрес не совпадает с выбранным Client, и включает нарастающую задержку до 5 секунд при попытках флуда.',
        code: `UUID = 7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90\nif (remoteMac != allowedClientMac) {\n    socket.close() // fail-closed + backoff up to 5s\n}`,
      },
      {
        title: 'Взаимное рукопожатие PSK + HMAC-SHA256',
        desc: 'Клиент и сервер формируют мастер-ключ SHA-256("BTRouter-PSK-v2:" + normalizedSecret) из 16-символьного PSK, обмениваются случайными 16-байтными nonce из SecureRandom и сверяют HMAC в постоянное время (MessageDigest.isEqual).',
        code: `master = SHA256("BTRouter-PSK-v2:" + secret)\nhmacC = HMAC_SHA256(master, nonceS || nonceC)\nrequire(MessageDigest.isEqual(hmacC, expected))`,
      },
      {
        title: 'Раздельные сессионные ключи AES-256-GCM',
        desc: 'После рукопожатия выводятся независимые ключи KEY-C2S и KEY-S2C. Каждый кадр шифруется 12-байтным nonce (4 байта направления + 8-байтный монотонный счётчик), привязанным как AAD — повтор или перестановка кадров обрывают сессию.',
        code: `nonce12 = direction4B || seqCounter8B\nciphertext = AES_256_GCM(KEY_S2C, nonce12, aad=nonce12, frame)`,
      },
      {
        title: 'Аппаратная защита ключей в AndroidKeyStore',
        desc: 'Ключ сопряжения шифруется аппаратным ключом btrouter_psk_wrap_key (AES-256-GCM) в AndroidKeyStore. Резервное копирование в облако и миграция sharedpref полностью запрещены (android:allowBackup="false").',
        code: `alias = "btrouter_psk_wrap_key"\nandroid:allowBackup="false"\nsharedpref -> excluded in data_extraction_rules.xml`,
      },
    ],
    en: [
      {
        title: 'Dedicated UUID & fail-closed MAC allowlist',
        desc: 'Instead of standard SPP, BTRouter binds a dedicated RFCOMM UUID 7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90. Server closes any incoming socket whose MAC address does not match the configured Client and applies progressive backoff up to 5 seconds.',
        code: `UUID = 7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90\nif (remoteMac != allowedClientMac) {\n    socket.close() // fail-closed + backoff up to 5s\n}`,
      },
      {
        title: 'Mutual PSK + HMAC-SHA256 handshake',
        desc: 'Client and Server derive master key SHA-256("BTRouter-PSK-v2:" + normalizedSecret) from a 16-char pairing secret, exchange 16-byte SecureRandom nonces, and verify HMAC digests using constant-time MessageDigest.isEqual.',
        code: `master = SHA256("BTRouter-PSK-v2:" + secret)\nhmacC = HMAC_SHA256(master, nonceS || nonceC)\nrequire(MessageDigest.isEqual(hmacC, expected))`,
      },
      {
        title: 'Directional AES-256-GCM framing',
        desc: 'Once authenticated, independent keys KEY-C2S and KEY-S2C are derived. Every frame uses a 12-byte nonce (4-byte direction tag + 8-byte strictly monotonic counter) authenticated as AAD.',
        code: `nonce12 = direction4B || seqCounter8B\nciphertext = AES_256_GCM(KEY_S2C, nonce12, aad=nonce12, frame)`,
      },
      {
        title: 'Hardware-backed AndroidKeyStore wrapping',
        desc: 'Both apps wrap the stored pairing secret using btrouter_psk_wrap_key (AES-256-GCM) inside AndroidKeyStore, with android:allowBackup="false" and sharedpref excluded from device transfers.',
        code: `alias = "btrouter_psk_wrap_key"\nandroid:allowBackup="false"\nsharedpref -> excluded in data_extraction_rules.xml`,
      },
    ],
  };

  const frameSegments = {
    ru: {
      dir: {
        title: '4-байтный идентификатор направления',
        desc: 'Отличает потоки Client→Server (KEY-C2S) от Server→Client (KEY-S2C) и входит в 12-байтный GCM nonce вместе с 8-байтным счётчиком, исключая повторное воспроизведение перехваченного кадра в обратную сторону.',
      },
      seq: {
        title: '8-байтный монотонный счётчик (AAD)',
        desc: 'Строго возрастает с каждым кадром и аутентифицируется как Additional Authenticated Data (AAD). Любая попытка переставить, пропустить или повторить кадр немедленно завершает сессию.',
      },
      payload: {
        title: 'Зашифрованная полезная нагрузка (AES-256-GCM)',
        desc: 'JSON-команды, входящие/исходящие СМС, события звонков, фрагменты контактов и телеметрия шифруются без утечки метаданных в открытом виде.',
      },
      tag: {
        title: '16-байтный GCM тег аутентификации (128 бит)',
        desc: 'Гарантирует целостность и подлинность каждого кадра: при модификации даже одного бита шифротекста или заголовка расшифровка отвергается.',
      },
    },
    en: {
      dir: {
        title: '4-byte direction identifier',
        desc: 'Separates Client→Server (KEY-C2S) and Server→Client (KEY-S2C) streams and forms the prefix of the 12-byte GCM nonce, preventing cross-direction reflection attacks.',
      },
      seq: {
        title: '8-byte monotonic sequence counter (AAD)',
        desc: 'Strictly increments per frame and is authenticated as Additional Authenticated Data (AAD). Reordered or replayed frames immediately terminate the connection.',
      },
      payload: {
        title: 'AES-256-GCM encrypted payload',
        desc: 'Carries JSON commands, SMS bodies, call state updates, chunked contact batches, and telemetry encrypted under the session directional key.',
      },
      tag: {
        title: '16-byte GCM authentication tag (128-bit)',
        desc: 'Protects full integrity of both ciphertext and AAD headers; a single flipped bit causes verification failure and immediate socket drop.',
      },
    },
  };

  const otpPresets = {
    ru: 'MIR-4412 19.11.26 Оплата 1 490.00 RUB. Код подтверждения: 649208. Никому не говорите код.',
    en: 'Your verification code is 839104 for card *9082 ($124.50 on 11/22/2026). Do not share it.',
    tr: '482910 tek kullanimlik sifreniz ile *7731 kartinizdan 850.00 TL harcama onaylayiniz. B001',
  };

  let currentLang = 'ru';
  let currentProtoStep = 0;
  let currentFrameSeg = 'dir';
  let pskVisible = false;
  let hotspotActive = false;
  let mobileDataActive = true;
  let missedCount = 2;
  let toastTimer = null;

  const tunnelPulseDot = document.getElementById('tunnelPulseDot');
  const tunnelPacketLabel = document.getElementById('tunnelPacketLabel');
  const srvStateText = document.getElementById('srvStateText');
  const srvLogList = document.getElementById('srvLogList');
  const srvPskValue = document.getElementById('srvPskValue');
  const btnTogglePsk = document.getElementById('btnTogglePsk');
  const cliDataState = document.getElementById('cliDataState');
  const cliStatusLine = document.getElementById('cliStatusLine');
  const cliCallBanner = document.getElementById('cliCallBanner');
  const cliChatList = document.getElementById('cliChatList');
  const missedCallsBadge = document.getElementById('missedCallsBadge');
  const cliToast = document.getElementById('cliToast');
  const tileMobileData = document.getElementById('tileMobileData');
  const tileHotspot = document.getElementById('tileHotspot');

  function triggerTunnelPulse(direction, label) {
    if (!tunnelPulseDot) return;
    tunnelPulseDot.classList.remove('animate-right', 'animate-left');
    void tunnelPulseDot.offsetWidth;
    tunnelPulseDot.classList.add(direction === 'left' ? 'animate-left' : 'animate-right');
    if (tunnelPacketLabel && label) {
      tunnelPacketLabel.textContent = label;
    }
  }

  function addServerLog(tag, tagClass, text) {
    if (!srvLogList) return;
    const item = document.createElement('div');
    item.className = 'srv-log-item';
    const badge = document.createElement('span');
    badge.className = `log-tag ${tagClass}`;
    badge.textContent = tag;
    const msg = document.createElement('span');
    msg.textContent = text;
    item.appendChild(badge);
    item.appendChild(msg);
    srvLogList.prepend(item);
    while (srvLogList.children.length > 4) {
      srvLogList.removeChild(srvLogList.lastElementChild);
    }
  }

  function showClientToast(text) {
    if (!cliToast) return;
    cliToast.textContent = text;
    cliToast.hidden = false;
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      cliToast.hidden = true;
    }, 2600);
  }

  // Simulator: Bank OTP arrival
  document.getElementById('btnSimOtp')?.addEventListener('click', () => {
    const otpCode = String(Math.floor(100000 + Math.random() * 899999));
    triggerTunnelPulse(
      'right',
      currentLang === 'ru' ? `СМС · ${otpCode}` : `SMS · ${otpCode}`
    );
    if (srvStateText) {
      srvStateText.textContent = currentLang === 'ru'
        ? `Передано СМС с кодом ${otpCode}`
        : `Forwarded SMS with code ${otpCode}`;
    }
    addServerLog(
      'СМС',
      'cyan',
      currentLang === 'ru'
        ? `Отправлено сообщение (код ${otpCode})`
        : `Sent text message (code ${otpCode})`
    );

    setTimeout(() => {
      const card = document.createElement('div');
      card.className = 'chat-item unread';
      card.innerHTML = `
        <div class="chat-avatar avatar-emerald">S</div>
        <div class="chat-body">
          <div class="chat-head">
            <span class="chat-sender">SberBank</span>
            <span class="chat-time">${currentLang === 'ru' ? 'сейчас' : 'now'}</span>
          </div>
          <div class="chat-preview">${
            currentLang === 'ru'
              ? `Код подтверждения ${otpCode} для входа`
              : `Verification code ${otpCode} to sign in`
          }</div>
        </div>
        <div class="chat-meta"><span class="unread-badge">1</span></div>
      `;
      if (cliChatList) {
        cliChatList.prepend(card);
        while (cliChatList.children.length > 4) {
          cliChatList.removeChild(cliChatList.lastElementChild);
        }
      }
      if (cliStatusLine) {
        cliStatusLine.textContent = currentLang === 'ru'
          ? `Код ${otpCode} найден и скопирован`
          : `Code ${otpCode} found & copied`;
      }
      showClientToast(
        currentLang === 'ru'
          ? `Код ${otpCode} скопирован — можно вставлять`
          : `Code ${otpCode} copied — ready to paste`
      );
    }, 280);
  });

  // Simulator: Incoming Call
  document.getElementById('btnSimCall')?.addEventListener('click', () => {
    triggerTunnelPulse(
      'right',
      currentLang === 'ru' ? 'Звонок' : 'Incoming call'
    );
    if (cliCallBanner) {
      cliCallBanner.hidden = false;
    }
    if (srvStateText) {
      srvStateText.textContent = currentLang === 'ru'
        ? 'Звонит +7 (916) 555-01-42'
        : 'Ringing +7 (916) 555-01-42';
    }
    addServerLog(
      'CALL',
      'crimson',
      currentLang === 'ru'
        ? 'Входящий звонок +7 916 555-01-42'
        : 'Incoming call +7 916 555-01-42'
    );
    if (cliStatusLine) {
      cliStatusLine.textContent = currentLang === 'ru'
        ? 'Входящий звонок на втором телефоне'
        : 'Incoming call on your SIM phone';
    }
  });

  // Client Decline Call button
  document.getElementById('btnDeclineCall')?.addEventListener('click', () => {
    if (cliCallBanner) {
      cliCallBanner.hidden = true;
    }
    missedCount += 1;
    if (missedCallsBadge) {
      missedCallsBadge.textContent = String(missedCount);
    }
    triggerTunnelPulse(
      'left',
      currentLang === 'ru' ? 'Сброс звонка' : 'Decline call'
    );
    addServerLog(
      'OK',
      'emerald',
      currentLang === 'ru'
        ? 'Звонок сброшен с основного телефона'
        : 'Call declined from primary phone'
    );
    if (srvStateText) {
      srvStateText.textContent = currentLang === 'ru'
        ? 'Звонок отклонён'
        : 'Call declined remotely';
    }
    if (cliStatusLine) {
      cliStatusLine.textContent = currentLang === 'ru'
        ? 'Звонок сброшен'
        : 'Call dismissed';
    }
    showClientToast(
      currentLang === 'ru' ? 'Звонок сброшен на втором телефоне' : 'Call declined on SIM phone'
    );
  });

  function toggleHotspot() {
    hotspotActive = !hotspotActive;
    tileHotspot?.classList.toggle('active', hotspotActive);
    tileHotspot?.setAttribute('aria-pressed', String(hotspotActive));
    triggerTunnelPulse(
      'left',
      currentLang === 'ru' ? 'Точка доступа' : 'Wi-Fi Hotspot'
    );
    addServerLog(
      'WI-FI',
      'cyan',
      hotspotActive
        ? (currentLang === 'ru' ? 'Раздача Wi-Fi включена' : 'Wi-Fi hotspot turned on')
        : (currentLang === 'ru' ? 'Раздача Wi-Fi выключена' : 'Wi-Fi hotspot turned off')
    );
    if (srvStateText) {
      srvStateText.textContent = hotspotActive
        ? (currentLang === 'ru' ? 'Точка доступа Wi-Fi включена' : 'Wi-Fi hotspot is active')
        : (currentLang === 'ru' ? 'Точка доступа Wi-Fi выключена' : 'Wi-Fi hotspot is off');
    }
    showClientToast(
      hotspotActive
        ? (currentLang === 'ru' ? 'Раздача Wi-Fi включена' : 'Hotspot turned On')
        : (currentLang === 'ru' ? 'Раздача Wi-Fi выключена' : 'Hotspot turned Off')
    );
  }

  function toggleMobileData() {
    mobileDataActive = !mobileDataActive;
    tileMobileData?.classList.toggle('active', mobileDataActive);
    tileMobileData?.setAttribute('aria-pressed', String(mobileDataActive));
    triggerTunnelPulse(
      'left',
      currentLang === 'ru' ? 'Интернет' : 'Mobile Data'
    );
    addServerLog(
      'NET',
      'emerald',
      mobileDataActive
        ? (currentLang === 'ru' ? 'Мобильный интернет включён' : 'Mobile data turned on')
        : (currentLang === 'ru' ? 'Мобильный интернет выключен' : 'Mobile data turned off')
    );
    if (cliDataState) {
      cliDataState.textContent = mobileDataActive ? '🌐 LTE' : '🌐 OFF';
    }
    showClientToast(
      mobileDataActive
        ? (currentLang === 'ru' ? 'Интернет включён' : 'Mobile Data On')
        : (currentLang === 'ru' ? 'Интернет выключен' : 'Mobile Data Off')
    );
  }

  document.getElementById('btnSimHotspot')?.addEventListener('click', toggleHotspot);
  tileHotspot?.addEventListener('click', toggleHotspot);
  tileMobileData?.addEventListener('click', toggleMobileData);

  // PSK Show/Hide toggle on Server phone
  btnTogglePsk?.addEventListener('click', () => {
    pskVisible = !pskVisible;
    if (srvPskValue) {
      srvPskValue.textContent = pskVisible ? 'A7K9-M4X2-P8Q3-R5N6' : '••••-••••-••••-••••';
    }
    btnTogglePsk.textContent = pskVisible
      ? translations[currentLang].srv_btn_hide
      : translations[currentLang].srv_btn_show;
  });

  // Interactive OTP Extractor Sandbox
  const otpInput = document.getElementById('otpInput');
  const otpExtractedCode = document.getElementById('otpExtractedCode');
  const otpIgnoredHint = document.getElementById('otpIgnoredHint');

  function extractOtp(text) {
    if (!text) return null;
    const cleaned = text
      .replace(/\b[BM]\d{3}\b/gi, ' ')
      .replace(/\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b/g, ' ')
      .replace(/\b\d{1,2}:\d{2}(?::\d{2})?\b/g, ' ')
      .replace(/(?:MIR|VISA|MC|CARD|KART|карт[аеуы]?|\*)\s*[-*]?\s*\d{4}\b/gi, ' ')
      .replace(/\+\d[\d\s()-]{6,}/g, ' ')
      .replace(/\d+(?:[., ]\d{2,3})*\s*(?:RUB|USD|EUR|TRY|TL|₽|\$|€)/gi, ' ');

    const keywordMatch = cleaned.match(
      /(?:код(?:\s+подтверждения)?|пароль|code|otp|pin|sifreniz|şifreniz|onay)[^0-9]{0,20}(\d{4,8})\b/i
    );
    if (keywordMatch) return keywordMatch[1];

    const standalone = cleaned.match(/\b(\d{4,8})\b/);
    return standalone ? standalone[1] : null;
  }

  function updateOtpSandbox() {
    if (!otpInput || !otpExtractedCode) return;
    const code = extractOtp(otpInput.value);
    otpExtractedCode.textContent = code || (currentLang === 'ru' ? 'не найден' : 'none');
  }

  otpInput?.addEventListener('input', updateOtpSandbox);

  document.querySelectorAll('.preset-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.preset-chip').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const preset = btn.getAttribute('data-preset');
      if (otpInput && preset && otpPresets[preset]) {
        otpInput.value = otpPresets[preset];
        updateOtpSandbox();
      }
    });
  });

  // Under-the-hood Deep Technical Drawer
  const btnToggleTechDrawer = document.getElementById('btnToggleTechDrawer');
  const techDrawerContent = document.getElementById('techDrawerContent');
  const techToggleLabel = document.getElementById('techToggleLabel');

  function setTechDrawerOpen(open) {
    if (!btnToggleTechDrawer || !techDrawerContent) return;
    btnToggleTechDrawer.setAttribute('aria-expanded', String(open));
    techDrawerContent.hidden = !open;
    if (techToggleLabel) {
      techToggleLabel.textContent = open
        ? translations[currentLang].tech_btn_close
        : translations[currentLang].tech_btn_open;
    }
  }

  btnToggleTechDrawer?.addEventListener('click', () => {
    const isExpanded = btnToggleTechDrawer.getAttribute('aria-expanded') === 'true';
    setTechDrawerOpen(!isExpanded);
  });

  // Auto-open deep technical drawer when clicking "Под капотом" links or loading with #under-the-hood
  document.querySelectorAll('.nav-tech-link').forEach((link) => {
    link.addEventListener('click', () => {
      setTechDrawerOpen(true);
    });
  });

  if (window.location.hash === '#under-the-hood') {
    setTechDrawerOpen(true);
  }

  // Protocol Explorer Tabs (inside Under-the-hood)
  const protoTitle = document.getElementById('protoTitle');
  const protoDesc = document.getElementById('protoDesc');
  const protoCode = document.getElementById('protoCode');

  function renderProtocolStep(idx) {
    currentProtoStep = idx;
    const data = protoSteps[currentLang][idx];
    if (!data) return;
    if (protoTitle) protoTitle.textContent = data.title;
    if (protoDesc) protoDesc.textContent = data.desc;
    if (protoCode) protoCode.textContent = data.code;

    document.querySelectorAll('.proto-tab').forEach((tab, i) => {
      const active = i === idx;
      tab.classList.toggle('active', active);
      tab.setAttribute('aria-selected', String(active));
    });
  }

  document.querySelectorAll('.proto-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      const idx = Number(tab.getAttribute('data-step') || 0);
      renderProtocolStep(idx);
    });
  });

  // Security Frame Segment Switcher (inside Under-the-hood)
  const frameDetailTitle = document.getElementById('frameDetailTitle');
  const frameDetailDesc = document.getElementById('frameDetailDesc');

  function renderFrameSegment(seg) {
    currentFrameSeg = seg;
    const info = frameSegments[currentLang][seg];
    if (!info) return;
    if (frameDetailTitle) frameDetailTitle.textContent = info.title;
    if (frameDetailDesc) frameDetailDesc.textContent = info.desc;

    document.querySelectorAll('.frame-seg').forEach((btn) => {
      btn.classList.toggle('active', btn.getAttribute('data-seg') === seg);
    });
  }

  document.querySelectorAll('.frame-seg').forEach((btn) => {
    btn.addEventListener('click', () => {
      const seg = btn.getAttribute('data-seg') || 'dir';
      renderFrameSegment(seg);
    });
  });

  // Samsung Accordion
  const accBtn = document.getElementById('samsungAccordionBtn');
  const accBody = document.getElementById('samsungAccordionBody');
  accBtn?.addEventListener('click', () => {
    const expanded = accBtn.getAttribute('aria-expanded') === 'true';
    accBtn.setAttribute('aria-expanded', String(!expanded));
    if (accBody) accBody.hidden = expanded;
  });

  // Copy Chips
  document.querySelectorAll('.copy-chip').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const text = btn.getAttribute('data-copy');
      if (!text) return;
      try {
        await navigator.clipboard.writeText(text);
        btn.textContent = '✓';
        setTimeout(() => {
          btn.textContent = translations[currentLang].btn_copy;
        }, 1400);
      } catch {
        btn.textContent = '✓';
        setTimeout(() => {
          btn.textContent = translations[currentLang].btn_copy;
        }, 1400);
      }
    });
  });

  // Mobile Menu Drawer
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileDrawer = document.getElementById('mobileDrawer');
  mobileMenuBtn?.addEventListener('click', () => {
    const expanded = mobileMenuBtn.getAttribute('aria-expanded') === 'true';
    mobileMenuBtn.setAttribute('aria-expanded', String(!expanded));
    if (mobileDrawer) mobileDrawer.hidden = expanded;
  });

  mobileDrawer?.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => {
      mobileMenuBtn?.setAttribute('aria-expanded', 'false');
      mobileDrawer.hidden = true;
    });
  });

  // Language Switcher
  function setLanguage(lang) {
    currentLang = lang === 'en' ? 'en' : 'ru';
    document.documentElement.lang = currentLang;
    document.title = currentLang === 'ru'
      ? 'BTRouter — СМС, коды и звонки со второго телефона по Bluetooth'
      : 'BTRouter — Texts, Bank Codes & Calls from Your Second Phone over Bluetooth';
    const dict = translations[currentLang];

    document.querySelectorAll('[data-i18n]').forEach((el) => {
      const key = el.getAttribute('data-i18n');
      if (key && dict[key]) {
        el.textContent = dict[key];
      }
    });

    if (btnTogglePsk) {
      btnTogglePsk.textContent = pskVisible ? dict.srv_btn_hide : dict.srv_btn_show;
    }

    const techOpen = btnToggleTechDrawer?.getAttribute('aria-expanded') === 'true';
    if (techToggleLabel) {
      techToggleLabel.textContent = techOpen ? dict.tech_btn_close : dict.tech_btn_open;
    }

    document.querySelectorAll('.lang-btn').forEach((btn) => {
      btn.classList.toggle('active', btn.getAttribute('data-lang') === currentLang);
    });

    renderProtocolStep(currentProtoStep);
    renderFrameSegment(currentFrameSeg);
    updateOtpSandbox();

    if (otpIgnoredHint) {
      otpIgnoredHint.textContent = dict.otp_ignored_hint;
    }
  }

  document.querySelectorAll('.lang-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      setLanguage(btn.getAttribute('data-lang') || 'ru');
    });
  });

  // Subtle Scroll Reveal (only for visible consumer sections, never inside collapsed drawer)
  const revealTargets = document.querySelectorAll(
    '.bento-card, .case-card, .metric-card, #privacy .sec-card, .step-item, .dl-card'
  );
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12 }
    );
    revealTargets.forEach((el) => {
      el.classList.add('reveal');
      observer.observe(el);
    });
  }

  // Initialize dynamic views
  setLanguage('ru');
})();
