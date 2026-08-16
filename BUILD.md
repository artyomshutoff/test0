# Сборка и установка

## В Android Studio (рекомендуется)
1. Установите актуальный Android Studio и Android SDK Platform 36.
2. Откройте папку `cytisine-app` через **File → Open**.
3. Если Android Studio предложит выбрать Gradle/JDK, используйте JDK 17 и Gradle 8.13. Проект использует Android Gradle Plugin 8.13.2 и Kotlin 2.2.21.
4. Дождитесь **Gradle Sync**. Если IDE сообщает, что Gradle Wrapper отсутствует, откройте встроенный Terminal и выполните `gradle wrapper --gradle-version 8.13` (нужен установленный Gradle 8.13) либо выберите локальную Gradle 8.13 в Settings → Build Tools → Gradle.
5. Подключите телефон с включённой **Отладкой по USB** и нажмите Run ▶, либо выберите **Build → Build APK(s)**.
6. APK после debug-сборки находится в `app/build/outputs/apk/debug/app-debug.apk`.

## Установка APK через ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## На телефоне
При первом запуске разрешите уведомления. На Android 12+ нажмите в приложении «Разрешить точные напоминания», если хотите максимально точные уведомления. Затем выберите дату начала курса и время первого приёма и нажмите «Сохранить и включить напоминания».

## Подпись release APK
В Android Studio: **Build → Generate Signed App Bundle or APK → APK**. Создайте/выберите keystore, выберите `release` и завершите мастер. Не публикуйте keystore и его пароль.

## Автоматическая сборка APK через GitHub Actions

В проект добавлен workflow `.github/workflows/build-apk.yml`.

1. Создайте новый репозиторий на GitHub и загрузите в него содержимое этой папки (важно: `build.gradle.kts` должен лежать в корне репозитория).
2. Откройте вкладку **Actions** → **Build Android APK**.
3. Нажмите **Run workflow** → **Run workflow**. Workflow также запускается автоматически при push в ветки `main` или `master`.
4. После успешной сборки откройте завершённый запуск. Внизу страницы, в разделе **Artifacts**, скачайте **Cytisine-APK**.
5. В скачанном ZIP будет файл `Cytisine.apk`. Перенесите его на Android-телефон и откройте для установки.

Это debug APK, подписанный стандартным отладочным ключом Android. Для личной установки этого достаточно. Для публикации в Google Play нужна отдельная release-подпись.
