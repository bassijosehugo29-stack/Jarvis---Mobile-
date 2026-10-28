# Missão 2 — implementação 3.0

Estado: compilação/testes em andamento. Esta documentação descreve o código implementado, sem afirmar validação em aparelho físico. Os resultados finais ficam em [VALIDATION.md](VALIDATION.md).

## Memória escolhida pelo usuário

Room armazena notas nas oito áreas: Você, Metas, Carreira, Projetos, Finanças, Aprendizado, Saúde e Relações. Cada nota possui UUID, título, conteúdo, categoria, updatedAt, tags, links, prioridade e autorização individual de digest (desligada inicialmente). Pesquisa normaliza caixa/acentos e inclui conteúdo/tags; filtro por categoria e ordem prioridade/data. Conversa comum não grava nem consulta notas automaticamente. “Adicionar uma nota” abre um rascunho; ditado privado exige serviço de reconhecimento no dispositivo e confirmação no editor.

Abertura do Second Brain pede BiometricPrompt (biometria forte ou credencial Android no API 30+) ou credencial de bloqueio nos APIs 26–29. Sem bloqueio de tela, o acesso não é liberado. A autenticação protege a sessão da interface e do repositório; a chave AES não está vinculada a um CryptoObject biométrico. Ao deixar o app, cancela trabalhos, limpa rascunhos e memória da interface e fecha a sessão. O conteúdo não participa de savedInstanceState, índices FTS, logs ou backups Android.

Backup manual pelo Storage Access Framework: envelope JARVISB1, PBKDF2-HMAC-SHA256/600.000 iterações, salt aleatório de 16 bytes e AES-256-GCM com IV aleatório de 12 bytes e autenticação. Senha de pelo menos oito caracteres, não salva; use uma senha longa exclusiva. Limite 50 MiB/10.000 notas. Apenas notas/vínculos/autorizações/prioridades entram; nenhum token, e-mail ou calendário. Importar autentica e valida tudo, mostra quantidade/política e só mescla após confirmação. UUID igual mantém versão local quando igual ou mais nova; transação Room impede importação parcial. O usuário é informado de que permissões de digest do backup também serão restauradas.

Grafo Canvas Compose, sem HTML/WebView: núcleo e oito hubs coloridos, ligações explícitas ou por tags comuns, toque para edição e zoom/pan com dois dedos. Limite visual de 80 notas, informado na tela; lista em páginas de 50 com todas pesquisáveis. Ligações não são inferências de uma IA. Silencioso estático, Assistente cadência reduzida e Cinema sinapses, pausadas quando a tela não está visível e respeitando animações desativadas.

## Agenda real do aparelho

CalendarContract.Instances expande recorrências dos calendários visíveis com acesso de leitura (inclusive agendas somente leitura). Janela: hoje e os sete dias seguintes; ordenação por início. All-day usa datas UTC e fim exclusivo, evitando mudança de dia no Brasil. Eventos cancelados são filtrados. Não há alteração durante leitura; agenda e calendário de criação anterior são caminhos separados. A consulta pede READ_CALENDAR; WRITE_CALENDAR é solicitado apenas na criação de evento anterior.

A tela oferece hoje/semana, cor/nome da agenda, próximo evento com contagem regressiva e observação do provider/virada do dia enquanto o app está visível. Nenhum link secreto de calendário é pedido. Google Agenda só aparece quando a conta já sincroniza com o Android; OAuth Google Calendar separado não foi implementado.

## Notícias e clima

Feeds oficiais EBC: últimas notícias, economia, e Radioagência pesquisa/inovação. Leitor RSS/Atom com tamanho limitado, recusa DTD, mostra apenas manchete, fonte, data quando fornecida e link original HTTPS. Assuntos incluem Brasil/Tecnologia e palavras-chave personalizáveis. Atualização manual ou ao pedir notícias/digest, TTL 15 min, ETag/Last-Modified, deduplicação por URL e retenção até sete dias quando a data existe. Se a data não vier, a interface informa; hora de consulta não é tratada como hora da notícia. Cache antigo é identificado, até 500 itens persistidos e 100 exibidos. Nenhum scraping do corpo integral da matéria.

Clima é opcional no DIGEST, por cidade escolhida após pesquisa; sem GPS. Nome da cidade vai ao geocodificador Open-Meteo; coordenadas escolhidas ao endpoint de previsão. Mostra mínima/máxima/probabilidade de chuva realmente retornadas, cache de uma hora e data/status. Não utiliza chave nem plano pago. Atribuição Open-Meteo/CC BY 4.0, uso pessoal não comercial. Desativar remove cidade e previsão.

## Morning Digest e voz compartilhada

Templates Kotlin determinísticos, sem enviar dados a modelo/serviço de IA. Inclui saudação, data local, agenda, contagem de e-mails AÇÃO disponíveis, manchetes filtradas, clima configurado e títulos de prioridades expressamente autorizadas. Curto até 100 palavras, médio até 220; omite partes inteiras para respeitar tamanho. Dados ausentes são identificados; fonte com falha não impede as demais. Não é um resumo semântico de conteúdo de e-mail: a prévia vem do Gmail e a classificação usa regras locais corrigíveis.

Gatilhos: primeira ativação manual de cada dia (preferência salva, sem despertar em segundo plano), “bom dia”, “resumo do dia” e botão DIGEST. Notas existentes bloqueadas pedem autenticação; cancelar permite relatório das demais fontes sem notas. A preferência automática pode ser desligada. Não consulta o conteúdo de notas na conversa livre; “quais são minhas metas” é uma consulta explícita.

Mesmo SpeechOutput/AudioFocus/Soundscape da Missão 1, sem segundo sistema de voz. Relatórios extensos são divididos em trechos mantendo captura suspensa durante toda a fala. Dados pessoais exigem Voice.isNetworkConnectionRequired=false e dados instalados, com nova verificação mesmo em mecanismo já aquecido. Sem voz local disponível, fica texto com orientação. Silencioso suprime voz/música/efeitos; os outros modos seguem seus volumes e controles existentes. O TTS pode ter pt-PT como fallback português local se pt-BR não existir; não se garante gênero/voz de estúdio.

## Gmail seguro e suas dependências

[Configuração OAuth](GMAIL-OAUTH.md). Adaptador AuthorizationClient e Gmail REST gmail.readonly, sem senha principal, client secret, envio para IA ou backend. OAuth da própria conta Android; token de acesso transitório em RAM, cache de credenciais do SDK Google. Conta/metadados/prévias e correções de classificação têm payload criptografado no armazenamento interno.

Primeira versão consulta até 100 mensagens da caixa de entrada dos últimos sete dias. Não representa toda a conta. Lê assunto/remetente/snippet, classifica localmente e permite corrigir a categoria apenas no JARVIS. Leitura completa é solicitada só ao abrir uma mensagem, convertida a texto seguro, sem WebView, imagens de rastreamento, anexos ou links executáveis. Nunca envia, exclui, arquiva, marca lida nem altera rótulos no Gmail.

Atualização manual e WorkManager aproximadamente a cada hora, com rede/bateria suficientes; o Android pode adiar. Se OAuth expirar, pede nova autorização no app e não abre telas silenciosamente no background. Worker não usa áudio. Desconectar cancela trabalho e limpa cache/conta, tentando revogar acesso no Google; sem rede, orienta revisar acessos da Conta Google. Um contador de geração invalida retornos atrasados de rede/worker/OAuth após desconexão ou exclusão.

A integração real não é testável sem registro OAuth para pacote+certificado e consentimento de conta. Nenhuma conta real foi usada em CI. O app mostra esse estado em vez de dados falsos. O escopo gmail.readonly é restrito e publicação ampla pode exigir verificação Google; não foi contratada infraestrutura nem iniciado processo pago.

## Limites e validação

Sem controle universal de telas de terceiros, escuta de fundo, sync notebook ou memória automática/RAG. Não há PDFs de referência disponíveis nesta revisão; grafo original compatível com o HUD existente. O modelo livre local de conversa é opcional e não substitui integrações oficiais.

Testes automatizados separam regras Kotlin, Room/Keystore/provider/TTS Android e UI com dados sintéticos. O teste de TTS confirma PCM por mecanismo fixture, não dicção humana Samsung. As telas pessoais usam FLAG_SECURE; capturas pretas delas são esperadas. Fluxo biométrico real, fala pt-BR, Google OAuth, bateria e fluidez precisam de teste físico no Galaxy. Não se afirma FPS ou autonomia sem medição. [Resultados e roteiro](TESTES.md).
