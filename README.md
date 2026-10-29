# JARVIS Mobile — Escuta inteligente

Continuação do mesmo Android Kotlin/Compose, com o núcleo holográfico azul, voz A, três modos e centrais preservados. A branch `fix/escuta-inteligente-20261029` parte da revisão existente `14acd222` e corrige o encerramento precoce entre pausas: resultados parciais revisáveis, junção de segmentos, janela de continuação de 3,3 s e confirmação única antes de responder/executar. Temporizadores e respostas atrasadas são invalidados por geração.

[Implementação, auditoria, limites e instalação](docs/ESCUTA-INTELIGENTE.md). O ZIP CyberBot informado não está acessível nesta tarefa; não foi executado ou declarado auditado. A base Android acessível usa conversa local, não Gemini; nenhuma integração/credencial Gemini foi inventada. Há um contrato de provedor para futura integração segura, sem chaves no APK.

**Compilar e baixar:** acompanhe o pull request e seus checks ou Actions → Android - APK de teste → Run workflow → branch `fix/escuta-inteligente-20261029`. O workflow publica `JARVIS-Escuta-APK` e `JARVIS-Voz-A-Escuta-APK`, além de testes, Lint, fontes do motor e comprovante de downloads. Login GitHub necessário; cada artifact contém um ZIP com um APK. Resultados efetivos e links da execução ficam no PR e nos artifacts, sem presumir aprovação antes do CI.

**Instale sem remover versões anteriores.** Os pacotes de teste `.escutapreview` ficam ao lado da 3.1/4 e usam armazenamento separado. Baixe o par da mesma execução, extraia em Meus Arquivos, instale primeiro a voz e depois o assistente. Notas/configurações antigas permanecem nas instalações anteriores e não são importadas automaticamente.

Os relatórios [da fase 4 anterior](docs/JARVIS-4-FASE-1.md) e [da 3.1](docs/DOWNLOADS-APK.md) são históricos. Esta revisão não declara teste físico no Galaxy S25 FE nem melhora acústica/latência/bateria sem medição. A geração local opcional continua limitada pelo modelo pequeno. A voz A mantém pitch/modelo aprovados; respostas longas evitam cortes no meio de abreviações/títulos.

## O que está no código

- **Second Brain:** oito áreas, notas com UUID, título, conteúdo e data; edição, exclusão, tags, vínculos, busca e prioridades. Nada é memorizado automaticamente. Notas só entram no digest quando autorizadas individualmente.
- **Grafo nativo Compose:** categorias coloridas, núcleo, vínculos manuais ou por tags, toque para editar e zoom. Até 80 notas desenhadas; lista paginada em 50 itens, todas pesquisáveis.
- **Agenda:** leitura dos calendários visíveis do Android mediante permissão, hoje e até sete dias à frente, recorrências, dia inteiro e próximo compromisso. Criação de eventos anterior preservada.
- **Radar:** RSS públicos brasileiros da EBC, assuntos/palavras-chave, cache e abertura da matéria original. Sem conteúdo inventado quando a fonte falha.
- **Digest:** resumo local curto ou médio com dados disponíveis, fontes e horário. Primeira ativação manual do dia, comando “bom dia” ou botão DIGEST. Clima opcional por cidade via Open-Meteo, sem GPS.
- **Gmail:** interface e adaptador oficial somente leitura, classificação local AÇÃO/INFO/RUÍDO corrigível e atualização manual/periódica. **OAuth ainda depende do registro do aplicativo/assinatura e consentimento da conta; não houve conexão real em CI.** Nenhuma mensagem de demonstração aparece como sua.

[Missão 2 e limitações](docs/MISSAO-2.md) · [privacidade/permissões](docs/PRIVACIDADE.md) · [configuração Gmail](docs/GMAIL-OAUTH.md).

## Primeiro uso

1. Instale primeiro o APK **JARVIS Voz A** e depois **JARVIS Mobile**, da mesma entrega, seguindo o [guia da prévia](docs/ESCUTA-INTELIGENTE.md). Use PIN, padrão ou senha do Android para poder abrir a memória.
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

Pelo celular: [GitHub Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → fix/escuta-inteligente-20261029**. Aguarde os **três jobs verdes**, abra a execução e baixe os artifacts **JARVIS-Voz-A-Escuta-APK** e **JARVIS-Escuta-APK**. Login no GitHub é necessário. Extraia cada ZIP em Meus Arquivos e instale primeiro `JARVIS-Voz-A-escuta-preview.apk`, depois `JARVIS-Mobile-4.0.1-escuta-preview.apk`. Não desinstale a 3.1. Retenção solicitada de 90 dias, sujeita ao limite do repositório. [Detalhes da fase 1](docs/ESCUTA-INTELIGENTE.md).

No computador: JDK 17, Platform/target/compile SDK 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1; Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose 2.1.20, KSP 2.1.20-2.0.1, Room 2.7.2. Android mínimo 26 no assistente e 30 no motor da voz A; ABIs arm64-v8a e x86_64. Configure ANDROID_HOME ou sdk.dir em local.properties. Não versione esse arquivo.

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew :app:testDebugUnitTest :voice-engine:testDebugUnitTest :app:lintDebug :voice-engine:lintDebug :app:assembleDebug :voice-engine:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/verify_apk.py
python3 scripts/bundle_voice_a_source.py
# Testes Android automatizados exigem emulador descartável (o script recusa aparelho físico); o teste de conversa usa o modelo real:
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

Windows: `.\gradlew.bat`. CMake obtém llama.cpp em commit fixado durante compilação; nenhum binário executável é baixado pelo app. [Arquitetura](docs/ARQUITETURA.md), [testes](docs/TESTES.md) e [validação real](docs/VALIDATION.md).

**Assinatura:** sem os quatro Secrets opcionais de assinatura, o CI usa chave debug temporária. Instale o par da mesma execução. A prévia 4 tem pacote separado para preservar a 3.1; não remova a instalação antiga para contornar conflito de certificado. Atualizações futuras sobre o mesmo pacote exigem a mesma chave privada. Para Gmail e atualizações estáveis, configure uma chave fixa conforme o [guia](docs/GMAIL-OAUTH.md). Nenhuma chave privada é guardada no repositório ou artefato.

Projeto pessoal independente; marcas pertencem a seus titulares.
