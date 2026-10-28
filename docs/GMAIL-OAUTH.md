# Gmail somente leitura e assinatura de teste

O restante do JARVIS funciona sem Gmail. Nesta entrega, o adaptador e a interface estão implementados, mas **não houve autorização ou leitura de uma conta real**. Não há senha ou chave embutida. Conectar Gmail depende de configuração Google sob controle do proprietário e do consentimento da conta. Não é necessário contratar API paga ou informar a senha ao JARVIS.

## 1. Fixar a assinatura antes de configurar OAuth

GitHub Actions só pode atualizar o mesmo aplicativo sem perda de dados quando usa a mesma chave privada. Sem Secrets, cada runner pode gerar um certificado debug diferente. `build/verification/apk.json`, no artefato JARVIS-relatorios, informa SHA-1/SHA-256 do certificado realmente usado e se a assinatura foi temporária.

Crie e guarde uma chave privada em um computador confiável (ferramenta gratuita keytool do JDK):

```sh
keytool -genkeypair -keystore jarvis-test.jks -alias jarvis-test -keyalg RSA -keysize 3072 -validity 10000
# Escolha senhas longas. Guarde arquivo e senhas fora do repositório.
base64 -w 0 jarvis-test.jks > jarvis-test.base64
keytool -list -v -keystore jarvis-test.jks -alias jarvis-test
```

Em Settings → Secrets and variables → Actions do repositório, crie quatro **Repository Secrets**:

| Nome | Valor privado |
| --- | --- |
| JARVIS_KEYSTORE_B64 | Conteúdo Base64 do JKS |
| JARVIS_KEYSTORE_PASSWORD | Senha do arquivo JKS |
| JARVIS_KEY_ALIAS | jarvis-test (ou seu alias escolhido) |
| JARVIS_KEY_PASSWORD | Senha da chave |

Não coloque esses valores em issues, chat, README, workflow ou commits. O workflow reconstrói a chave em RUNNER_TEMP com permissões 600; não faz upload/cache dela. Ele falha se Secrets parciais forem fornecidos. Em pull requests de forks, Secrets não são disponibilizados. Não precisa dar permissão de escrita de código ao workflow.

Depois execute Actions → Android - APK de teste → Run workflow → main. Use o certificado do **APK final** em apk.json. Não use o certificado de um runner anterior. Se a assinatura mudou, exporte as notas com senha antes de desinstalar a versão antiga; a desinstalação apaga dados locais/modelo. Configure a assinatura fixa antes de confiar no app para dados importantes.

## 2. Configurar o projeto Google, sem ativar cobrança

1. No Google Cloud Console, crie/selecione um projeto gratuito e ative **Gmail API**, sem habilitar billing.
2. Em Google Auth Platform / tela de consentimento OAuth, configure o aplicativo e, se externo em teste, adicione explicitamente sua conta como usuário de teste.
3. Configure apenas o escopo `https://www.googleapis.com/auth/gmail.readonly`. Ele é **restrito**. No uso pessoal/teste, respeite as limitações exibidas pelo Google; publicar para outras pessoas pode exigir verificação. Se o console exigir uma etapa indisponível, mantenha o módulo desconectado; não contrate serviços como atalho.
4. Crie credencial OAuth **Android** com pacote `br.com.jarvis.mobile` e SHA-1 do certificado do APK que vai instalar.
5. Não crie client secret web, não baixe senha de serviço nem peça senha principal de e-mail. O AuthorizationClient usa a identidade do app Android; não há client secret a inserir no código nem backend nesta arquitetura.
6. Com Google Play Services habilitado no celular, abra E-MAILS → **Conectar Gmail pelo Google** e confirme na tela oficial as permissões de leitura. Pode ser necessário desbloquear/selecionar sua conta Google no próprio sistema.

Se houver erro `DEVELOPER_ERROR`, confira pacote, SHA-1, cliente Android, projeto de consentimento, Gmail API ativa e usuário de teste. Uma assinatura diferente invalida a configuração. Contas corporativas podem ter políticas administrativas. Apps em teste podem exigir consentimento novamente após sete dias; o JARVIS pede reconexão, não guarda senha.

## 3. O que validar na sua conta

Após consentimento, confira endereço da conta e horário da atualização. São até 100 mensagens da entrada dos últimos sete dias, não todas as pastas. AÇÃO/INFO/RUÍDO são sugestões locais e podem errar; correções não alteram o Gmail. “Ler texto da mensagem” busca conteúdo apenas a pedido; não carrega rastreadores/anexos. Não há enviar/excluir/responder/marcar lida.

Atualizações em background usam WorkManager com rede e bateria, aproximadamente por hora, sem áudio/microfone. “Desconectar e limpar cache” cancela trabalho, remove dados locais e tenta revogar o grant. Sem rede, verifique manualmente Conta Google → Segurança → Conexões com terceiros. Apagar dados locais não apaga mensagens originais.

A execução automatizada valida parsing/classificação/cache/invalidação com fixtures sintéticos; **não valida OAuth real**. Nenhum e-mail é enviado para serviços de IA.

Referências oficiais: [autorização Android](https://developer.android.com/identity/authorization), [escopos Gmail](https://developers.google.com/workspace/gmail/api/auth/scopes), [regras de testes OAuth](https://support.google.com/cloud/answer/15549945).
