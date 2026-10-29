# JARVIS: nome, ícone e escuta, sem reformulação

## Material disponível e bloqueio

Em 29/10/2026 foram conferidos arquivos acessíveis em `/workspace` e `/tmp`, nomes internos de 102 ZIPs, a main e cinco branches remotas. **JARVIS-CyberBot-Codigo-Sanitizado-v1.0.36.zip não está acessível**, sem caminho/identificador de upload utilizável. Não houve execução, modificação ou auditoria alegada desse código. Não se assume que seja Android ou contenha a interface original.

A base acessível é JARVIS Mobile 3.1, main **50abb6eb0661e519ec8dc704d0c9b4e12f154f7b**. Seus **157 arquivos foram conferidos contra os hashes GitHub antes de alterar qualquer um**. Essa base não é automaticamente o CyberBot comprado. Sua interface, voz e efeitos originais continuam sem referência. O APK original não foi solicitado.

## Alterações delimitadas

1. **Nome JARVIS:** launcher, identificação no cabeçalho e rodapé e rótulo de alarme. Não se renomeiam classes ou identidades de integrações.
2. **Ícone:** núcleo azul original, fundo preto, adaptive icon (26+), camada monocromática (33+) e PNGs em cinco densidades. Nada substitui o núcleo interno. [Origem](branding/ORIGEM.md).
3. **Escuta:** portados isoladamente SpeechTurn, RecognitionWindow, AndroidSpeechInput e o contrato de callbacks do PR #1. Nenhuma mudança visual, ajuste de TTS, perfil vocal, música, integração ou módulo de contexto daquele PR foi importado.

`scripts/check_preservation.py` verifica a [lista de hashes anterior](PRESERVATION-BASELINE.json). Voz, sons, animações, telas, dados, configurações, comandos e integrações devem ser idênticos à **base disponível**. Em JarvisScreen.kt só a string do nome pode mudar: restaurando-a virtualmente, o hash deve coincidir com a base inteira. Há a mesma verificação estrita no rótulo de alarme e no manifesto (apenas icon/roundIcon).

O ViewModel só conecta callbacks e conserva transcrição não confirmada. Em truncamento/erro com texto, pausa a sessão e mantém o conteúdo no campo existente para revisão; não executa, não responde por voz e não reinicia para executar apenas a cauda. Ditado local envia um rascunho ao editor existente, sem salvar automaticamente. Não há nova tela ou controle.

## Fim de fala

- Estados: IDLE, LISTENING, WAITING_FINAL, CONTINUATION, COMMITTED, UNCONFIRMED, CANCELLED.
- Parciais revisam hipóteses, sem repetição. Finais consecutivos são unidos por sobreposição linear. Janelas/generações invalidam callbacks antigos e respostas de turnos cancelados.
- Final do serviço sozinho não autoriza agir. A quietude de **3.300 ms** considera começo de fala, parcial alterado e energia sustentada durante fala sinalizada. Retomar a fala invalida a confirmação pendente.
- Ruído isolado, RMS inválido, parcial repetido e atraso do final não acrescentam outra espera indiscriminada. Relógio monotônico, sem polling permanente. Final tardio não acrescenta automaticamente mais 3,3 s.
- O serviço encerrando cedo abre outra instância/listener em primeiro plano, mantendo o prefixo. Trocas têm pequenos intervalos: não se promete áudio contínuo sem lacunas em todos os provedores.
- Parciais nunca executam comandos. Final truncado mantém o rascunho e falha de forma fechada. Turno máximo 120 s, espera inicial 10 s, tolerância de final 1,5 s e tentativas limitadas para serviço ocupado.
- Stop/close cancelam agendamentos e gerações. Sair continua desligando o microfone. Sem novo serviço, permissão ou gravação.

Silêncio não revela intenção semântica arbitrária. Pausas além da janela e perda acústica durante reinício ainda exigem teste no aparelho. O exemplo WhatsApp valida o **pedido completo**; não adiciona busca interna de conversas nem altera comandos.

## Testes e build

- SpeechTurnTest: relógio injetado, exemplo solicitado, pausas, revisões, callbacks antigos, truncamento, ruído, cancelamento e limites.
- PreservedListeningTest: eventos nativos controlados com **Handler real no Android**. Pausa real de pelo menos 3.000 ms entre “Jarvis, eu preciso que você abra o WhatsApp” e “e procure uma conversa.”, nenhuma entrega na pausa, uma solicitação completa. Salva `preserved-listening-evidence.json`. Não é microfone acústico.
- Regressão do ViewModel: texto truncado retido sem ação, fala ou reinício automático. Testes existentes de TTS e dos módulos mantidos.
- LauncherIdentityTest: rótulo JARVIS, adaptive icon, monochrome e captura do ícone renderizado pelo Android.
- Actions: unidade, Lint, assinatura/alinhamento, regressão Android35 e download do artifact publicado comparado ao APK assinado. Só resultados da execução concluída/PR confirmam aprovação, não esta descrição de testes.

JDK17, SDK35, NDK27.2.12479018, CMake3.22.1, Gradle8.11.1:

```sh
python3 scripts/check_preservation.py
python3 -m pip install onnx==1.18.0
python3 scripts/prepare_voice_a.py
./gradlew :app:testDebugUnitTest :voice-engine:testDebugUnitTest :app:lintDebug :app:lintPreservation :voice-engine:lintDebug :app:assemblePreservation :voice-engine:assembleDebug
```

Regressão Android segue `scripts/test_on_emulator.sh`, com emulador35 e modelo de teste já existente. Nenhuma IA, API paga ou credencial foi adicionada. Esta máquina não tem SDK; compilação Android/testes instrumentados usam CI.

## Distribuição

Artifact **JARVIS-teste-tecnico-base-disponivel**, contendo APK e aviso explícito. Label JARVIS; package **br.com.jarvis.mobile.preservacaopreview**, separado da base e prévias conhecidas (.v4preview e .escutapreview), com dados separados, sem migração. O package CyberBot original não foi fornecido; não se alega comparação.

**Não é o produto final solicitado:** equivalência visual e voz do CyberBot não foram verificadas. TTS/efeitos do repositório não foram alterados. Não se distribui novo motor: a voz da base depende do motor existente com certificado correspondente ou da opção explícita de voz Android já oferecida nos ajustes. Não desinstale nem substitua sua instalação original.

Chrome com login GitHub → baixar ZIP → extrair em Meus Arquivos. Retenção solicitada 90 dias, sujeita à expiração GitHub. Sem loja, merge automático ou serviços pagos.

## O que falta

Disponibilizar **o ZIP de código-fonte citado**, sem exigir APK. Se contiver projeto Android e recursos, aplicar somente escuta/nome/ícone nessa base, conferir os demais arquivos e compilar teste com package separado. Se contiver apenas backend, web ou automação, faltará **o módulo Android com telas, recursos, build e integração da voz original**. Não será legítimo recriá-lo como se fosse o comprado.

Fidelidade ao CyberBot está **bloqueada** mesmo com testes JARVIS Mobile aprovados. Comparação visual original e teste acústico no Galaxy S25 FE não ocorreram. A missão final não está declarada encerrada.
