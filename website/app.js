(function () {
  const translations = {
    ru: {
      nav_use_cases: 'Для кого',
      nav_features: 'Возможности',
      nav_setup: 'Настройка',
      nav_download: 'Скачать',
      nav_troubleshooting: 'Помощь',
      nav_tech: 'Подробно',
      hero_pill: 'BTRouter · Android 10+ · Прямой Bluetooth',
      hero_title_lead: 'Второй телефон в рюкзаке — все СМС и звонки',
      hero_title_accent: 'у вас в руках',
      hero_subtitle:
        'BTRouter соединяет ваш основной смартфон со вторым телефоном, где стоит нужная SIM-карта. Читайте и отправляйте СМС, копируйте коды из банков за секунду, сбрасывайте лишние звонки и включайте Wi-Fi удалённо — даже без интернета.',
      hero_cta_demo: 'Попробовать в действии',
      hero_cta_download: 'Скачать v1.1.0',
      hero_meta_line: 'Работает офлайн · Без облаков и аккаунтов · Открытый код MIT',
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
      cases_eyebrow: '01 / ДЛЯ ЧЕГО ЭТО НУЖНО',
      cases_title: 'Удобно каждый день и в поездках',
      cases_desc:
        'Не носите два телефона в руках и не переставляйте постоянно SIM-карты — пусть второй смартфон спокойно лежит в рюкзаке, кармане или дома.',
      case_1_title: 'В поездках и за границей',
      case_1_desc:
        'Оставьте домашнюю или зарубежную SIM-карту во втором телефоне в сумке и получайте все коды подтверждения от банков и сервисов прямо на основной экран.',
      case_2_title: 'Рабочий и личный номер',
      case_2_desc:
        'Не доставайте второй смартфон ради каждого уведомления: переписывайтесь по СМС, сбрасывайте спам-звонки и смотрите пропущенные с одного устройства.',
      case_3_title: 'Интернет и раздача Wi\u2011Fi',
      case_3_desc:
        'Если второй телефон — Samsung, включайте на нём мобильный интернет и точку доступа Wi-Fi в одно касание с основного телефона, не доставая его из рюкзака.',
      features_eyebrow: '02 / ВОЗМОЖНОСТИ',
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
      notif_connected: 'Подключён',
      notif_now: 'сейчас',
      feat_calls_title: 'Звонки и пропущенные',
      feat_calls_desc:
        'Узнавайте о входящем звонке сразу и сбрасывайте ненужные вызовы прямо с основного телефона. Пропущенные сохраняются в отдельный список со счётчиком.',
      call_mini_decline: 'Сбросить',
      call_mini_declined: 'Сброшено ✓',
      feat_contacts_title: 'Контакты с обоих телефонов',
      feat_contacts_desc:
        'Пишите людям из записной книжки любого из двух смартфонов — переключение между адресными книгами и недавними номерами работает в один тап.',
      book_client: 'Основной',
      book_server: 'С телефона SIM',
      feat_samsung_title: 'Точка доступа в один клик (Samsung)',
      feat_samsung_desc:
        'Удобные круглые кнопки прямо на главном экране позволяют удалённо включать интернет и раздачу Wi-Fi на втором телефоне Samsung.',
      feat_sync_title: 'Ни одно сообщение не потеряется, даже если вы отошли',
      feat_sync_desc:
        'Если телефоны временно потеряли связь, всё накопившееся автоматически догрузится при возвращении в радиус Bluetooth. А после перезагрузки приложение само запустится заново без лишних напоминаний.',
      flow_1: 'Отошли от телефона',
      flow_2: 'Сохранено в очереди',
      flow_3: 'Мгновенно догружено',
      priv_eyebrow: '03 / ПРИВАТНОСТЬ',
      priv_title: 'Ваши сообщения остаются только между вами и вашими телефонами',
      priv_seal: 'Прямой канал без серверов, аккаунтов и чужих облаков.',
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
      setup_eyebrow: '04 / БЫСТРЫЙ СТАРТ',
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
      guide_acc_kicker: 'ПОШАГОВО · ДЛЯ НОВИЧКОВ',
      guide_acc_title: 'Подробная инструкция по установке и настройке',
      guide_acc_sub: 'Понятное руководство со скриншотами: установка APK, Play Защита, разрешения, батарея и связка телефонов',
      samsung_acc_intro:
        'На телефоне с SIM (Samsung с One UI 5+) откройте «Настройки → Режимы и сценарии → Сценарии → +», выберите условие «Уведомление получено» от BTRouterServer и добавьте 4 простых сценария по ключевым словам (отключив автовозврат):',
      btn_copy: 'Копировать',
      kw_hotspot_on: 'Мобильная точка доступа → Вкл.',
      kw_hotspot_off: 'Мобильная точка доступа → Выкл.',
      kw_data_on: 'Мобильные данные → Вкл.',
      kw_data_off: 'Мобильные данные → Выкл.',
      dl_eyebrow: '05 / СКАЧАТЬ v1.1.0',
      dl_title: 'Выберите приложение для каждого телефона',
      dl_desc:
        'Оба установочных файла бесплатны и подходят для любых смартфонов на Android 10 и выше.',
      dl_client_role: 'На ваш основной телефон',
      dl_client_sub: 'Приём СМС, кодов и звонков',
      dl_client_desc:
        'Устанавливается на смартфон, которым вы пользуетесь каждый день: показывает СМС-чаты, автоматически копирует коды и выводит пропущенные звонки.',
      dl_server_role: 'На второй телефон с SIM-картой',
      dl_server_sub: 'Работает в фоне и передаёт сигнал',
      dl_server_desc:
        'Устанавливается на смартфон с нужной SIM-картой: тихо работает в фоне, пересылает СМС и звонки только вашему телефону и следит за зарядом.',
      dl_ver_badge: 'Версия 1.1.0',
      dl_btn_client: 'Скачать BTRouterClient v1.1.0 (.apk)',
      dl_btn_server: 'Скачать BTRouterServer v1.1.0 (.apk)',
      dl_notice_text: 'Все APK подписаны и проверены apksigner; контрольные суммы SHA256SUMS.txt и список изменений доступны на GitHub.',
      dl_notice_link: 'Релиз v1.1.0 и SHA-256 на GitHub →',
      ts_eyebrow: '06 / РЕШЕНИЕ ПРОБЛЕМ',
      ts_title: 'Если что-то пошло не так',
      ts_desc:
        'Решения известных проблем, с которыми можно столкнуться при работе со связкой двух телефонов.',
      ts_0_tag: 'Установка · Play Protect',
      ts_0_title: 'Google Play Защита блокирует установку BTRouterServer',
      ts_0_sub: 'Окно «App blocked to protect your device» без кнопки «Всё равно установить» — почему появляется и как установить безопасно',
      ts_1_tag: 'СМС · VoLTE',
      ts_1_title: 'СМС долго висят в «Отправка…» или приходят дубликаты',
      ts_1_sub: 'Теряется подтверждение RP-ACK при включённом VoLTE на телефоне с SIM',
      ts_2_tag: 'Samsung One UI',
      ts_2_title: 'Перестали срабатывать кнопки «Точка доступа» или «Интернет»',
      ts_2_sub: 'Перезапуск сервиса «Режимы и сценарии» и проверка условий 4 сценариев',
      tech_eyebrow: 'ПОДРОБНО · ДЛЯ СПЕЦИАЛИСТОВ',
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
      build_sub: 'Запуск testDebugUnitTest, lintDebug и assembleDebug (Client versionCode 24 / Server versionCode 20)',
      footer_troubleshooting: 'Решение проблем',
      footer_tech: 'Подробно',
    },
    en: {
      nav_use_cases: 'Use Cases',
      nav_features: 'Features',
      nav_setup: 'Setup',
      nav_download: 'Download',
      nav_troubleshooting: 'Help',
      nav_tech: 'Details',
      hero_pill: 'BTRouter · Android 10+ · Direct Bluetooth',
      hero_title_lead: 'Your second phone stays in your bag — all texts and calls',
      hero_title_accent: 'right in your hand',
      hero_subtitle:
        'BTRouter links your everyday smartphone to your secondary phone holding the SIM card. Read and reply to SMS, copy bank verification codes in one second, dismiss unwanted calls, and toggle Wi-Fi hotspot remotely — even offline.',
      hero_cta_demo: 'See It in Action',
      hero_cta_download: 'Download v1.1.0',
      hero_meta_line: 'Works offline · Zero cloud accounts · Open-source MIT',
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
      cases_eyebrow: '01 / WHO IT HELPS',
      cases_title: 'Built for travel, work, and everyday convenience',
      cases_desc:
        'Stop juggling two phones in your hands or swapping SIM trays — keep your second phone tucked safely in your bag, pocket, or at home.',
      case_1_title: 'Travel & roaming abroad',
      case_1_desc:
        'Keep your home or local SIM card inside your secondary phone in a bag and receive every bank SMS and login code directly on your main screen.',
      case_2_title: 'Work & personal numbers',
      case_2_desc:
        'No need to pull out your work phone for every ping: answer texts, dismiss spam calls, and review missed callers right from your daily phone.',
      case_3_title: 'Remote internet & Wi\u2011Fi hotspot',
      case_3_desc:
        'If your SIM phone is a Samsung, switch mobile data and Wi-Fi hotspot on or off remotely in one tap without ever taking it out of your backpack.',
      features_eyebrow: '02 / FEATURES',
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
      notif_connected: 'Connected',
      notif_now: 'now',
      feat_calls_title: 'Incoming calls & missed log',
      feat_calls_desc:
        'Get instant alerts when your second phone rings and decline unwanted calls remotely. Missed calls are saved neatly with an unread counter.',
      call_mini_decline: 'Decline',
      call_mini_declined: 'Declined ✓',
      feat_contacts_title: 'Contacts from both phones',
      feat_contacts_desc:
        'Message people saved on either smartphone — switch between your primary contacts, SIM phone contacts, and recent recipients in one tap.',
      book_client: 'Primary',
      book_server: 'SIM Phone',
      feat_samsung_title: 'One-tap Wi-Fi hotspot (Samsung)',
      feat_samsung_desc:
        'Handy round buttons right on the main screen let you toggle mobile data and Wi-Fi hotspot on a Samsung SIM phone.',
      feat_sync_title: 'Never miss a text, even if you step away',
      feat_sync_desc:
        'If your phones briefly move out of Bluetooth range, queued messages catch up automatically once you reconnect — and everything restarts smoothly after a reboot.',
      flow_1: 'Stepped away',
      flow_2: 'Held in queue',
      flow_3: 'Delivered on return',
      priv_eyebrow: '03 / PRIVACY',
      priv_title: 'Your messages stay strictly between your two phones',
      priv_seal: 'Direct Bluetooth link with zero third-party cloud servers.',
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
      setup_eyebrow: '04 / EASY SETUP',
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
      guide_acc_kicker: 'STEP-BY-STEP · BEGINNER FRIENDLY',
      guide_acc_title: 'Detailed step-by-step installation & setup guide',
      guide_acc_sub: 'Plain-language walkthrough with screenshots: APK install, Play Protect, permissions, battery & pairing',
      samsung_acc_intro:
        'On a Samsung SIM phone (One UI 5+), open Settings → Modes and Routines → Routines → "+", trigger on "Notification received" from BTRouterServer, and add 4 simple routines using these keywords (disable auto-revert):',
      btn_copy: 'Copy',
      kw_hotspot_on: 'Mobile Hotspot → On',
      kw_hotspot_off: 'Mobile Hotspot → Off',
      kw_data_on: 'Mobile Data → On',
      kw_data_off: 'Mobile Data → Off',
      dl_eyebrow: '05 / DOWNLOAD v1.1.0',
      dl_title: 'Get the right app for each phone',
      dl_desc:
        'Both installers are completely free and work on any smartphone running Android 10 or newer.',
      dl_client_role: 'For your everyday phone',
      dl_client_sub: 'Reads SMS, copies codes & shows calls',
      dl_client_desc:
        'Install on the smartphone you hold in your hand: shows SMS threads, copies verification codes, and notifies you about calls.',
      dl_server_role: 'For your second phone with SIM',
      dl_server_sub: 'Runs quietly in the background',
      dl_server_desc:
        'Install on the phone carrying your SIM card: forwards texts and calls exclusively to your main phone and reports battery & signal.',
      dl_ver_badge: 'Version 1.1.0',
      dl_btn_client: 'Download BTRouterClient v1.1.0 (.apk)',
      dl_btn_server: 'Download BTRouterServer v1.1.0 (.apk)',
      dl_notice_text: 'All APKs are signed and apksigner-verified; SHA256SUMS.txt checksums and release notes are available on GitHub.',
      dl_notice_link: 'v1.1.0 Release & SHA-256 on GitHub →',
      ts_eyebrow: '06 / TROUBLESHOOTING',
      ts_title: 'Troubleshooting common issues',
      ts_desc:
        'Known fixes for the most common carrier and Samsung One UI quirks you may run into.',
      ts_0_tag: 'Install · Play Protect',
      ts_0_title: 'Google Play Protect blocks installing BTRouterServer',
      ts_0_sub: '“App blocked to protect your device” with no “Install anyway” button — why it happens and how to install safely',
      ts_1_tag: 'SMS · VoLTE',
      ts_1_title: 'Texts stay stuck on “Sending…” or arrive as duplicates',
      ts_1_sub: 'Missing RP-ACK delivery confirmation when VoLTE is enabled on the SIM phone',
      ts_2_tag: 'Samsung One UI',
      ts_2_title: 'Hotspot or Mobile Data toggles stopped triggering',
      ts_2_sub: 'Restarting Modes and Routines and verifying the 4 keyword rules',
      tech_eyebrow: 'DETAILS · FOR DEVELOPERS',
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
      build_sub: 'Run testDebugUnitTest, lintDebug, and assembleDebug (Client versionCode 24 / Server versionCode 20)',
      footer_troubleshooting: 'Troubleshooting',
      footer_tech: 'Details',
    },
  };

  const troubleshootingBodies = {
    ru: {
      issueBody0: `
        <div class="issue-split">
          <div class="issue-split-text">
            <div class="issue-block">
              <h3 class="issue-step-title">Почему появляется эта блокировка</h3>
              <p>Это усиленная защита Google Play Protect при установке APK из браузера: <code>BTRouterServer</code> запрашивает системные разрешения на <strong>SMS</strong> (без них невозможна пересылка сообщений) и подписан новым релизным сертификатом, который пока не набрал репутации в Google Play. Вредоносного кода в приложении нет — весь код открыт в репозитории GitHub.</p>
            </div>
            <div class="issue-block">
              <h3 class="issue-step-title">Способ 1 — установка прямо с телефона</h3>
              <ul>
                <li>Откройте <strong>Google Play</strong> → иконка профиля (справа сверху) → <strong>Play Защита</strong> (Play Protect) → шестерёнка ⚙ (справа сверху).</li>
                <li>Временно отключите <strong>«Сканировать приложения с помощью Play Защиты»</strong>.</li>
                <li><em>На Samsung также проверьте:</em> <code>Настройки → Безопасность и конфиденциальность → Автоблокировка</code> (Auto Blocker) должна быть выключена на время установки.</li>
                <li>Если ранее была установлена старая тестовая (<code>debug</code>) сборка <code>1.0.xx</code> — удалите её перед установкой релиза <code>1.1.0</code>.</li>
                <li>Установите <code>BTRouterServer-v1.1.0.apk</code> и сразу <strong>включите Play Защиту обратно</strong>.</li>
                <li>Если Android 13+ ограничит выдачу прав SMS/звонков, откройте <code>Настройки → Приложения → BT Router Server → ⋮ (справа сверху) → Разрешить ограниченные настройки</code>.</li>
              </ul>
            </div>
            <div class="issue-block">
              <h3 class="issue-step-title">Способ 2 — через ПК и ADB (без отключения Play Защиты)</h3>
              <p>Если на телефоне включена отладка по USB, команда <code>adb install BTRouterServer-v1.1.0.apk</code> устанавливает приложение в обход браузерной блокировки Play Protect.</p>
            </div>
          </div>
          <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/play-protect-block.jpg" data-lightbox-alt="Блокировка Google Play Protect при установке BTRouterServer" data-lightbox-caption="Окно Google Play Protect при установке BTRouterServer из браузера (нажмите для увеличения)">
            <img src="./assets/guide/play-protect-block.jpg" alt="Блокировка Google Play Protect при установке BTRouterServer" loading="lazy" />
            <span class="guide-shot-cap">Нажмите, чтобы увеличить</span>
          </button>
        </div>
        <div class="issue-note">
          <span class="issue-note-title">Проверка подлинности APK</span>
          <ul>
            <li><code>BTRouterClient</code> не запрашивает SMS-разрешения, поэтому обычно устанавливается без блокировки.</li>
            <li>Хэш-суммы релизных файлов публикуются в <code>SHA256SUMS.txt</code> на странице GitHub Releases, а сертификат подписи <code>v1.1.0</code> имеет отпечаток SHA-256: <code>0d13e10fa3639c9f591c63102200fcb65d24327c8bc63f5b67abb99336521fea</code>.</li>
          </ul>
        </div>
      `,
      issueBody1: `
        <div class="issue-block">
          <h3 class="issue-step-title">Почему так происходит</h3>
          <p>На некоторых сочетаниях прошивки второго телефона, SIM-карты, оператора и роуминга подтверждение <code>RP-ACK</code> в режиме <code>SMS over IMS</code> (<code>VoLTE</code>) теряется. Из-за этого на основном телефоне долго висит статус <code>Sending…</code>, а модем телефона с SIM делает повторные попытки и отправляет дубликаты.</p>
        </div>
        <div class="issue-block">
          <h3 class="issue-step-title">Что сделать</h3>
          <p>Отключите <strong>VoLTE</strong> в настройках мобильной сети на телефоне с SIM (<code>BTRouterServer</code>). Без <code>VoLTE</code> сообщение уходит по стандартному каналу (<code>Телефон с SIM → SMSC</code>), подтверждение <code>RP-ACK</code> приходит сразу, статус на основном телефоне обновляется мгновенно, и повторов больше нет.</p>
        </div>
        <div class="issue-note">
          <span class="issue-note-title">Полезно знать</span>
          <ul>
            <li>На скорость 4G-интернета и работу точки доступа отключение <code>VoLTE</code> <strong>не влияет</strong>.</li>
            <li>Включённый <code>VoLTE</code> у собеседника никак не мешает — доставка <code>SMSC → получатель</code> идёт отдельным независимым соединением.</li>
            <li>Единственный нюанс: если в вашей сети или стране полностью отключены 2G и 3G, без <code>VoLTE</code> могут не проходить обычные голосовые звонки на телефон с SIM (там, где 2G/3G работают, звонки идут через них).</li>
          </ul>
        </div>
      `,
      issueBody2: `
        <div class="issue-block">
          <h3 class="issue-step-title">1. Перезапустите движок «Режимы и сценарии» на телефоне с SIM</h3>
          <ul>
            <li>Откройте <code>Настройки → Приложения → Режимы и сценарии → Остановить</code> (принудительно).</li>
            <li>Там же перейдите в <code>Память → Очистить кэш</code> (именно кэш, <strong>не</strong> данные — чтобы не стереть настроенные сценарии).</li>
            <li>Там же откройте <code>Батарея → Без ограничений</code>.</li>
            <li>Откройте приложение <strong>Режимы и сценарии</strong>, чтобы его фоновый сервис заново поднялся, и проверьте, не висит ли какой-то из сценариев в состоянии «Выполняется».</li>
          </ul>
        </div>
        <div class="issue-block">
          <h3 class="issue-step-title">2. Проверьте сами 4 сценария</h3>
          <ul>
            <li>В условии «Уведомление получено» должно быть выбрано строго приложение <code>BTRouterServer</code> (если оставить «Любое приложение», уведомления от самих сценариев могут перекрёстно запускать друг друга).</li>
            <li>В поле ключевых слов — <strong>одно точное слово</strong> (<code>BTR_HOTSPOT_ON</code>, <code>BTR_HOTSPOT_OFF</code>, <code>BTR_MOBILE_DATA_ON</code>, <code>BTR_MOBILE_DATA_OFF</code>) без лишних пробелов и дополнительных слов.</li>
            <li>В блоке «Когда сценарий завершится» выберите <strong>«Ничего не делать»</strong>.</li>
            <li>Выключите и снова включите тумблеры этих 4 сценариев — на One UI это заново перерегистрирует их фильтры.</li>
          </ul>
        </div>
      `,
    },
    en: {
      issueBody0: `
        <div class="issue-split">
          <div class="issue-split-text">
            <div class="issue-block">
              <h3 class="issue-step-title">Why this block appears</h3>
              <p>Google Play Protect Enhanced Fraud Protection automatically blocks APK files downloaded via a browser when they request sensitive <strong>SMS</strong> permissions (required for forwarding texts) and use a newly issued release certificate without Play Store history yet. There is no malware in BTRouter — all source code is public on GitHub.</p>
            </div>
            <div class="issue-block">
              <h3 class="issue-step-title">Method 1 — install directly on the phone</h3>
              <ul>
                <li>Open <strong>Google Play</strong> → profile icon (top right) → <strong>Play Protect</strong> → gear icon ⚙ (top right).</li>
                <li>Temporarily turn off <strong>“Scan apps with Play Protect”</strong>.</li>
                <li><em>On Samsung phones also check:</em> <code>Settings → Security and privacy → Auto Blocker</code> must be turned off during installation.</li>
                <li>If an older test (<code>debug</code>) <code>1.0.xx</code> build was installed earlier, uninstall it first to avoid a signature mismatch.</li>
                <li>Install <code>BTRouterServer-v1.1.0.apk</code> and immediately <strong>turn Play Protect back on</strong>.</li>
                <li>If Android 13+ locks SMS/Call permissions for sideloaded apps, go to <code>Settings → Apps → BT Router Server → ⋮ (top right) → Allow restricted settings</code>.</li>
              </ul>
            </div>
            <div class="issue-block">
              <h3 class="issue-step-title">Method 2 — install via PC &amp; ADB (no need to toggle Play Protect)</h3>
              <p>With USB debugging enabled, running <code>adb install BTRouterServer-v1.1.0.apk</code> from a computer bypasses the browser sideload restriction cleanly.</p>
            </div>
          </div>
          <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/play-protect-block.jpg" data-lightbox-alt="Google Play Protect block dialog when installing BTRouterServer" data-lightbox-caption="Google Play Protect dialog when installing BTRouterServer from a browser (click to enlarge)">
            <img src="./assets/guide/play-protect-block.jpg" alt="Google Play Protect block dialog when installing BTRouterServer" loading="lazy" />
            <span class="guide-shot-cap">Click to enlarge</span>
          </button>
        </div>
        <div class="issue-note">
          <span class="issue-note-title">Verify APK integrity</span>
          <ul>
            <li><code>BTRouterClient</code> does not request SMS permissions, so it normally installs without this block.</li>
            <li>Official checksums live in <code>SHA256SUMS.txt</code> on GitHub Releases, and the <code>v1.1.0</code> signing certificate SHA-256 fingerprint is <code>0d13e10fa3639c9f591c63102200fcb65d24327c8bc63f5b67abb99336521fea</code>.</li>
          </ul>
        </div>
      `,
      issueBody1: `
        <div class="issue-block">
          <h3 class="issue-step-title">Why it happens</h3>
          <p>On certain combinations of SIM phone firmware, carrier, and roaming networks, the <code>RP-ACK</code> acknowledgement gets dropped in <code>SMS over IMS</code> (<code>VoLTE</code>) mode. Because no confirmation returns, your primary phone stays stuck on <code>Sending…</code> while the SIM phone modem retries and sends duplicates.</p>
        </div>
        <div class="issue-block">
          <h3 class="issue-step-title">How to fix it</h3>
          <p>Disable <strong>VoLTE</strong> in the mobile network settings on the SIM phone (<code>BTRouterServer</code>). With <code>VoLTE</code> turned off, SMS travels over the standard channel (<code>SIM Phone → SMSC</code>), <code>RP-ACK</code> returns right away, your primary phone updates immediately, and retries stop.</p>
        </div>
        <div class="issue-note">
          <span class="issue-note-title">Good to know</span>
          <ul>
            <li>Turning off <code>VoLTE</code> <strong>does not affect</strong> 4G internet speeds or Wi-Fi hotspot tethering.</li>
            <li>Whether the recipient has <code>VoLTE</code> enabled does not interfere — <code>SMSC → recipient</code> delivery runs over a separate connection.</li>
            <li>One caveat: if your carrier has completely shut down both 2G and 3G, standard voice calls to the SIM phone may stop working without <code>VoLTE</code> (where 2G/3G are active, calls simply fall back to them).</li>
          </ul>
        </div>
      `,
      issueBody2: `
        <div class="issue-block">
          <h3 class="issue-step-title">1. Restart the Modes and Routines engine on the SIM phone</h3>
          <ul>
            <li>Open <code>Settings → Apps → Modes and Routines → Force stop</code>.</li>
            <li>In the same screen, go to <code>Storage → Clear cache</code> (cache only, <strong>not</strong> Clear data — so you keep your rules).</li>
            <li>Set <code>Battery → Unrestricted</code>.</li>
            <li>Open the <strong>Modes and Routines</strong> app so its background service restarts, and check that none of the routines are stuck in “Running”.</li>
          </ul>
        </div>
        <div class="issue-block">
          <h3 class="issue-step-title">2. Double-check all 4 routines</h3>
          <ul>
            <li>Under the “Notification received” condition, select strictly <code>BTRouterServer</code> (if any rule still says “Any app”, routine notifications can cross-trigger each other).</li>
            <li>In the keyword field, keep <strong>one exact word</strong> (<code>BTR_HOTSPOT_ON</code>, <code>BTR_HOTSPOT_OFF</code>, <code>BTR_MOBILE_DATA_ON</code>, <code>BTR_MOBILE_DATA_OFF</code>) with no extra spaces or words.</li>
            <li>Set “When routine ends” to <strong>“Do nothing”</strong>.</li>
            <li>Toggle the switches for all 4 routines off and back on — on One UI this re-registers their notification filters.</li>
          </ul>
        </div>
      `,
    },
  };

  const detailedGuideBodies = {
    ru: `
      <div class="guide-stages">
        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">01</span>
            <h3>Определитесь, какое приложение на какой телефон ставить</h3>
          </div>
          <div class="guide-stage-body">
            <p>Система состоит из двух разных программ. Не перепутайте их при установке:</p>
            <div class="guide-roles-grid">
              <div class="guide-role-card guide-role-server">
                <span class="guide-role-label">Телефон с SIM-картой (второй / запасной)</span>
                <strong>BT Router Server</strong>
                <p>Принимает СМС и звонки по мобильной сети и молча пересылает их только вашему основному телефону.</p>
              </div>
              <div class="guide-role-card guide-role-client">
                <span class="guide-role-label">Основной телефон (который всегда с вами)</span>
                <strong>BTRouterClient</strong>
                <p>Показывает чаты с СМС, автоматически копирует коды банков и выводит уведомления о звонках.</p>
              </div>
            </div>
            <p>Оба файла доступны в разделе <a href="#download" class="guide-inline-link">05 / Скачать v1.1.0</a> ниже на этой странице.</p>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">02</span>
            <h3>Скачайте и установите оба APK (что делать, если Google Play Защита блокирует установку)</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Откройте эту страницу на каждом телефоне и нажмите <strong>Скачать</strong> для нужного приложения (<code>BTRouterServer</code> на телефон с SIM, <code>BTRouterClient</code> на основной).</li>
              <li>Когда браузер спросит разрешение на установку неизвестных приложений — откройте <code>Настройки</code> и включите тумблер <strong>«Разрешить для этого источника»</strong>.</li>
              <li>Если раньше на телефоне уже стояла старая тестовая версия (<code>1.0.xx</code>) — предварительно удалите её, чтобы не возник конфликт подписи.</li>
            </ol>
            <div class="guide-block-grid">
              <div>
                <p><strong>Если появилось окно Google Play Защиты «App blocked to protect your device» (без кнопки «Всё равно установить»):</strong></p>
                <p>Так Android реагирует на новые APK из браузера, которые запрашивают доступ к СМС (<code>BTRouterServer</code>). Вирусов здесь нет — исходный код полностью открыт на GitHub.</p>
                <ol>
                  <li>Откройте приложение <strong>Google Play</strong> → нажмите на иконку профиля справа сверху → <strong>Play Защита</strong> (Play Protect) → значок шестерёнки ⚙ справа сверху.</li>
                  <li>Временно выключите <strong>«Сканировать приложения с помощью Play Защиты»</strong>. <em>(На Samsung также временно отключите <code>Настройки → Безопасность и конфиденциальность → Автоблокировка</code>.)</em></li>
                  <li>Запустите скачанный <code>BTRouterServer-v1.1.0.apk</code> ещё раз — установка пройдёт без блокировки.</li>
                  <li>После установки обязательно <strong>включите Play Защиту обратно</strong>.</li>
                  <li>Если позже Android 13+ не даёт включить разрешения СМС/телефона, откройте <code>Настройки → Приложения → BT Router Server → ⋮ (справа сверху) → Разрешить ограниченные настройки</code>.</li>
                </ol>
              </div>
              <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/play-protect-block.jpg" data-lightbox-alt="Предупреждение Google Play Protect при установке BTRouterServer" data-lightbox-caption="Окно Google Play Protect при установке BTRouterServer из браузера (нажмите для увеличения)">
                <img src="./assets/guide/play-protect-block.jpg" alt="Предупреждение Google Play Protect при установке BTRouterServer" loading="lazy" />
                <span class="guide-shot-cap">Окно Google Play Protect (нажмите, чтобы увеличить)</span>
              </button>
            </div>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">03</span>
            <h3>Свяжите два смартфона по Bluetooth</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Откройте системные <strong>Настройки → Bluetooth</strong> на обоих телефонах.</li>
              <li>Найдите второй смартфон в списке доступных устройств и выполните обычное сопряжение (подтвердите одинаковый код на обоих экранах).</li>
              <li>Достаточно просто добавить устройства в список <strong>«Связанные / Сопряжённые»</strong> — держать их принудительно подключёнными через меню Bluetooth не нужно, приложения свяжутся сами.</li>
            </ol>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">04</span>
            <h3>Обязательно проверьте разрешения и режим батареи на ОБОИХ телефонах</h3>
          </div>
          <div class="guide-stage-body">
            <p>Чтобы Android не усыплял приложения в фоне и сообщения не терялись, откройте <code>Настройки → Приложения</code> и убедитесь, что в пункте <strong>«Запрещено»</strong> (Not allowed) пусто (<strong>«Нет запрещённых разрешений»</strong>), отключите <strong>«Приостанавливать работу, если не используется»</strong> (Manage app if unused) и выберите <strong>Батарея → Без ограничений</strong> (Unrestricted).</p>

            <div class="guide-device-panel">
              <h4>1. На телефоне с SIM-картой — BT Router Server</h4>
              <ul>
                <li><strong>Разрешения (все 6 пунктов в «Разрешено»):</strong> <code>Журналы вызовов</code> (Call logs), <code>Контакты</code> (Contacts), <code>Устройства поблизости</code> (Nearby devices), <code>Уведомления</code> (Notifications), <code>Телефон</code> (Phone), <code>SMS</code>. В пункте «Запрещено» должно быть пусто.</li>
                <li><strong>Пауза неиспользуемого приложения:</strong> тумблер <code>Manage app if unused</code> выключен.</li>
                <li><strong>Батарея:</strong> <code>Без ограничений</code> (Unrestricted).</li>
              </ul>
              <div class="guide-shots-grid">
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/server-permissions.jpg" data-lightbox-alt="Разрешения приложения BT Router Server" data-lightbox-caption="BT Router Server: все 6 разрешений выданы, запрещённых нет, автоотзыв выключен">
                  <img src="./assets/guide/server-permissions.jpg" alt="Разрешения приложения BT Router Server" loading="lazy" />
                  <span class="guide-shot-cap">Разрешения BT Router Server: 6 разрешены, запрещённых нет</span>
                </button>
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/server-battery.jpg" data-lightbox-alt="Настройка батареи BT Router Server" data-lightbox-caption="BT Router Server: в разделе Батарея выбран пункт Unrestricted (Без ограничений)">
                  <img src="./assets/guide/server-battery.jpg" alt="Настройка батареи BT Router Server" loading="lazy" />
                  <span class="guide-shot-cap">Батарея BT Router Server: Без ограничений (Unrestricted)</span>
                </button>
              </div>
            </div>

            <div class="guide-device-panel">
              <h4>2. На основном телефоне — BTRouterClient</h4>
              <ul>
                <li><strong>Разрешения (все 3 пункта в «Разрешено»):</strong> <code>Контакты</code> (Contacts), <code>Устройства поблизости</code> (Nearby devices), <code>Уведомления</code> (Notifications). В пункте «Запрещено» должно быть пусто.</li>
                <li><strong>Пауза неиспользуемого приложения:</strong> тумблер <code>Manage app if unused</code> выключен.</li>
                <li><strong>Батарея:</strong> <code>Без ограничений</code> (Unrestricted).</li>
              </ul>
              <div class="guide-shots-grid">
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/client-permissions.jpg" data-lightbox-alt="Разрешения приложения BTRouterClient" data-lightbox-caption="BTRouterClient: все 3 разрешения выданы, запрещённых нет, автоотзыв выключен">
                  <img src="./assets/guide/client-permissions.jpg" alt="Разрешения приложения BTRouterClient" loading="lazy" />
                  <span class="guide-shot-cap">Разрешения BTRouterClient: 3 разрешены, запрещённых нет</span>
                </button>
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/client-battery.jpg" data-lightbox-alt="Настройка батареи BTRouterClient" data-lightbox-caption="BTRouterClient: в разделе Батарея выбран пункт Unrestricted (Без ограничений)">
                  <img src="./assets/guide/client-battery.jpg" alt="Настройка батареи BTRouterClient" loading="lazy" />
                  <span class="guide-shot-cap">Батарея BTRouterClient: Без ограничений (Unrestricted)</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">05</span>
            <h3>Настройте приложение BT Router Server (на телефоне с SIM)</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Откройте <strong>BT Router Server</strong> (при необходимости выберите язык <strong>RU</strong> или <strong>EN</strong>).</li>
              <li>Нажмите кнопку выбора клиентского устройства (<strong>«Выбрать устройство Client»</strong>) и укажите ваш основной телефон из списка Bluetooth.</li>
              <li>В блоке секретного ключа нажмите <strong>«Показать»</strong> или <strong>«Копир.»</strong> — запомните/скопируйте 16-символьный ключ вида <code>XXXX-XXXX-XXXX-XXXX</code> (удобно переслать себе в заметку или набрать вручную на основном телефоне).</li>
              <li>Убедитесь, что шлюз запущен — в шторке уведомлений появится постоянный значок службы Server.</li>
            </ol>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">06</span>
            <h3>Настройте BTRouterClient (на основном телефоне) и проверьте связь</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Откройте <strong>BTRouterClient</strong> и нажмите на шестерёнку <strong>⚙ Настройки</strong> (при желании там же переключите язык RU / EN).</li>
              <li>В пункте <strong>«Устройство Server»</strong> выберите телефон с SIM-картой.</li>
              <li>В поле <strong>«Ключ сопряжения»</strong> введите тот же 16-символьный ключ с экрана Server и нажмите <strong>«Сохранить настройки шлюза»</strong>.</li>
              <li>Вернитесь на главный экран Client — через пару секунд статус должен загореться зелёным <code>● В СЕТИ</code> с уровнем батареи, оператором и сигналом.</li>
              <li>Отправьте тестовое СМС через Client. <em>Если СМС долго висит в статусе «Отправка…» или приходят дубли — посмотрите решение про VoLTE в разделе <a href="#troubleshooting" class="guide-inline-link">06 / Помощь</a>.</em></li>
            </ol>
          </div>
        </div>
      </div>
    `,
    en: `
      <div class="guide-stages">
        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">01</span>
            <h3>Check which app goes on each smartphone</h3>
          </div>
          <div class="guide-stage-body">
            <p>BTRouter uses two companion apps. Make sure each phone gets the right one:</p>
            <div class="guide-roles-grid">
              <div class="guide-role-card guide-role-server">
                <span class="guide-role-label">Secondary phone holding the SIM card</span>
                <strong>BT Router Server</strong>
                <p>Receives SMS and incoming calls over the cellular network and forwards them privately to your main phone.</p>
              </div>
              <div class="guide-role-card guide-role-client">
                <span class="guide-role-label">Primary everyday smartphone</span>
                <strong>BTRouterClient</strong>
                <p>Shows SMS chats, auto-copies bank verification codes, and displays call alerts and missed callers.</p>
              </div>
            </div>
            <p>Both installers are linked in section <a href="#download" class="guide-inline-link">05 / Download v1.1.0</a> below.</p>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">02</span>
            <h3>Download &amp; install both APKs (if Google Play Protect blocks the install)</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Open this page on each phone and tap <strong>Download</strong> for that device’s app (<code>BTRouterServer</code> on the SIM phone, <code>BTRouterClient</code> on your main phone).</li>
              <li>When Android prompts for browser install permission, open <code>Settings</code> and switch on <strong>“Allow from this source”</strong>.</li>
              <li>If an older test build (<code>1.0.xx</code>) was installed previously, uninstall it first so signatures do not conflict.</li>
            </ol>
            <div class="guide-block-grid">
              <div>
                <p><strong>If Google Play Protect shows “App blocked to protect your device” (with no “Install anyway” option):</strong></p>
                <p>Android flags new browser-downloaded APKs that request SMS access (<code>BTRouterServer</code>). There is no malware — the full source code is public on GitHub.</p>
                <ol>
                  <li>Open <strong>Google Play</strong> → tap your profile icon (top right) → <strong>Play Protect</strong> → gear icon ⚙ (top right).</li>
                  <li>Temporarily turn off <strong>“Scan apps with Play Protect”</strong>. <em>(On Samsung phones also turn off <code>Settings → Security and privacy → Auto Blocker</code> during install.)</em></li>
                  <li>Open the downloaded <code>BTRouterServer-v1.1.0.apk</code> again and complete installation.</li>
                  <li>Right after installing, <strong>turn Play Protect back on</strong>.</li>
                  <li>If Android 13+ locks SMS/Phone permissions afterwards, open <code>Settings → Apps → BT Router Server → ⋮ (top right) → Allow restricted settings</code>.</li>
                </ol>
              </div>
              <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/play-protect-block.jpg" data-lightbox-alt="Google Play Protect warning when installing BTRouterServer" data-lightbox-caption="Google Play Protect dialog when installing BTRouterServer from a browser (click to enlarge)">
                <img src="./assets/guide/play-protect-block.jpg" alt="Google Play Protect warning when installing BTRouterServer" loading="lazy" />
                <span class="guide-shot-cap">Google Play Protect dialog (click to enlarge)</span>
              </button>
            </div>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">03</span>
            <h3>Pair both smartphones in Android Bluetooth settings</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Open system <strong>Settings → Bluetooth</strong> on both phones.</li>
              <li>Find the other smartphone in available devices and pair them just like wireless headphones (confirm the pairing code on both screens).</li>
              <li>They only need to appear in <strong>Paired / Saved devices</strong> — you do not need to force an active Bluetooth connection in system settings; the apps connect automatically.</li>
            </ol>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">04</span>
            <h3>Check App Permissions and Battery on BOTH phones (very important)</h3>
          </div>
          <div class="guide-stage-body">
            <p>To stop Android from killing background delivery, open <code>Settings → Apps</code> for each app and make sure <strong>Not allowed</strong> says <strong>“No permissions denied”</strong>, switch <strong>“Manage app if unused” OFF</strong>, and set <strong>Battery → Unrestricted</strong>.</p>

            <div class="guide-device-panel">
              <h4>1. On the SIM phone — BT Router Server</h4>
              <ul>
                <li><strong>Allowed permissions (all 6):</strong> <code>Call logs</code>, <code>Contacts and accounts</code>, <code>Nearby devices</code>, <code>Notifications</code>, <code>Phone</code>, <code>SMS</code>. Under “Not allowed”, it must read <strong>No permissions denied</strong>.</li>
                <li><strong>Unused app settings:</strong> turn <code>Manage app if unused</code> <strong>OFF</strong>.</li>
                <li><strong>Battery:</strong> choose <code>Unrestricted</code>.</li>
              </ul>
              <div class="guide-shots-grid">
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/server-permissions.jpg" data-lightbox-alt="BT Router Server permissions screen" data-lightbox-caption="BT Router Server: all 6 permissions allowed, No permissions denied, Manage app if unused OFF">
                  <img src="./assets/guide/server-permissions.jpg" alt="BT Router Server permissions screen" loading="lazy" />
                  <span class="guide-shot-cap">BT Router Server permissions: 6 allowed, none denied</span>
                </button>
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/server-battery.jpg" data-lightbox-alt="BT Router Server battery screen" data-lightbox-caption="BT Router Server: Battery set to Unrestricted">
                  <img src="./assets/guide/server-battery.jpg" alt="BT Router Server battery screen" loading="lazy" />
                  <span class="guide-shot-cap">BT Router Server battery: Unrestricted selected</span>
                </button>
              </div>
            </div>

            <div class="guide-device-panel">
              <h4>2. On your primary phone — BTRouterClient</h4>
              <ul>
                <li><strong>Allowed permissions (all 3):</strong> <code>Contacts and accounts</code>, <code>Nearby devices</code>, <code>Notifications</code>. Under “Not allowed”, it must read <strong>No permissions denied</strong>.</li>
                <li><strong>Unused app settings:</strong> turn <code>Manage app if unused</code> <strong>OFF</strong>.</li>
                <li><strong>Battery:</strong> choose <code>Unrestricted</code>.</li>
              </ul>
              <div class="guide-shots-grid">
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/client-permissions.jpg" data-lightbox-alt="BTRouterClient permissions screen" data-lightbox-caption="BTRouterClient: all 3 permissions allowed, No permissions denied, Manage app if unused OFF">
                  <img src="./assets/guide/client-permissions.jpg" alt="BTRouterClient permissions screen" loading="lazy" />
                  <span class="guide-shot-cap">BTRouterClient permissions: 3 allowed, none denied</span>
                </button>
                <button type="button" class="guide-shot-btn" data-lightbox-src="./assets/guide/client-battery.jpg" data-lightbox-alt="BTRouterClient battery screen" data-lightbox-caption="BTRouterClient: Battery set to Unrestricted">
                  <img src="./assets/guide/client-battery.jpg" alt="BTRouterClient battery screen" loading="lazy" />
                  <span class="guide-shot-cap">BTRouterClient battery: Unrestricted selected</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">05</span>
            <h3>Configure BT Router Server (on the SIM phone)</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Open <strong>BT Router Server</strong> (you can switch between <strong>RU</strong> and <strong>EN</strong> at the top).</li>
              <li>Tap <strong>Select Client Device</strong> and pick your primary smartphone from the paired Bluetooth list.</li>
              <li>Tap <strong>Show</strong> or <strong>Copy</strong> next to the 16-character Pairing Key (<code>XXXX-XXXX-XXXX-XXXX</code>) so you can enter it on your main phone.</li>
              <li>Confirm the persistent Server gateway notification appears in the Android status shade.</li>
            </ol>
          </div>
        </div>

        <div class="guide-stage">
          <div class="guide-stage-head">
            <span class="guide-stage-num">06</span>
            <h3>Configure BTRouterClient (on your primary phone) &amp; run a quick test</h3>
          </div>
          <div class="guide-stage-body">
            <ol>
              <li>Open <strong>BTRouterClient</strong> and tap the gear button <strong>⚙ Settings</strong> (you can also switch RU / EN there).</li>
              <li>Under <strong>Server device</strong>, pick your secondary SIM phone.</li>
              <li>Paste or type the same 16-character pairing key from Server and tap <strong>Save Gateway Settings</strong>.</li>
              <li>Return to the Client home screen — within a few seconds the badge turns green <code>● ONLINE</code> showing battery, carrier, and cellular signal.</li>
              <li>Send a quick test SMS through Client. <em>If texts stay on “Sending…” or duplicate on VoLTE, check the VoLTE fix in <a href="#troubleshooting" class="guide-inline-link">06 / Troubleshooting</a>.</em></li>
            </ol>
          </div>
        </div>
      </div>
    `,
  };

  const scenarioData = {
    ru: [
      {
        badge: 'Сценарий 01 · Поездка',
        signal: '● Bluetooth активен',
        title: 'Банковский код без перестановки SIM',
        desc: 'Вы оплачиваете покупку в поездке — запасной телефон в рюкзаке ловит СМС от домашнего банка и через секунду показывает готовый код для вставки на основном экране.',
        rows: [
          { label: 'Отправитель', value: 'NovaBank' },
          { label: 'Сообщение', value: 'Подтверждение оплаты 1 490 RUB' },
        ],
        highlightCode: '649 208',
        highlightChip: 'Скопировано в буфер',
      },
      {
        badge: 'Сценарий 02 · Встреча или работа',
        signal: '● Тихий контроль',
        title: 'Рабочий номер под рукой, без лишнего шума',
        desc: 'Во время встречи звонит незнакомый номер на рабочий смартфон — отклоняйте вызов одной кнопкой с основного экрана или ответьте по СМС, не доставая второй телефон.',
        rows: [
          { label: 'Входящий вызов', value: '+7 (916) 555-01-42' },
          { label: 'Действие', value: 'Удалённый сброс и запись в пропущенные' },
        ],
        highlightCode: 'СБРОШЕНО',
        highlightChip: 'В списке пропущенных: +1',
      },
      {
        badge: 'Сценарий 03 · Samsung One UI',
        signal: '● Быстрый переключатель',
        title: 'Интернет со второго телефона в одно касание',
        desc: 'Нужен Wi-Fi на ноутбуке или основном телефоне? Нажмите круглую кнопку «Точка доступа» — второй смартфон Samsung включит модем и начнёт раздавать сеть из сумки.',
        rows: [
          { label: 'Мобильные данные', value: 'Включены (LTE 4/4)' },
          { label: 'Точка доступа Wi-Fi', value: 'Активна по команде с Client' },
        ],
        highlightCode: 'WI-FI ON',
        highlightChip: 'Батарея SIM-шлюза: 84%',
      },
    ],
    en: [
      {
        badge: 'Scenario 01 · Traveling',
        signal: '● Bluetooth Active',
        title: 'Bank codes without swapping SIM cards',
        desc: 'Paying online while abroad? Your secondary phone inside your backpack receives the SMS from your home bank and puts the ready-to-paste code straight on your main screen.',
        rows: [
          { label: 'Sender', value: 'International Bank' },
          { label: 'Message', value: 'Purchase approval $124.50' },
        ],
        highlightCode: '839 104',
        highlightChip: 'Copied to clipboard',
      },
      {
        badge: 'Scenario 02 · At Work',
        signal: '● Quiet Control',
        title: 'Handle your work SIM without touching a second phone',
        desc: 'An unknown caller rings your work phone during a meeting — dismiss the call in one tap from your daily phone or reply by SMS without pulling out the second device.',
        rows: [
          { label: 'Incoming call', value: '+1 (415) 555-0192' },
          { label: 'Action', value: 'Remote decline & log in missed calls' },
        ],
        highlightCode: 'DECLINED',
        highlightChip: 'Missed badge updated',
      },
      {
        badge: 'Scenario 03 · Samsung One UI',
        signal: '● Quick Toggle',
        title: 'Share hotspot from your bag in a single tap',
        desc: 'Need mobile Wi-Fi for your laptop? Tap the round Hotspot tile on your main screen and your Samsung SIM phone turns on tethering without leaving your backpack.',
        rows: [
          { label: 'Mobile Data', value: 'Enabled (LTE 4/4)' },
          { label: 'Wi-Fi Hotspot', value: 'Active via Client toggle' },
        ],
        highlightCode: 'WI-FI ON',
        highlightChip: 'SIM Phone battery: 84%',
      },
    ],
  };

  const contactBookSamples = {
    ru: {
      client: [
        { name: 'Алексей (Работа)', phone: '+7 903 •••-12-08' },
        { name: 'Мария', phone: '+7 916 •••-44-91' },
      ],
      server: [
        { name: 'Курьер Доставка', phone: '+7 926 •••-77-15' },
        { name: 'Отель Стамбул', phone: '+90 532 •••-84-20' },
      ],
    },
    en: {
      client: [
        { name: 'Alex (Work)', phone: '+1 415 •••-1208' },
        { name: 'Maria', phone: '+1 650 •••-4491' },
      ],
      server: [
        { name: 'Delivery Courier', phone: '+44 7700 •••-715' },
        { name: 'Istanbul Hotel', phone: '+90 532 •••-8420' },
      ],
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
    ru: 'CARD-4412 19.11.26 Оплата 1 490.00 RUB. Код подтверждения: 649208. Никому не говорите код.',
    en: 'Your verification code is 839104 for card *9082 ($124.50 on 11/22/2026). Do not share it.',
    tr: '482910 tek kullanimlik sifreniz ile *7731 kartinizdan 850.00 TL harcama onaylayiniz. B001',
  };

  let currentLang = 'ru';
  let currentScenario = 0;
  let currentBook = 'client';
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
  const btnFeatureDataToggle = document.getElementById('btnFeatureDataToggle');
  const featureDataSwitch = document.getElementById('featureDataSwitch');
  const btnFeatureHotspotToggle = document.getElementById('btnFeatureHotspotToggle');
  const featureHotspotSwitch = document.getElementById('featureHotspotSwitch');
  const btnFeatureCallDecline = document.getElementById('btnFeatureCallDecline');

  function syncToggleUI() {
    tileMobileData?.classList.toggle('active', mobileDataActive);
    tileMobileData?.setAttribute('aria-pressed', String(mobileDataActive));
    featureDataSwitch?.classList.toggle('on', mobileDataActive);
    btnFeatureDataToggle?.setAttribute('aria-pressed', String(mobileDataActive));

    tileHotspot?.classList.toggle('active', hotspotActive);
    tileHotspot?.setAttribute('aria-pressed', String(hotspotActive));
    featureHotspotSwitch?.classList.toggle('on', hotspotActive);
    btnFeatureHotspotToggle?.setAttribute('aria-pressed', String(hotspotActive));

    if (cliDataState) {
      cliDataState.textContent = mobileDataActive ? 'LTE' : 'OFF';
    }
  }

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
        <div class="chat-avatar avatar-emerald">M</div>
        <div class="chat-body">
          <div class="chat-head">
            <span class="chat-sender">MyBank</span>
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

  // Interactive Call Mini-Pill inside Bento Card 3
  let callPillTimer = null;
  btnFeatureCallDecline?.addEventListener('click', () => {
    btnFeatureCallDecline.classList.add('is-declined');
    btnFeatureCallDecline.textContent = translations[currentLang].call_mini_declined;
    if (callPillTimer) clearTimeout(callPillTimer);
    callPillTimer = setTimeout(() => {
      btnFeatureCallDecline.classList.remove('is-declined');
      btnFeatureCallDecline.textContent = translations[currentLang].call_mini_decline;
    }, 1800);
  });

  function toggleHotspot() {
    hotspotActive = !hotspotActive;
    syncToggleUI();
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
    syncToggleUI();
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
    showClientToast(
      mobileDataActive
        ? (currentLang === 'ru' ? 'Интернет включён' : 'Mobile Data On')
        : (currentLang === 'ru' ? 'Интернет выключен' : 'Mobile Data Off')
    );
  }

  document.getElementById('btnSimHotspot')?.addEventListener('click', toggleHotspot);
  tileHotspot?.addEventListener('click', toggleHotspot);
  btnFeatureHotspotToggle?.addEventListener('click', toggleHotspot);
  tileMobileData?.addEventListener('click', toggleMobileData);
  btnFeatureDataToggle?.addEventListener('click', toggleMobileData);

  // PSK Show/Hide toggle
  btnTogglePsk?.addEventListener('click', () => {
    pskVisible = !pskVisible;
    if (srvPskValue) {
      srvPskValue.textContent = pskVisible ? 'A7K9-M4X2-P8Q3-R5N6' : '••••-••••-••••-••••';
    }
    btnTogglePsk.textContent = pskVisible
      ? translations[currentLang].srv_btn_hide
      : translations[currentLang].srv_btn_show;
  });

  // Interactive Scenario Switcher (#use-cases)
  const scenarioBadge = document.getElementById('scenarioBadge');
  const scenarioSignalPill = document.getElementById('scenarioSignalPill');
  const scenarioStageTitle = document.getElementById('scenarioStageTitle');
  const scenarioStageDesc = document.getElementById('scenarioStageDesc');
  const scenarioVisualCard = document.getElementById('scenarioVisualCard');

  function renderScenario(idx) {
    currentScenario = idx;
    const item = scenarioData[currentLang][idx];
    if (!item) return;
    if (scenarioBadge) scenarioBadge.textContent = item.badge;
    if (scenarioSignalPill) scenarioSignalPill.textContent = item.signal;
    if (scenarioStageTitle) scenarioStageTitle.textContent = item.title;
    if (scenarioStageDesc) scenarioStageDesc.textContent = item.desc;
    if (scenarioVisualCard) {
      scenarioVisualCard.innerHTML = `
        ${item.rows
          .map(
            (r) => `
          <div class="sv-row">
            <span class="sv-label">${r.label}</span>
            <span>${r.value}</span>
          </div>`
          )
          .join('')}
        <div class="sv-highlight">
          <code class="sv-code">${item.highlightCode}</code>
          <span class="sv-chip">${item.highlightChip}</span>
        </div>
      `;
    }
    document.querySelectorAll('.scenario-tab').forEach((tab, i) => {
      const active = i === idx;
      tab.classList.toggle('active', active);
      tab.setAttribute('aria-selected', String(active));
    });
  }

  document.querySelectorAll('.scenario-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      renderScenario(Number(tab.getAttribute('data-scenario') || 0));
    });
  });

  // Interactive Contacts Switcher Micro-Widget
  const contactsMiniList = document.getElementById('contactsMiniList');
  function renderContactsMini(book) {
    currentBook = book === 'server' ? 'server' : 'client';
    const rows = contactBookSamples[currentLang][currentBook];
    if (contactsMiniList && rows) {
      contactsMiniList.innerHTML = rows
        .map((c) => `<div class="contacts-mini-row"><span>${c.name}</span><code>${c.phone}</code></div>`)
        .join('');
    }
    document.querySelectorAll('.contacts-seg-btn').forEach((btn) => {
      btn.classList.toggle('active', btn.getAttribute('data-book') === currentBook);
    });
  }

  document.querySelectorAll('.contacts-seg-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      renderContactsMini(btn.getAttribute('data-book') || 'client');
    });
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

  document.querySelectorAll('.nav-tech-link').forEach((link) => {
    link.addEventListener('click', () => {
      setTechDrawerOpen(true);
    });
  });

  if (window.location.hash === '#under-the-hood') {
    setTechDrawerOpen(true);
  }

  // Protocol Explorer Tabs
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

  // Security Frame Segment Switcher
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

  // Detailed Setup Guide Accordion
  const fullGuideBtn = document.getElementById('fullGuideAccordionBtn');
  const fullGuideBody = document.getElementById('fullGuideAccordionBody');
  const fullGuideCard = fullGuideBtn?.closest('.guide-accordion');
  fullGuideBtn?.addEventListener('click', () => {
    const expanded = fullGuideBtn.getAttribute('aria-expanded') === 'true';
    fullGuideBtn.setAttribute('aria-expanded', String(!expanded));
    if (fullGuideBody) fullGuideBody.hidden = expanded;
    fullGuideCard?.classList.toggle('is-open', !expanded);
  });

  // Samsung Accordion
  const accBtn = document.getElementById('samsungAccordionBtn');
  const accBody = document.getElementById('samsungAccordionBody');
  const accCard = accBtn?.closest('.samsung-accordion');
  accBtn?.addEventListener('click', () => {
    const expanded = accBtn.getAttribute('aria-expanded') === 'true';
    accBtn.setAttribute('aria-expanded', String(!expanded));
    if (accBody) accBody.hidden = expanded;
    accCard?.classList.toggle('is-open', !expanded);
  });

  // Screenshot Lightbox Modal
  const imageLightbox = document.getElementById('imageLightbox');
  const lightboxImg = document.getElementById('lightboxImg');
  const lightboxCaption = document.getElementById('lightboxCaption');
  const lightboxCloseBtn = document.getElementById('lightboxCloseBtn');
  let lastLightboxTrigger = null;

  function closeLightbox() {
    if (!imageLightbox || imageLightbox.hidden) return;
    imageLightbox.hidden = true;
    document.body.style.overflow = '';
    if (lightboxImg) {
      lightboxImg.src = '';
      lightboxImg.alt = '';
    }
    if (lastLightboxTrigger && typeof lastLightboxTrigger.focus === 'function') {
      lastLightboxTrigger.focus();
    }
    lastLightboxTrigger = null;
  }

  document.addEventListener('click', (e) => {
    const trigger = e.target instanceof Element ? e.target.closest('.guide-shot-btn') : null;
    if (!trigger || !imageLightbox || !lightboxImg) return;
    const src = trigger.getAttribute('data-lightbox-src');
    if (!src) return;
    lastLightboxTrigger = trigger;
    lightboxImg.src = src;
    lightboxImg.alt = trigger.getAttribute('data-lightbox-alt') || '';
    if (lightboxCaption) {
      lightboxCaption.textContent = trigger.getAttribute('data-lightbox-caption') || '';
    }
    imageLightbox.hidden = false;
    document.body.style.overflow = 'hidden';
    lightboxCloseBtn?.focus();
  });

  lightboxCloseBtn?.addEventListener('click', closeLightbox);
  imageLightbox?.addEventListener('click', (e) => {
    if (e.target === imageLightbox) closeLightbox();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && imageLightbox && !imageLightbox.hidden) {
      closeLightbox();
    }
  });

  // Troubleshooting Accordions
  document.querySelectorAll('.issue-trigger').forEach((btn) => {
    btn.addEventListener('click', () => {
      const expanded = btn.getAttribute('aria-expanded') === 'true';
      const bodyId = btn.getAttribute('aria-controls');
      const body = bodyId ? document.getElementById(bodyId) : null;
      const card = btn.closest('.issue-card');
      btn.setAttribute('aria-expanded', String(!expanded));
      if (body) body.hidden = expanded;
      card?.classList.toggle('is-open', !expanded);
    });
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

    renderScenario(currentScenario);
    renderContactsMini(currentBook);
    renderProtocolStep(currentProtoStep);
    renderFrameSegment(currentFrameSeg);
    updateOtpSandbox();

    if (otpIgnoredHint) {
      otpIgnoredHint.textContent = dict.otp_ignored_hint;
    }

    const issueBodies = troubleshootingBodies[currentLang];
    if (issueBodies) {
      Object.entries(issueBodies).forEach(([id, html]) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = html;
      });
    }

    if (fullGuideBody && detailedGuideBodies[currentLang]) {
      fullGuideBody.innerHTML = detailedGuideBodies[currentLang];
    }
  }

  document.querySelectorAll('.lang-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      setLanguage(btn.getAttribute('data-lang') || 'ru');
    });
  });

  // Subtle Scroll Reveal
  const revealTargets = document.querySelectorAll(
    '.bento-card, .scenario-stage, .setup-step, .dl-card, .issue-card'
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
      { threshold: 0.1 }
    );
    revealTargets.forEach((el) => {
      el.classList.add('reveal');
      observer.observe(el);
    });
  }

  // Smooth scroll for in-page navigation without polluting the URL bar
  document.addEventListener('click', (e) => {
    const anchor = e.target instanceof Element ? e.target.closest('a[href^="#"]') : null;
    if (!anchor) return;
    const href = anchor.getAttribute('href');
    if (!href || href === '#') return;
    e.preventDefault();

    if (href === '#under-the-hood') {
      setTechDrawerOpen(true);
    }

    if (href === '#top') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      const target = document.querySelector(href);
      if (target) {
        requestAnimationFrame(() => {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
      }
    }

    if (window.location.hash) {
      history.replaceState(null, '', window.location.pathname + window.location.search);
    }
  });

  // Initialize dynamic views
  syncToggleUI();
  setLanguage('ru');
})();
