# JARVIS 4.0 — Fase 1

Esta fase continua o projeto existente na branch `jarvis-4-fase1`. A main 3.1 permanece intacta. APKs e resultados finais desta fase ainda estão em validação; não confundir resultados históricos com a prévia.

## O que mudou

Núcleo circular com centro branco, luz azul elétrica, anéis concêntricos independentes, órbitas oblíquas e profundidade por composição de vetores. Sem vídeo de fundo, WebView ou efeito de blur pesado. Formação, repouso, escuta, processamento, fala e erro têm comportamentos distintos. Brushes, vetores e paths reutilizados por drawWithCache; o relógio da animação só invalida desenho.

O núcleo usa RMS fornecido pelo reconhecedor e um envelope de PCM16 do TextToSpeech quando disponível. A posição no PCM é estimada pelo início da reprodução; não mede o som físico do alto-falante nem garante sincronia labial. Voz sem callbacks PCM compatíveis usa pulsação visual decorativa durante a fala. Nenhuma amostra de áudio é gravada: somente níveis temporários, com limite de memória.

Animação prevista: 12 fps em repouso Assistente, 20 em repouso Cinema, até 30 em estados ativos. Oculto ou animações do Android desativadas: sem relógio. Silencioso em repouso: estático. Isso é cadência programada, não promessa de FPS físico.

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

FrameMetrics/CPU/PSS são coletados em estados visuais sintéticos aquecidos; startup/idle são comparados com a 3.1 em instalações novas no mesmo emulador. Amostras são curtas, incluem o ambiente de teste e não são benchmark do Samsung. Arquivos brutos acompanham a entrega. Batterystats virtual não mede mAh, autonomia ou temperatura reais. A medição comparativa recusa execução fora do CI e em dispositivo que não seja emulador, pois recria exclusivamente os fixtures do runner.

## Compilar esta fase

JDK 17, Android SDK 35, NDK 27.2.12479018, CMake 3.22.1 e Gradle 8.11.1 conforme a base.

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew -PjarvisAbis=arm64-v8a :app:assemblePreview :voice-engine:assemblePreview
```

Para emulador debug, não fixe jarvisAbis: o padrão inclui arm64-v8a e x86_64. O workflow desta branch publica dois artifacts separados, fontes GPL e demonstração/métricas, e baixa novamente os APKs para conferir seus hashes/assinaturas. Pelo celular use Actions → Android - APK de teste → Run workflow → **jarvis-4-fase1**. Login no GitHub é necessário; cada artifact é um ZIP com um APK. Use o par da mesma execução e instale o motor da prévia antes do assistente se quiser a voz A.

Fonte e resultados efetivos serão registrados após o término do CI. Não há teste físico concluído no Galaxy S25 FE nesta fase.
