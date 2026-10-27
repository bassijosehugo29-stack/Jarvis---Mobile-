# Baixar e instalar no Galaxy S25 FE

O projeto já foi enviado ao repositório e o APK debug 0.1.0 foi compilado com sucesso. Você não precisa instalar ferramentas de programação no celular.

## Instalar o APK gerado

1. Abra [o artefato JARVIS-Mobile-debug](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460/artifacts/12766140769) no navegador. Entre na sua conta GitHub se solicitado.
2. Baixe o ZIP e use **Meus Arquivos** da Samsung para extraí-lo. O arquivo de instalação é `app-debug.apk`.
3. Toque no APK e, se o Android solicitar, permita a instalação dessa origem. O **Bloqueador Automático** da Samsung pode impedir a instalação externa. Se você optar por desativá-lo para instalar este APK de teste, reative-o depois.
4. Abra **JARVIS Mobile**, toque em **ATIVAR JARVIS** e conceda permissão de microfone durante o uso.
5. Diga **“Jarvis”**, aguarde **“Às suas ordens, senhor”** e fale **“abrir ChatGPT”**. O aplicativo oficial do ChatGPT precisa estar instalado.
6. Volte ao JARVIS, ative a escuta e diga **“Jarvis, alarme para 7:30”**. Confirme e salve na tela do relógio.

Se você baixou o APK direto na conversa, pode começar no passo 3, sem extrair ZIP. Não use os ZIPs da release antiga `Javis`: aquela tag aponta para o commit inicial e não contém este APK.

## Gerar outro APK pelo navegador do celular

1. Abra [Actions do repositório](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions).
2. Selecione **Android - APK de teste → Run workflow → main → Run workflow**. Use **Site para computador** no navegador se necessário para ver o botão.
3. Aguarde os jobs **debug-apk** e **Testes instrumentados Android 35**. Confirme os resultados reais; não considere uma execução com erro como validada.
4. Abra a execução concluída e baixe **JARVIS-Mobile-debug** em **Artifacts**.

Os artefatos ficam disponíveis por 7 dias. APK debug de outra execução pode ter chave de assinatura diferente; um conflito de assinatura exige desinstalar a versão anterior antes de instalar a nova. Isso apaga os dados locais do app.

## Voz e limitações

O app prefere reconhecimento no dispositivo e solicita pt-BR. Baixe o modelo de reconhecimento e a voz de texto para fala em português brasileiro nas configurações do Samsung. Um provedor convencional pode exigir internet; a preferência offline não é uma garantia.

A escuta para ao sair do app. Ao voltar, ative novamente. Os botões ChatGPT e alarme funcionam sem microfone. Ainda não há sincronização com notebook nem escuta em segundo plano.

O repositório é público e usa runners padrão do GitHub. Nenhuma API paga, chave OpenAI, publicação na Play Store ou assinatura adicional foi usada.
