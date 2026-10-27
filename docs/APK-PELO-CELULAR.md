# Baixar e instalar no Galaxy S25 FE

O projeto já foi enviado ao repositório e o APK debug 0.3.0 foi compilado com sucesso. Você não precisa instalar ferramentas de programação no celular.

## Instalar o APK gerado

**Se você instalou a versão anterior, desinstale-a primeiro.** A nova compilação de teste usa outro certificado e não atualiza diretamente a versão anterior. Os modelos de voz do Samsung/Google não pertencem ao app JARVIS e não precisam ser removidos.

1. Abra [o artefato JARVIS-Mobile-debug](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439/artifacts/12771677174) no navegador. Entre na sua conta GitHub se solicitado.
2. Baixe o ZIP e use **Meus Arquivos** da Samsung para extraí-lo. O arquivo de instalação é `app-debug.apk`.
3. Toque no APK e, se o Android solicitar, permita a instalação dessa origem. O **Bloqueador Automático** da Samsung pode impedir a instalação externa. Se você optar por desativá-lo para instalar este APK de teste, reative-o depois.
4. Abra **JARVIS Mobile**, toque no **microfone central / Ativar JARVIS** e conceda permissão de microfone durante o uso.
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

Na versão 0.3, toque no ícone de ajustes no canto superior direito para abrir **Voz e permissões**, consultar os exemplos de comandos ou abrir as permissões do Android.

A versão 0.3 tem HUD animado. O movimento ambiente não indica microfone ativo: confira a indicação de microfone no rodapé e o indicador de privacidade do Android. Os eventos de nível de voz dependem do serviço de reconhecimento instalado.
