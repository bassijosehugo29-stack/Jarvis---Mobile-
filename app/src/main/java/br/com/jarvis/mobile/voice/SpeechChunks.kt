package br.com.jarvis.mobile.voice

/** Bounded utterances keep long digests within native TTS deadlines without reopening the microphone. */
fun speechChunks(text:String,limit:Int=420):List<String>{
    require(limit>=40)
    val chunks=mutableListOf<String>();var rest=text.trim()
    while(rest.length>limit){
        val candidate=rest.take(limit+1)
        val sentence=maxOf(candidate.lastIndexOf(". "),candidate.lastIndexOf("; "),candidate.lastIndexOf("? "),candidate.lastIndexOf("! "))
        val space=candidate.lastIndexOf(' ')
        val cut=if(sentence>=limit/3)sentence+1 else if(space>0)space else limit
        chunks+=rest.take(cut).trim();rest=rest.drop(cut).trimStart()
    }
    if(rest.isNotBlank())chunks+=rest
    return chunks.ifEmpty{listOf("Às suas ordens, senhor.")}
}
