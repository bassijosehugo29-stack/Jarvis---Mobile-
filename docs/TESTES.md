# Plano de testes — versão 0.4.0

**Confirmado:** 39 testes de unidade e 11 instrumentados aprovados, total 50, sem falhas. [Execução completa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094); Lint com 0 erros e 10 avisos. Detalhes em [VALIDATION.md](VALIDATION.md).

## Automatizados

- CommandParserTest: ativação, comandos explícitos de aplicativos, agenda e alarme; um título de compromisso não pode virar comando de abrir app.
- AppResolverTest: nomes instalados, acentos, aliases, ausência de correspondência, múltiplas opções e respostas numeradas.
- EventParserTest: conversa com dados incompletos, preservação de título, calendário pt-BR, palavras/números, duração, limites, datas e horários conflitantes, passado e mudanças de fuso.
- ConversationGateTest: ativação, janela, expiração e reinício.
- JarvisScreenTest: núcleo e pausa, ausência de atalhos ChatGPT/alarme, configurações, cabeçalho fixo e fonte ampliada.
- MainActivityVisualTest: Activity real com microfone desligado e captura da tela.
- InstalledAppsTest: consulta real de atividades de launcher exportadas e construção de intent explícito.
- CalendarProviderTest: permissão no emulador, criação de calendário LOCAL isolado, gravação/leitura real de evento (título, fuso, início/fim, agenda e ausência de alarme) e remoção dos dados de teste em finally. Nenhuma conta externa é usada.
- AlarmIntentTest: intent oficial, horário, solicitação de dispensa de UI apenas com horário válido.
- Android Lint e compilação de APK de app e testes.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

A máquina local não tem Android SDK. A evidência de compilação/testes vem do GitHub Actions, com emulador Android 35 no segundo job. Não confundir a verificação Python de estrutura com compilação Kotlin.

## Aceitação manual no Galaxy S25 FE

Registre APK, Android/One UI, provedor de reconhecimento/TTS e modelos pt-BR.

| Caso | Esperado |
| --- | --- |
| Abrir pela primeira vez | HUD e núcleo; sem atalhos ChatGPT/alarme; microfone desligado; nenhum pedido de calendário |
| Negar microfone | Orientação, sem crash e sem áudio capturado |
| Ativar/permitir e dizer Jarvis | Resposta exata “Às suas ordens, senhor” |
| Pedir abrir WhatsApp antes de Jarvis | Não abre |
| Jarvis abrir WhatsApp / entrar no Instagram | Abre app instalado, desliga microfone antes de sair |
| App ausente / desativado | Mensagem de ausência, sem instalar ou abrir uma busca web |
| Nome ambíguo | Pergunta e aguarda número/nome completo; não escolhe sozinho |
| App em Pasta Segura ou outro perfil | Informa ausência se não visível ao launcher do perfil atual |
| Jarvis abrir ChatGPT | Apenas pacote oficial com.openai.chatgpt |
| Jarvis agende dentista amanhã às quinze horas | Primeiro pedido completo solicita calendário; após permitir, salva com duração padrão de uma hora |
| Negar calendário / não perguntar novamente | Não grava; orienta pelas configurações |
| Várias agendas | Pergunta a agenda; escolha por voz é lembrada |
| Agenda removida ou tornada somente leitura | Revalida e pergunta novamente / orienta configurar agenda |
| Agende dentista → amanhã → quinze e trinta | Coleta todos os campos por voz e só então salva |
| Agende amanhã às 15h → Consulta | Pergunta título e salva após resposta |
| Dia 30/10 às 14h por 30 minutos | Conferir data, fuso, horário final e título no calendário |
| Data inválida, horário passado ou duas datas | Não salva por suposição; pergunta/corrige |
| Agendar todo dia / daqui a duas horas | Explica formato suportado; não cria evento parcial |
| Cancelar/pausar/esperar mais de 20 segundos | Descarta pedido incompleto; nenhuma gravação posterior |
| Sair durante seleção/permissão | Microfone desligado, pedido incompleto cancelado |
| Interromper gravação ou receber resposta incerta | Conferir agenda antes de repetir para evitar duplicata |
| Alarme para sete e meia | Envia 07:30 ao relógio com skip-UI; conferir resultado real no Samsung Clock |
| Criar alarme sem horário | Pergunta o horário por voz |
| 25:90 / hora ambígua | Não solicita alarme com horário inventado |
| TTS indisponível / falha | Texto permanece disponível; sem sobreposição de escuta à fala |
| JARVIS falando | Microfone pausado, animação de saída decorativa |
| Modelo pt-BR ausente / modo avião | Orienta configurar; offline depende do provedor/modelo instalado |
| Ir para home, bloquear, trocar app e voltar | Microfone liberado; ativação manual ao retornar |
| Fonte 150%, TalkBack, rotação | Núcleo e configurações alcançáveis, cabeçalho e rodapé protegidos das barras |

As capturas e vídeo do CI usam um emulador, não um Galaxy S25 FE físico. A prévia é silenciosa e mostra o microfone desligado. O launcher do emulador é parado antes das capturas para evitar sua sobreposição de ANR; erros do app não são suprimidos. Após os testes de aceite, remova os compromissos e alarmes de teste.
