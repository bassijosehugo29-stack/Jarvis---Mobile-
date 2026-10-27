# Jarvis---Mobile-

**JARVIS Mobile**

Assistente Android nativo em **Kotlin + Jetpack Compose**, com interface preta, azul e dourada. Versão 0.3.0, destinada primeiro ao Samsung Galaxy S25 FE. Android 8+ (API 26); compilação e target API 35.

## O que esta versão faz

- Escuta pt-BR após tocar em **Ativar JARVIS**, somente enquanto a Activity estiver visível.
- “Jarvis” → **“Às suas ordens, senhor”**, usando TextToSpeech do Android.
- Após a resposta, aceita comandos por 20 segundos; também aceita a ativação e o comando numa frase.
- “Jarvis, abrir ChatGPT” abre apenas o pacote oficial `com.openai.chatgpt`, se instalado.
- “Jarvis, abrir WhatsApp” / “entrar no Instagram” procura o nome entre aplicativos instalados do perfil atual. Se houver ambiguidade, pergunta por voz.
- “Jarvis, agende dentista amanhã às quinze horas” salva diretamente no calendário Android, com permissão no primeiro uso. Pergunta os dados ausentes e qual agenda usar se houver várias.
- “Jarvis, alarme para sete e trinta” solicita o alarme ao relógio oficial com dispensa de UI quando suportada; o relógio pode ainda exigir confirmação. Não afirma que verificou um alarme salvo.
- “Jarvis, ajuda” explica os comandos; “Jarvis, parar” desliga a escuta.
- A tela principal usa o núcleo para ativar/pausar a voz; os atalhos de aplicativos foram removidos.

## Versão 0.4.0 — ações por voz

A revisão amplia os comandos e remove os botões individuais. Veja exemplos, permissões e limites em [COMANDOS-0.4.md](docs/COMANDOS-0.4.md). A compilação desta revisão ainda está em validação; os resultados da 0.3 abaixo são históricos e não validam o código novo.


Não é um modelo de linguagem nem controla a interface interna do ChatGPT. Não exige assinatura ou chave de API. Nenhum áudio ou histórico é salvo pelo app; o texto exibido permanece apenas na memória da tela/ViewModel.

## Histórico: versão 0.3.0 — HUD holográfico

Interface inspirada na referência visual enviada pelo proprietário: núcleo geométrico projetado em 3D, anéis luminosos em movimento, grade de fundo e painéis angulares. O relógio e a bateria mostram dados reais do Android. A animação ambiente funciona enquanto a tela está visível; o microfone só é ativado pelo usuário.

Quando o serviço Android fornece eventos RMS, os anéis e a barra de entrada reagem ao nível de voz. Ao responder, o núcleo exibe uma animação de saída; isso não representa medição de áudio do TTS. A captura de áudio e as animações são interrompidas quando a Activity deixa de estar visível.

- [APK de teste 0.3.0](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439/artifacts/12771677174).
- [Compilação e 22 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439): 14 de unidade e 8 instrumentados Android 35; Lint com 0 erros e 9 avisos não impeditivos.
- [Capturas e vídeo real da interface no emulador](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439/artifacts/12771093811). O vídeo é uma prévia silenciosa do HUD, com o microfone desligado.
- Commit validado: `5870ac9ee8246486554600c332a41cd89ca72046`.
- APK universal de 10401534 bytes, incluindo ARM64. SHA-256: `49a90f913e33d16ea6fd26640cf543766769af5034c8978ad2bf916192cb5433`.

Desinstale a versão anterior antes de instalar esta compilação debug: o certificado de assinatura mudou. Depois permita novamente o microfone.

Os comandos existentes foram mantidos. Esta revisão prioriza o visual e as animações no celular; conversa livre com modelo de linguagem e controle/sincronização do notebook continuam como evoluções futuras. Mais detalhes em [VISUAL-0.3.md](docs/VISUAL-0.3.md) e [VALIDATION.md](docs/VALIDATION.md).

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
- `actions/`: launcher oficial de apps, intent de alarme e gravação/consulta de confirmação pelo Calendar Provider; a Activity solicita permissões e inicia intents.
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
