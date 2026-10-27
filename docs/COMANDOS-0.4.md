# Comandos de voz — versão 0.4.0

A tela principal mantém o HUD, o núcleo para ativar/pausar o microfone e as configurações. Os atalhos de ChatGPT/alarme foram removidos. Não é necessário tocar em botões de cada ação.

Ative o núcleo e diga **Jarvis**. Depois da resposta “Às suas ordens, senhor”, fale seu pedido em até 20 segundos; também pode dizer tudo numa frase. As perguntas seguintes aceitam respostas por voz por 20 segundos após a fala do JARVIS. “Cancelar” ou “parar” interrompem a escuta e descartam pedidos incompletos. Sair do JARVIS também cancela pedidos incompletos.

| Pedido | Comportamento |
| --- | --- |
| “Jarvis, abrir WhatsApp” | Procura o aplicativo instalado e inicia sua tela pública |
| “Jarvis, entrar no Instagram” | Mesmo fluxo, usando o nome do app |
| “Jarvis, abrir ChatGPT” | Abre exclusivamente o pacote oficial com.openai.chatgpt |
| “Jarvis, abrir agenda” | Abre o calendário; se houver várias opções, pergunta qual por voz |
| “Jarvis, agende dentista amanhã às quinze horas” | Salva um compromisso de uma hora na agenda escolhida |
| “Jarvis, agende reunião dia 30/10 às 14:30 por 30 minutos” | Salva título, data, horário e duração informados |
| “Jarvis, agende dentista” | Pergunta o dia e depois o horário; diga “amanhã”, depois “quinze e trinta” |
| “Jarvis, alarme para sete e trinta” | Solicita 07:30 ao relógio oficial, pedindo para dispensar a tela quando suportado |
| “Jarvis, criar alarme” | Pergunta o horário por voz |

## Agenda e permissões

O calendário só é solicitado no primeiro comando de agendamento completo. O Android exibe a permissão READ_CALENDAR/WRITE_CALENDAR; é necessário autorizá-la para gravar diretamente. O app consulta os nomes das agendas visíveis que permitem escrita e só lê de volta o evento recém-inserido para confirmar o salvamento. Não lê compromissos anteriores, não cria convidados, não envia convites e não adiciona lembretes nesta versão.

Com uma única agenda elegível, usa essa agenda. Com mais de uma, pergunta até cinco opções numeradas, e aceita uma resposta como “um”, “dois” ou o nome completo não ambíguo. A agenda escolhida fica como preferência local para próximos pedidos. Se deixar de estar disponível, o app pergunta novamente. Sem agenda elegível, informa que é necessário configurar uma; não cria uma conta ou uma agenda silenciosamente.

Datas: hoje, amanhã, depois de amanhã, dias da semana, DD/MM[/AAAA] ou “dia 30 de outubro [de 2026]”. Sem ano, datas de dia/mês usam a próxima ocorrência; dia da semana significa a próxima ocorrência, inclusive na semana seguinte se for o mesmo dia. Horários: 15:30, 15h30, quinze horas, três da tarde, sete e meia. A duração é de 60 minutos quando omitida, ou de 1 a 720 minutos quando explícita (incluindo “por meia hora” e “por uma hora e meia”). Usa o fuso do celular. Horários passados, ambíguos por mudança de fuso, datas inválidas, recorrência e offsets relativos como “daqui a duas horas” exigem uma nova indicação e não são gravados por suposição.

Só diz “Agendado” após inserção e leitura de confirmação do título/horário pelo provedor Android. Isso confirma o evento no calendário local; eventual sincronização da conta é responsabilidade do sistema e da conexão da conta. Se o salvamento ficar sem confirmação ou for interrompido, confira a agenda antes de repetir para evitar duplicatas.

## Aplicativos e alarmes

A busca considera apenas atividades exportadas do launcher do perfil atual. Não pede QUERY_ALL_PACKAGES nem mantém uma lista remota de aplicativos. O nome falado deve corresponder ao nome exibido no celular ou a um alias suportado. Nomes ambíguos geram pergunta por voz; aplicativos ausentes, desativados, de outro perfil ou de Pasta Segura podem não estar disponíveis. O JARVIS abre a tela pública do aplicativo; ações internas dependem das integrações oficiais que cada app oferece. Não há automação universal, serviço de acessibilidade ou envio de mensagens/compras.

Alarmes usam ACTION_SET_ALARM, com EXTRA_SKIP_UI=true quando o horário é válido. O relógio pode mostrar sua própria confirmação e não fornece ao JARVIS um resultado de salvamento. Por isso a resposta é “Solicitando ao relógio…”, sem alegar que verificou um alarme criado. O usuário deve conferir no Samsung Clock.

Ao abrir outro aplicativo, a escuta do JARVIS é desligada. Para novo pedido, volte ao JARVIS e ative o núcleo. Não há escuta oculta, ativação com o app fechado ou controle do notebook nesta versão. Todas as bibliotecas continuam gratuitas; não há IA conversacional, API paga ou chave de API.

## Validação

[Compilação confirmada e 50 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094), incluindo escrita real no Calendar Provider de uma agenda LOCAL de teste. Voz no Galaxy e sincronização de contas exigem validação física. Detalhes em [VALIDATION.md](VALIDATION.md).
