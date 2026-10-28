# Instalar pelo celular — versão 0.6

[Baixar APK de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231/artifacts/12787512824) · [Compilação e 77 testes aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40621754231).

1. Baixe o ZIP em Artifacts, logado no GitHub, e extraia em Meus Arquivos. O APK direto fornecido na conversa já pode ser instalado sem extrair.
2. Instale app-debug.apk e permita essa origem quando o Android solicitar. Se houver conflito de assinatura, desinstale o JARVIS anterior antes. Isso limpa preferências/modelo local, sem apagar eventos já salvos no calendário.
3. Escolha **Assistente**. Nos ajustes, use **Listar vozes instaladas**, escolha uma opção portuguesa e **Testar voz**. Aumente o volume de mídia e confira saída Bluetooth/fones. Ajuste tom/velocidade. Se faltarem dados de voz, use **Ajustar voz do Android**.
4. Experimente **Cinema**, com música/efeitos e seus volumes nos ajustes. A trilha reduz durante respostas e para durante captura. **Silêncio** desliga todo áudio do JARVIS; toque para uma única escuta ou escreva sem microfone. O reconhecedor pode ter avisos próprios do Android.
5. Para conversar livremente, peça **Baixar conversa offline • 491 MB**, por Wi-Fi, e aguarde **Conversa offline pronta**. Download separado gratuito; modelo leve, com limitações. Comandos de abrir apps/alarmes continuam sem ele.
6. Ative o núcleo e permita microfone durante o uso. Em Assistente/Cinema, ouça “Às suas ordens, senhor” e fale. Para parar, toque no núcleo/diga parar. Sair ou bloquear interrompe áudio e captura.

Voz humana e comportamento físico Samsung ainda precisam de teste. Não há promessa de timbre específico; depende das vozes instaladas. [Roteiro e limites](MISSAO-1.md).

## Gerar outra versão pelo celular

[Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) → **Android - APK de teste → Run workflow → main**. Use **Site para computador** se o menu não aparecer. Aguarde debug-apk e Testes instrumentados Android 35 aprovados. Em Artifacts, baixe JARVIS-Mobile-debug, extraia e instale. Artefatos duram sete dias. Não use a release antiga Javis, que não contém esta versão. O fluxo não publica Play Store nem cria release ou serviços pagos.
