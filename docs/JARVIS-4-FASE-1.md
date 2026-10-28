# JARVIS 4.0 — Fase 1

Esta fase continua o projeto existente na branch `jarvis-4-fase1`. A main 3.1 permanece intacta. APKs e resultados finais desta fase ainda estão em validação; não confundir resultados históricos com a prévia.

## O que mudou

Núcleo circular com centro branco, luz azul elétrica, anéis concêntricos independentes, órbitas oblíquas e profundidade por composição de vetores. Sem vídeo de fundo, WebView ou efeito de blur pesado. Formação, repouso, escuta, processamento, fala e erro têm comportamentos distintos. Brushes, vetores, estilos de traço, arcos e contornos reutilizados por drawWithCache; o relógio da animação só invalida desenho em uma camada do núcleo. Dois paths de marcadores substituem 96 comandos separados por frame. Os arcos giram por transformações e os contornos orgânicos pulsam sem reconstruir os vértices a cada atualização. O modo Assistente usa duas passagens de brilho e o Cinema preserva três.

O núcleo usa RMS fornecido pelo reconhecedor e um envelope de PCM16 do TextToSpeech quando disponível. A posição no PCM é estimada pelo início da reprodução; não mede o som físico do alto-falante nem garante sincronia labial. Voz sem callbacks PCM compatíveis usa pulsação visual decorativa durante a fala. Nenhuma amostra de áudio é gravada: somente níveis temporários, com limite de memória.

Cadência programada: 8 atualizações/s em repouso Assistente, 10 durante processamento Assistente para priorizar inferência, 20 em repouso Cinema, até 30 durante escuta/fala e processamento Cinema. Oculto ou animações do Android desativadas: sem relógio. Silencioso em repouso: estático. Isso é cadência programada, não promessa de FPS físico.

Respostas faladas continuam pelo SpeechOutput compartilhado. Texto longo recolhe em três linhas nos modos de voz, com leitura completa por toque; Silencioso mantém texto integral. Sessão ativa alterna fala e escuta sem novo toque a cada frase. Pausa automática após 90 segundos sem solicitação reconhecida, fora de fala/processamento; sair ou bloquear encerra a captura.

O núcleo pode interromper uma resposta e retomar a escuta da sessão. Interrupção por voz é opcional e desligada inicialmente: Android 13+, reconhecimento local pt-BR e rota de fone com fio/USB. Durante a fala, a frase exata “Jarvis, pare” ou “Jarvis, escute” solicita interrupção. Falha do reconhecedor ou troca para alto-falante desativa esse detector. Não é escuta livre simultânea em toda saída de som; a validação acústica em fones reais continua pendente.

O motor A conserva modelo/pitch e permanece substituível pela opção Android. Modelos continuam separados: conversa livre local de aproximadamente 491 MB é opcional, e o motor A é um APK à parte. Não foi incorporada trilha definitiva; o ambiente original anterior permanece experimental e opcional.

## Android e WhatsApp

MainActivity recebe ACTION_ASSIST. O botão “Usar como assistente do Android” pede a função oficial via RoleManager/ActivityResult quando disponível, com alternativa para as configurações do fabricante. O gesto do sistema abre o app; a captura depende de toque e permissão. Não há hotword, serviço residente de voz, acessibilidade ou Ok Jarvis com o aplicativo fechado.

“Jarvis, abre o WhatsApp” passa pelo parser local, consulta launchers instalados e abre o componente exportado identificado pelo Android. Ausência/ambiguidade têm resposta explícita. WhatsApp Business tem alias separado. Não automatiza telas nem envia mensagens. Pasta Segura/perfil de trabalho podem limitar a visibilidade de apps.

Referências oficiais: [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager#ROLE_ASSISTANT), [ACTION_ASSIST](https://developer.android.com/reference/android/content/Intent#ACTION_ASSIST), [limites de SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [rotas de áudio](https://developer.android.com/reference/android/media/AudioManager#getAudioDevicesForAttributes(android.media.AudioAttributes)). O gesto/seleção real da One UI e o WhatsApp instalado no Galaxy ainda precisam de aceite físico.

## Dados e assinatura da prévia

Não desinstale a 3.1 nem o motor atual. Como a assinatura antiga do CI é temporária e sua chave privada não está disponível, não há atualização segura sobre aquele APK sem a chave original. Esta entrega é uma variante de teste do mesmo código: `br.com.jarvis.mobile.v4preview`, com motor `br.com.jarvis.voicea.v4preview`.

A prévia fica ao lado da instalação atual, sem acesso automático às notas, calendários, cache, tokens ou preferências privados dela. Os dados originais permanecem na 3.1. O código Room, esquema, backups, segurança, centrais e formatos de importação não foram removidos ou alterados. Um backup criptografado de notas pode ser importado manualmente na prévia; não existe migração oculta. Acesso ao CalendarProvider depende de permissão própria; OAuth precisa corresponder ao pacote/certificado da prévia.

Compilação normal `debug` mantém o pacote original e exige a chave original para atualizar uma instalação existente sem perda. Não se deve desinstalar o app para contornar essa falta de chave. A variante ARM64 distribuída prioriza o Galaxy S25 FE.

## Testes e medições

Novos testes cobrem o contrato de animação/níveis PCM, comando WhatsApp, rejeição de eco, ciclo de dois turnos com entrada ASR fixture e TTS real de teste, integração ACTION_ASSIST e recusa do detector em alto-falante. O teste Faber também exige amplitude PCM real chegando ao callback visual. Isso não valida um microfone/fone físico nem qualidade de conversa aberta.

CoreShowcaseActivity só existe no debug de CI e é excluída da prévia entregue. Suas capturas e vídeo de estados são rotulados como demonstração com sinal simulado. A captura da tela inicial e o vídeo curto da MainActivity são do app real no emulador.

Frames reais do compositor (gfxinfo), CPU do processo e PSS são coletados em estados visuais sintéticos fora do relógio virtual do Compose; startup/idle são comparados com a 3.1 em instalações novas no mesmo emulador. Startup usa TotalTime de am start -W (primeiro desenho, não readiness do reconhecedor/modelo); PSS/CPU são medidos sem modelo de conversa carregado e sem captura ou síntese em curso. CPU é percentual de um núcleo e não inclui o processo do motor da voz, compositor, serviços do sistema ou consumo total do telefone. Amostras são curtas, incluem o ambiente de teste e não são benchmark do Samsung. Arquivos brutos acompanham a entrega. Batterystats virtual não mede mAh, autonomia ou temperatura reais. A medição comparativa recusa execução fora do CI e em dispositivo que não seja emulador, pois recria exclusivamente os fixtures do runner.

## Teste no Galaxy (pendente)

1. Mantenha a 3.1 instalada. No Chrome com login no GitHub, baixe o par **da mesma execução**, extraia os ZIPs em Meus Arquivos e instale primeiro a Voz A da prévia e depois JARVIS 4 · Prévia. Autorize a instalação pelo Android se solicitado. O armazenamento da prévia é separado; notas da 3.1 não aparecerão nela automaticamente.
2. Em Assistente → configurações, selecione A · Executivo clássico e use Testar voz; ajuste o volume de mídia. Se preferir, escolha explicitamente uma voz Android instalada.
3. Toque no núcleo, permita microfone durante o uso e diga “Jarvis”. Confira a resposta falada, depois faça outra pergunta sem novo toque. A conversa livre exige o modelo local opcional; comandos Android são resolvidos localmente.
4. Diga “Jarvis, abre o WhatsApp”. Com o app instalado no mesmo perfil, ele deve abrir; ao sair do JARVIS, a captura deve parar. Sem WhatsApp instalado, a resposta deve informar a ausência. Não remova aplicativos ou dados para testar essa condição.
5. Durante uma resposta, toque no núcleo: a fala deve parar e a sessão retomar a escuta. A interrupção por voz requer adesão nos ajustes, Android 13+, fone com fio/USB e reconhecedor local pt-BR disponível. Experimente “Jarvis, pare”; não se presume suporte por Bluetooth ou alto-falante.
6. Compare Silêncio, Assistente e Cinema. Silêncio deve manter respostas em texto e nenhum áudio próprio. Cinema deve mostrar partículas e movimentos completos. Música anterior é experimental/opcional, sem trilha definitiva nova.
7. Use “Usar como assistente do Android” e confira as opções da One UI. O gesto, quando atribuído ao JARVIS, deve abrir a tela; a escuta ainda depende de ação explícita. Não há hotword com o aplicativo fechado.
8. Volte à tela inicial ou bloqueie o telefone. A captura e a reprodução devem encerrar. Fluidez, aquecimento, autonomia e reconhecimento acústico precisam ser avaliados no aparelho; nenhuma dessas verificações físicas foi concluída pelo CI.

## Compilar esta fase

JDK 17, Android SDK 35, NDK 27.2.12479018, CMake 3.22.1 e Gradle 8.11.1 conforme a base.

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew -PjarvisAbis=arm64-v8a :app:assemblePreview :voice-engine:assemblePreview
```

Para emulador debug, não fixe jarvisAbis: o padrão inclui arm64-v8a e x86_64. O workflow desta branch publica dois artifacts separados, fontes GPL e demonstração/métricas, e baixa novamente os APKs para conferir seus hashes/assinaturas. Pelo celular use Actions → Android - APK de teste → Run workflow → **jarvis-4-fase1**. Login no GitHub é necessário; cada artifact é um ZIP com um APK. Use o par da mesma execução e instale o motor da prévia antes do assistente se quiser a voz A.

A rodada 58bc2ec passou em 93 testes de unidade e 48 instrumentados, mas as amostras de renderização mostraram custo maior em repouso: 74,96% de um núcleo e frame p95 de 202,71 ms no emulador SwiftShader. Esses resultados motivaram o cache de geometria e o agrupamento de marcadores. A primeira tentativa da rodada otimizada 9fe2eeb passou nos 93 testes de unidade e em 47 dos 48 testes Android. A segunda resposta do teste do modelo opcional local excedeu o limite de 100 segundos; não houve crash Java ou nativo do JARVIS. A captura jarvis-cinema.png dessa tentativa mostra um diálogo de ANR do Pixel Launcher, um problema do emulador; a imagem não será apresentada como demonstração limpa do app. Foi solicitada uma única repetição do job Android com o mesmo código, preservando o registro da falha. A repetição também excedeu o prazo, desta vez na primeira resposta, e os outros 47 testes Android passaram. Por isso o teste de geração passou a configurar explicitamente o modo Silencioso, a aguardar o término real do trabalho antes de verificar o conteúdo e a exportar diagnóstico da fixture pública em caso de falha; voz real e retomada de sessão continuam em testes próprios. O limite de 100 segundos e a exigência de preservar o nome Rafael nos dois turnos não foram relaxados. A política do modo Assistente reduz o relógio em repouso/processamento para reservar recursos; Cinema conserva a apresentação completa. O script de testes recusa aparelhos físicos antes de qualquer instalação e encerra o Pixel Launcher no emulador antes dos testes. A nova rodada e suas métricas serão registradas após o CI; não se infere fluidez física do Galaxy a partir de emulador. Não há teste físico concluído no Galaxy S25 FE nesta fase.
