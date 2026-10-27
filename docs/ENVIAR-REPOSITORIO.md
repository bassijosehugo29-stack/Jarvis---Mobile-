# Enviar a versão revisada ao repositório existente

O envio inicial já foi concluído e o acesso foi liberado. Guarde este procedimento apenas como alternativa para manutenção futura caso a integração fique sem acesso de escrita. O repositório já existe e tem um commit inicial; não crie outro e não use force push. O APK de teste foi compilado e validado no GitHub Actions; este documento serve para manutenção dos fontes.

No celular, instale Termux pelo F-Droid, salve `JARVIS-Mobile-revisado.zip` em Downloads e execute:

```sh
pkg update
pkg install git gh unzip
termux-setup-storage
gh auth login --web --git-protocol https --scopes workflow
gh auth setup-git
mkdir -p ~/jarvis-entrega
cd ~/jarvis-entrega
unzip ~/storage/downloads/JARVIS-Mobile-revisado.zip
git clone https://github.com/bassijosehugo29-stack/Jarvis---Mobile-.git repositorio
cd repositorio
git switch main
git pull --ff-only origin main
git config user.name "Seu nome"
git config user.email "SEU_EMAIL_PRIVADO_DO_GITHUB"
cp -R ../JARVIS-Mobile/. .
git diff --stat
git status --short
```

Esses comandos mantêm o diretório `.git` e o histórico existentes e não apagam outros arquivos remotos. Confira as alterações antes de seguir. O README do pacote revisado preserva o título do arquivo remoto original. Se outras pessoas alteraram o projeto depois da entrega, revise conflitos de conteúdo antes do próximo passo.

```sh
git add .
git commit -m "Importar JARVIS Mobile e configurar APK de teste"
git push origin main
```

O login ocorre no fluxo oficial do GitHub; não envie tokens pela conversa. Se a branch estiver protegida, envie uma branch de trabalho e abra um PR, respeitando a proteção.

Abra https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions e aguarde os dois jobs. Só considere compilado se `debug-apk` concluir com sucesso. Para confirmar todos os testes automatizados, aguarde também `Testes instrumentados Android 35`. O artefato `JARVIS-Mobile-debug` contém o APK de teste; extraia antes de instalar.

O repositório consultado é público e usa runners padrão do GitHub, sem plano pago. Nenhuma publicação na Play Store é realizada.
