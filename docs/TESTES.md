# Testes da versão 0.6

A revisão cinematográfica passou em 55 testes de unidade e 22 instrumentados, total 77, sem falhas; execução 40621754231. Novos testes cobrem política dos três modos, sanidade do PCM original, persistência em DataStore reaberto, seleção imediata de modos, silêncio sem iniciar TTS, parâmetros/markers de voz e AudioTrack real com duck/interrupção de foco. Resultados, integridade e limites em VALIDATION.md.

# Cobertura preservada da base 0.5

A versão 0.5 passou em 47 testes de unidade e 15 instrumentados (62 total), além de Lint e compilação; execução 40604934666. Resultados confirmados e links serão registrados em [VALIDATION.md](VALIDATION.md). Resultados da 0.4 não validam o novo runtime.

## Cobertura

- CommandParserTest: pedidos explícitos, transcrições da palavra Jarvis, preparação do modelo, alarmes/datas e perguntas de conversa que não podem executar ações.
- AppResolverTest: rótulos, aliases, acentos, ambiguidade e respostas numeradas com/sem Jarvis.
- EventParserTest/ConversationGateTest: campos ausentes, títulos, fusos, durações, limites, conflitos, expiração e cancelamento.
- VoiceSelectionTest: prioridade pt-BR, voz instalada, pt-PT como alternativa e rejeição de outros idiomas.
- ChatHistoryTest: contexto, limites, limpeza e delimitadores de papéis.
- JarvisScreenTest/MainActivityVisualTest: núcleo, ausência de atalhos, mensagem sem microfone, diagnósticos de voz, ajustes, cabeçalho, fonte ampliada e Activity real.
- SpeechOutputTest: API TTS real com serviço de fixture no APK de testes, produzindo PCM, completando callback, fallback sem pt-BR e falha de síntese explícita. O fixture usa Java/Android puro porque roda em processo separado sem bibliotecas do APK alvo. Não comprova alto-falante nem voz humana do Galaxy.
- LocalConversationTest: modelo oficial com hash conferido, importação, duas mensagens pela Activity real, inferência JNI e contexto do nome Rafael. Exporta transcrição fictícia para os relatórios.
- CalendarProviderTest, InstalledAppsTest e AlarmIntentTest: gravação/leitura em calendário LOCAL isolado e limpo ao terminar, launcher exportado, intents e limites das confirmações oficiais.
- Lint, compilação Kotlin/Java/C++/Compose e inspeção de integridade/alinhamento do APK.

## Executar

JDK 17, SDK 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1 e dispositivo/emulador ARM64 ou x86_64. Modelo precisa de cerca de 491 MB e acesso inicial à internet, sem chave ou inference API.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

O shell preserva o resultado real dos testes e tenta coletar relatórios, capturas/transcrição e erros de runtime mesmo após falha. Só grava prévia e reinstala a Activity se os testes passarem. O CI não suprime erros do app. O launcher do emulador é parado para impedir sua sobreposição de ANR às capturas. O SDK Android não está instalado no executor local: evidência de compilação vem do GitHub Actions, não da verificação Python de estrutura.

## Aceite no Galaxy S25 FE

1. Testar voz com volume de mídia audível, alto-falante, fones/Bluetooth e uma voz pt-BR instalada. Negar/remover voz deve gerar orientação, sem fingir que falou.
2. Preparar modelo por Wi-Fi, interromper/retomar, cancelar e importar o arquivo oficial. Modelo errado/incompleto deve ser recusado; sem modelo, comandos ainda funcionam e conversa pede preparação.
3. Ativar e ouvir a saudação; perguntar sem repetir Jarvis, ouvir uma resposta e fazer uma pergunta que dependa do contexto anterior. Interromper no núcleo e conferir o retorno da escuta após fala.
4. Testar mensagem por texto sem microfone; keyboard, fonte 150%, TalkBack, rotação e erro de volume visível.
5. Abrir aplicativo, criar alarme e agendar compromisso; conferir o resultado nos apps oficiais. Perguntar sobre um alarme não pode criar um alarme.
6. Sair, bloquear, trocar app, atender chamada e voltar: microfone/TTS param; reativação é manual. Nenhuma conversa incompleta pode disparar ação posterior.
7. Validar modelos pt-BR offline em modo avião; disponibilidade real é do provedor. Medir latência, aquecimento, bateria e português do modelo local. Não concluir desempenho do Galaxy a partir do emulador.

Apenas o dono pode confirmar que ouviu a voz física no aparelho. O teste emulador valida componentes e integração, sem substituir esse aceite. Remova compromissos e alarmes de teste ao terminar.
