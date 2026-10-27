# Jarvis---Mobile-

**JARVIS Mobile**

Assistente Android nativo em **Kotlin + Jetpack Compose**, com interface preta, azul e dourada. Versão inicial 0.1.0, destinada primeiro ao Samsung Galaxy S25 FE. Android 8+ (API 26); compilação e target API 35.

## O que esta versão faz

- Escuta pt-BR após tocar em **ATIVAR JARVIS**, somente enquanto a Activity estiver visível.
- “Jarvis” → **“Às suas ordens, senhor”**, usando TextToSpeech do Android.
- Após a resposta, aceita comandos por 20 segundos; também aceita a ativação e o comando numa frase.
- “Jarvis, abrir ChatGPT” abre apenas o pacote oficial `com.openai.chatgpt`, se instalado.
- “Jarvis, alarme para 7:30” abre o relógio do Android com o horário preenchido. **Confirme e salve no relógio**. Não afirma que um alarme foi salvo.
- “Jarvis, criar alarme” ou horários por extenso/ambíguos abrem a interface do relógio para escolher o horário.
- “Jarvis, ajuda” explica os comandos; “Jarvis, parar” desliga a escuta.
- Os botões ChatGPT e alarme funcionam sem acesso ao microfone.

Não é um modelo de linguagem nem controla a interface interna do ChatGPT. Não exige assinatura ou chave de API. Nenhum áudio ou histórico é salvo pelo app; o texto exibido permanece apenas na memória da tela/ViewModel.

## Estado da entrega e validação

**APK debug 0.1.0 compilado com sucesso** em 27/10/2026 no GitHub Actions, commit `456f5a33efd1c830d49e7d86e7f8d1e13d8bf409`.

- [Baixar o APK de teste como artefato ZIP](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460/artifacts/12766140769) — entre na conta GitHub, extraia o ZIP e instale `app-debug.apk`.
- [Execução com logs e relatórios](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460).
- 14 testes de unidade aprovados, sem falhas ou testes ignorados.
- Android Lint: 0 erros e 9 avisos não impeditivos, detalhados no relatório.
- 4 testes instrumentados aprovados no emulador Android 35, sem falhas. A execução completa concluiu com **success**.
- APK universal de 10.335.998 bytes, com suporte ARM64 para o Galaxy S25 FE. SHA-256 do APK: `e9a589a5bfa73eae0c20e8885dc8574741800d599d555278aec956f4b409f8c5`.

O ZIP original de 37 arquivos foi preservado. O projeto foi enviado à branch `main`, mantendo o histórico e o título do README existente. A voz, as permissões e o Samsung Clock precisam da validação física no Galaxy S25 FE conforme [TESTES.md](docs/TESTES.md). Mais evidências em [VALIDATION.md](docs/VALIDATION.md).

## Gerar APK pelo celular

Guia completo em **[APK-PELO-CELULAR.md](docs/APK-PELO-CELULAR.md)**. O projeto já está no repositório oficial. Para gerar outro APK, execute **Actions → Android - APK de teste → Run workflow**. Quando ficar verde, baixe o artefato **JARVIS-Mobile-debug**, extraia o ZIP e instale `app-debug.apk`.

O workflow executa testes de unidade, Android Lint, `assembleDebug` e compila o APK de testes. Um segundo job executa os testes instrumentados no emulador Android 35. Só disponibiliza APK se todos esses passos passarem. Ele não publica na Play Store nem cria uma release pública.

## Compilar no computador

Requisitos: JDK **17**, Android Studio Meerkat ou superior compatível com AGP 8.9.2, SDK Platform **35** e Build Tools **35.0.0**, internet para baixar dependências gratuitas. Não precisa NDK, Firebase, conta OpenAI ou serviços pagos.

1. Abra esta pasta como projeto no Android Studio e aceite instalar os SDKs indicados.
2. Em Gradle JDK, selecione JDK 17.
3. Configure `local.properties` com `sdk.dir=/caminho/do/android-sdk`, ou a variável `ANDROID_HOME`. Não versione `local.properties`.
4. Se o Studio solicitar o wrapper antes da sincronização, execute o comando abaixo no terminal da pasta do projeto. O launcher busca o JAR **oficial** do wrapper e valida seu SHA-256 antes de executá-lo. Requer `curl`/`wget` e `sha256sum`/`shasum` no Linux/macOS; PowerShell no Windows. O JAR é obtido automaticamente pelo launcher ou gerado pelo CI; o ZIP original foi preparado sem esse binário. Você também pode usar Gradle 8.11.1 instalado para executar `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`.

Linux/macOS:

```sh
chmod +x gradlew scripts/bootstrap-wrapper.sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk` (APK universal, assinado com chave de debug, não destinado à publicação).

Instalação com computador/depuração USB:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Uma compilação nova do CI pode usar outra chave debug. Se surgir conflito de assinatura, desinstale a versão antiga antes de instalar a nova (isso apaga os dados locais do aplicativo).

## Voz no Galaxy S25 FE

1. Conceda **Microfone → Permitir durante o uso do app** quando solicitado.
2. Configure o mecanismo de texto para fala em português brasileiro, baixe a voz pt-BR e teste a reprodução nas configurações do Samsung (os nomes dos menus variam conforme a One UI).
3. No serviço de reconhecimento de voz do aparelho, baixe o modelo pt-BR offline quando oferecido. O app prefere `createOnDeviceSpeechRecognizer` no Android 12+ quando disponível.
4. Se só existir o serviço convencional, `EXTRA_PREFER_OFFLINE` é uma preferência: **o provedor do Android pode usar internet e processar áudio remotamente**. O app exibe essa condição. Não existe garantia universal de funcionamento offline.
5. Em modo avião, teste a disponibilidade real da voz e do reconhecimento pt-BR. A ausência de modelo resulta em uma mensagem orientando a configuração.

O app não declara permissão de internet; um serviço de voz instalado tem permissões e política próprias. Sem APIs pagas não significa funcionamento offline garantido. O recurso pode produzir sinais sonoros do provedor entre sessões; a v0.1 não usa um detector local dedicado de palavra-chave. A escuta é renovada em sessões, com pequenos intervalos.

## Arquitetura e privacidade

- `domain/`: parser de comandos e janela de ativação, sem dependências Android.
- `voice/`: contrato substituível `SpeechInput`, adaptador `SpeechRecognizer`, saída TextToSpeech com tratamento de inicialização/timeout.
- `JarvisViewModel`: estado, ciclo de conversa, pausa de reconhecimento durante a fala e repetição limitada de erros.
- `actions/`: intents oficiais para abrir ChatGPT e preencher alarme; a Activity inicia a ação.
- `ui/`: tela Compose, tema escuro e núcleo animado apenas durante atividade.
- `MainActivity`: permissão em runtime, estado visível e execução dos intents.

Ao sair, bloquear a tela ou trocar de aplicativo (quando a Activity recebe `onStop`), o microfone é liberado. Ao retornar, ative novamente. Não há gravação em segundo plano, serviço de acessibilidade, serviço persistente, inicialização automática, análise de notificações, armazenamento de áudio, telemetria ou chaves secretas. Veja [ARQUITETURA.md](docs/ARQUITETURA.md) para as restrições de uma evolução futura.

## Testes

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
# Com dispositivo/emulador conectado:
./gradlew connectedDebugAndroidTest
```

O workflow também inclui um segundo job com emulador Android 35 para executar os testes instrumentados de Compose e intents. Voz real, TTS, permissões e Samsung Clock precisam dos testes manuais em [TESTES.md](docs/TESTES.md).

## Custos e publicação

Bibliotecas Android/AndroidX/Kotlin gratuitas, nenhum SDK comercial ou integração paga. GitHub Actions em repositórios privados usa a franquia do seu plano; confirme que ainda há minutos e armazenamento gratuitos e mantenha gastos adicionais bloqueados. Não habilite cobrança para este projeto. O APK pode ser gerado localmente sem cobrança de serviço. O workflow gera apenas artefatos de teste; não publica o aplicativo na Play Store nem cria releases.

As marcas JARVIS, Samsung e ChatGPT pertencem aos respectivos titulares. Este é um projeto pessoal independente.
