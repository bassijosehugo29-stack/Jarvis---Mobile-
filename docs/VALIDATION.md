# Validação da Missão 1 — JARVIS Mobile 0.6.0

## Resultado confirmado

- [GitHub Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231): debug-apk e Testes instrumentados Android 35 com success.
- Fonte `dc00bef9bf5595aed7dac1e41dac2c0ea71e9ae3`; documentação posterior não altera o código validado.
- **55 testes de unidade + 22 instrumentados = 77, sem falhas**.
- testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest e connectedDebugAndroidTest concluídos.
- **Lint: 0 erros, 19 avisos não impeditivos**. Não se usou baseline para esconder erros.
- [APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231/artifacts/12787512824); [evidências Android](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231/artifacts/12787209902). Artefatos retidos sete dias.

## O que foi comprovado

ExperiencePolicyTest cobre modo padrão, silêncio em todos os estados, volumes independentes, duck, captura/background e limites numéricos. OriginalScoreTest verifica PCM não nulo, sem saturação, bordas zeradas e sinais distintos. ExperienceStoreTest grava/reabre DataStore em arquivo real, conferindo modo, volumes e perfil vocal. ExperienceModesTest percorre os três modos na Activity, verifica resposta em texto sem escuta automática e cancela inicialização real de TTS ao sair.

SoundscapeTest usa AudioTrack Android de verdade: avanço do playback head, redução de ganho, parada em Silencioso/captura/background e interrupção por requisição concorrente de foco, sem retomada automática. Não comprova alto-falante físico. SpeechOutputTest verifica gate sem inicializar engine no Silencioso/voz desligada/volume zero, PCM, falha explícita, fallback português, tom/velocidade e callbacks de início/range via serviço Java exclusivo do APK de testes. Esse serviço produz um tom, não uma voz humana; não é teste da voz Samsung.

Os testes anteriores de parser, contexto de conversa, launcher exportado, pacote ChatGPT, intents de alarme, Calendar Provider, Activity/Compose, campo de texto, cabeçalho fixo e fonte ampliada foram preservados e executados. A conversa real importa pesos oficiais com SHA-256 conferido e envia duas mensagens pela Activity, verificando o nome Rafael no contexto. A transcrição exportada contém dados fictícios e texto efetivamente gerado, sem API remota. O modelo pode formular a resposta de maneira simples ou inadequada; o teste não atesta qualidade geral.

Capturas cinema/silencioso são da Activity real sem microfone iniciado. A prévia de vídeo é captura do emulador, sem áudio gravado; não representa teste perceptivo da música. Fonte ampliada usa fixture de Compose. Consulte os arquivos brutos do artefato para evidências originais.

## Integridade do APK

- 18333609 bytes; ARM64-v8a/x86_64; SHA-256 `175247796161809bc1632c7c7c96346f62931c36390d74241a2c67a2b15b1c28`.
- Certificado debug SHA-256 `fa4a6ce51e3a0adb7e6fe7c44e6da5f06436310832fdc61593ffe59a0c8b4937`.
- Certificado diferente da 0.5: true; conflito de assinatura exige desinstalar a versão anterior.
- ZIP e APK com CRC válido, manifesto/DEX/recursos/bloco de assinatura presentes, modelo JSON conferido e sem pesos GGUF embutidos.
- Alinhamento ELF de todas as bibliotecas nativas de 64 bits e alinhamento ZIP dos segmentos não comprimidos conferidos em 16 KB.

A leitura local do certificado não substitui verificação criptográfica independente. As instalações e testes Android foram feitos pelo CI. A máquina local não possui SDK Android: a verificação Python de estrutura é distinta da compilação/testes do GitHub.

## Correções e limites

Uma revisão preliminar parou por link para guia de voz ainda não enviado. Outra apontou inferência recursiva de tipos Kotlin no coordenador de foco/áudio; as declarações foram explicitadas. Dois testes reais de áudio apontaram a verificação prematura do estado do AudioTrack estático; a carga de PCM passou a ocorrer antes de conferir STATE_INITIALIZED. A importação de arquivo passou a abrir e fechar o stream no mesmo contexto IO, eliminando o aviso Recycle. Capturas de modos com captureToImage vieram incompletas; o teste final captura a janela via UiAutomation, preservando as evidências anteriores. A entrada passou a aguardar o encerramento completo do trabalho, além de pensar/falar; o teste usa a ação Enviar do teclado para não depender de um alvo de toque durante transições do IME. O código final acima foi recompilado/testado após as correções. Os sinais de confirmação/escuta foram ordenados; uma indicação visual de erro não pode desativar o gate de silêncio durante captura.

Ainda requer aceite físico no Samsung Galaxy S25 FE: ouvir pt-BR, selecionar voz disponível, volume/roteamento Bluetooth/DND, testar reconhecimento/RMS, sons do provedor Android, permissões, pausa ao bloquear/sair, aparência/fluidez, música/efeitos e bateria/temperatura. Não se mediram FPS/autonomia nem se garante timbre masculino/qualidade de estúdio. [Roteiro](MISSAO-1.md).

Sem bloqueio de compilação na revisão validada. Sem API paga, credenciais embutidas, mensalidade, publicação ou módulos adicionais desta missão.

## Limite das capturas de modos

As capturas brutas após alternar modos continuam parcialmente incompletas no emulador, mesmo usando UiAutomation e espera de composição: faltam elementos estáticos, inclusive partes da barra do próprio Android. A captura inicial da Activity e a prévia gravada após a reinstalação mostram o HUD completo. Isso não permite concluir, só pelas capturas, como a troca será desenhada no Galaxy; o aceite visual no aparelho continua necessário. As imagens originais foram preservadas sem redesenho ou preenchimento artificial. Os testes de seleção/persistência e áudio dos três modos passaram; não são teste de perfeição dos pixels.
