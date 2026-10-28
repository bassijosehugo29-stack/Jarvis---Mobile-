# Privacidade e permissões — JARVIS 3.1

Sem telemetria, anúncios, gravação contínua, servidor de IA ou credenciais embutidas. Use dados de teste até concluir as verificações no aparelho.

| Permissão/recurso | Quando e para quê |
| --- | --- |
| RECORD_AUDIO | Somente ao ativar captura por ação explícita; encerrada em onStop. Nunca usado pelo Worker. |
| READ_CALENDAR | Solicitada ao consultar agenda; leitura dos calendários já disponíveis no aparelho. |
| WRITE_CALENDAR | Solicitada apenas na criação de eventos, recurso anterior. Consultar agenda não pede escrita. |
| SET_ALARM | Intent oficial para o app relógio; não agenda exact alarms ocultos. |
| USE_BIOMETRIC | Autenticação Android para abrir Second Brain e confirmar exclusão completa. Sem acesso a biometria/PIN brutos. |
| INTERNET | Download de modelo solicitado, RSS, clima opcional e Gmail autorizado. |
| OAuth gmail.readonly | Tela oficial Google quando o usuário conecta conta; somente leitura. Sem senha no JARVIS. |
| Arquivos SAF | Escolha explícita de arquivo/destino pelo Android; sem acesso amplo ao armazenamento. |

Dependências WorkManager/Google podem acrescentar permissões normais de rede/wakelock/boot e componentes de jobs ao manifesto final. Servem ao agendador de sincronização de dados; não existe serviço foreground de microfone, hotword de fundo ou autorização de acessibilidade. O relatório apk.json lista permissões reais do APK.

## Armazenamento e exposição

Room interno guarda payloads AES-256-GCM; chaves separadas de memória/cache no Android Keystore. Metadados UUID/tipo/data permanecem em colunas, sem texto pessoal pesquisável. Autenticação de notas é uma proteção de sessão na interface/repositório, não autenticação criptográfica por operação da chave. Proteção do dispositivo e Android continuam essenciais. Backup Android é desativado e regras excluem nuvem/transferência. Não são prometidas proteção contra aparelho comprometido nem remoção física forense de blocos flash.

Notas só são salvas após confirmação; saúde/finanças/relações são opcionais. Rascunhos e previews não são persistidos automaticamente. Somente notas com autorização individual fornecem títulos ao digest, e apenas após desbloqueio. Conversa local não recebe notas/e-mails nem memoriza automaticamente. FLAG_SECURE protege telas de notas/e-mail/digest e respostas pessoais; impede capturas/gravação dessas telas conforme o Android. Não coloque conteúdo real nos relatórios de bug.

Tokens de acesso Gmail ficam em RAM; cache gerenciado pelo SDK Google, não em SharedPreferences próprio. Conta/metadados/snippets e cidade/cache têm payload criptografado. Logs não registram tokens, notas, assuntos, corpo de e-mail ou comandos privados. CI usa exclusivamente fixtures de teste. Backup externo só inclui notas/vínculos/flags, criptografado por senha que não guardamos; os arquivos exportados ficam sob controle do usuário.

## Motor independente da voz A

JARVIS Voz A é instalado como segundo aplicativo, sem nenhuma permissão Android solicitada, inclusive sem INTERNET ou RECORD_AUDIO. O modelo vem no APK. O JARVIS só o utiliza se certificado e identidade esperados coincidirem. Texto e PCM de síntese ficam em RAM; o motor não guarda histórico/gravações e não cria worker de captura. Arquivos persistidos são apenas recursos estáticos do fonemizador. Logs das bibliotecas são filtrados no próprio processo, com stderr suprimido; debug só permite eventos técnicos estáticos. O CI usa frases públicas e disponibiliza suas amostras deliberadamente.

A alternativa “Voz do Android” é uma escolha explícita e continua sujeita à política de vozes locais para dados pessoais. Reconhecimento de fala é separado da síntese: instalar este motor não instala um reconhecedor offline. [Identidade/licenças e limitações](VOZ-A.md).

## Voz, rede e exclusão

Respostas pessoais exigem voz declarada local e instalada, mesmo se uma voz on-line estava selecionada anteriormente. Sem voz local, texto e aviso. O TTS nativo e suas declarações dependem do provedor instalado. O ditado privado recusa fallback para rede e pode exigir instalar pt-BR. Reconhecimento geral de comandos prioriza on-device, mas pode usar o serviço Android; para conteúdo sensível use texto ou ditado local. A permissão de microfone não é autorização para escuta em segundo plano.

RSS envia pedidos públicos à EBC; clima envia cidade na pesquisa/coordenadas selecionadas ao Open-Meteo apenas se configurado. Gmail envia requisições de leitura ao Google após OAuth. Provedores recebem metadados normais de rede como IP; não há intermediário de IA. Modelo de conversa é baixado a pedido e roda localmente depois; nenhuma conversa vai a uma API de inferência.

PRIVACIDADE → “Apagar meus dados do JARVIS” exige confirmação e autenticação do dispositivo. Remove notas, cache, conta/conexão Gmail, cidade, preferências das centrais, chaves locais e histórico de conversa em RAM; cancela Worker e invalida respostas atrasadas. Mantém arquivos de modelo/software, ajustes não pessoais de áudio e dados originais de agendas/provedores. Não apaga backups externos: exclua-os onde foram salvos. Tenta revogar acesso Google; sem rede, mostra que a revogação remota não foi confirmada. Você também pode revogar permissões/terceiros nas configurações Android/Conta Google.
