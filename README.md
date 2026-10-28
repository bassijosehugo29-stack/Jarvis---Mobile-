# JARVIS Mobile — Missão 1 (0.6.0)

Assistente Android nativo em Kotlin/Jetpack Compose. HUD preto, azul-marinho, ciano e dourado; núcleo holográfico interativo, voz configurável e áudio original. Projeto existente e comandos preservados. Destinado inicialmente ao Samsung Galaxy S25 FE; Android 8+ ARM64, com x86_64 incluído para emulador. compileSdk/targetSdk 35.

## APK confirmado

- [Baixar APK de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231/artifacts/12787512824) — extraia o ZIP e instale app-debug.apk.
- [Compilação e 77 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231): 55 de unidade + 22 instrumentados Android 35; Lint com 0 erros e 19 avisos não impeditivos.
- [Relatórios, capturas, vídeo e conversa de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231/artifacts/12787209902). Os artefatos do CI duram sete dias.
- Código validado: `dc00bef9bf5595aed7dac1e41dac2c0ea71e9ae3`. Documentação posterior não altera esse APK.
- APK: 18333609 bytes; SHA-256 `175247796161809bc1632c7c7c96346f62931c36390d74241a2c67a2b15b1c28`.

A assinatura debug pode mudar entre runners. Se houver conflito, desinstale a versão anterior antes de instalar. Isso remove preferências e o modelo de conversa baixado, sem apagar eventos já salvos no calendário. [Instalar pelo celular](docs/APK-PELO-CELULAR.md).

## Três modos com escolha salva

- **Silencioso:** sem voz, música ou efeitos do JARVIS; respostas em texto, núcleo estático em repouso e uma escuta por toque. Escrever dispensa microfone. O reconhecedor Android pode emitir avisos próprios; usar texto evita chamar esse serviço.
- **Assistente (padrão):** voz, efeitos discretos e animação moderada; música opcional, desligada inicialmente.
- **Cinematográfico:** partículas, formação e movimentos completos do núcleo, ambiente eletrônico original e efeitos de processamento. A música reduz durante a fala e para durante captura.

O seletor fica na tela principal. Trocar de modo interrompe a sessão em curso e não inicia microfone. DataStore salva modo, chaves, volumes e perfil vocal. Nos ajustes, voz/música/efeitos têm controles independentes; música possui preferência própria para Assistente e Cinema. Ao sair, bloquear ou abrir outro app, áudio e captura param. Não há escuta oculta.

O núcleo mostra inicialização, espera, escuta pelo RMS real, processamento, fala pelos callbacks TTS e erro temporário com explicação. Cadências de desenho são limitadas por modo/estado; não houve medição de bateria/FPS no Galaxy. [Especificação, áudio e limites](docs/MISSAO-1.md).

## Primeiro uso

1. Escolha Assistente. Abra ajustes, use **Listar vozes instaladas** e **Testar voz**. Ajuste o volume de mídia; confira saída em alto-falante/fones/Bluetooth.
2. Escolha mecanismo/voz portuguesa disponível e ajuste velocidade/tom. Perfil inicial calmo: 0,90× e 0,88×. Android não informa gênero de forma padronizada, por isso não se garante voz masculina ou qualidade de estúdio; não há clonagem. **Ajustar voz do Android** permite instalar/configurar dados de idioma.
3. Toque no núcleo, permita microfone durante o uso e ouça **“Às suas ordens, senhor”**. Em Assistente/Cinema, a sessão continua enquanto visível; Jarvis é opcional nos próximos pedidos. No Silencioso, toque a cada solicitação.
4. Para conversa livre, use **Baixar conversa offline • 491 MB** por Wi-Fi e aguarde **Conversa offline pronta**; ou importe o modelo oficial já baixado. Pode conversar por texto sem microfone. Os comandos funcionam sem esse download.

Qwen2.5 0.5B Instruct Q4_K_M (Apache-2.0) com llama.cpp (MIT) gera respostas no celular, por CPU. Modelo de 491400032 bytes, tamanho/SHA-256 conferidos, separado do APK. Leve, com contexto curto e saída limitada: pode ser simples, lento ou errado; não equivale ao ChatGPT e não busca informações atuais. Falas geradas não executam ações no telefone. [Detalhes](docs/CONVERSA-0.5.md).

## Comandos preservados

- “Jarvis, abrir ChatGPT”: abre somente com.openai.chatgpt, quando instalado.
- “Abrir WhatsApp” / “entrar no Instagram”: abre aplicativo instalado pelo nome, perguntando em caso de ambiguidade.
- “Alarme para sete e trinta”: solicita ao relógio oficial via intent; ele pode exigir confirmação e não retorna comprovante de salvamento.
- “Agende dentista amanhã às quinze horas”: funcionalidade anterior preservada, com permissão de calendário e confirmação após gravação/leitura pelo Calendar Provider. Sem novos módulos de agenda nesta missão.
- “Parar”: encerra escuta; toque no núcleo também interrompe fala/processamento.

[Formatos de comandos](docs/COMANDOS-0.4.md). Não controla telas internas de qualquer app, mensagens/compras, notebook ou serviços que não ofereçam integração oficial.

## Som, privacidade e custos

OriginalScore.kt sintetiza trilha de 16 s e cinco efeitos originais, sem samples. As novas composições e seu áudio renderizado estão sob CC0; licenças do restante do projeto permanecem próprias. Um foco de áudio coordena TTS/AudioTrack e interrompe ao perder prioridade. Música reduz a 12% de seu volume configurado durante fala e silencia durante escuta; controles não alteram o volume global do Android.

Sem API paga, chaves, assinaturas, backend de inferência, gravação, telemetria, boot, acessibilidade ou serviço de captura em segundo plano. O único tráfego próprio é o download de modelo solicitado. SpeechRecognizer/TTS são provedores Android, com pt-BR preferencial e fallback pt-PT na fala; idiomas/vozes/rede dependem desses provedores. Não se promete operação universal offline do reconhecimento/TTS. O download solicitado pode continuar pelo DownloadManager, com notificação, sem microfone.

Não houve teste físico no Galaxy, nem comprovação de voz humana Samsung, Bluetooth/DND ou autonomia. Nenhuma Play Store, release pública ou cobrança foi ativada. Os runners padrão do repositório público são usados; não habilite cobrança para este projeto. Nenhum módulo novo de e-mail, notícias, agenda ou memória avançada foi iniciado.

## Compilar

Pelo celular: [Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → main**. Aguarde ambos os jobs verdes, baixe JARVIS-Mobile-debug, extraia e instale. Use Site para computador se necessário. [Guia](docs/APK-PELO-CELULAR.md).

No computador: JDK 17, SDK Platform 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1, Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose 2.1.20. O wrapper oficial é obtido/verificado pelos launchers; configure ANDROID_HOME ou sdk.dir em local.properties.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Emulador/dispositivo para teste de conversa real:
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

No Windows use .\gradlew.bat. O CMake busca llama.cpp por commit fixo na compilação; nenhum código nativo é baixado em execução. Pesos GGUF não são versionados. [Validação](docs/VALIDATION.md), [testes](docs/TESTES.md), [arquitetura](docs/ARQUITETURA.md).

Projeto pessoal independente; marcas pertencem a seus titulares.
