package br.com.jarvis.mobile.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.brain.*
import br.com.jarvis.mobile.experience.OperationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

@Composable
fun BrainPanel(state:BrainState,mode:OperationMode,onUnlock:()->Unit,onEdit:(Note?)->Unit,onSave:(Note)->Unit,onDelete:(String)->Unit,
    onClose:()->Unit,onExport:(CharArray)->Unit,onImport:()->Unit,onPreview:(CharArray)->Unit,onConfirmImport:()->Unit,
    onCancelImport:()->Unit,onDictate:()->Unit){
    var query by remember{mutableStateOf("")};var category by remember{mutableStateOf<NoteCategory?>(null)}
    var export by remember{mutableStateOf(false)}
    if(!state.unlocked){
        HudPanel(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){
            Text("Sua memória, sob seu controle",color=Gold,fontSize=20.sp)
            Text("Oito áreas pessoais, sem memorização automática. Use a biometria ou a credencial do celular para abrir.",color=Muted,modifier=Modifier.padding(vertical=16.dp))
            Button(onClick=onUnlock,modifier=Modifier.fillMaxWidth().testTag("brain-unlock")){Text("Desbloquear Second Brain")}
        }};return
    }
    val draft=state.draft
    if(draft!=null){NoteEditor(draft,state.notes,state.busy,state.error,onSave,onDelete,onClose,onDictate);return}
    val selected=remember(state.notes,query,category){searchNotes(state.notes,query,category)}
    var page by remember(query,category,state.notes){mutableIntStateOf(0)}
    val pages=((selected.size+49)/50).coerceAtLeast(1)
    Text("${state.notes.size} notas · ${state.notes.map{it.category}.distinct().size}/8 áreas em uso",color=Gold,fontFamily=FontFamily.Monospace)
    Text("Vínculos manuais e tags em comum. Toque em uma nota; use dois dedos para mover e ampliar.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
    NeuralGraph(selected.take(80),mode,{onEdit(it)},Modifier.fillMaxWidth().height(310.dp).testTag("brain-graph"))
    if(selected.size>80)Text("Mostrando 80 notas no grafo. A lista e os filtros dão acesso às demais.",color=Muted,fontSize=12.sp)
    OutlinedTextField(query,{query=it},label={Text("Pesquisar título, conteúdo ou tags")},modifier=Modifier.fillMaxWidth().testTag("brain-search"))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(category==null,{category=null},label={Text("Todas")})
        NoteCategory.entries.forEach{c->FilterChip(category==c,{category=c},label={Text(c.title,color=Color(c.color))})}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Button(onClick={onEdit(null)},modifier=Modifier.weight(1f).testTag("brain-add")){Text("Nova nota")}
        OutlinedButton(onClick=onImport,modifier=Modifier.weight(1f)){Text("Importar")}
    }
    OutlinedButton(onClick={export=true},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("Exportar backup com senha")}
    if(state.error.isNotBlank())Text(state.error,color=Gold,modifier=Modifier.padding(vertical=12.dp))
    if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(selected.isEmpty())Text("Nenhuma nota nesta seleção. Crie uma nota para começar.",color=Muted,modifier=Modifier.padding(vertical=18.dp))
    if(pages>1)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick={page--},enabled=page>0){Text("Anterior")}
        Text("${page+1}/$pages · 50 por página",color=Muted,fontSize=12.sp)
        TextButton(onClick={page++},enabled=page<pages-1){Text("Próxima")}
    }
    selected.drop(page*50).take(50).forEach{note->
        HudPanel(Modifier.fillMaxWidth().padding(top=8.dp).clickable{onEdit(note)},accent=Color(note.category.color)){
            Column(Modifier.padding(14.dp)){
                Text(note.category.title.uppercase(),fontSize=10.sp,color=Color(note.category.color))
                Text(note.title,color=Cyan,fontSize=16.sp,modifier=Modifier.padding(top=4.dp))
                Text(note.content.take(120),color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=6.dp))
            }
        }
    }
    if(export)PasswordDialog("Exportar backup","A senha protege o arquivo fora do celular. Não a guardamos; sem ela, o backup não poderá ser recuperado.",
        {export=false},{value->export=false;onExport(value)})
    if(state.importUri!=null&&state.importPreview==null)PasswordDialog("Abrir backup","Digite a senha do arquivo. Nada será alterado antes da revisão.",onCancelImport,onPreview)
    state.importPreview?.let{notes->AlertDialog(onDismissRequest=onCancelImport,title={Text("Revisar importação")},
        text={Text("${notes.size} notas serão mescladas. Versões locais iguais ou mais recentes serão mantidas. Autorizações de digest do backup também serão restauradas.")},
        confirmButton={TextButton(onClick=onConfirmImport,enabled=!state.busy){Text("Confirmar importação")}},
        dismissButton={TextButton(onClick=onCancelImport){Text("Cancelar")}})}
}

@Composable
private fun NoteEditor(note:Note,notes:List<Note>,busy:Boolean,error:String,onSave:(Note)->Unit,onDelete:(String)->Unit,onClose:()->Unit,onDictate:()->Unit){
    var title by remember(note){mutableStateOf(note.title)};var content by remember(note){mutableStateOf(note.content)}
    var category by remember(note){mutableStateOf(note.category)};var tags by remember(note){mutableStateOf(note.tags.joinToString(", "))}
    var links by remember(note){mutableStateOf(note.links)};var digest by remember(note){mutableStateOf(note.includeInDigest)}
    var pinned by remember(note){mutableStateOf(note.pinned)};var delete by remember{mutableStateOf(false)}
    Text("Revisar e confirmar memória",color=Gold,fontSize=20.sp)
    Text("O conteúdo só será persistido ao salvar. Saúde, finanças e relações são opcionais. Rascunhos são descartados ao sair do aplicativo.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=10.dp))
    OutlinedTextField(title,{title=it.take(160)},label={Text("Título")},modifier=Modifier.fillMaxWidth().testTag("note-title"))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        NoteCategory.entries.forEach{c->FilterChip(category==c,{category=c},label={Text(c.title)})}}
    OutlinedTextField(content,{content=it.take(20000)},label={Text("Conteúdo")},minLines=5,maxLines=14,modifier=Modifier.fillMaxWidth().testTag("note-content"))
    OutlinedButton(onClick=onDictate,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Ditar nova nota · somente no dispositivo")}
    OutlinedTextField(tags,{tags=it.take(1000)},label={Text("Tags separadas por vírgula")},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(digest,{digest=it});Text("Autorizar esta nota no digest",color=Muted)}
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pinned,{pinned=it});Text("Marcar como prioridade",color=Muted)}
    if(error.isNotBlank())Text(error,color=Gold)
    Text("Vínculos manuais",color=Gold)
    notes.filter{it.id!=note.id}.take(100).forEach{other->
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(other.id in links,{checked->links=if(checked)(links+other.id).distinct()else links-other.id});Text(other.title,color=Muted,fontSize=13.sp)}
    }
    Button(onClick={onSave(note.copy(title=title.trim(),category=category,content=content.trim(),tags=tags.split(',').map{it.trim()}.filter{it.isNotEmpty()}.distinct().take(30),links=links,includeInDigest=digest,pinned=pinned))},
        enabled=!busy&&title.isNotBlank()&&content.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("note-save")){Text("Confirmar e salvar")}
    OutlinedButton(onClick=onClose,modifier=Modifier.fillMaxWidth()){Text("Voltar sem salvar")}
    if(notes.any{it.id==note.id})TextButton(onClick={delete=true},modifier=Modifier.fillMaxWidth()){Text("Excluir nota",color=Gold)}
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Excluir esta nota?")},text={Text("Esta exclusão remove a nota do JARVIS. Backups exportados continuam sob seu controle.")},
        confirmButton={TextButton(onClick={delete=false;onDelete(note.id)}){Text("Excluir")}},dismissButton={TextButton(onClick={delete=false}){Text("Cancelar")}})
}

@Composable
internal fun PasswordDialog(title:String,description:String,onCancel:()->Unit,onConfirm:(CharArray)->Unit){
    var password by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onCancel,title={Text(title)},text={Column{Text(description);OutlinedTextField(password,{password=it},label={Text("Senha · mínimo 8 caracteres")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Password,autoCorrectEnabled=false))}},
        confirmButton={TextButton(onClick={val chars=password.toCharArray();password="";onConfirm(chars)},enabled=password.length>=8){Text("Continuar")}},dismissButton={TextButton(onClick={password="";onCancel()}){Text("Cancelar")}})
}

@Composable
private fun NeuralGraph(notes:List<Note>,mode:OperationMode,onSelect:(Note)->Unit,modifier:Modifier){
    val phase=remember{mutableFloatStateOf(0f)};var scale by remember{mutableFloatStateOf(1f)};var pan by remember{mutableStateOf(Offset.Zero)}
    val device=rememberHudDeviceState()
    LaunchedEffect(mode,device.visible){if(!device.visible||mode==OperationMode.SILENT||!android.animation.ValueAnimator.areAnimatorsEnabled())return@LaunchedEffect
        while(isActive){delay(if(mode==OperationMode.CINEMATIC)34 else 120);phase.floatValue=(phase.floatValue+0.025f)%1f}}
    val positions=remember(notes){notes.associate{note->
        val category=note.category.ordinal;val inCategory=notes.filter{it.category==note.category}.indexOf(note)
        val angle=(category*PI/4-PI/2+(inCategory%5-2)*0.065).toFloat();val radius=0.29f+(inCategory%4)*0.045f
        note.id to Offset(cos(angle)*radius,sin(angle)*radius)
    }}
    val links=remember(notes){noteLinks(notes)}
    Canvas(modifier.pointerInput(notes,scale,pan){detectTapGestures{tap->
        val diameter=min(size.width,size.height).toFloat();val center=Offset(size.width/2f,size.height/2f)+pan
        notes.minByOrNull{(center+positions.getValue(it.id)*diameter*scale-tap).getDistance()}?.let{
            if((center+positions.getValue(it.id)*diameter*scale-tap).getDistance()<26.dp.toPx())onSelect(it)
        }
    }}.pointerInput(Unit){detectTransformGestures{_,delta,zoom,_->scale=(scale*zoom).coerceIn(0.65f,3f);pan=(pan+delta).let{Offset(it.x.coerceIn(-500f,500f),it.y.coerceIn(-500f,500f))}}}){
        val d=size.minDimension;val origin=center+pan
        fun point(id:String)=origin+positions.getValue(id)*d*scale
        repeat(3){drawCircle(Cyan.copy(alpha=0.08f),d*(0.18f+it*0.12f)*scale,origin,style=Stroke(0.6.dp.toPx()))}
        links.forEach{link->if(link.from in positions&&link.to in positions){val a=point(link.from);val b=point(link.to)
            drawLine(if(link.manual)Gold.copy(alpha=0.45f)else Cyan.copy(alpha=0.18f),a,b,0.9.dp.toPx())
            if(mode==OperationMode.CINEMATIC)drawCircle(Cyan.copy(alpha=0.8f),1.8.dp.toPx(),a+(b-a)*phase.floatValue)}}
        NoteCategory.entries.forEach{category->val a=(category.ordinal*PI/4-PI/2).toFloat();val p=origin+Offset(cos(a),sin(a))*d*0.20f*scale
            drawLine(Color(category.color).copy(alpha=0.25f),origin,p,0.8.dp.toPx());drawCircle(Color(category.color),3.dp.toPx()*scale,p)}
        notes.forEach{note->val p=point(note.id);val color=Color(note.category.color);val pulse=1f+0.16f*sin(phase.floatValue*2*PI.toFloat()+note.category.ordinal)
            drawLine(color.copy(alpha=0.13f),origin,p,0.6.dp.toPx());drawCircle(color.copy(alpha=0.10f),10.dp.toPx()*pulse*scale,p)
            drawCircle(color,4.dp.toPx()*scale,p)}
        drawCircle(Cyan.copy(alpha=0.1f),26.dp.toPx()*scale,origin);drawCircle(Cyan,15.dp.toPx()*scale,origin,style=Stroke(1.dp.toPx()))
        drawCircle(Gold,4.dp.toPx()*scale,origin)
    }
}
