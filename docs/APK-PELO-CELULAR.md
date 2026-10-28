# Gerar e instalar o APK pelo celular

A versão 3.1 usa dois APKs: instale **JARVIS Voz A** primeiro e depois **JARVIS Mobile**, ambos da mesma execução. O modelo pt-BR já vem no motor; não precisa contratar serviço ou instalar uma voz de terceiros. [Resultado real da validação](VALIDATION.md) · [Identidade da voz A](VOZ-A.md).

**Downloads separados desta entrega (exigem login no GitHub):**

1. [Baixar JARVIS Voz A — APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12830015162) — ZIP com `JARVIS-Voz-A-1.0.0-debug.apk`.
2. [Baixar JARVIS Mobile 3.1 — APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12829740920) — ZIP com `JARVIS-Mobile-3.1.0-debug.apk`.

Cada ZIP contém somente um APK. Abra no Chrome, entre na sua conta GitHub e extraia os dois arquivos em **Meus Arquivos → Downloads**. Instale primeiro a voz A, depois o assistente. [Execução](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024) · [Detalhes e integridade da entrega](DOWNLOADS-APK.md).

## Gerar no GitHub, sem Android Studio no celular

1. Entre na sua conta e abra [Actions do projeto](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions).
2. Escolha **Android - APK de teste → Run workflow → main → Run workflow**. Se o menu não aparecer, use Chrome → ⋮ → **Para computador**. O push de código na main também dispara o fluxo.
3. Aguarde os três jobs verdes: **debug-apk**, **Testes instrumentados Android 35** e **Verificar downloads publicados**. O terceiro baixa os dois artifacts já publicados e confere os APKs contra os hashes e assinaturas da compilação. Se algum falhar, abra o log; não confunda o APK compilado de um run com falha com a entrega validada.
4. Abra essa execução e vá a **Artifacts** e baixe **JARVIS-Voz-A-APK** e **JARVIS-Mobile-3.1-APK**. Abra no Chrome com login no GitHub; o aplicativo GitHub pode não oferecer a mesma opção. Cada download é um ZIP com um único APK. Em **Meus Arquivos → Downloads**, extraia ambos e instale primeiro **JARVIS-Voz-A-1.0.0-debug.apk** e depois **JARVIS-Mobile-3.1.0-debug.apk**.
5. Autorize **Instalar apps desconhecidos** apenas para o navegador/Meus Arquivos usado, se o Android pedir, e instale. Você pode desabilitar a origem depois. A retenção solicitada é de 90 dias nas novas execuções, inclusive após o workflow terminar. Confira a expiração efetiva registrada na API/relatório; depois dela, gere outra execução. Artifacts não são hospedagem permanente.

A release antiga “Javis” com ZIP de algumas centenas de bytes não contém este APK. Não precisa comprar serviço, publicar loja nem habilitar cobrança. O projeto é público; repositórios privados têm franquia de Actions conforme conta.

## Links e verificação dos downloads

Os links corretos são do GitHub, no formato `https://github.com/OWNER/REPO/actions/runs/RUN/artifacts/ID`. Não use caminhos internos `/workspace/...` nem URLs temporárias de armazenamento. Login e acesso de leitura ao repositório são necessários, conforme [documentação GitHub](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

No resumo da execução, **Verificar downloads publicados** lista os dois links, nomes reais e tamanhos. O artifact **JARVIS-comprovante-downloads** contém DOWNLOADS.md, downloads.json e SHA256SUMS. Os arquivos são baixados novamente do servidor pelo CI e comparados byte a byte por SHA-256 com a compilação assinada, além de CRC/estrutura APK e identidade do modelo.

## Atualização e assinatura

Sem Secrets opcionais, o certificado debug do CI é temporário e pode mudar a cada execução. Os dois APKs precisam da mesma assinatura, conferida pelo assistente. Se houver “aplicativo não instalado” por conflito de assinatura, **exporte as notas com senha antes de desinstalar** a versão anterior e instalar a nova. Desinstalar remove notas/cache, preferências e o modelo baixado; não remove eventos do calendário nem e-mails originais. Reimporte o backup de notas após instalar e autenticar. A primeira transição da 0.6 também pode exigir desinstalação.

Configure uma assinatura privada fixa para atualizações futuras e Gmail conforme [GMAIL-OAUTH.md](GMAIL-OAUTH.md). O artefato JARVIS-relatorios inclui apk.json com SHA-1/SHA-256 do certificado e hash SHA-256 do arquivo realmente compilado. Não publique a chave privada.

## Primeiro teste no Galaxy

- Configure PIN/biometria de bloqueio Android; abrir Second Brain exige autenticação.
- No JARVIS, escolha Assistente e selecione **A · Executivo clássico → Testar voz** nos ajustes. O motor requer Android 11+, compatível com o S25 FE. Aumente volume de mídia e confira Bluetooth/fones. “Voz do Android” é uma alternativa explícita, com dados instalados pelo provedor. Dados pessoais ficam em texto se não houver voz local.
- Toque no núcleo e permita microfone durante o uso. Primeira ativação diária pode ler o digest; a opção fica na central DIGEST. Diga “Jarvis, minha agenda”, “notícias” ou “abrir ChatGPT”. Sair/bloquear interrompe captura/áudio.
- Abra ⋮ para Second Brain/agenda/radar/digest/e-mails/privacidade. CalendarProvider pede permissão apenas quando necessário. Gmail é opcional e ainda exige OAuth; os demais módulos não dependem dele.
- Para conversa livre, baixe o modelo offline gratuito de 491 MB por Wi-Fi. Comandos simples não dependem desse download.

[Limitações e roteiro físico](TESTES.md) · [Missão 2](MISSAO-2.md) · [permissões e dados](PRIVACIDADE.md).
