# JARVIS 4.0 — Fase 1

Esta fase continua o projeto existente na branch `jarvis-4-fase1`. A main 3.1 permanece intacta. A prévia ARM64 foi compilada, assinada, publicada e baixada novamente para conferência. Os três jobs da [execução 40751349413](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413) terminaram com sucesso em 28/10/2026. Fonte compilada: `6aef8bdcd8c2aa7ca9285baa809e90a46e8b2f50`. Este relatório trata da prévia 4; os relatórios 3.1 permanecem históricos.

## Downloads pelo celular

É necessário login no GitHub no Chrome. Cada artifact baixa um ZIP com um APK. Extraia em Meus Arquivos, instale primeiro a voz da prévia e depois o assistente. **Mantenha a 3.1 e seu motor instalados.**

| Download | APK dentro do ZIP | Tamanho do APK |
| --- | --- | ---: |
| [JARVIS Voz A · Prévia 4](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12845160681) | JARVIS-Voz-A-4-preview.apk | 107.412.527 bytes (107,4 MB) |
| [JARVIS Mobile 4 · Fase 1](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12845130698) | JARVIS-Mobile-4.0.0-fase1-preview.apk | 16.535.550 bytes (16,5 MB) |

Disponíveis até **26/01/2027 às 19:06:54 UTC**, conforme a retenção atual de 90 dias. [Comprovante de download](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12844183820) · [fontes correspondentes do motor](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12845155688).

SHA-256 do assistente: `298ce61906b4b86b726293e4dd3bfb6edcb3e57f58ad61afbf387cf6ce146d9c`.
SHA-256 da voz: `aa911da7abacae01ae7b265cd0bb15a3b00f5008e1a13a963053dd1a62585ae3`.
Certificado SHA-256 de ambos: `dd6772291a36f060694644ea3f5bd1e24e1b88aacbee223634f9896ecb46aa6b` (chave de debug temporária). As assinaturas foram verificadas com apksigner antes do upload, e o job final comprovou que os dois APKs baixados são idênticos aos assinados. CRC/estrutura do APK do assistente também foram conferidos depois do download externo. Bibliotecas ARM64 e alinhamento ZIP/ELF de 16 KB verificados. Sem classes/serviços das fixtures de teste no APK distribuído.

## Capturas e demonstração

[Baixar capturas, dois vídeos e medições](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12843874193). `screenshots/jarvis-v4-main-real.png` é a tela real da MainActivity em uma instalação nova no emulador Android 35. `jarvis-hud.mp4` mostra a tela real por seis segundos. `jarvis-v4-estados.mp4` demonstra os seis estados com sinal de áudio **simulado**, sem reproduzir uma conversa real; a tela contém essa identificação. As capturas `jarvis-v4-real-<estado>-cinematic.png` também são demonstrações explicitamente simuladas. Nenhuma é captura de um Galaxy S25 FE.

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

## Resultado efetivo da validação

- **93 testes de unidade:** 88 no assistente e 5 no motor; zero falhas e zero ignorados.
- **48 testes Android 35 x86_64:** zero falhas e zero ignorados, em 5min39s. [Relatórios Android e áudios de fixtures públicas](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12843834470).
- **Lint da variante entregue:** zero erros e 44 avisos (33 app + 11 motor). Debug também teve zero erros; não se somam avisos duplicados entre variantes. [Relatórios de unidade, Lint e verificação dos APKs](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40751349413/artifacts/12844376225).
- Testes reais do motor A geraram PCM a partir do modelo aprovado, incluindo frases novas; callback de amplitude positiva conferido. O motor não tem permissão de rede ou microfone. Isso não comprova audição/rota acústica no telefone.
- O fluxo de dois turnos usou ASR de fixture e TTS Android de teste; comprovou retomada automática, interrupção ao sair e descarte de resultado tardio. Não valida reconhecimento acústico de microfone.
- O modelo de conversa livre real foi testado no modo Silencioso com duas mensagens e contexto. Levou 2min40s no emulador; o conteúdo continha o nome da fixture nos dois turnos. A resposta usou o pronome de modo inadequado (“Meu nome é Rafael”), um limite do modelo pequeno. Esse teste não valida a combinação acústica do modelo com a voz A nem qualidade de diálogo aberto.
- ACTION_ASSIST resolveu a MainActivity e a abriu oficialmente pelo Android (`Status: ok`); testes verificaram ausência de captura automática. Interrupção simultânea foi recusada em alto-falante conforme a política. Parsing do comando WhatsApp e resolução/ausência cobertos; não houve WhatsApp oficial instalado nesse emulador.
- Logs coletados de AndroidRuntime e crashes nativos do app estão vazios. Isso não equivale a garantia de ausência de bugs.
- Shell validado; proteção do script de testes contra aparelho físico comprovada com adb falso, recusando antes de instalar/instrumentar. A medição comparativa também exige CI e emulador antes de recriar os fixtures.

## Medições e pendências de desempenho

Medições reais do mesmo emulador Android 35 x86_64, perfil Pixel 6 com SwiftShader (GPU emulada por software), instalações novas. Três inicializações por versão e amostras curtas de cinco segundos em repouso Assistente. O modelo de conversa não estava carregado; sem captura ou síntese em curso. O desempenho medido **não confirmou a fluidez desejada**, e não se afirma melhoria de CPU/frames.

| Métrica | Base 3.1 | Prévia 4 |
| --- | ---: | ---: |
| Mediana TotalTime de am start -W | 3.254 ms | 3.500 ms |
| PSS do processo principal | 79,5 MiB | 91,5 MiB |
| CPU do processo, percentual de um núcleo | 36,8% | 80,2% |
| Frame p95 (timestamps gfxinfo) | 76,6 ms | 376,7 ms |
| APK assistente distribuído | 21.044.192 bytes | 16.535.550 bytes |
| APK motor A distribuído | 143.608.608 bytes | 107.412.527 bytes |

A distribuição 4 contém ARM64 para o Galaxy; a anterior também trazia x86_64. Os modelos/qualidade da voz foram mantidos. Não atribuir a redução de APK apenas à renderização.

Os sete estados visuais (seis Cinema mais repouso Silencioso) têm CPU/PSS/frames registrados separadamente no JSON. No repouso Silencioso, foram desenhados somente dois frames durante a amostra de 2,2 s, com relógio do núcleo desligado; houve 12,7% de CPU do processo incluindo HUD/ambiente. Os arquivos brutos de meminfo, gfxinfo, batterystats e startup estão no artifact visual. Frames com timestamps inválidos ou duração fora de 0–1000 ms não entram nos percentis do coletor; são amostras de diagnóstico, não benchmark de publicação. CPU não inclui compositor, sistema ou processo separado da voz.

A geometria reutilizada, agrupamento de marcadores e redução de cadência foram implementados, porém **não se considera a otimização de fluidez encerrada** diante desses números. Precisam de perfil em GPU real/Android 16 no Galaxy para orientar o próximo ajuste. Não houve medição física de autonomia, mAh, temperatura, reconhecimento, fones ou rota de som. Batterystats virtual serve só como diagnóstico.

As tentativas anteriores 58bc2ec/9fe2eeb registraram custos altos de desenho e dois timeouts de 100 s no teste do modelo local, além de ANR do Pixel Launcher em uma captura do emulador. O teste passou a isolar geração em Silencioso e a verificar término do trabalho antes do conteúdo, mantendo o limite e a exigência de contexto. Voz real e retomada de sessão seguem em testes próprios; não se atribui a causa dos timeouts apenas ao launcher. Os resultados finais acima correspondem exclusivamente à fonte 6aef8bd desta execução.

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

Fonte compilada e resultados efetivos registrados acima. Não há teste físico concluído no Galaxy S25 FE nesta fase.
