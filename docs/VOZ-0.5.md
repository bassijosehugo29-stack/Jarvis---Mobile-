# Preparar som e conversa no Galaxy

A versão 0.5 está [compilada e aprovada nos testes](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666). O som e a voz física do Galaxy ainda precisam ser conferidos abaixo.

1. Instale o novo APK. Se o Android informar conflito de assinatura com o JARVIS anterior, desinstale a versão anterior primeiro.
2. Abra os ajustes do JARVIS e toque em **Testar voz**. Com o JARVIS aberto, aumente o volume de **mídia** pelos botões do aparelho.
3. Se não ouvir, confira a saída de mídia do Samsung: o som pode estar em fones ou Bluetooth. A mensagem do JARVIS informa volume zero, mecanismo indisponível, falha ou timeout.
4. Em **Ajustar voz do Android**, selecione um mecanismo instalado e uma voz em português brasileiro. Use a busca das Configurações por “texto para fala” se o menu direto não abrir. Instale os dados gratuitos de português se o mecanismo solicitar. O app tenta outros mecanismos disponíveis e pt-PT se pt-BR faltar.
5. Em **Conversa offline**, toque em **Baixar conversa offline • 491 MB** com Wi-Fi não medido. Aguarde **Conversa offline pronta**. O APK não contém esses pesos. Também pode importar o arquivo oficial já baixado. O app confere SHA-256 e tamanho antes de carregar.
6. Volte à tela principal e ative o núcleo. Ele fala “Às suas ordens, senhor” imediatamente. Depois, converse sem repetir Jarvis em cada frase. Exemplos: “Meu nome é Rafael”, “Qual é o meu nome?”, “Me explique o que é amizade”.
7. Para parar, toque no núcleo ou diga parar. Ao sair, bloquear a tela ou abrir outro app, o microfone é desligado; ative novamente ao voltar. Também pode escrever mensagens sem conceder microfone.

A fala gerada usa o TTS do Android; o modelo Qwen gera o texto. São componentes separados. Um teste com tom PCM no emulador não prova a voz humana ou o alto-falante do Galaxy. O aceite físico inclui ouvir o teste, fazer duas perguntas, confirmar respostas faladas e verificar que a escuta para ao sair.

A conversa usa um modelo local leve, com contexto curto. Pode ser lenta, simples ou imprecisa. Não há pesquisa na internet nem os mesmos recursos do ChatGPT. As perguntas/respostas do modelo ficam no aparelho e em memória; serviços Android de reconhecimento/TTS têm sua própria disponibilidade e podem usar rede. Não há cobrança de API ou mensalidade implementada. O download por Wi-Fi e os serviços de voz dependem da conexão/provedor do usuário.
