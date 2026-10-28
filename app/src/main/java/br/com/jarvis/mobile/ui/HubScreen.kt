package br.com.jarvis.mobile.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.JarvisState
import br.com.jarvis.mobile.brain.BrainState
import br.com.jarvis.mobile.brain.Note
import br.com.jarvis.mobile.domain.Command
import br.com.jarvis.mobile.experience.ExperienceSettings
import br.com.jarvis.mobile.hub.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BrainActions(val unlock:()->Unit,val edit:(Note?)->Unit,val save:(Note)->Unit,val delete:(String)->Unit,val close:()->Unit,
    val export:(CharArray)->Unit,val importBackup:()->Unit,val preview:(CharArray)->Unit,val confirm:()->Unit,val cancel:()->Unit,val dictate:()->Unit)

@Composable
fun HubScreen(state:HubState,jarvis:JarvisState,brain:BrainState,model:HubViewModel,brainActions:BrainActions,
    onBack:()->Unit,onNavigate:(HubPanel)->Unit,onToggle:()->Unit,onMode:(ExperienceSettings)->Unit,onGmail:()->Unit,
    onArticle:(String)->Unit,onErase:()->Unit){
    var erase by remember{mutableStateOf(false)}
    Surface(Modifier.fillMaxSize(),color=Background){Box(Modifier.fillMaxSize()){
        HolographicBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()){
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick=onBack){Text("‹ Núcleo",color=Cyan)}
                Text(state.panel.title,color=Gold,fontSize=16.sp,fontFamily=FontFamily.Monospace,modifier=Modifier.weight(1f).padding(start=8.dp))
            }
            ModeSelector(jarvis.experience,onMode,Modifier.padding(horizontal=20.dp))
            if(jarvis.voiceIssue.isNotBlank())Text(jarvis.voiceIssue,color=Gold,fontSize=12.sp,modifier=Modifier.padding(horizontal=20.dp,vertical=8.dp))
            if(state.panel!=HubPanel.BRAIN)TextButton(onClick=onToggle,modifier=Modifier.fillMaxWidth()){
                Text(if(jarvis.enabled||jarvis.speaking||jarvis.working)"Pausar JARVIS" else "Ativar JARVIS",color=Cyan)
            }else Text(if(jarvis.listening)"Ditado local em uso · toque no núcleo para interromper" else "Memória protegida · ditado apenas no dispositivo",color=Muted,fontSize=11.sp,modifier=Modifier.padding(20.dp,8.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).testTag("hub-content"),verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(state.notice.isNotBlank())Text(state.notice,color=Gold,fontSize=13.sp)
                if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                when(state.panel){
                    HubPanel.MENU->MenuPanel(onNavigate)
                    HubPanel.BRAIN->BrainPanel(brain,jarvis.experience.mode,brainActions.unlock,brainActions.edit,brainActions.save,brainActions.delete,brainActions.close,
                        brainActions.export,brainActions.importBackup,brainActions.preview,brainActions.confirm,brainActions.cancel,brainActions.dictate)
                    HubPanel.AGENDA->AgendaPanel(state,model)
                    HubPanel.NEWS->NewsPanel(state,model,onArticle)
                    HubPanel.EMAIL->MailPanel(state,model,onGmail)
                    HubPanel.DIGEST->DigestPanel(state,model)
                    HubPanel.PRIVACY->{
                        Text("Seus dados ficam sob seu controle",color=Gold,fontSize=21.sp)
                        Text("Notas só são salvas após confirmação. Não enviamos notas ou e-mails para serviços de IA. Leituras pessoais usam somente vozes declaradas locais pelo Android; instale os dados de voz em português para ouvir.",color=Muted)
                        Text("O calendário é consultado no aparelho com sua permissão. Gmail usa autorização Google, sem senha no JARVIS. O radar busca feeds públicos; clima só consulta a cidade escolhida, sem GPS.",color=Muted)
                        Text("Ao sair: microfone, fala e animações param. A sincronização de e-mail autorizada pode continuar pelo Android; nunca liga o microfone. Revogue permissões nas configurações do sistema quando desejar.",color=Muted)
                        OutlinedButton(onClick={erase=true},modifier=Modifier.fillMaxWidth().testTag("erase-personal-data")){Text("Apagar meus dados do JARVIS",color=Gold)}
                        Text("Remove notas, caches pessoais, conexão de e-mail e chaves locais. Não apaga agendas ou mensagens originais, nem backups que você exportou.",color=Muted,fontSize=12.sp)
                    }
                    HubPanel.HOME->Unit
                }
            }
        }
    }}
    if(erase)AlertDialog(onDismissRequest={erase=false},title={Text("Apagar dados pessoais?")},text={Text("A exclusão local é definitiva. Notas, conexão de e-mail, caches e chaves serão removidos. Agendas, mensagens dos provedores e backups externos permanecem. Será necessário confirmar sua identidade.")},
        confirmButton={TextButton(onClick={erase=false;onErase()}){Text("Apagar dados locais")}},dismissButton={TextButton(onClick={erase=false}){Text("Cancelar")}})
}

@Composable
private fun MenuPanel(navigate:(HubPanel)->Unit){
    Text("Uma central para o seu dia",fontSize=24.sp,color=Cyan)
    Text("Dados reais, fontes identificadas e memória que você escolhe guardar.",color=Muted)
    val descriptions=mapOf(HubPanel.BRAIN to "Oito áreas pessoais · notas e conexões",HubPanel.AGENDA to "Hoje e sete dias à frente · calendário Android",
        HubPanel.NEWS to "Manchetes brasileiras · feeds públicos",HubPanel.DIGEST to "Seu relatório diário · curto ou médio",
        HubPanel.EMAIL to "Gmail · leitura e classificação local",HubPanel.PRIVACY to "Permissões, dados e exclusão")
    descriptions.forEach{(panel,description)->HudPanel(Modifier.fillMaxWidth().testTag("open-${panel.name}").clickable{navigate(panel)}){
        Column(Modifier.padding(18.dp)){Text(panel.title,color=Gold,fontFamily=FontFamily.Monospace);Text(description,color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=6.dp))}
    }}
}

@Composable
private fun AgendaPanel(state:HubState,model:HubViewModel){
    var week by remember{mutableStateOf(false)}
    SourceLine(state.agenda.info)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(!week,{week=false},label={Text("Hoje")});FilterChip(week,{week=true},label={Text("Até 7 dias à frente")})}
    if(state.agenda.info.status==SourceStatus.NEEDS_PERMISSION){Button(onClick=model::requestAgendaPermission,modifier=Modifier.fillMaxWidth()){Text("Permitir leitura do calendário")};return}
    OutlinedButton(onClick={model.refreshAgenda()},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("Atualizar agenda")}
    val next=nextEntry(state.agenda.entries,state.now)
    next?.let{HudPanel(Modifier.fillMaxWidth(),accent=Gold){Column(Modifier.padding(16.dp)){
        Text("PRÓXIMO COMPROMISSO",color=Gold,fontSize=11.sp);Text(it.title,color=Cyan,fontSize=20.sp);Text(countdown(it.start,state.now),color=Muted)
    }}}
    val items=if(week)state.agenda.entries else todayEntries(state.agenda.entries,Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).toLocalDate(),ZoneId.systemDefault())
    if(items.isEmpty()&&state.agenda.info.status==SourceStatus.READY)Text("Nenhum compromisso neste período.",color=Muted)
    items.forEach{event->HudPanel(Modifier.fillMaxWidth(),accent=Color(event.color)){Column(Modifier.padding(16.dp)){
        Text(event.calendar,color=Color(event.color),fontSize=12.sp);Text(event.title,color=Cyan,fontSize=17.sp)
        Text(if(event.allDay)"${dayText(event.start,true)} · dia inteiro" else "${dateText(event.start)} — ${timeText(event.end)}",color=Muted,fontSize=13.sp)
        if(!event.allDay&&event.start<=state.now&&event.end>state.now)Text("Em andamento",color=Gold)
    }}}
}

@Composable
private fun NewsPanel(state:HubState,model:HubViewModel,open:(String)->Unit){
    var topic by remember{mutableStateOf("")}
    SourceLine(state.news.info)
    Text("Fontes públicas EBC · títulos e links originais",color=Muted,fontSize=12.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick={model.refreshNews()},enabled=!state.busy,modifier=Modifier.weight(1f)){Text("Atualizar")}
        OutlinedButton(onClick=model::readHeadlines,modifier=Modifier.weight(1f)){Text("Ler manchetes")}
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        (listOf("Brasil","Tecnologia","Economia","Saúde","Educação")+state.preferences.topics).distinct().forEach{value->
            FilterChip(value in state.preferences.topics,{val updated=if(value in state.preferences.topics)state.preferences.topics-value else state.preferences.topics+value
                model.updatePreferences(state.preferences.copy(topics=updated));model.newsTopic("")},label={Text(value)})
        }
    }
    OutlinedTextField(topic,{topic=it.take(60)},label={Text("Adicionar assunto ou palavra-chave")},modifier=Modifier.fillMaxWidth())
    TextButton(onClick={if(topic.isNotBlank()){model.updatePreferences(state.preferences.copy(topics=state.preferences.topics+topic.trim()));topic="";model.newsTopic("")}}){Text("Salvar assunto")}
    if(state.newsTopic.isNotBlank())TextButton(onClick={model.newsTopic("")}){Text("Filtro por voz: ${state.newsTopic} · limpar")}
    val items=model.visibleNews()
    if(items.isEmpty())Text("Sem manchetes para os assuntos selecionados. Atualize ou escolha outro assunto.",color=Muted)
    items.take(100).forEach{news->HudPanel(Modifier.fillMaxWidth().clickable{open(news.url)}){Column(Modifier.padding(16.dp)){
        Text(news.title,color=Cyan,fontSize=16.sp);Text(news.source,color=Gold,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
        Text(news.publishedAt?.let(::dateText)?:"Horário de publicação não informado",color=Muted,fontSize=11.sp)
        Text("Abrir matéria original ↗",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
    }}}
}

@Composable
private fun MailPanel(state:HubState,model:HubViewModel,connect:()->Unit){
    var category by remember{mutableStateOf<EmailCategory?>(null)}
    SourceLine(state.mail.info)
    if(state.mail.account.isBlank()){
        Text("Conexão segura com o Gmail",color=Gold,fontSize=21.sp)
        Text("Acesso real exige um cliente OAuth registrado para este aplicativo e a assinatura deste APK. O JARVIS não pede sua senha e não exibe mensagens de exemplo como se fossem suas.",color=Muted)
        Text("Use o guia GMAIL-OAUTH.md no repositório. Se a configuração estiver pronta, autorize sua conta pela tela oficial do Google.",color=Muted,fontSize=12.sp)
        Button(onClick=connect,modifier=Modifier.fillMaxWidth().testTag("gmail-connect")){Text("Conectar Gmail pelo Google")};return
    }
    Text(state.mail.account,color=Gold,fontSize=16.sp)
    Text("${state.mail.items.count{it.category==EmailCategory.ACTION}} AÇÃO entre ${state.mail.items.size} mensagens sincronizadas. Até 100 da entrada dos últimos sete dias. Classificações são sugestões locais e podem ser corrigidas.",color=Muted,fontSize=12.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick={model.refreshMail()},enabled=!state.busy,modifier=Modifier.weight(1f)){Text("Atualizar")}
        OutlinedButton(onClick=connect,modifier=Modifier.weight(1f)){Text("Autorizar")}
    }
    TextButton(onClick={model.disconnectMail()}){Text("Desconectar e limpar cache")}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(category==null,{category=null},label={Text("Todos")})
        EmailCategory.entries.forEach{c->FilterChip(category==c,{category=c},label={Text(c.title)})}
    }
    state.mail.items.filter{category==null||it.category==category}.forEach{mail->HudPanel(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp)){
            Text(mail.category.title+(if(mail.manual)" · sua escolha" else " · sugestão"),color=Gold,fontSize=11.sp)
            Text(mail.subject,color=Cyan,fontSize=16.sp);Text(mail.sender,color=Muted,fontSize=12.sp)
            Text(mail.preview,color=Muted,fontSize=13.sp,modifier=Modifier.padding(vertical=8.dp));Text(dateText(mail.receivedAt),color=Muted,fontSize=11.sp)
            TextButton(onClick={model.mailDetail(mail.id)}){Text("Ler texto da mensagem")}
            Row(Modifier.horizontalScroll(rememberScrollState())){EmailCategory.entries.forEach{c->TextButton(onClick={model.classifyMail(mail.id,c)}){Text(c.title,fontSize=11.sp)}}}
        }
    }}
    if(state.mailDetail.isNotBlank())AlertDialog(onDismissRequest=model::closeMailDetail,title={Text("Mensagem · somente leitura")},
        text={Text(state.mailDetail,modifier=Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick=model::closeMailDetail){Text("Fechar")}})
}

@Composable
private fun DigestPanel(state:HubState,model:HubViewModel){
    var city by remember{mutableStateOf("")}
    Text("Seu dia, com informações disponíveis",color=Gold,fontSize=22.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(!state.preferences.mediumDigest,{model.updatePreferences(state.preferences.copy(mediumDigest=false))},label={Text("Curto · cerca de 1 min")})
        FilterChip(state.preferences.mediumDigest,{model.updatePreferences(state.preferences.copy(mediumDigest=true))},label={Text("Médio · cerca de 2 min")})
    }
    Row(verticalAlignment=Alignment.CenterVertically){Switch(state.preferences.autoDigest,{model.updatePreferences(state.preferences.copy(autoDigest=it))});Text("Primeira ativação do dia",color=Muted,modifier=Modifier.padding(start=12.dp))}
    Button(onClick={model.handle(Command.Digest())},enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("digest-generate")){Text("Gerar DIGEST")}
    state.digest?.let{digest->HudPanel(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
        Text(dateText(digest.createdAt),color=Gold,fontSize=11.sp);Text(digest.text,color=Cyan,fontSize=16.sp,modifier=Modifier.padding(top=12.dp))
        OutlinedButton(onClick=model::readDigest,modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Ler em voz alta")}
    }}}
    Text("O modo Silencioso mostra texto. Dados pessoais só são lidos por vozes locais instaladas. A leitura depende do modo e das opções de voz.",color=Muted,fontSize=12.sp)
    HorizontalDivider(color=Outline)
    Text("Fontes do resumo",color=Gold)
    SourceLine(state.agenda.info);SourceLine(state.mail.info);SourceLine(state.news.info);SourceLine(state.weather.info)
    Text("Previsão do tempo · opcional",color=Gold,fontSize=16.sp)
    Text("Ao pesquisar, o nome da cidade é enviado ao Open-Meteo. Sem GPS, chaves ou assinatura. Uso pessoal não comercial · atribuição CC BY 4.0.",color=Muted,fontSize=12.sp)
    OutlinedTextField(city,{city=it.take(100)},label={Text("Cidade brasileira")},modifier=Modifier.fillMaxWidth())
    OutlinedButton(onClick={model.findCity(city)},enabled=!state.busy&&city.trim().length>=2,modifier=Modifier.fillMaxWidth()){Text("Buscar cidade")}
    state.cityResults.forEach{candidate->TextButton(onClick={model.selectCity(candidate)},modifier=Modifier.fillMaxWidth()){Text(candidate.name)}}
    if(state.weather.city.isNotBlank()){Text(state.weather.city,color=Cyan);TextButton(onClick={model.disableWeather()}){Text("Desativar e remover cidade")}}
}

@Composable
private fun SourceLine(info:SourceInfo){
    Text(info.message+(if(info.updatedAt>0)" · ${dateText(info.updatedAt)}" else ""),color=if(info.status in setOf(SourceStatus.READY))Muted else Gold,fontSize=12.sp)
}
private fun dateText(epoch:Long)=Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm",Locale.forLanguageTag("pt-BR")))
private fun timeText(epoch:Long)=Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun dayText(epoch:Long,allDay:Boolean)=Instant.ofEpochMilli(epoch).atZone(if(allDay)ZoneOffset.UTC else ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM"))
