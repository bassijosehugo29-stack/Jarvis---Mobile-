# Conversa e saída de voz — 0.5

Revisão em validação. Compilar não garante áudio audível no Galaxy: a saída e o mecanismo instalado exigem teste físico. O código anterior podia mostrar texto e seguir sem áudio quando a voz pt-BR faltava. Esta revisão usa vozes em português disponíveis, incluindo pt-PT como alternativa, e tenta os mecanismos instalados. A fala usa mídia, com atributos de áudio de fala, volume explícito e foco temporário; os botões de volume controlam mídia com o JARVIS aberto. Volume zero, falha de síntese, voz ausente e timeout são exibidos. Há Testar voz e acesso aos ajustes Android. Fones/Bluetooth, DND e modelos Samsung/Google continuam sendo condições do aparelho.

## Preparar uma vez

Nos ajustes, escolha **Baixar conversa offline • 491 MB**. O download pelo DownloadManager ocorre por Wi-Fi, tem notificação do sistema e pode continuar com o app fechado; não mantém microfone aberto. É possível cancelar nos ajustes. Importe o arquivo oficial já baixado como alternativa. O arquivo é validado por tamanho e SHA-256 antes de ser carregado, e modelos diferentes são recusados. Desinstalar o app remove esse arquivo local; uma nova instalação precisará baixá-lo/importá-lo novamente.

O modelo é [Qwen2.5 0.5B Instruct Q4_K_M](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF), licenciado em [Apache-2.0](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/blob/main/LICENSE). O arquivo tem 491400032 bytes; SHA-256 74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db. A especificação está em app/src/main/assets/conversation-model.json e é usada pelo app e pelo teste do CI. Não há token, login, inference API ou cobrança de serviço. O usuário precisa de internet para esse download inicial. Uma conexão já pode ter custos próprios; o download automático é restrito a Wi-Fi não medido.

## Conversar

Após preparar, ative o núcleo. A saudação é falada imediatamente e o JARVIS volta a ouvir depois de cada resposta. Não é necessário repetir Jarvis em cada frase durante essa sessão visível. Também há envio por texto, sem permissão de microfone. O microfone fica pausado enquanto a IA pensa ou fala e é desligado ao sair/bloquear/abrir outro aplicativo. O núcleo permite interromper a resposta. Ao voltar, ative de novo.

Perguntas livres são respondidas por inferência real no aparelho usando [llama.cpp](https://github.com/ggml-org/llama.cpp/tree/74d4f5b041ad837153b0e90fc864b8290e01d8d5), MIT, com JNI, CPU e 2.048 tokens de contexto. O modelo recebe contexto curto de até três turnos anteriores em memória, sem gravação de histórico. As respostas são limitadas a 192 tokens. É um modelo leve: qualidade, português, velocidade e precisão são limitados e não equivalem aos modelos do ChatGPT. Não pesquisa informações atualizadas na internet.

Os comandos explícitos de aplicativos/agenda/alarme seguem no dispatcher original. Texto gerado pela IA jamais é enviado a esse dispatcher: o modelo conversa, sem poder executar tarefas por conta própria. Somente a fala/mensagem do usuário aciona comandos. A confirmação de eventos continua vindo do Calendar Provider, e o relógio continua responsável pelos alarmes.

## Compilar e testar

Além do SDK 35/Java 17, a revisão usa NDK 27.2.12479018 e CMake 3.22.1, gratuitos. O build busca llama.cpp por commit fixo, sem download de código em runtime. ABIs do aplicativo com conversa: ARM64-v8a (Galaxy) e x86_64 (emulador). A biblioteca JNI é ligada para páginas de 16 KB. A licença MIT do runtime acompanha os assets. Os pesos não são incluídos no APK ou no GitHub: o teste baixa o modelo oficial separadamente, verifica seu SHA e o transfere para o emulador.

Testes novos: seleção de voz em português, fallback quando pt-BR falta, síntese Android com serviço de teste que produz PCM real, falhas explícitas, entrada de texto, contexto limitado/sanitização e inferência com os pesos oficiais em duas perguntas que exigem memória do nome Rafael. O serviço TTS sintético é exclusivo do APK de testes; não é instalado com o APK do usuário. O PCM desse fixture confirma o caminho de reprodução e callbacks, sem substituir teste de voz humana real no Samsung. Uma transcrição da inferência é guardada nos relatórios do CI.
