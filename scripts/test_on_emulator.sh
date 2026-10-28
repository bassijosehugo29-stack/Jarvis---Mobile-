#!/usr/bin/env sh
# Keep the real Gradle result while collecting evidence even after a failed test.
set -u
jarvis_emulator_flag=$(adb shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')
if [ "$jarvis_emulator_flag" != "1" ]; then
    printf '%s\n' 'Testes destrutivos de fixtures: execute somente em emulador descartável. Nenhum aplicativo foi instalado ou removido.' >&2
    exit 2
fi
./gradlew --no-daemon --max-workers=2 :voice-engine:assembleDebug || exit $?
adb install -r voice-engine/build/outputs/apk/debug/voice-engine-debug.apk || exit $?
adb shell settings put global animator_duration_scale 1
adb shell am force-stop com.google.android.apps.nexuslauncher || true
./gradlew --no-daemon --max-workers=2 :app:connectedDebugAndroidTest
jarvis_test_status=$?
mkdir -p app/build/reports/screenshots app/build/reports/conversation
adb pull /sdcard/Pictures/JARVIS/. app/build/reports/screenshots/ || true
adb pull /sdcard/Download/JARVIS-testes/. app/build/reports/conversation/ || true
adb logcat -d -s AndroidRuntime:E > app/build/reports/conversation/runtime-errors.txt || true
adb logcat -d -s JarvisVoiceA:D sherpa-onnx:V libc:F TextToSpeechService:W TextToSpeech:W > app/build/reports/conversation/voice-engine-diagnostic.txt || true
# CI contains only public fixture phrases. Capture native backtraces as well as Java failures.
adb logcat -b crash -d > app/build/reports/conversation/native-crashes.txt || true
if [ "$jarvis_test_status" -ne 0 ]; then
    exit "$jarvis_test_status"
fi
set -e
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put global animator_duration_scale 1
adb shell am start -W -n br.com.jarvis.mobile/.MainActivity
adb shell am force-stop com.google.android.apps.nexuslauncher
adb shell screenrecord --size 720x1600 --bit-rate 2500000 --time-limit 6 /sdcard/jarvis-hud.mp4
adb pull /sdcard/jarvis-hud.mp4 app/build/reports/screenshots/jarvis-hud.mp4
adb shell am start -W -n br.com.jarvis.mobile/.diagnostics.CoreShowcaseActivity --ez sequence true
adb shell screenrecord --size 720x1600 --bit-rate 2500000 --time-limit 16 /sdcard/jarvis-v4-estados.mp4
adb pull /sdcard/jarvis-v4-estados.mp4 app/build/reports/screenshots/jarvis-v4-estados.mp4
python3 scripts/measure_v4.py
