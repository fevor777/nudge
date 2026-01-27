# Установка Android эмулятора на Ubuntu

## Требования

| Компонент | Минимум | Рекомендуется |
|-----------|---------|---------------|
| RAM | 8 GB | 16 GB |
| Диск | 15 GB | 30 GB |
| CPU | С поддержкой VT-x/AMD-V | — |

---

## 1. Проверка и включение KVM

```bash
# Установить утилиты
sudo apt update
sudo apt install cpu-checker qemu-kvm

# Проверить поддержку виртуализации
kvm-ok

# Добавить пользователя в группу kvm
sudo usermod -aG kvm $USER
sudo chown $USER /dev/kvm

# Перелогиниться для применения изменений
logout
```

---

## 2. Установка Java

```bash
sudo apt install openjdk-17-jdk
```

---

## 3. Установка Android SDK

```bash
# Создать папку для SDK
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk

# Скачать command-line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip

# Переместить в правильную структуру
mkdir -p cmdline-tools/latest
mv cmdline-tools/bin cmdline-tools/latest/
mv cmdline-tools/lib cmdline-tools/latest/
mv cmdline-tools/NOTICE.txt cmdline-tools/latest/ 2>/dev/null
mv cmdline-tools/source.properties cmdline-tools/latest/ 2>/dev/null

# Удалить архив
rm commandlinetools-linux-*.zip
```

---

## 4. Настройка переменных окружения

Добавить в `~/.bashrc`:

```bash
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

---

## 5. Установка компонентов SDK

```bash
# Принять лицензии
yes | sdkmanager --licenses

# Установить основные компоненты
sdkmanager "platform-tools" "emulator"
sdkmanager "platforms;android-34"

# Установить образ системы (с Play Store для Outlook!)
sdkmanager "system-images;android-34;google_apis_playstore;x86_64"
```

---

## 6. Создание виртуального устройства

### Телефон (Pixel 6)

```bash
avdmanager create avd \
  -n Pixel6 \
  -k "system-images;android-34;google_apis_playstore;x86_64" \
  -d "pixel_6"
```

### Планшет (Pixel Tablet — 10.95")

```bash
avdmanager create avd \
  -n PixelTablet \
  -k "system-images;android-34;google_apis_playstore;x86_64" \
  -d "pixel_tablet"
```

### Большой экран (13.5")

```bash
avdmanager create avd \
  -n LargeScreen \
  -k "system-images;android-34;google_apis_playstore;x86_64" \
  -d "13.5in Freeform"
```

### Nexus 10 (10.1")

```bash
avdmanager create avd \
  -n Nexus10 \
  -k "system-images;android-34;google_apis_playstore;x86_64" \
  -d "Nexus 10"
```

### Проверить созданные устройства

```bash
avdmanager list avd
```

---

## 7. Доступные устройства с большим экраном

| Устройство | Экран | ID |
|------------|-------|-----|
| Pixel Tablet | 10.95" | `pixel_tablet` |
| Pixel C | 10.2" | `pixel_c` |
| Nexus 10 | 10.1" | `Nexus 10` |
| Nexus 9 | 8.9" | `Nexus 9` |
| 10.1" WXGA Tablet | 10.1" | `10.1in WXGA (Tablet)` |
| 13.5" Freeform | 13.5" | `13.5in Freeform` |

Посмотреть все устройства:

```bash
avdmanager list device
```

---

## 8. Запуск эмулятора

```bash
# Обычный запуск
emulator -avd PixelTablet

# С аппаратным ускорением графики (быстрее)
emulator -avd PixelTablet -gpu host

# В фоне
emulator -avd PixelTablet &

# Без звука (если проблемы с аудио)
emulator -avd PixelTablet -no-audio
```

---

## 9. Установка MS Outlook

### Вариант 1: Через Play Store (рекомендуется)

1. Откройте Play Store в эмуляторе
2. Войдите в Google аккаунт
3. Найдите "Microsoft Outlook"
4. Установите

### Вариант 2: Через ADB

```bash
# Если есть APK файл
adb install outlook.apk

# Запустить Outlook
adb shell am start -n com.microsoft.office.outlook/.MainActivity
```

---

## 10. Полезные команды

```bash
# Список подключённых устройств
adb devices

# Сделать скриншот
adb exec-out screencap -p > screenshot.png

# Записать видео экрана
adb shell screenrecord /sdcard/video.mp4
adb pull /sdcard/video.mp4

# Установить APK
adb install app.apk

# Скопировать файл на устройство
adb push local_file.txt /sdcard/

# Скопировать файл с устройства
adb pull /sdcard/remote_file.txt ./

# Выключить эмулятор
adb emu kill

# Список установленных приложений
adb shell pm list packages

# Удалить приложение
adb uninstall com.example.app
```

---

## 11. Свой размер экрана

```bash
# Создать устройство
avdmanager create avd \
  -n CustomLarge \
  -k "system-images;android-34;google_apis_playstore;x86_64"
```

Отредактировать `~/.android/avd/CustomLarge.avd/config.ini`:

```ini
hw.lcd.width=1920
hw.lcd.height=1200
hw.lcd.density=240
```

---

## 12. Устранение проблем

### Эмулятор не запускается

```bash
# Проверить KVM
ls -la /dev/kvm
sudo chown $USER /dev/kvm
```

### Медленная работа

```bash
# Использовать GPU хоста
emulator -avd PixelTablet -gpu host

# Выделить больше RAM в config.ini
hw.ramSize=4096
```

### Нет звука

```bash
emulator -avd PixelTablet -no-audio
```

### Чёрный экран

```bash
# Попробовать software rendering
emulator -avd PixelTablet -gpu swiftshader_indirect
```

---

## Быстрый старт (одной командой)

```bash
# После установки всех компонентов
emulator -avd PixelTablet -gpu host
```
