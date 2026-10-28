# JARVIS Voz A — separate local TTS engine

This companion application is distributed under GPL-3.0-or-later. It communicates with JARVIS Mobile through Android's standard TextToSpeech binder protocol; it is a separate APK/process. The application does not have INTERNET, RECORD_AUDIO or storage/account permissions and never logs or saves synthesis text.

The original Kotlin TTS service and DSP source are in this directory. The Kotlin JNI binding (Tts.kt) is copyright Xiaomi, Apache-2.0, copied without changes from sherpa-onnx v1.13.8. SHERPA-LICENSE contains that license. Native libraries are extracted from the hash-pinned public sherpa-onnx AAR. Sherpa-onnx source/build files: https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8 .

The runtime includes the eSpeak NG phonemizer (GPL-3.0-or-later); corresponding source is available through the pinned upstream source/build recipe and https://github.com/espeak-ng/espeak-ng . ONNX Runtime is MIT licensed: https://github.com/microsoft/onnxruntime . Any redistribution must provide the corresponding sources required by these licenses; build artifacts include a source bundle/manifest. No change to the licensing of the separate JARVIS Mobile APK is implied.

Approved neural voice: pt_BR-faber-medium, 22,050 Hz, original SHA-256 858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7. Its dataset is declared CC0 in the upstream model card: https://huggingface.co/rhasspy/piper-voices/blob/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/pt/pt_BR/faber/medium/MODEL_CARD . We retain its exact FP32 initializers, adding only loader metadata. No reference voice was cloned and no pitch shift is applied.
