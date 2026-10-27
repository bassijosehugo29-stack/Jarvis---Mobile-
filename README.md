# Jarvis---Mobile-

JARVIS Mobile **0.4.0** — assistente Android nativo em Kotlin e Jetpack Compose, com HUD holográfico preto, azul e dourado. Destinado inicialmente ao Samsung Galaxy S25 FE. Android 8+ (minSdk 26), compileSdk/targetSdk 35 e Java 17.

## APK e resultado confirmado

- [Baixar APK de teste 0.4.0](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094/artifacts/12776313560): extraia o ZIP e instale app-debug.apk.
- [Compilação e 50 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094): 39 testes de unidade e 11 instrumentados no Android 35, sem falhas. Android Lint: 0 erros e 10 avisos não impeditivos.
- [Capturas e vídeo silencioso no emulador](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094/artifacts/12777163969).
- Commit de código validado: `8a3b9e0ed1752d4460d880d9be6758caafb57770`. A atualização posterior de documentação não altera esse código.
- APK universal de 10434378 bytes, com ARM64-v8a, ARMv7, x86 e x86_64. SHA-256: `1d0aab0754edc2b6c9414d2aea998f120f4653c3d908fa60de43256a2c51b5c0`.

**Desinstale a versão anterior do JARVIS antes de instalar este debug:** o certificado é diferente. Isso remove a preferência local de agenda; compromissos já salvos no calendário e modelos de voz do Samsung/Google permanecem. [Guia pelo celular](docs/APK-PELO-CELULAR.md).

## Pedidos por voz

A tela principal mantém o núcleo para ativar/pausar e as configurações. Os botões individuais de ChatGPT e alarme foram removidos.

Ative o núcleo e diga **Jarvis** para ouvir **“Às suas ordens, senhor”**. Depois fale em até 20 segundos, ou diga a ativação e o comando numa frase:

- “Jarvis, abrir WhatsApp” / “entrar no Instagram”: abre o aplicativo instalado pelo nome. Nomes ambíguos geram uma pergunta por voz.
- “Jarvis, abrir ChatGPT”: abre apenas o app oficial com.openai.chatgpt, se instalado.
- “Jarvis, agende dentista para amanhã às quinze horas”: solicita permissão de calendário no primeiro uso e salva um compromisso. A duração padrão é uma hora; também aceita “por meia hora”, “por uma hora e meia” e minutos explícitos.
- “Jarvis, agende dentista”: pergunta a data e depois o horário. Com várias agendas, pergunta qual usar e lembra a escolha localmente.
- “Jarvis, alarme para sete e trinta”: solicita 07:30 ao relógio oficial com dispensa de UI quando suportada. O relógio pode exigir confirmação; JARVIS não recebe resultado de salvamento do alarme.
- “Jarvis, ajuda” explica os pedidos; “Jarvis, parar” ou “cancelar” desliga a escuta e descarta pedidos incompletos.

O compromisso só é confirmado como “Agendado” após inserção e consulta de confirmação pelo provedor de calendário Android. Não cria convidados ou lembretes. A sincronização com a conta é feita pelo sistema. Sem data/horário suficientes, ou com entradas inválidas/conflitantes, não grava por suposição. [Formatos e limites dos comandos](docs/COMANDOS-0.4.md).

## Limites e privacidade

O app abre atividades públicas do launcher no perfil atual, sem QUERY_ALL_PACKAGES. Pasta Segura, perfis de trabalho e apps sem ícone público podem não estar disponíveis. Abrir um aplicativo não permite controlar todas as suas funções internas; integrações dependem do que cada app oferece oficialmente. Não há acessibilidade, envio automático de mensagens ou compras.

O microfone só começa após ativação e permissão. É pausado durante TTS e desligado ao sair, bloquear a tela ou abrir outro app. Para um novo pedido, volte e ative o núcleo. Não há escuta oculta, serviço persistente, boot automático, gravação de áudio, histórico permanente, telemetria ou integração com notebook. Pedidos incompletos não continuam em background. A única preferência persistida é o ID da agenda escolhida; eventos ficam no provedor Android.

READ_CALENDAR/WRITE_CALENDAR só são solicitadas para gravar um compromisso pedido por voz. O app consulta metadados das agendas e o evento recém-inserido; não lê compromissos anteriores. A permissão pode ser revogada nas configurações Android. Uma escrita já entregue ao provedor pode concluir se for interrompida; diante de resultado incerto, confira a agenda antes de repetir.

O HUD usa relógio/bateria reais e animações Canvas enquanto visível; quando o serviço emite RMS, a entrada reage à voz. A animação de resposta TTS é decorativa. [Referência do visual](docs/VISUAL-0.3.md).

## Voz em português brasileiro

Usa SpeechRecognizer e TextToSpeech do Android em pt-BR, sem API de IA. No Android 12+ prefere reconhecimento no dispositivo, se disponível. Baixe os modelos gratuitos de voz/reconhecimento pt-BR nas configurações Samsung/Google. O modelo instalado e o provedor determinam a disponibilidade real.

EXTRA_PREFER_OFFLINE é uma preferência: o provedor convencional pode usar internet e processar áudio fora do aparelho. O JARVIS não declara INTERNET nem usa uma API paga, mas isso não garante funcionamento offline do serviço Android. As sessões têm pequenos intervalos e podem emitir bipes do provedor. Voz, RMS, permissões e Samsung Clock ainda exigem [aceite físico no Galaxy S25 FE](docs/TESTES.md); o emulador não comprova o comportamento do microfone no aparelho.

## Gerar APK pelo celular

Abra [Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → main**. Use “Site para computador” se necessário. Aguarde os dois jobs verdes, baixe **JARVIS-Mobile-debug**, extraia e instale app-debug.apk. Os artefatos duram sete dias. O fluxo gera APK de teste, relatórios, capturas e vídeo; não publica na Play Store nem cria releases. [Passo a passo](docs/APK-PELO-CELULAR.md).

## Compilar no computador

Requisitos: JDK 17, Android Studio compatível com AGP 8.9.2, SDK Platform 35 e Build Tools 35.0.0. Gradle 8.11.1 e Kotlin/Compose 2.1.20; dependências AndroidX gratuitas. Configure ANDROID_HOME ou local.properties com sdk.dir. Não versione local.properties.

```sh
chmod +x gradlew scripts/bootstrap-wrapper.sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Com emulador/dispositivo conectado:
./gradlew connectedDebugAndroidTest
```

No PowerShell, use `.\gradlew.bat` com as mesmas tarefas. O launcher obtém o JAR oficial do wrapper e verifica o SHA-256 antes de executá-lo; também é possível gerar com Gradle 8.11.1 instalado (`gradle wrapper --gradle-version 8.11.1 --distribution-type bin`). Saída: app/build/outputs/apk/debug/app-debug.apk.

## Projeto e custos

O projeto original de 37 arquivos foi preservado; as revisões continuam seu código e histórico, sem force push. [Arquitetura](docs/ARQUITETURA.md), [testes](docs/TESTES.md) e [evidência de validação](docs/VALIDATION.md).

Nenhuma chave de API, assinatura extra ou integração paga. Este repositório público usa runners padrão gratuitos do GitHub; o workflow não habilita cobrança. Em forks privados, respeite a franquia gratuita ou compile localmente. Não há IA de conversa livre ou controle do notebook nesta versão.

Projeto pessoal independente; as marcas JARVIS, Samsung e ChatGPT pertencem a seus titulares.
