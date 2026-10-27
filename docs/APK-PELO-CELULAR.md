# Gerar e instalar o APK usando o celular

O repositório deste projeto já está definido: https://github.com/bassijosehugo29-stack/Jarvis---Mobile-. Para baixar um APK gerado, avance para as etapas 2 e 3. A etapa 1 serve para importar uma cópia em outro repositório.

Não é necessário compilar no telefone. O GitHub Actions compila e você baixa o APK pelo navegador. Nenhuma chave OpenAI, cartão, assinatura ou serviço de compilação pago é necessário. Este guia não publica o aplicativo.

## 1. Colocar os arquivos no seu GitHub

Baixe `JARVIS-Mobile.zip` desta entrega. **Enviar apenas esse ZIP ao GitHub não funciona**: os arquivos precisam estar extraídos na raiz do repositório, inclusive `.github/workflows/android-debug.yml`.

Você pode fazer o envio com um cliente Git Android de sua confiança, preservando as pastas ocultas. Uma opção gratuita, com comandos reproduzíveis, é o **Termux obtido do F-Droid oficial** (`https://f-droid.org/packages/com.termux/`):

1. Salve o ZIP na pasta Downloads do celular com o nome `JARVIS-Mobile.zip`.
2. Abra o Termux e execute:

```sh
pkg update
pkg install git gh unzip
termux-setup-storage
```

Conceda acesso aos arquivos quando o Android solicitar. Depois:

```sh
mkdir -p ~/projetos
unzip ~/storage/downloads/JARVIS-Mobile.zip -d ~/projetos
cd ~/projetos/JARVIS-Mobile
git init -b main
git config user.name "Seu nome"
git config user.email "SEU_EMAIL_PRIVADO_DO_GITHUB"
git add .
git commit -m "Versao inicial JARVIS Mobile"
gh auth login --web --git-protocol https --scopes workflow
gh auth setup-git
```

Use o e-mail `noreply` mostrado em GitHub → Settings → Emails se desejar manter seu e-mail pessoal privado. O login abre um fluxo oficial do GitHub com código de dispositivo. Não coloque tokens no código ou aqui na conversa.

Antes de continuar, em GitHub → Settings → Billing and licensing (o nome pode variar), verifique a franquia de Actions e armazenamento para repositórios privados. Mantenha gastos adicionais bloqueados e **não habilite cobrança**. Se não houver franquia disponível, espere a renovação ou compile localmente no computador.

Para criar um repositório **privado na sua conta**, conectar o Git e enviar os fontes, execute no Termux:

```sh
gh repo create JARVIS-Mobile --private --source=. --remote=origin --push
```

Se já existe um repositório vazio seu, use em vez desse último comando:

```sh
git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git
gh auth setup-git
git push -u origin main
```

Não use os dois métodos para o mesmo repositório. O projeto não contém sua conta nem credenciais. Se optar por um repositório público para a gratuidade de runners padrão, os fontes ficarão públicos; essa decisão é sua e não é necessária para o guia acima.

## 2. Executar o workflow

1. Pelo Chrome/Samsung Internet, abra o seu repositório em `github.com`. Ative **Site para computador** se algum controle não aparecer.
2. Abra **Actions**. Habilite os workflows se o GitHub solicitar.
3. O primeiro push em `main` já deve iniciar **Android - APK de teste**.
4. Para executar novamente: selecione esse workflow → **Run workflow** → branch `main` → **Run workflow**.
5. Aguarde o resultado verde. O fluxo instala SDK/Java/Gradle, gera o wrapper oficial, executa testes de unidade e lint e monta o APK.
6. Se ficar vermelho, abra o job `debug-apk` e veja o primeiro passo com erro. Não há APK válido enquanto o passo de compilação não passar. Os relatórios ficam em `JARVIS-relatorios` quando disponíveis.

## 3. Baixar e instalar

1. Abra a execução verde. Em **Artifacts**, toque em **JARVIS-Mobile-debug**. Você precisa estar conectado à conta que tem acesso ao repositório privado.
2. Extraia o ZIP de artefato pelo app **Meus Arquivos** da Samsung. Dentro estará `app-debug.apk`.
3. Toque nesse APK e autorize a instalação dessa origem quando o Android solicitar. Dependendo da One UI, o **Bloqueador Automático** pode impedir instalações externas; se você escolher desativá-lo para este teste, reative-o depois.
4. Instale, abra **JARVIS Mobile**, toque em **ATIVAR JARVIS** e permita acesso ao microfone durante o uso.
5. Diga “Jarvis”, aguarde “Às suas ordens, senhor” e depois diga “abrir ChatGPT”. Para um alarme, ative novamente e diga “Jarvis, alarme para 7:30”. Confirme no relógio.

Os artefatos expiram em 7 dias; execute novamente se precisar. APK debug é para teste pessoal. Uma chave diferente entre execuções pode exigir desinstalar a versão anterior. Os botões de acesso direto funcionam mesmo quando a voz ainda não está configurada.

Consulte o resultado real da compilação no Actions. Testes com voz e relógio no Galaxy precisam ser realizados no aparelho; os testes automatizados não substituem essa verificação.
