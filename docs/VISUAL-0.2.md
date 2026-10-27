# Alterações visuais da 0.2.0

- O símbolo central é o controle do microfone, com indicação clara para ativar ou pausar. A letra J gigante e os marcadores de relógio foram removidos.
- Fundo preto, iluminação azul suave, tipografia branca e detalhes dourados. Ícones vetoriais próprios, sem biblioteca comercial ou imagens baixadas em runtime.
- Cabeçalho e indicação de privacidade ficam fora do conteúdo rolável. Barras do sistema protegidas por safeDrawingPadding.
- ChatGPT e Alarmes aparecem em cartões compactos. Com fonte a partir de 135%, os cartões ficam em uma coluna para manter a leitura.
- Instruções e informações do reconhecedor ficam no painel Voz e permissões. Os textos técnicos sobre APIs e SDK não aparecem na tela inicial.
- A conversa aparece depois de reconhecer fala; a tela inicial não mostra um cartão de resposta vazio.

Validação: [execução completa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171), 22 testes aprovados. As [capturas do emulador](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171/artifacts/12769648745) mostram a Activity real e a tela com fonte de 150%. A captura da Activity foi revista visualmente antes da entrega. O comportamento de voz não foi alterado nesta revisão.

A assinatura debug mudou em relação à 0.1. É necessário desinstalar a versão anterior para instalar este APK. Esta é uma compilação de teste, sem publicação na Play Store.
