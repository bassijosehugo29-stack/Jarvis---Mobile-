# JARVIS Mobile 0.6 — Missão 1

Revisão cinematográfica em validação. **O APK 0.6 só deve ser instalado depois de ambos os jobs de Actions aprovados.** A validação 0.5 abaixo é da base preservada, não desta revisão.

A 0.6 acrescenta modos Silencioso, Assistente e Cinematográfico com DataStore; núcleo com seis estados; ajustes de voz/velocidade/tom; trilha original sintetizada localmente, efeitos independentes e redução da música durante TTS. Não inicia módulos novos de agenda, e-mail, notícias ou memória avançada. [Escopo, controles e limitações](docs/MISSAO-1.md).

# Jarvis---Mobile-

JARVIS Mobile **0.5.0** — assistente Android em Kotlin/Jetpack Compose, com HUD preto, azul e dourado, conversa local e comandos de aplicativos, agenda e alarme. Destinado inicialmente ao Galaxy S25 FE. Android 8+ em aparelhos ARM64; x86_64 incluído para testes. compileSdk/targetSdk 35.

## APK e validação

- [Baixar o APK 0.5](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666/artifacts/12780696806): extraia o ZIP e instale app-debug.apk.
- [Compilação e 62 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666): 47 de unidade e 15 instrumentados Android 35, sem falhas; Lint com 0 erros e 19 avisos não impeditivos.
- [Capturas, vídeo e transcrição real da conversa de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666/artifacts/12779839416).
- Código validado: `e0d7e8c411e398d6e18ea9cdc5a41681bde0d92c`. Documentação posterior não altera esse código.
- APK: 17496508 bytes. SHA-256: `8ac0a1688bd700813161f365149ff11a711d7500e94d7faf8a24c5a1273a2a4f`.

A assinatura debug mudou: desinstale o JARVIS anterior antes de instalar. Faça isso antes de preparar o modelo para evitar baixá-lo novamente. [Instalação pelo celular](docs/APK-PELO-CELULAR.md).

## Primeiro uso: som e conversa

1. Nos ajustes, toque em **Testar voz**, aumente o volume de mídia e confira a saída em alto-falante/fones/Bluetooth. Use **Ajustar voz do Android** para escolher/instalar uma voz em português quando necessário.
2. Escolha **Baixar conversa offline • 491 MB**, usando Wi-Fi não medido. Aguarde **Conversa offline pronta**. Pode importar o modelo oficial já baixado como alternativa. Tamanho e SHA-256 são verificados; arquivos diferentes não são carregados.
3. Ative o núcleo e permita microfone durante o uso. A saudação **“Às suas ordens, senhor”** é falada imediatamente; depois, converse sem repetir Jarvis a cada frase.

Há campo de mensagem para conversa por texto, sem exigir microfone. Perguntas livres usam inferência real no celular; o contexto curto mantém até três turnos anteriores em memória. Ao sair, bloquear ou abrir outro app, a escuta e a fala param. Para continuar, volte e ative o núcleo. O núcleo também interrompe uma resposta em andamento.

A 0.4 só aceitava comandos fixos e podia ficar sem som quando a voz pt-BR faltava ou o texto não passava pela ativação. A 0.5 aceita conversa durante a sessão ativada, tenta vozes/mecanismos em português, usa volume de mídia e exibe falhas sem escondê-las. O teste do emulador verifica PCM e callbacks com um serviço auxiliar; ouvir a voz real do Samsung exige [teste físico](docs/VOZ-0.5.md).

## Modelo gratuito e limites

Usa [Qwen2.5 0.5B Instruct Q4_K_M](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF), Apache-2.0, com [llama.cpp](https://github.com/ggml-org/llama.cpp/tree/74d4f5b041ad837153b0e90fc864b8290e01d8d5), MIT, por JNI/CPU. Não usa uma API de inferência, chave, login ou assinatura. O modelo tem **491400032 bytes**, baixados separadamente do APK uma vez. Desinstalar o app remove o arquivo local. O DownloadManager pode continuar o download solicitado com o app fechado, usando notificação do sistema; isso não mantém microfone ligado.

É um modelo leve, com contexto curto e resposta de até 192 tokens: pode errar, ser simples ou levar alguns segundos. Não tem os mesmos recursos dos modelos do ChatGPT e não busca informações atualizadas na internet. O texto gerado jamais é enviado ao dispatcher de ações do celular. [Arquitetura e formatos](docs/CONVERSA-0.5.md).

## Comandos preservados

- “Jarvis, abrir WhatsApp” / “entrar no Instagram”: abre o app instalado pelo nome de exibição. Pergunta em caso de ambiguidade.
- “Jarvis, abrir ChatGPT”: apenas o pacote oficial com.openai.chatgpt.
- “Jarvis, agende dentista amanhã às quinze horas”: salva um compromisso após permissão de calendário no primeiro uso. Pergunta os dados ausentes e qual agenda usar se houver várias. Duração padrão: uma hora; aceita meia hora e durações explícitas.
- “Jarvis, alarme para sete e trinta”: solicita 07:30 ao relógio oficial, que pode exigir confirmação e não retorna comprovação do salvamento.
- “Parar” desliga a escuta. Durante uma sessão visível ativada, o nome Jarvis é opcional nos pedidos.

A agenda só confirma após inserção e leitura do evento pelo Calendar Provider; não cria convidados ou lembretes. Perguntar sobre um alarme não cria um alarme. Apps precisam ter atividade pública no launcher do perfil atual. Controle de funções internas depende do que cada aplicativo oferece oficialmente. [Formatos dos comandos](docs/COMANDOS-0.4.md), atualizados quanto à sessão na 0.5.

## Privacidade

Sem histórico permanente, gravação de áudio, telemetria, servidor próprio, acessibilidade, inicialização por boot ou escuta em segundo plano. O único tráfego próprio é o download solicitado do modelo pelo Android; as mensagens do modelo não saem do aparelho. Preferências locais guardam o calendário escolhido e o estado do modelo. Eventos ficam no Calendar Provider e a sincronização da conta é responsabilidade do sistema.

SpeechRecognizer e TextToSpeech usam pt-BR quando disponível, com pt-PT como alternativa de fala. A preferência offline do reconhecedor depende do serviço/modelos instalados; esses provedores Android podem processar áudio por rede. Portanto o modelo de conversa é local, mas não há garantia universal de reconhecimento/TTS offline. Não houve teste físico no Galaxy nesta sessão.

## Gerar pelo celular ou compilar

No navegador: [Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → main**. Aguarde ambos os jobs aprovados, baixe **JARVIS-Mobile-debug**, extraia e instale. Artefatos duram sete dias. Use “Site para computador” se o menu não aparecer. [Guia](docs/APK-PELO-CELULAR.md).

No computador: JDK 17, SDK Platform 35, Build Tools 35.0.0, NDK **27.2.12479018**, CMake **3.22.1**, Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose 2.1.20. Todas as ferramentas são gratuitas. Configure ANDROID_HOME ou local.properties com sdk.dir.

```sh
chmod +x gradlew scripts/bootstrap-wrapper.sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Para o teste real de conversa com emulador/dispositivo conectado:
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

No PowerShell, use .\gradlew.bat para tarefas Gradle. O launcher busca e valida o wrapper oficial; também pode gerar com Gradle 8.11.1 instalado. O CMake busca o runtime por commit fixo apenas na compilação; não há código nativo baixado em runtime. As operações de inferência são otimizadas mesmo no APK debug e a biblioteca JNI tem segmentos ELF alinhados a 16 KB. Os pesos não são versionados ou incluídos no APK.

## Projeto, testes e custos

[Resultados e limites](docs/VALIDATION.md), [testes](docs/TESTES.md), [arquitetura](docs/ARQUITETURA.md), [preparar voz](docs/VOZ-0.5.md). O projeto original e o histórico foram preservados, sem force push. Nenhum serviço pago, cobrança de API, assinatura extra, Play Store ou release pública foi utilizado. O repositório público usa runners padrão gratuitos do GitHub; forks privados precisam respeitar sua franquia ou compilar localmente. Não habilite cobrança para este projeto.

Projeto pessoal independente; marcas JARVIS, Samsung e ChatGPT pertencem aos respectivos titulares.
