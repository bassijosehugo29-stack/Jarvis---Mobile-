# Validação — JARVIS Mobile 3.1 / Voz A · Executivo clássico

**Histórico da primeira integração de voz A.** Para os APKs atuais, links separados, assinaturas/hashes e a execução aprovada da correção de entrega, consulte [DOWNLOADS-APK.md](DOWNLOADS-APK.md). A recompilação da entrega preservou o código dos aplicativos e usa outro certificado de teste; os hashes abaixo pertencem à primeira entrega.

## Resultado confirmado em 28/10/2026

[Execução 40711202098](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40711202098): **debug-apk** e **Testes instrumentados Android 35** concluídos com success. Fonte testada: `9806fec2cbebf1f54cd2456d5a54484bf2421164`. Alterações posteriores somente em Markdown documentam o resultado sem mudar os APKs. [Validação histórica da 3.0](VALIDATION-3.0.md).

- **82 testes de unidade do assistente + 5 do motor + 44 Android = 131 aprovados**, zero falhas e zero ignorados/pulados. Duração de unidade: 2,606 s no assistente e 0,166 s no motor; suíte Android: 2 min 29,68 s. Esses tempos não medem o Galaxy.
- testDebugUnitTest, lintDebug e assembleDebug nos dois módulos; assembleDebugAndroidTest e connectedDebugAndroidTest no assistente aprovados. Emulador API 35 / x86_64, modelo real incluído.
- **Lint: zero erros, 30 avisos no assistente e 9 no motor**, sem baseline. Avisos de atualização/target/KTX e, no motor, textos programáticos, regras de backup e serviço exportado necessário à interoperabilidade TTS.
- [APKs](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40711202098/artifacts/12827071247) · [unidade, Lint, assinatura e integridade](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40711202098/artifacts/12826343518) · [Android, WAVs, capturas e vídeo](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40711202098/artifacts/12827123187) · [fontes correspondentes do motor](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40711202098/artifacts/12826791121). Artefatos retidos por sete dias; a geração manual continua disponível.

## O que foi efetivamente comprovado

O Faber pt-BR é carregado pelo motor real `br.com.jarvis.voicea` via TextToSpeechService. As quatro frases aprovadas e a frase inédita “Reunião de revisão às nove e quarenta, com duração de vinte minutos.” foram sintetizadas em WAV/PCM não vazio, 22.050 Hz, mono/16 bits, com durações distintas. O SpeechOutput do aplicativo selecionou o motor aprovado, recebeu PCM real e concluiu a reprodução. As quatro WAVs públicas ficam no artefato Android. São evidências de síntese em emulador; não equivalem a ouvir o alto-falante físico do Samsung.

O teste Android conferiu certificado/metadata e ausência de permissões de rede/microfone. O motor **não solicita nenhuma permissão Android**; a síntese com modelo incluído não depende de rede, conta, API ou voz instalada pelo Google/Samsung. Reconhecimento da fala é separado e continua dependendo do serviço pt-BR do Android. Ditado pessoal mantém a política de reconhecimento local.

Todos os 401 inicializadores ONNX são idênticos aos do Faber da audição aprovada. O grafo original é mantido, adicionando somente metadata para o carregador. O hash original é `858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7`; o contêiner incluído é `956b4f1733903891c4ba0973d0603b2a3d8c09c8432fb3bb5203a90a7431daca`. Modelos e binários de build têm hashes fixados.

O DSP passou em preservação de frequência/duração, sinal finito/limitado, silêncio e normalização aproximada a -20 LUFS. Não há pitch shift. A velocidade relativa é ajustável; pitch da voz A permanece natural. Um adaptador Java fornece a assinatura boxed exigida pelo JNI, testada por reflexão e cancelamento. Isso corrige a incompatibilidade de callback das lambdas Kotlin 2 observada na primeira integração.

A migração DataStore adota A sem carregar engine/pitch antigos e preserva modo/volumes. Seleção explícita ANDROID mantém seus próprios ajustes. Testes anteriores de modos, silêncio, foco, duck, lifecycle, comandos, memória, backup, calendário, RSS, digest e conversa JNI passaram novamente. Nenhum desses módulos ganhou integração funcional nesta etapa. A voz fixture de tom continua somente no APK de androidTest e não substitui o Faber nos novos testes.

Os arquivos AndroidRuntime e buffer de crashes nativos coletados ao fim estão vazios. Diagnósticos do motor registram oito entregas PCM concluídas, sem eventos de falha e sem conteúdo pessoal. A proteção nativa suprime logs das bibliotecas/stderr; somente eventos técnicos estáticos são permitidos no debug. CI usa exclusivamente textos públicos/fixtures.

## Integridade dos arquivos entregues

| Arquivo | Pacote / versão | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| JARVIS-Mobile-3.1.0-debug.apk | br.com.jarvis.mobile / 3.1.0 (8) | 21044192 | `c811e8d1ea90968508ae6fbbd7485e8438ab766f5539c2c8a724d9bdefb8b8b3` |
| JARVIS-Voz-A-1.0.0-debug.apk | br.com.jarvis.voicea / 1.0.0 (1) | 143608608 | `b44f9a88d86a20501393474392fba947253f998c4cac0e348b2606bd8606aa4b` |

Compile/target SDK 35; ABIs arm64-v8a e x86_64. Assistente minSdk 26; motor minSdk 30 (Android 11+), compatível com o S25 FE. As seis bibliotecas do assistente e dez do motor passaram em alinhamento ELF/ZIP de 16 KB. apksigner verificou ambas as assinaturas no CI. Os downloads foram reconstruídos sem modificar os APKs; hashes, tamanhos, CRC, alinhamento e hash do modelo foram reconferidos localmente e correspondem ao CI.

Certificado comum SHA-1 `33469f56e8acbca2fd5cb341159c58711a6f775b`; SHA-256 `256a7b974d9a4d5c257acef5ebea5a728d20cda80e115b98a096ce63bad70f98`. **Assinatura debug temporária** nesta execução. Instale os dois APKs do mesmo par. Se a versão anterior tiver outro certificado, exporte as notas antes de desinstalar; desinstalação apaga dados locais. Nenhuma chave privada vai no repositório/artefato.

Os instrumentados recompilam a mesma revisão em outro job. Seu certificado temporário difere do par distribuído, mas também coincide entre assistente e motor desse job; regras/código/modelos são os mesmos. Não houve teste físico dos arquivos distribuídos no Galaxy. Os fontes GPL correspondentes do motor, incluindo o adaptador novo e fontes exatas das dependências, acompanham a entrega. [Licenças](../voice-engine/THIRD-PARTY.md).

## Limites e próximo teste

A compilação está desbloqueada. O executor local não tem Android SDK; compilação, Lint e testes Android foram executados no GitHub Actions. O verificador Python local só inspeciona estrutura e não foi usado como substituto.

No Galaxy S25 FE ainda é necessário validar timbre/dicção percebidos, latência, alto-falante/fones/Bluetooth/DND, reconhecimento pt-BR, permissões e interrupção ao sair/bloquear. Não há medição física de FPS, temperatura ou bateria. Runtime/fonemizador/DSP e saída física podem causar diferenças sutis em relação ao MP3 da audição; a identidade/pesos escolhidos permanecem os mesmos. [Roteiro de aceite](TESTES.md) · [Instalação pelo celular](APK-PELO-CELULAR.md).

Gmail real e autenticação continuam dependentes do registro OAuth/consentimento já documentado na 3.0; não foram conectados ou alterados nesta missão. Não houve clonagem de uma pessoa do vídeo, serviço pago, nova credencial embutida, publicação em loja ou release.
