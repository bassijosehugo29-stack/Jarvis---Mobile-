# Escuta inteligente — revisão Android

Esta revisão continua o Android Kotlin/Compose existente a partir de `14acd22277b9def6fbaa8b63fc420b15bf5c74b3` (branch `jarvis-4-fase1`, com o núcleo azul). Foi criada a branch `fix/escuta-inteligente-20261029`. Não se recriou o aplicativo nem se converteu um projeto de outra plataforma em Android.

## Escopo real da auditoria e bloqueios

O arquivo informado `JARVIS-CyberBot-Codigo-Sanitizado-v1.0.36.zip` **não apareceu entre os arquivos acessíveis** em `/workspace`, anexos locais e `/tmp` nesta tarefa. Não existe identificador de upload ou URL desse ZIP na mensagem. Portanto, o CyberBot não foi extraído, executado, modificado nem auditado, e nenhum componente dele é declarado reaproveitado. A ausência não bloqueou o desenvolvimento Android. Não foi necessário solicitar um APK ao usuário.

O repositório GitHub estava acessível. Foram conferidos por hash de blob os 173 arquivos da base Android; a inspeção funcional focou voz, roteamento, cancelamento, interface, persistência, permissões e CI. Uma varredura estática inicial de 163 arquivos de fonte/documentação não encontrou padrões de chaves Google/OpenAI ou chaves privadas. Isso não é garantia de ausência de segredos nem uma auditoria de segurança exaustiva. Os 20 arquivos conferidos de memória/centrais/segurança e regras de backup não foram alterados.

**Gemini:** não há cliente Gemini na base Android acessível. A conversa livre existente é local, com modelo opcional de 491 MB. Sem o ZIP, configuração de conta ou backend autenticado, não foi possível validar o suposto Gemini gratuito nem medir sua latência de cinco segundos. Nenhuma chave foi embutida, conta paga ativada ou chamada Gemini simulada. `ConversationClient`/`ConversationFactory` permitem um adaptador futuro sem acoplar credenciais à UI; o funcionamento local foi preservado.

A [API Gemini exige autenticação](https://ai.google.dev/gemini-api/docs/api-key), e o [plano gratuito possui modelos/cotas próprios e condições de uso dos dados](https://ai.google.dev/gemini-api/docs/pricing). Uma integração real precisa de configuração adequada e consentimento para transmissão. Não se promete gratuidade só por escolher o nome de um modelo, nem se coloca uma chave compartilhada no APK.

## Defeitos encontrados e correção

A implementação anterior solicitava `EXTRA_PARTIAL_RESULTS=false`, ignorava `onPartialResults` e `onEndOfSpeech`, e enviava o primeiro `onResults` direto ao roteador. O encerramento precoce decidido pelo provedor Android se tornava uma resposta do JARVIS. Também faltava uma verificação de geração após a conclusão assíncrona de um provedor de conversa: uma implementação que ignorasse cancelamento poderia retornar texto/erro fora de hora.

Agora há duas camadas:

- `RecognitionWindow`: uma janela nativa com reconhecedor/listener próprios e configuração oficial pt-BR, resultados parciais e preferência local. Hints estáticos de vocabulário em Android 13+ incluem Jarvis/WhatsApp/ChatGPT/alarme/agenda, sem dados pessoais. Cada callback carrega a identidade da janela; uma janela destruída não pode responder pela próxima.
- `SpeechTurn`: máquina de estados sem dependência Android e com relógio monotônico injetável. Estados: LISTENING, WAITING_FINAL, CONTINUATION, COMMITTED, UNCONFIRMED e CANCELLED, além de IDLE. A revisão invalida confirmações pendentes quando a fala retoma. Só COMMITTED entrega texto ao roteador.

Parciais revisam a hipótese atual, em vez de concatenar cada callback. Segmentos finais sucessivos são unidos com sobreposição textual em tempo linear. Um resultado final nativo não autoriza resposta imediata: o app mantém uma janela de continuação de **3,3 s desde a última evidência de fala**, e reabre rapidamente o reconhecedor se o provedor terminar a sessão antes disso. Resultados finais atrasados não acrescentam indiscriminadamente mais 3,3 s. Parciais repetidos, RMS não finito e ruído sem evento de fala não renovam o prazo; energia sustentada é um sinal secundário com debounce.

Se há fala ainda sem resultado final no limite, o app solicita confirmação ao provedor (`stopListening`) e aguarda um prazo adicional limitado. Nunca transforma automaticamente uma hipótese parcial em alarme, abertura de app ou outro comando. Se o final corta literalmente a cauda do parcial, ou o reconhecimento falha, o rascunho é preservado para revisão explícita. O botão Revisar transcrição só aparece nessa falha: preenche o editor, mas não envia. Ditado de notas também entrega rascunho para revisão, sem salvar automaticamente.

`stop`, saída da tela, troca de modo e cancelamento invalidam temporizadores, janelas e callbacks; os reconhecedores são destruídos e o texto temporário é limpo ao sair. Um turno tem limite máximo de 120 s, e a sessão tem pausa por inatividade. Não há gravação de áudio bruto, serviço de microfone oculto ou nova permissão. Ditado privado continua exigindo reconhecedor local, inclusive com fábricas alternativas.

O detector opcional “Jarvis, pare” durante a fala conserva o caminho rápido de resultado final. Não recebe o atraso de continuação. Suas restrições anteriores de fone com fio/USB, Android 13+ e reconhecimento local permanecem; não se declara interrupção por voz em qualquer alto-falante ou Bluetooth.

## Contexto, latência, voz e consumo

O roteador recebe uma frase completa, incluindo complementos ditos depois da pausa. O contexto local de conversa existente permanece. Um resultado ou erro de provedor cancelado é descartado por geração e estado da coroutine, inclusive se a implementação não cooperar; o prazo antigo não pode cancelar uma solicitação nova. Testes usam um provedor adversarial apenas como fixture, não como conexão Gemini.

A cadência configurada de retorno à escuta após TTS foi reduzida de 800 para 450 ms, com o efeito de escuta ainda terminando antes do microfone. Esse ganho nominal de 350 ms não é medição no Samsung e não significa que uma API remota passou de cinco segundos para outro valor. Esperar a pausa natural e reduzir latência total são objetivos distintos: não se adivinha que a pessoa terminou antes de haver evidência.

O motor A mantém modelo e pitch natural aprovados. A divisão de respostas longas evita separar títulos como “Sr.” e “Dr.” dos nomes; não modifica texto exibido nem aplica pitch grave artificial. Qualidade/naturalidade acústica precisam ser ouvidas no aparelho.

O núcleo holográfico, estados e três modos foram preservados. Os anéis usam primitivas de arco para evitar máscaras de paths transformados por ângulo; os contornos, iluminação e partículas continuam. O monitor do modelo deixou de consultar disco/DownloadManager a cada 1,5 s em repouso: consulta na entrada e acompanha progresso somente durante download. Nenhuma melhora de FPS, CPU ou bateria é declarada sem os relatórios do CI/aparelho. O benchmark em emulador não mede autonomia, temperatura ou mAh reais.

## Compilação e entrega

O workflow existente compila o assistente e o motor A, executa testes/Lint, publica artifacts separados por 90 dias e os baixa novamente para comparar hashes/assinaturas/alinhamento de 16 KB. Os resultados reais, links e fonte compilada aparecem na execução e no pull request; este texto não pressupõe testes ainda não executados.

Artifacts: `JARVIS-Escuta-APK` e `JARVIS-Voz-A-Escuta-APK`. Arquivos: `JARVIS-Mobile-4.0.1-escuta-preview.apk` e `JARVIS-Voz-A-escuta-preview.apk`. No Chrome com login GitHub, abra a execução, baixe cada ZIP, extraia em Meus Arquivos e instale primeiro a voz, depois o assistente. A opção Voz do Android continua disponível.

**Não desinstale versões anteriores.** Como as chaves temporárias dos APKs anteriores não estão disponíveis, a prévia usa pacotes separados: `br.com.jarvis.mobile.escutapreview` e `br.com.jarvis.voicea.escutapreview`. Isso preserva as instalações 3.1/4 e seus dados. A prévia não importa automaticamente notas/configurações privadas; backup criptografado de notas pode ser importado manualmente. Uma futura atualização no mesmo pacote exige a mesma chave privada; o par da mesma execução precisa compartilhar o certificado.

No computador, JDK 17, Gradle 8.11.1, SDK 35, NDK 27.2.12479018 e CMake 3.22.1:

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew :app:testDebugUnitTest :voice-engine:testDebugUnitTest :app:lintDebug :voice-engine:lintDebug
./gradlew -PjarvisAbis=arm64-v8a :app:assemblePreview :voice-engine:assemblePreview
```

Para emulador não fixe jarvisAbis. O script `scripts/test_on_emulator.sh` recusa aparelho físico e usa fixtures descartáveis. Pelo celular: Actions → Android - APK de teste → Run workflow → branch `fix/escuta-inteligente-20261029`, ou acompanhe os checks do PR.

## Validação disponível e limites

- `SpeechTurnTest`: pausas de 2,9/3 s, revisões, final atrasado, prazo não renovado por parciais duplicados/ruído, final truncado, sobreposição longa, janela antiga, cancelamento e limites. Relógio falso, sem microfone.
- `NaturalListeningTest`: eventos nativos de fixture com Handler Android real; une “alarme para sete … e trinta”, impede alarme às 07:00, permite uma única ação às 07:30 e rejeita callbacks antigos. Cobre cancelamento, rascunho, interrupção rápida, intent oficial e rejeição de fábrica de rede no ditado local.
- `ResponseCancellationTest`: resposta/erro atrasados de provedor cancelado não substituem um turno novo. Não é teste de uma API externa.
- Testes de UI exigem revisão e envio explícitos do rascunho; testes de speechChunks verificam preservação de texto/abreviações. A suíte anterior permanece.
- Testes instrumentados não validam captura acústica, o serviço de reconhecimento da One UI ou qualidade vocal no Galaxy S25 FE. A disponibilidade pt-BR e o respeito aos callbacks dependem do provedor. Reabrir o serviço pode criar um pequeno intervalo sem captura; não se promete áudio contínuo sem lacunas. Sobreposições textuais não têm timestamps fonéticos e podem deduplicar uma repetição intencional na fronteira.
- Uma retomada após a confirmação completa do turno é uma nova fala; a interrupção simultânea universal durante TTS não foi implementada. Não há “Ok Jarvis” com o app fechado.

Referências oficiais: [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent), [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer). O Android trata os extras de silêncio conforme o provedor e não oferece garantia de hotword contínua por essa API; por isso a correção não depende só de aumentar um extra.

No Galaxy, teste “Jarvis, alarme para sete”, faça uma pausa de 2–3 s e continue “e trinta”. Não deve haver resposta na pausa nem alarme às sete em ponto. Confira o texto completo antes da única solicitação ao relógio. Compare os três modos, saia/bloqueie para verificar encerramento da captura e use o rascunho quando o reconhecimento falhar. Todos esses testes físicos estão pendentes até execução no aparelho.
