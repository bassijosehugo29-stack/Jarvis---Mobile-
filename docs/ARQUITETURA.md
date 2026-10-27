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
                                 AndroidActions → ChatGPT / relógio Android
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

Esta fase requer trabalho adicional e testes no Android/One UI alvo. A v0.3 não inclui serviço, notificação, permissões de background nem gravação oculta.

## Alarmes

O app usa `AlarmClock.ACTION_SET_ALARM`, permissão normal `com.android.alarm.permission.SET_ALARM` e `EXTRA_SKIP_UI=false`. A UI oficial confirma o alarme. Não usa `AlarmManager`, não agenda exact alarms, não precisa `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, receivers ou serviço próprio. Depois de salvo no relógio, quem executa o alarme é o relógio.

Horários com dígitos usam formato de 24 horas ou qualificadores “da manhã/tarde/noite”. Horários por extenso, relativos, com datas ou ambíguos vão para o relógio sem horário. Não oferece alarme recorrente, data específica ou temporizador nesta versão.

## Dados

Sem analytics, banco de dados, gravação de áudio, HTTP próprio ou credenciais. O pacote ChatGPT é aberto por intent: não há envio automático de prompts nem API OpenAI. Preferência offline não substitui a política do provedor de reconhecimento/TTS instalado pelo usuário.

## Evolução para sincronização com notebook

A UI consome estado da ViewModel e os comandos são representados por tipos de domínio; não dependem do transporte. Para sincronização futura, adicione uma interface de repositório de sincronização que receba apenas eventos explicitamente permitidos, com implementações separadas por plataforma. Um transporte gratuito em rede local poderá usar pareamento autorizado, autenticação por dispositivo, conexão cifrada, expiração/revogação de sessão e resolução explícita de conflitos. A Activity continua sendo dona das ações que precisam de interação Android.

A v0.3 não implementa sincronização, servidor, acesso ao notebook ou transmissão de áudio. Nenhum endpoint ou credencial foi incluído. A separação atual permite adicionar esse recurso sem acoplar o parser a uma API paga.

## HUD e nível de voz na 0.3

O reconhecedor encaminha onRmsChanged somente durante uma sessão aceita. A ViewModel normaliza o valor finito para 0..1, ignora eventos fora do foreground/escuta e zera o nível em parada, erro, resultado e TTS. O HUD usa esse nível como envelope visual, sem gravar áudio e sem adicionar APIs ou permissões.

Um observador do ciclo de vida liga/desliga o relógio e a animação com o estado STARTED. A bateria é lida por BatteryManager; o relógio usa o fuso do aparelho. O poliedro é projetado matematicamente e desenhado em Canvas. Não há leituras fictícias de CPU, rede ou localização.
