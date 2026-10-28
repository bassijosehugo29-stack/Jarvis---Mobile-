package br.com.jarvis.mobile.hub

import br.com.jarvis.mobile.brain.Note
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HubPanel(val title: String) { HOME("JARVIS"), MENU("SUAS CENTRAIS"), BRAIN("SECOND BRAIN"), AGENDA("AGENDA"), NEWS("RADAR"), DIGEST("DIGEST"), EMAIL("E-MAILS"), PRIVACY("PRIVACIDADE") }
enum class SourceStatus { NOT_CONFIGURED, NEEDS_PERMISSION, LOADING, READY, STALE, ERROR, NEEDS_AUTH }
data class SourceInfo(val status: SourceStatus = SourceStatus.NOT_CONFIGURED, val message: String = "Não configurado", val updatedAt: Long = 0)
data class AgendaEntry(val id: Long, val calendarId: Long, val title: String, val start: Long, val end: Long,
    val allDay: Boolean, val calendar: String, val color: Int)
data class AgendaSnapshot(val entries: List<AgendaEntry> = emptyList(), val info: SourceInfo = SourceInfo(SourceStatus.NEEDS_PERMISSION,"Permita a leitura do calendário para consultar sua programação."))
data class NewsItem(val id: String, val title: String, val url: String, val source: String, val publishedAt: Long?, val topics: Set<String>)
data class NewsSnapshot(val items: List<NewsItem> = emptyList(), val info: SourceInfo = SourceInfo(message="Atualize para buscar os feeds públicos da EBC."))
enum class EmailCategory(val title: String) { ACTION("AÇÃO"), INFO("INFO"), NOISE("RUÍDO") }
data class EmailItem(val id: String, val account: String, val sender: String, val subject: String, val preview: String,
    val receivedAt: Long, val category: EmailCategory, val unread: Boolean = false, val manual: Boolean = false)
data class MailSnapshot(val account: String = "", val items: List<EmailItem> = emptyList(), val info: SourceInfo = SourceInfo(message="Gmail não conectado. É necessário configurar OAuth para este APK."))
data class WeatherSnapshot(val city: String = "", val low: Double? = null, val high: Double? = null,
    val rain: Int? = null, val info: SourceInfo = SourceInfo(message="Clima não configurado. Escolha uma cidade, sem usar GPS."))
data class DigestSnapshot(val text: String, val createdAt: Long, val medium: Boolean, val privateData: Boolean)
data class HubPreferences(val topics: Set<String> = setOf("Brasil","Tecnologia"), val mediumDigest: Boolean = false,
    val autoDigest: Boolean = true, val lastAutoDate: String = "")

/** Classifications are suggestions, never actions or assertions that a reply is legally required. */
fun classifyEmail(subject: String, preview: String, labels: Set<String> = emptySet()): EmailCategory {
    val value=br.com.jarvis.mobile.brain.normalizedSearch("$subject $preview")
    if (Regex("\\b(nao precisa responder|no reply required|apenas para conhecimento)\\b").containsMatchIn(value)) return EmailCategory.INFO
    if (Regex("\\b(aguardando (sua |seu )?(resposta|retorno)|preciso (da sua|de sua)|favor responder|por favor.*(responda|confirme)|acao necessaria|action required|aprovacao pendente|prazo para|reply needed)\\b").containsMatchIn(value)) return EmailCategory.ACTION
    if (labels.any { it in setOf("CATEGORY_PROMOTIONS","CATEGORY_SOCIAL") } ||
        Regex("\\b(newsletter|descadastre|unsubscribe|oferta imperdivel|cupom|promocao|ofertas da semana)\\b").containsMatchIn(value)) return EmailCategory.NOISE
    return EmailCategory.INFO
}

fun todayEntries(entries: List<AgendaEntry>, date: LocalDate, zone: ZoneId): List<AgendaEntry> = entries.filter {
    if (it.allDay) {
        val begin=Instant.ofEpochMilli(it.start).atZone(ZoneOffset.UTC).toLocalDate()
        val end=Instant.ofEpochMilli(it.end).atZone(ZoneOffset.UTC).toLocalDate()
        !date.isBefore(begin) && date.isBefore(end)
    } else it.start < date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() && it.end > date.atStartOfDay(zone).toInstant().toEpochMilli()
}
fun nextEntry(entries: List<AgendaEntry>, now: Long): AgendaEntry? = entries.filter { !it.allDay && it.start>=now }.minByOrNull { it.start }
fun countdown(start: Long, now: Long): String {
    val minutes=((start-now).coerceAtLeast(0)/60000)
    return when { start<=now -> "Agora"; minutes==0L -> "Em menos de um minuto"; minutes<60 -> "Em $minutes min"
        minutes<1440 -> "Em ${minutes/60} h ${minutes%60} min"; else -> "Em ${minutes/1440} dia(s)" }
}

/** Source-grounded templates. Never asks the language model to invent missing appointments/mail. */
object DigestBuilder {
    fun build(now: Instant, zone: ZoneId, agenda: AgendaSnapshot, mail: MailSnapshot, news: NewsSnapshot,
        weather: WeatherSnapshot, notes: List<Note>?, medium: Boolean): DigestSnapshot {
        val local=now.atZone(zone); val limit=if(medium)220 else 100
        val greeting=when(local.hour){in 5..11->"Bom dia";in 12..17->"Boa tarde";else->"Boa noite"}
        val parts=mutableListOf("Às suas ordens, senhor. $greeting. Hoje é ${local.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM",Locale.forLanguageTag("pt-BR")))}.")
        val events=todayEntries(agenda.entries,local.toLocalDate(),zone)
        if(agenda.info.status==SourceStatus.READY) {
            parts+=if(events.isEmpty()) "Não há compromissos no calendário para hoje." else "Agenda de hoje: "+events.take(if(medium)4 else 2).joinToString("; ") {
                it.title+if(it.allDay) ", dia inteiro" else ", às "+Instant.ofEpochMilli(it.start).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
            }+"."
        } else parts+="Agenda indisponível; confira a permissão."
        if(mail.info.status in setOf(SourceStatus.READY,SourceStatus.STALE)) {
            val action=mail.items.filter { it.category==EmailCategory.ACTION }
            parts+=(if(mail.info.status==SourceStatus.STALE)"No último cache, " else "")+"${action.size} e-mail(s) classificados como ação entre as mensagens sincronizadas dos últimos sete dias."
            if(medium) action.take(2).forEach { parts+="${it.subject}." }
        } else parts+="E-mails indisponíveis: conta não conectada ou autorização pendente."
        if(weather.low!=null && weather.high!=null) parts+="Em ${weather.city}, previsão de ${weather.low.toInt()} a ${weather.high.toInt()} graus"+
            (weather.rain?.let { ", com $it por cento de chance de chuva" }?:"")+"."+
            if(weather.info.status==SourceStatus.STALE)" Clima do último cache." else ""
        else parts+="Clima não configurado ou indisponível."
        if(news.items.isNotEmpty()) {
            if(news.info.status==SourceStatus.STALE) parts+="Notícias do último cache:"
            else parts+="Manchetes:"
            news.items.take(if(medium)3 else 1).forEach { parts+="${it.title}, fonte ${it.source}." }
        } else parts+="Notícias indisponíveis no momento."
        if(notes==null) parts+="Metas protegidas: desbloqueie a memória para consultar."
        else notes.filter { it.includeInDigest }.sortedWith(compareByDescending<Note>{it.pinned}.thenByDescending{it.updatedAt})
            .take(if(medium)3 else 1).forEach { parts+="Prioridade autorizada: ${it.title}." }
        // Whole sentences only: avoid cutting a factual number/date in the middle.
        val result=StringBuilder();var words=0
        for(part in parts){val size=part.split(Regex("\\s+")).size;if(words+size>limit)continue;result.append(part).append(' ');words+=size}
        return DigestSnapshot(result.toString().trim(),now.toEpochMilli(),medium,true)
    }
}
