# Testes — JARVIS 3.1

Resultados efetivamente executados ficam em [VALIDATION.md](VALIDATION.md); testes adicionados não devem ser contados como aprovados antes do CI concluir.

## Voz A

Unidade: DSP finito/limitado, normalização, silêncio, preservação de frequência/tempo e assinatura JNI boxed/cancelamento do adaptador PCM. Android: motor instalado com identidade/certificado/ausência de rede e microfone; síntese real das quatro frases aprovadas e de uma inédita; WAV/PCM não vazio e seleção pelo SpeechOutput. Migração de preferências preserva o modo e adota A sem pitch/engine antigos; ANDROID continua persistindo seu pitch explícito. As quatro WAV públicas são coletadas como evidência, sem gravação de texto pessoal.

## Cobertura preservada da 3.0

| Área | Verificação automatizada |
| --- | --- |
| Notas/regras | Oito categorias, padrões de consentimento, busca acentos/conteúdo/tags, filtros, prioridades, vínculos e merge por UUID/data; lote sintético de 1.000 notas. |
| Backup | AES-GCM/PBKDF2 round-trip, salts/IVs distintos, formato inválido, senha incorreta, adulteração; importação sem escrita antes da confirmação e sem parcial em dados inválidos. |
| Android privado | Room real, payload sem título/conteúdo em claro, Keystore e AAD por registro, reabertura persistente, bloqueio e limpeza. Invalidação de worker/OAuth atrasado após desconexão/exclusão. |
| Interface | Editor/salvar/excluir/pesquisar, grafo nativo, memória bloqueada, paginação com 1.000 notas. Lifecycle real limpa rascunho e fecha sessão; FLAG_SECURE presente. |
| Agenda | UTC/dia inteiro/fim exclusivo, evento atravessando meia-noite, contagem regressiva e próximo evento. Provider real em agenda LOCAL somente leitura com recorrência, cancelamento, dia inteiro e ausência de mutação. |
| Notícias | RSS/Atom, entidade HTML, data desconhecida/futura, URL insegura/DTD recusadas. Transporte fixture testa cache Room, TTL/ETag/304, deduplicação, filtro e fallback off-line; disponibilidade real tem relatório separado. |
| Clima | Sem requisição antes de opt-in, cache, nulo não vira zero, indisponibilidade e desativação, com transporte fixture e Room real. |
| Digest/e-mail | Dados ausentes distintos de vazio real, somente notas autorizadas, fontes/cache, data local, limites de palavras, regras AÇÃO/INFO/RUÍDO. Parsing de metadata Gmail sintético, sem conta real. |
| Integração | Comandos novos pelo roteador compartilhado; Silencioso não reproduz áudio, não abre microfone nem apresenta Gmail falso. |
| Voz privada | Mecanismo fixture real produz PCM local; engine já aquecida com voz declarada on-line é recusada para dados pessoais, retornando texto. |

Testes anteriores de modos/áudio/foco/duck/persistência/voz/parser/apps/alarmes/criação de calendário/conversa JNI e Compose permanecem. Os fixtures ficam exclusivamente no APK de androidTest, separado do APK entregue. A voz fixture produz tom PCM: não comprova dicção/alto-falante do Samsung. O teste do grafo não é medição de FPS, bateria ou benchmark do Galaxy.

## Executar

JDK 17 e SDK/NDK/CMake conforme README. Para unidade/lint/compilar:

```sh
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew :app:testDebugUnitTest :voice-engine:testDebugUnitTest :app:lintDebug :voice-engine:lintDebug :app:assembleDebug :voice-engine:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/verify_apk.py
```

Para Android, use emulador API 35 com Google APIs e KVM ou aparelho de teste. O teste de conversa importa modelo oficial real de aproximadamente 491 MB, com hash validado, e usa dados fictícios:

```sh
python3 scripts/fetch_test_model.py /tmp/jarvis-conversation-model.gguf
adb push /tmp/jarvis-conversation-model.gguf /data/local/tmp/jarvis-conversation-model.gguf
adb shell chmod 644 /data/local/tmp/jarvis-conversation-model.gguf
sh scripts/test_on_emulator.sh
```

O script preserva o código real dos testes, coleta relatórios/capturas/transcrição fictícia, eventos estáticos do motor e buffers de crash do emulador. Não transforma falha em sucesso. Telas pessoais não são desprotegidas no debug para gerar screenshots; FLAG_SECURE pode produzir imagens pretas, como esperado. Algumas capturas históricas de troca de modos são parciais no emulador; isso está registrado na validação da base.

`python3 scripts/probe_public_sources.py` consulta somente feeds públicos e previsão de uma localização de CI, sem contas. Retorna relatório de disponibilidade em build/verification/public-sources.json; indisponibilidade externa não é substituída por notícias falsas. `verify_apk.py` verifica assinatura com apksigner, CRC, versão, permissões, ABIs e alinhamento de 16 KB. Ambos os relatórios são artefatos, sem chaves privadas/tokens.

## Aceite manual no Galaxy S25 FE

1. Instalar primeiro JARVIS Voz A e depois JARVIS Mobile da mesma entrega, conferir os três modos e volumes, selecionar A nos ajustes, ouvir “Às suas ordens, senhor”. Testar mídia zerada, alto-falante, fones/Bluetooth/DND e voz ausente: erro visível, sem fingir fala.
2. Autorizar/recusar microfone; comandos pt-BR, parada, sair/bloquear/trocar app/chamada. Nenhuma retomada ou escuta de fundo. Silencioso só deve ativar uma captura por toque; use texto para evitar sons do reconhecedor Android.
3. Desbloquear Second Brain com biometria e PIN real, cancelar, fechar/reabrir e verificar bloqueio. Criar/editar/apagar, tags/vínculos, pesquisa/categorias. Ditado de notas só com reconhecedor local; sem ele, editor e aviso.
4. Exportar com senha longa, importar com prévia/confirmar; tentar senha errada/arquivo alterado. Confirmar que notas não autorizadas não aparecem no digest. Guardar backup/senha fora do app.
5. Permitir só leitura e conferir calendário sincronizado, recorrência, dia inteiro e próximo evento. Agendar um compromisso pela função anterior e verificar no app oficial; remover testes depois.
6. Atualizar notícias, filtrar e abrir fonte original; modo avião/cache antigo. Configurar/remover cidade e conferir data/previsão/indisponibilidade. Conferir digest curto/médio e primeira ativação diária, inclusive em Silencioso.
7. Só depois de configurar OAuth da assinatura instalada, autorizar Gmail na tela Google, conferir conta/amostra real e corrigir classificação local. Testar expiração, offline, reconexão e desconexão; garantir que o Gmail original não foi alterado.
8. Apagar dados pelo painel com autenticação; conferir notas/cache/conta removidos e originais/backups externos preservados. Sem rede, revisar revogação na Conta Google.
9. Preparar conversa local opcional e testar contexto/interrupção. Medir latência, temperatura, bateria e fluidez reais; não inferir do emulador. Fonte ampliada/teclado/TalkBack/rotação também precisam de aceite no aparelho.

Biometria física, ASR/TTS Samsung e OAuth real não são simulados como aceitos. Não há medição de desempenho/autonomia física nesta entrega.
