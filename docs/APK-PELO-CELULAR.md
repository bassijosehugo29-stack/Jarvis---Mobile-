# Baixar e instalar no Galaxy S25 FE

O projeto está no repositório. Você não precisa instalar ferramentas de programação no celular. Use um APK de uma execução concluída com sucesso nos dois jobs; a [versão 0.4.0 está aprovada](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094). [Baixar artefato APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094/artifacts/12776313560). Evidência em [VALIDATION.md](VALIDATION.md).

## Instalar

**Desinstale a versão anterior do JARVIS antes de instalar outro APK debug se o certificado mudou.** Isso apaga os dados do aplicativo, incluindo a agenda preferida; não apaga compromissos do calendário nem os modelos de voz do Samsung/Google.

1. Abra [Actions do repositório](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions) no navegador e entre na sua conta GitHub.
2. Abra a execução aprovada da versão 0.4, baixe **JARVIS-Mobile-debug** em **Artifacts** e extraia o ZIP em **Meus Arquivos** da Samsung.
3. Toque em `app-debug.apk`. Se você baixou o APK direto da conversa, não precisa extrair um ZIP. Autorize a instalação dessa origem quando o Android solicitar. Se o Bloqueador Automático da Samsung impedir a instalação, a escolha de desativá-lo temporariamente é sua; reative a proteção depois.
4. Abra **JARVIS Mobile**, toque no núcleo **Ativar JARVIS** e conceda microfone durante o uso. A tela não tem atalhos individuais para ChatGPT/alarme.
5. Diga **“Jarvis, abrir WhatsApp”** (ou outro aplicativo instalado, pelo nome). O app de destino abre e a escuta do JARVIS é desligada.
6. Volte, ative o núcleo e diga **“Jarvis, agende dentista amanhã às quinze horas”**. No primeiro pedido, permita **Calendário**. Se houver várias agendas, responda o número da desejada. Sem duração explícita, o compromisso dura uma hora.
7. Confira o compromisso no calendário. Para testar alarme, volte ao JARVIS e diga **“Jarvis, alarme para sete e trinta”**. Confira o resultado no Samsung Clock, que pode exigir confirmação própria.

Não use os ZIPs da release antiga **Javis**: aquela tag aponta para o commit inicial e não contém o APK atual.

## Gerar outro APK pelo navegador do celular

1. Abra [Actions](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions).
2. Selecione **Android - APK de teste → Run workflow → main → Run workflow**. Use **Site para computador** no navegador se necessário para ver o botão.
3. Aguarde **debug-apk** e **Testes instrumentados Android 35**; ambos precisam concluir com sucesso.
4. Baixe **JARVIS-Mobile-debug**, extraia e instale o APK como acima.

Artefatos ficam disponíveis por sete dias. Compilações em runners diferentes podem usar outro certificado debug; em conflito de assinatura, desinstale a versão anterior. O workflow não publica na Play Store nem cria uma release.

## Voz e limites

Configure reconhecimento e texto para fala pt-BR no Samsung, com modelos locais gratuitos quando disponíveis. Um provedor convencional pode exigir internet; a preferência offline não é garantia.

Ao sair do JARVIS, o microfone é desligado; volte e ative o núcleo para outro pedido. O HUD animado não indica captura de áudio: confira o rodapé e o indicador Android de microfone. Ajustes, exemplos e permissões ficam no ícone superior direito.

Abrir um app não permite controlar todas as funções dentro dele. O app precisa aparecer no launcher do perfil atual; Pasta Segura e outros perfis podem não estar disponíveis. A sincronização do calendário depende da conta configurada no Android. Não há controle do notebook, escuta oculta, modelo de conversa livre, serviço pago ou chave de API. Mais formatos e limitações em [COMANDOS-0.4.md](COMANDOS-0.4.md).
