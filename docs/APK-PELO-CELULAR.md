# Gerar e instalar o APK pelo celular

[Baixar APK 3.0 de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106/artifacts/12812505712) · [120 testes e compilação aprovados](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40678656106). Extraia o ZIP e instale app-debug.apk, ou instale o APK direto fornecido na conversa. [Hash e relatório](VALIDATION.md).

## Gerar no GitHub, sem Android Studio no celular

1. Entre na sua conta e abra [Actions do projeto](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions).
2. Escolha **Android - APK de teste → Run workflow → main → Run workflow**. Se o menu não aparecer, use Chrome → ⋮ → **Para computador**. O push de código na main também dispara o fluxo.
3. Aguarde os dois jobs verdes: **debug-apk** e **Testes instrumentados Android 35**. Se algum falhar, abra o log; não confunda o APK compilado de um run com falha com a entrega validada.
4. Abra essa execução e vá a **Artifacts → JARVIS-Mobile-debug**. É preciso estar logado no GitHub. O download é ZIP; extraia em **Meus Arquivos** e toque em **app-debug.apk**.
5. Autorize **Instalar apps desconhecidos** apenas para o navegador/Meus Arquivos usado, se o Android pedir, e instale. Você pode desabilitar a origem depois. Artefatos expiram após sete dias: gere outra execução se necessário.

A release antiga “Javis” com ZIP de algumas centenas de bytes não contém este APK. Não precisa comprar serviço, publicar loja nem habilitar cobrança. O projeto é público; repositórios privados têm franquia de Actions conforme conta.

## Atualização e assinatura

Sem Secrets opcionais, o certificado debug do CI é temporário e pode mudar a cada execução. Se houver “aplicativo não instalado” por conflito de assinatura, **exporte as notas com senha antes de desinstalar** a versão anterior e instalar a nova. Desinstalar remove notas/cache, preferências e o modelo baixado; não remove eventos do calendário nem e-mails originais. Reimporte o backup de notas após instalar e autenticar. A primeira transição da 0.6 também pode exigir desinstalação.

Configure uma assinatura privada fixa para atualizações futuras e Gmail conforme [GMAIL-OAUTH.md](GMAIL-OAUTH.md). O artefato JARVIS-relatorios inclui apk.json com SHA-1/SHA-256 do certificado e hash SHA-256 do arquivo realmente compilado. Não publique a chave privada.

## Primeiro teste no Galaxy

- Configure PIN/biometria de bloqueio Android; abrir Second Brain exige autenticação.
- No JARVIS, escolha Assistente e teste uma voz portuguesa **local instalada**. Instale pt-BR nos ajustes de texto para fala do Android, aumente volume de mídia e confira Bluetooth/fones. Dados pessoais ficam em texto se não houver voz local.
- Toque no núcleo e permita microfone durante o uso. Primeira ativação diária pode ler o digest; a opção fica na central DIGEST. Diga “Jarvis, minha agenda”, “notícias” ou “abrir ChatGPT”. Sair/bloquear interrompe captura/áudio.
- Abra ⋮ para Second Brain/agenda/radar/digest/e-mails/privacidade. CalendarProvider pede permissão apenas quando necessário. Gmail é opcional e ainda exige OAuth; os demais módulos não dependem dele.
- Para conversa livre, baixe o modelo offline gratuito de 491 MB por Wi-Fi. Comandos simples não dependem desse download.

[Limitações e roteiro físico](TESTES.md) · [Missão 2](MISSAO-2.md) · [permissões e dados](PRIVACIDADE.md).
