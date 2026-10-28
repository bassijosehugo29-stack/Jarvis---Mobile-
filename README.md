# JARVIS Mobile 4.0 — Fase 1 · Prévia cinematográfica

Continuação do mesmo aplicativo Android em Kotlin/Jetpack Compose, na branch `jarvis-4-fase1`. A main e a instalação 3.1 permanecem preservadas. Núcleo azul elétrico com centro branco, anéis independentes, seis estados e resposta à amplitude de áudio disponível. A voz A aprovada, os três modos, Second Brain, notas, agenda, backups e demais centrais continuam no projeto. Nenhuma trilha definitiva nova foi integrada.

Sessões em primeiro plano retomam a escuta depois da resposta, sem toque a cada turno; sair/bloquear encerra a captura. O núcleo permite interromper por toque. Interrupção por voz é opcional e experimental, restrita a Android 13+, reconhecimento local pt-BR e fone com fio/USB. ACTION_ASSIST e seleção oficial de assistente estão integrados quando suportados pelo sistema. “Jarvis, abre o WhatsApp” usa os aplicativos instalados; ausência e ambiguidade são tratadas. Não há ativação “Ok Jarvis” com o app fechado.

**Prévia instalada ao lado da 3.1:** os pacotes têm sufixo `.v4preview`, porque a chave privada da entrega anterior não está disponível. **Não desinstale a 3.1 nem seu motor.** Os dados privados antigos continuam nela; a prévia tem armazenamento separado e não os importa automaticamente. Use apenas os dois APKs da mesma execução. A distribuição de teste é ARM64, adequada à arquitetura do Galaxy S25 FE.

[Escopo, validação, medições, downloads e limitações desta fase](docs/JARVIS-4-FASE-1.md). Os resultados históricos da 3.1 estão em [DOWNLOADS-APK.md](docs/DOWNLOADS-APK.md) e [VALIDATION.md](docs/VALIDATION.md); não são testes da 4.0. O teste físico no Samsung permanece pendente.

## O que está no código

- **Second Brain:** oito áreas, notas com UUID, título, conteúdo e data; edição, exclusão, tags, vínculos, busca e prioridades. Nada é memorizado automaticamente. Notas só entram no digest quando autorizadas individualmente.
- **Grafo nativo Compose:** categorias coloridas, núcleo, vínculos manuais ou por tags, toque para editar e zoom. Até 80 notas desenhadas; lista paginada em 50 itens, todas pesquisáveis.
- **Agenda:** leitura dos calendários visíveis do Android mediante permissão, hoje e até sete dias à frente, recorrências, dia inteiro e próximo compromisso. Criação de eventos anterior preservada.
- **Radar:** RSS públicos brasileiros da EBC, assuntos/palavras-chave, cache e abertura da matéria original. Sem conteúdo inventado quando a fonte falha.
- **Digest:** resumo local curto ou médio com dados disponíveis, fontes e horário. Primeira ativação manual do dia, comando “bom dia” ou botão DIGEST. Clima opcional por cidade via Open-Meteo, sem GPS.
- **Gmail:** interface e adaptador oficial somente leitura, classificação local AÇÃO/INFO/RUÍDO corrigível e atualização manual/periódica. **OAuth ainda depende do registro do aplicativo/assinatura e consentimento da conta; não houve conexão real em CI.** Nenhuma mensagem de demonstração aparece como sua.

[Missão 2 e limitações](docs/MISSAO-2.md) · [privacidade/permissões](docs/PRIVACIDADE.md) · [configuração Gmail](docs/GMAIL-OAUTH.md).

## Primeiro uso

1. Instale primeiro o APK **JARVIS Voz A** e depois **JARVIS Mobile**, da mesma entrega, seguindo o [guia pelo celular](docs/APK-PELO-CELULAR.md). Use PIN, padrão ou senha do Android para poder abrir a memória.
2. Escolha **Assistente → ajustes → A · Executivo clássico → Testar voz**. O modelo já está no APK do motor. Ajuste o volume de mídia; confira alto-falante/fones/Bluetooth. “Voz do Android” continua sendo uma alternativa explícita nos ajustes.
3. Toque no núcleo e permita microfone durante o uso. A primeira ativação diária pode apresentar o digest. Para comandos, diga “Jarvis, minha agenda” ou “Jarvis, abrir ChatGPT”. A opção “Primeira ativação do dia” fica no DIGEST.
4. Abra **⋮ → SECOND BRAIN**, autentique no Android e crie uma nota. Revise antes de salvar. Para ouvir dados pessoais é obrigatória uma voz local; sem ela, o texto permanece visível com orientação.
5. Em **AGENDA**, permita leitura quando quiser consultar seus calendários. Em **RADAR**, escolha assuntos e atualize. Configure clima apenas se desejar enviar a cidade ao Open-Meteo.
6. Gmail é opcional; leia o [guia OAuth](docs/GMAIL-OAUTH.md). O restante funciona sem Gmail.

O perfil A mantém o pitch natural do modelo aprovado, sem redução artificial, e permite ajustar a velocidade relativa ao ritmo base. Runtime, fonemizador e alto-falantes podem mudar detalhes em relação à audição. A opção “Voz do Android” usa os dados instalados do provedor; gênero/qualidade dependem dele. Não há clonagem de uma pessoa do vídeo.

## Três modos com escolha salva

| Modo | Comportamento |
| --- | --- |
| Silencioso | Respostas em texto, sem fala/música/efeitos do JARVIS; animações reduzidas. Microfone apenas por toque explícito, uma escuta por vez. |
| Assistente | Padrão, voz e efeitos discretos, animação moderada. Música opcional inicialmente desligada. |
| Cinematográfico | Núcleo e partículas completos, trilha original, efeitos, redução da música na fala e pausa durante captura. |

DataStore persiste modos, volumes, perfil vocal, assuntos e preferências do digest. Trocar de modo encerra a sessão em curso. Ao sair/bloquear/abrir outro aplicativo, captura, fala e trilha param e as notas são bloqueadas. Não há escuta oculta. O serviço Android de reconhecimento pode emitir seus próprios avisos; para não chamá-lo, use texto.

## Comandos

“Jarvis, minha agenda”, “o que tenho hoje”, “próximo compromisso”, “agenda da semana”, “meus e-mails”, “tem e-mail importante”, “notícias”, “notícias de tecnologia”, “bom dia”, “abrir Second Brain”, “adicionar uma nota”, “quais são minhas metas”. Esses comandos são resolvidos localmente e consultam somente as fontes autorizadas.

Continuam disponíveis abrir ChatGPT oficial (`com.openai.chatgpt`), abrir aplicativos instalados pelo nome, alarmes pelo relógio Android, criação confirmada de eventos e “parar”. Abrir um aplicativo não permite controlar automaticamente suas telas internas. Alarmes oficiais podem exigir confirmação; não há retorno de salvamento pelo intent.

Conversa livre local continua opcional: baixe por Wi-Fi o modelo gratuito de 491 MB ou importe o GGUF oficial. [Detalhes da conversa](docs/CONVERSA-0.5.md). Esse modelo pequeno pode errar; não recebe notas/e-mails automaticamente e não executa ações com o texto que gera. Não substitui o ChatGPT.

## Segurança e custos

Notas e caches pessoais têm payload AES-GCM com chaves não exportáveis do Android Keystore; o banco não é SQLCipher (UUIDs/tipos/datas de atualização são metadados). Second Brain pede biometria ou credencial do dispositivo e bloqueia ao sair. Exportação é manual pelo Android, com senha, PBKDF2 e AES-GCM; revise importações antes de confirmar. Backup automático Android dos dados é desativado.

Dados pessoais só usam vozes declaradas locais e instaladas pelo Android; sem fallback on-line. Ditado de notas exige reconhecedor no dispositivo, sem fallback para rede. Reconhecimento geral de comandos prioriza o dispositivo, mas o serviço Android pode usar rede; para conteúdo sensível use o editor ou ditado local. Telas pessoais bloqueiam captura; portanto prints dessas telas podem sair pretos por proteção.

Sem APIs pagas obrigatórias, chaves embutidas, backend, telemetria ou assinaturas. Tráfego próprio: modelo solicitado, RSS EBC, Open-Meteo se configurado e Gmail somente após OAuth. WorkManager pode atualizar e-mails autorizados aproximadamente a cada hora; Android decide quando executar, sem microfone. EBC/Open-Meteo podem estar indisponíveis. Open-Meteo gratuito é para uso pessoal não comercial, com atribuição CC BY 4.0.

Nenhuma loja, release pública ou serviço pago foi ativado. Actions usa runners padrão; repositórios privados estão sujeitos à franquia da conta, sem ativar cobrança.

## Compilar e verificar

Pelo celular: [GitHub Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → jarvis-4-fase1**. Aguarde os **três jobs verdes**, abra a execução e baixe os artifacts **JARVIS-Voz-A-4-Preview-APK** e **JARVIS-4-Fase1-APK**. Login no GitHub é necessário. Extraia cada ZIP em Meus Arquivos e instale primeiro `JARVIS-Voz-A-4-preview.apk`, depois `JARVIS-Mobile-4.0.0-fase1-preview.apk`. Não desinstale a 3.1. Retenção solicitada de 90 dias, sujeita ao limite do repositório. [Detalhes da fase 1](docs/JARVIS-4-FASE-1.md).

No computador: JDK 17, Platform/target/compile SDK 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1; Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose 2.1.20, KSP 2.1.20-2.0.1, Room 2.7.2. Android mínimo 26 no assistente e 30 no motor da voz A; ABIs arm64-v8a e x86_64. Configure ANDROID_HOME ou sdk.dir em local.properties. Não versione esse arquivo.

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew :app:testDebugUnitTest :voice-engine:testDebugUnitTest :app:lintDebug :voice-engine:lintDebug :app:assembleDebug :voice-engine:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/verify_apk.py
python3 scripts/bundle_voice_a_source.py
# Testes Android exigem emulador/dispositivo; o teste de conversa usa o modelo real:
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

Windows: `.\gradlew.bat`. CMake obtém llama.cpp em commit fixado durante compilação; nenhum binário executável é baixado pelo app. [Arquitetura](docs/ARQUITETURA.md), [testes](docs/TESTES.md) e [validação real](docs/VALIDATION.md).

**Assinatura:** sem os quatro Secrets opcionais de assinatura, o CI usa chave debug temporária. Instale o par da mesma execução. A prévia 4 tem pacote separado para preservar a 3.1; não remova a instalação antiga para contornar conflito de certificado. Atualizações futuras sobre o mesmo pacote exigem a mesma chave privada. Para Gmail e atualizações estáveis, configure uma chave fixa conforme o [guia](docs/GMAIL-OAUTH.md). Nenhuma chave privada é guardada no repositório ou artefato.

Projeto pessoal independente; marcas pertencem a seus titulares.
