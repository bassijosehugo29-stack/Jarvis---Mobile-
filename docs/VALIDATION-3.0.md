# Validação — JARVIS Mobile 3.0.0

## Resultado confirmado

- [Execução 40678656106](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106), em 28/10/2026: **debug-apk** e **Testes instrumentados Android 35** concluídos com success.
- Fonte validada: `5b4a37e193fce7411017b8aa37e490ffc95ce425`. Atualizações posteriores apenas de Markdown não alteram esse APK; o workflow ignora pushes só de documentação.
- **80 testes de unidade + 40 instrumentados = 120, zero falhas e zero ignorados/pulados**. Unidade: 2.354s; suite Android: 1m50.45s no emulador API 35/x86_64. Essas durações não medem desempenho do Galaxy.
- testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest e connectedDebugAndroidTest aprovados. Regressões da Missão 1 foram preservadas.
- **Lint: zero erros, 29 avisos** sobre atualizações de versões/target e sugestões KTX. Não foi criado baseline para esconder erros.
- [APK de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106/artifacts/12812505712) · [relatórios de unidade, Lint, assinatura e fontes](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106/artifacts/12812350825) · [relatórios Android e evidências](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106/artifacts/12811757747). Artefatos retidos por sete dias.

## O que os testes comprovaram

Notas: categorias/consentimento, pesquisa, ordenação, vínculos, merge por data, Room real, armazenamento criptografado, reabertura, CRUD e bloqueio do repositório. Backup: criptografia autenticada, senha incorreta/adulteração rejeitadas, prévia sem salvar e importação sem alteração parcial. Desconectar/apagar invalida atualizações atrasadas de cache e OAuth; reautorizar a mesma conta mantém correções locais. Exclusão remove registros/chaves locais, sem alterar mensagens/calendários originais.

Grafo/editor: Compose nativo, salvar/excluir/buscar, memória bloqueada sem exibir conteúdo, lote sintético de 1.000 notas com paginação/limite visual. Lifecycle da Activity fecha sessão e descarta rascunho ao sair; FLAG_SECURE presente. O teste autentica o estado de fixture diretamente para verificar UI/lifecycle: **não valida impressão digital ou PIN físico**.

Agenda: consultas de calendário LOCAL isolado somente leitura, expansão de três recorrências, dia inteiro UTC/fim exclusivo, exclusão de cancelados e contagem do provider inalterada. Testes antigos de criação verificada de evento também passaram. Não se testou uma conta Google Agenda real, apenas o provider oficial que expõe agendas já sincronizadas.

Notícias: RSS/Atom, datas desconhecidas/futuras, links seguros, DTD recusada, cache/TTL/ETag/304, deduplicação, filtros e fallback offline. Clima: opt-in, dados fornecidos, probabilidade de chuva desconhecida permanece nula, cache e desativação. Transporte fixture usa Room real; disponibilidade externa é verificada separadamente.

Digest e e-mail: datas/fatos/limites de texto, ausência de fonte distinta de agenda vazia, somente notas autorizadas, identificação de cache, parsing e classificação local com dados sintéticos. Comando de e-mail pela Activity usa o mesmo roteador e permanece sem som/microfone no Silencioso, sem simular conta conectada. **OAuth, mensagens e sincronização de conta Gmail real não foram validados**: configuração e consentimento permanecem necessários.

Voz: SpeechOutput com API TTS real e mecanismo Java de teste gera PCM, callbacks de início/palavra/conclusão e parâmetros de velocidade/tom. Voz declarada on-line, mesmo após engine aquecida, é recusada para resposta pessoal e resulta em texto. Voz local de fixture produz áudio. O mecanismo é exclusivamente do APK de androidTest, não está no APK entregue e produz um tom, não voz humana. Modos, silêncio, DataStore, AudioTrack, duck e perda de foco passaram nos testes preservados.

Conversa livre: importação do modelo oficial com SHA-256 conferido, inferência JNI real e contexto com nome fictício Rafael em duas mensagens. O modelo respondeu literalmente “Meu nome é Rafael” em ambos os turnos: isso valida geração/contexto, **não qualidade linguística ou compreensão geral**. O modelo pequeno pode errar pronomes e conteúdo. Não recebe notas/e-mails e não executa ações pelo texto gerado.

O arquivo AndroidRuntime de erros coletado ao fim dos testes está vazio. Captura inicial da Activity foi inspecionada: HUD e seletor preservados, centrais em ⋮, sem atalhos de apps. Capturas/vídeo são do emulador. Algumas capturas históricas após troca de modo podem omitir camadas estáticas; [limite já documentado](VALIDATION-0.6.md). Telas pessoais bloqueiam captura por projeto e não são desprotegidas no debug. Não se promete perfeição visual no hardware físico a partir de um print.

## Integridade do APK entregue

- Pacote `br.com.jarvis.mobile`, versionName 3.0.0, versionCode 7, minSdk 26, compile/target SDK 35; arm64-v8a e x86_64.
- **21044164 bytes**; SHA-256 `d82c0b6b2f65bae54ae2d44ecaf0a0c3e5fe2614e3968f62f827d721699772c4`.
- Certificado SHA-1 `49a2836a3c5676f625747bd16a1ffb410144e897`; SHA-256 `9d62385b20aff3bec86a2aa061a9ae8e3d566247d86046845abea340e746f820`.
- **Assinatura debug temporária** nesta execução: não há Secrets de assinatura fixa configurados. Atualização com outro certificado exige exportar notas/desinstalar/reinstalar. [Guia e OAuth](GMAIL-OAUTH.md).
- apksigner verificou assinatura criptográfica; zipalign verificou ZIP/16 KB. CRC, hash e alinhamento ELF de todas as bibliotecas de 64 bits também foram conferidos localmente após download, com correspondência ao relatório do CI.
- Modelo GGUF não embutido; classes FixtureTtsService/FixtureTtsControl ausentes do APK entregue. Nenhuma chave privada/token é artefato.
- Os testes instrumentados recompilam a mesma revisão em outro job; sem assinatura fixa, seu certificado debug é diferente do APK distribuído. As regras/código são os mesmos; não houve teste físico desse arquivo no Galaxy.

## Disponibilidade e bloqueios

Checagem em `2026-10-28T11:17:21.010568+00:00`: os três feeds oficiais da EBC responderam HTTP 200 com dez itens cada. Open-Meteo respondeu HTTP 200 para a localização pública de teste São Paulo, com previsão realmente fornecida. Isso comprova disponibilidade nesse instante; não garante serviço contínuo e não configura clima pessoal no app.

**Gmail real:** registrar cliente OAuth Android com pacote e SHA-1 do APK instalado, ativar Gmail API, configurar consentimento/usuário de teste e autorizar a própria conta. Recomenda-se assinatura fixa por Secrets antes disso para não trocar a identidade em cada build. [Menor caminho para conectar](GMAIL-OAUTH.md). O escopo gmail.readonly é restrito; publicação ampla pode exigir verificação Google. Não foi solicitado serviço pago, senha principal ou infraestrutura comercial.

**Samsung Galaxy S25 FE:** ainda validar ASR/TTS pt-BR instalado, voz física, biometria/PIN, alto-falante/Bluetooth/DND, permissões, pausa ao bloquear, aparência/fluidez/bateria e a conta real. Sem medições de FPS, autonomia ou temperatura. [Roteiro de aceite](TESTES.md). Operação geral de reconhecimento depende do serviço Android; ditado de notas recusa fallback para rede.

Sem bloqueio de compilação nesta revisão. Não há SDK Android no executor local: o CI executou a compilação/testes; o script Python de estrutura não foi apresentado como substituto. Nenhum aplicativo foi publicado em loja ou release, nenhum serviço pago/chave de API foi adicionado. [Validação histórica da 0.6](VALIDATION-0.6.md).
