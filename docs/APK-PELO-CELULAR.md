# Instalar e preparar o JARVIS pelo celular

A versão 0.5 tem [compilação e 62 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666). [Baixar artefato APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666/artifacts/12780696806).

1. Desinstale o JARVIS anterior se houver conflito de assinatura. Este debug usa outro certificado. Isso apaga preferências/modelo local; não remove eventos já salvos no calendário nem vozes do Samsung.
2. Baixe o ZIP do artefato no navegador, logado no GitHub, e extraia em Meus Arquivos. Instale app-debug.apk. Se baixou o APK direto da conversa, instale sem extrair ZIP. Autorize a origem quando o Android solicitar. O Bloqueador Automático Samsung pode impedir APK externo; se você optar por desativá-lo temporariamente para instalar, reative depois.
3. Nos ajustes do JARVIS, use **Testar voz** e aumente o volume de mídia. Confira fones/Bluetooth e **Ajustar voz do Android** se necessário. [Guia de voz](VOZ-0.5.md).
4. Escolha **Baixar conversa offline • 491 MB** usando Wi-Fi não medido. Aguarde **Conversa offline pronta**. O download é separado do APK, gratuito e validado por SHA-256. Pode importar o arquivo oficial já baixado como alternativa. Desinstalar o JARVIS remove esse arquivo local.
5. Ative o núcleo, permita microfone e ouça “Às suas ordens, senhor”. Depois converse naturalmente. Não precisa repetir Jarvis em cada frase. O campo de texto funciona sem microfone.
6. Para testar ações, diga abrir WhatsApp, agende dentista amanhã às quinze horas ou alarme para sete e trinta. No primeiro agendamento, permita Calendário e escolha por voz se houver várias agendas. Confira compromissos/alarmes no app oficial.

Ao sair, bloquear ou abrir outro app, a escuta para. Volte e ative para continuar. A conversa usa um modelo local leve e pode ser simples/lenta/imprecisa; o TTS e reconhecimento dependem dos mecanismos Android instalados. Não há mensalidade ou API paga implementada.

## Gerar uma nova compilação

Abra [Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → Android - APK de teste → Run workflow → main. Use Site para computador se necessário. Espere debug-apk e Testes instrumentados Android 35 terminarem com sucesso. Baixe JARVIS-Mobile-debug, extraia e instale. Artefatos duram sete dias; runners diferentes podem usar outra assinatura debug. Não use a release antiga Javis, que contém o código inicial.

O fluxo não publica na Play Store nem cria release. Resultados e limitações em [VALIDATION.md](VALIDATION.md).
