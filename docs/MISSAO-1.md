# Missão 1 — identidade cinematográfica (0.6)

## Estado da entrega

Implementação em validação. Compilação e testes desta revisão ainda não confirmados; consulte a execução final em VALIDATION.md quando concluída. Base preservada: 0.5, commit e0d7e8c411e398d6e18ea9cdc5a41681bde0d92c, execução 40604934666, 47 testes de unidade e 15 instrumentados aprovados.

## Três modos

- **Silencioso**: gate de áudio desativa TTS, música e efeitos do JARVIS mesmo com preferências individuais ligadas. Núcleo parado em repouso e animação reduzida em trabalho. Cada toque autoriza uma única escuta; depois, para e responde em texto. Escrever nunca exige permissão de microfone. O serviço de reconhecimento pode emitir avisos sonoros próprios do Android, fora do controle do app; para silêncio do sistema também, use texto.
- **Assistente (padrão)**: voz, efeitos discretos, animação moderada; música desligada por padrão. Pode ativá-la nos ajustes.
- **Cinematográfico**: formação do núcleo, partículas, arcos e trilha ambiente original. A música tem preferência própria, independente da opção do modo Assistente. Efeitos de processamento em loop somente neste modo.

A troca interrompe a escuta/fala em andamento. Não inicia microfone sozinha. Modo, opções, três volumes, velocidade, tom e mecanismo/voz escolhidos ficam em Preferences DataStore. O modo Silencioso não apaga os volumes dos demais modos. Desinstalar limpa essas preferências.

## Visual e consumo

HUD original preto/azul-marinho/ciano, dourado discreto, núcleo vetorial projetado, anéis concêntricos e painéis translúcidos. Não copia marcas, layout proprietário, amostras ou gravações do vídeo. Examinados quadros aos 4, 28 e 55 segundos do vídeo anexado (73,65 s, 576×1024, áudio AAC mono); foi possível inspecionar a faixa tecnicamente, mas não escutá-la com as ferramentas desta sessão. Não houve clonagem de voz ou reutilização de música.

Estados: inicializando (formação), espera (pulso lento), escutando (nível RMS real do reconhecedor), processando (rotação mais rápida), respondendo (callbacks reais do TTS), erro (tom quente por 3,5 s, com explicação em texto). O núcleo segue sendo o controle de iniciar/parar.

Redesenho limitado a 30 Hz no Cinema ativo, 15 Hz no Cinema em repouso/Assistente ativo, 5 Hz no Assistente em repouso e 4 Hz no Silencioso em trabalho; Silencioso em repouso é estático. São limites de atualização solicitados, não medidas de desempenho do Galaxy. Respeita animações desativadas no Android. Ao sair, cancela captura, TTS, inferência, trilha e efeitos; não há serviço oculto. Relógio/bateria usam dados reais e param de atualizar fora da tela.

## Voz

Usa Android TextToSpeech, prioridade pt-BR e fallback pt-PT quando necessário. Perfil inicial: velocidade 0,90×, tom 0,88×, dicção e pausas do motor. Ajustes permitem escolher mecanismo, listar vozes portuguesas, selecionar voz e testar. O Android não expõe gênero de forma padronizada: não se garante voz masculina ou qualidade de estúdio só alterando pitch. Use as vozes disponíveis no Samsung/Google, sem clonagem.

“Testar voz” não reproduz áudio no Silencioso, com respostas desligadas ou volume próprio zerado. Hardware de volume controla mídia; os volumes do app não alteram o volume global do Android. A animação de resposta começa no callback onStart e termina em conclusão/erro/cancelamento; avanços de palavras usam onRangeStart quando o motor oferece. Sem esses avanços, há pulso visual durante a fala, sem alegar análise de áudio exata.

## Música e efeitos originais

OriginalScore.kt contém síntese original PCM mono 22.050 Hz, com loop de 16 s, harmônicos, pulsos e cinco sinais: inicialização, escuta, processamento, confirmação e atenção. Não contém samples/licenças de terceiros. Gerado uma vez em thread de trabalho e reproduzido via AudioTrack estático; sem stream, internet, arquivo musical externo ou mensalidade. Fades de borda evitam cliques; as frequências e o desenho estão versionados e podem ser alterados como o restante do projeto.

Voz/música/efeitos têm volumes e chaves independentes. Música reduz para 12% do volume configurado antes/durante a fala, com rampa curta; para completamente durante captura, evitando autoescuta. Efeitos param na captura e na fala. O sinal de escuta termina antes de começar o reconhecedor. Um único foco de áudio coordena TTS e trilha; perda/interrupção de foco para o áudio e exige ação do usuário para retomar. Chamadas, DND, Bluetooth e decisões do Android continuam soberanos.

## Funcionalidades preservadas e limites

Conversa local Qwen (download opcional gratuito de 491 MB), pt-BR, comando Jarvis dentro da sessão autorizada, ChatGPT oficial, abertura de apps, alarmes por intent, agenda já existente, permissões e interrupção ao sair preservados. Nenhum módulo novo de agenda/e-mail/notícias/memória foi iniciado nesta missão. O modelo leve não tem qualidade ou conhecimento atual do ChatGPT. Voz/reconhecimento dependem dos motores/idiomas instalados e podem usar rede do provedor Android. Não há API paga, credenciais embutidas, assinatura, escuta de fundo, boot, acessibilidade, publicação ou release.

## Aceite no Galaxy S25 FE

Após instalar o APK validado: confira mídia/Bluetooth, Testar voz, seleção de voz portuguesa; alterne os três modos e reabra para conferir persistência. No Cinema, habilite música/efeitos, teste cada volume, fale e confira redução da trilha; no Silencioso, confirme ausência de áudio do JARVIS e uma escuta por toque. Saia/bloqueie a tela e confira indicador de microfone apagado e áudio parado. Teste comandos preservados no app oficial. A voz humana real, hardware Samsung, autonomia térmica/bateria e beleza sonora exigem aceite físico; o emulador não os comprova.
