# Arquitetura e evolução da escuta

## Fluxo atual

```text
Tela Compose → permissão RECORD_AUDIO → ViewModel → SpeechInput
                                                     ↓ texto final pt-BR
                                           CommandParser + ConversationGate
                                                     ↓ comando autorizado
                           TextToSpeech (microfone pausado durante a fala)
                                                     ↓ conclusão / timeout
                            próxima sessão de voz OU evento para Activity
                                                     ↓
                                 AndroidActions → app instalado / relógio Android
```

Todos os callbacks de reconhecimento e mudanças de sessão são coordenados na thread principal. A Activity coleta ações somente em estado STARTED. Não há execução deliberada de ações em segundo plano. O gate usa tempo monotônico e exige “Jarvis” antes de aceitar comandos, com janela de 20 segundos renovável. Não é uma autenticação: qualquer pessoa próxima pode falar comandos durante a escuta.

O reconhecimento é feito por sessões oficiais do Android, não por um detector dedicado de wake word. Há pausas de aproximadamente 800–1000 ms entre sessões; erros não transitórios levam a até três tentativas. Não promete escuta contínua sem lacunas. O microfone é cancelado durante o TTS e destruído em `onStop`. Ao voltar, a ativação é manual.

`SpeechInput` é a fronteira para outro mecanismo de reconhecimento gratuito ou para um serviço futuro. O parser e o gate não dependem de Activity. O contrato não elimina as exigências do sistema para uma implementação em segundo plano.

## Segunda fase, não implementada nem declarada no Manifest

Para escutar além da tela atual, será preciso implementar e validar:

- Serviço de primeiro plano com `foregroundServiceType="microphone"`, permissões `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` (Android 14+) e `RECORD_AUDIO`, iniciando enquanto a Activity está visível e a permissão está válida. Em Android 12+ há limites para iniciar serviços a partir do background; Android 14+ aplica as regras de permissão “durante o uso”.
- Notificação persistente com ação **Parar**. No Android 13+, tratar `POST_NOTIFICATIONS` para a experiência de notificação, respeitando que a negativa não remove as obrigações de um foreground service.
- Coordenador de voz pertencente ao Service, sem guardar referência à Activity; microfone liberado na parada. Não transferir diretamente esta ViewModel para o Service.
- Mecanismo local dedicado de palavra-chave (licença gratuita verificada), análise de bateria, transparência sobre microfone e avaliação de pt-BR no dispositivo. O Android desaconselha usar `SpeechRecognizer` como reconhecimento contínuo de longa duração; o serviço não deve simplesmente repetir sessões indefinidamente.
- Convivência com chamadas, perda de foco, economia de bateria Samsung, bloqueio de tela, remoção de tarefa, revogação de permissão e morte do processo.
- Abertura de aplicativos a partir do background deve respeitar as restrições de início de Activity. Preferir uma ação de notificação que o usuário toque. Não usar overlay, acessibilidade, bypass de bateria ou permissões extras para contornar essas regras.
- Não iniciar gravação por boot, não pedir exclusão de otimização de bateria como padrão, não prometer funcionamento permanente.

Esta fase requer trabalho adicional e testes no Android/One UI alvo. A v0.4 não inclui serviço, notificação, permissões de background nem gravação oculta.

## Alarmes

O app usa `AlarmClock.ACTION_SET_ALARM`, permissão normal `com.android.alarm.permission.SET_ALARM` e `EXTRA_SKIP_UI=true` quando existe um horário válido. O relógio pode exigir interação mesmo assim. Não há retorno que comprove o salvamento, por isso o JARVIS informa a solicitação sem alegar que verificou um alarme. Não usa AlarmManager, exact alarms, receiver ou serviço próprio.

`PortugueseTime` interpreta números e palavras como “sete e meia”, “quinze e trinta” e “três da tarde”. Ausência de horário ou ambiguidade gera pergunta por voz. Recorrência, data específica e temporizadores não são implementados.

## Aplicativos instalados e conversa

`InstalledApps` consulta somente MAIN + LAUNCHER no perfil atual, com a declaração de queries correspondente. `AppResolver` resolve o nome de exibição normalizado ou aliases conhecidos. Com mais de uma opção, a ViewModel pergunta antes de criar o intent explícito para o componente exportado selecionado. Não existe QUERY_ALL_PACKAGES, busca remota ou tentativa de manipular telas privadas de outro app. ChatGPT mantém pacote oficial exclusivo.

A ViewModel mantém apenas um pedido incompleto: horário de alarme, dados do compromisso, escolha de aplicativo ou de agenda. Novo comando explícito substitui o anterior. Cancelar, pausar, sair da Activity ou expirar a janela invalidam pedidos incompletos. Trabalho de consulta/gravação usa Dispatchers.IO e é cancelado ao parar. Uma escrita que já chegou ao provedor pode concluir; não há repetição automática após resultado incerto.

## Agenda

`EventParser` é Kotlin puro, usa Clock injetável e o fuso do celular. Campos ausentes geram perguntas. Um evento completo tem título, início, fim e fuso; a duração padrão de uma hora é informada na documentação/configurações e na confirmação falada. Só aceita uma data e um horário, rejeita recorrência/offsets relativos, datas inválidas, horários passados e ambiguidades de horário de verão.

A primeira gravação solicita READ_CALENDAR e WRITE_CALENDAR pela Activity, com o microfone parado durante o diálogo do Android. A operação só continua com a Activity visível, permissão concedida e pedido pendente recente. Saída/rotação durante o pedido cancela o contexto e pode exigir repetir o comando. Não retoma a escuta em background.

`CalendarStore` lista agendas visíveis, sincronizáveis e com acesso CONTRIBUTOR ou superior; uma opção única é usada diretamente. Com várias, o usuário escolhe por voz e o ID fica em SharedPreferences. O app revalida a agenda antes de escrever. Os únicos dados lidos são metadados das agendas e o evento recém-criado, sem leitura do histórico do calendário. Depois de inserir, compara título e início/fim numa consulta ao URI retornado, e só então confirma “Agendado”. Não cria convidados ou lembretes e não controla o resultado da sincronização da conta.

## Dados

Sem analytics, gravação de áudio, HTTP próprio ou credenciais. O único ajuste persistido pelo JARVIS é o ID da agenda preferida; compromissos ficam no provedor de calendário do Android. O pacote ChatGPT é aberto por intent: não há envio automático de prompts nem API OpenAI. Preferência offline não substitui a política do provedor de reconhecimento/TTS instalado pelo usuário.

## Evolução para sincronização com notebook

A UI consome estado da ViewModel e os comandos são representados por tipos de domínio; não dependem do transporte. Para sincronização futura, adicione uma interface de repositório de sincronização que receba apenas eventos explicitamente permitidos, com implementações separadas por plataforma. Um transporte gratuito em rede local poderá usar pareamento autorizado, autenticação por dispositivo, conexão cifrada, expiração/revogação de sessão e resolução explícita de conflitos. A Activity continua sendo dona das ações que precisam de interação Android.

A v0.4 não implementa sincronização, servidor, acesso ao notebook ou transmissão de áudio. Nenhum endpoint ou credencial foi incluído. A separação atual permite adicionar esse recurso sem acoplar o parser a uma API paga.

## HUD e nível de voz na 0.3

O reconhecedor encaminha onRmsChanged somente durante uma sessão aceita. A ViewModel normaliza o valor finito para 0..1, ignora eventos fora do foreground/escuta e zera o nível em parada, erro, resultado e TTS. O HUD usa esse nível como envelope visual, sem gravar áudio e sem adicionar APIs ou permissões.

Um observador do ciclo de vida liga/desliga o relógio e a animação com o estado STARTED. A bateria é lida por BatteryManager; o relógio usa o fuso do aparelho. O poliedro é projetado matematicamente e desenhado em Canvas. Não há leituras fictícias de CPU, rede ou localização.
