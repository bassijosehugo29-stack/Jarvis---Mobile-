# Entrega dos APKs pelo GitHub — 28/10/2026

A entrega anterior por caminhos internos do Codex apresentou timeout/erro ao ler os arquivos no celular. Os APKs estavam íntegros; esta correção troca a entrega por dois artifacts externos do GitHub. Nenhum arquivo do aplicativo, do motor de voz, da interface ou das funcionalidades foi alterado.

**Execução concluída com sucesso:** [40717061024](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024). Os três jobs — debug-apk, Testes instrumentados Android 35 e Verificar downloads publicados — terminaram com success. **87 testes de unidade + 44 Android = 131 aprovados**, sem falhas/ignorados; Lint com zero erros e 39 avisos (30 no assistente, 9 no motor).

Os dois artifacts foram publicados e baixados novamente pelo CI, que confirmou APKs idênticos aos arquivos assinados da compilação. Os dois downloads pela API GitHub também foram repetidos com sucesso **após o término do workflow**. [Comprovante publicado](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12828584225).

## Baixar no Samsung Galaxy S25 FE

É obrigatório entrar no GitHub pelo Chrome para baixar artifacts. [Regra oficial](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

| Instalar nesta ordem | Nome dentro do ZIP | Tamanho do APK | Tamanho do download ZIP |
| --- | --- | ---: | ---: |
| [1. Baixar JARVIS Voz A — APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12830015162) | JARVIS-Voz-A-1.0.0-debug.apk | 143608608 bytes | 94905369 bytes |
| [2. Baixar JARVIS Mobile 3.1 — APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12829740920) | JARVIS-Mobile-3.1.0-debug.apk | 21044192 bytes | 15772750 bytes |

1. Abra os dois links no Chrome, com login no GitHub. Se a conta abrir no navegador interno do Codex, copie o link e abra no Chrome.
2. Cada link baixa um ZIP, formato usado pelo GitHub Actions. Em **Meus Arquivos → Downloads**, abra e extraia os dois ZIPs. Não renomeie ZIP para APK.
3. Instale `JARVIS-Voz-A-1.0.0-debug.apk` primeiro e depois `JARVIS-Mobile-3.1.0-debug.apk`. Se o Android pedir, autorize a instalação pela origem Meus Arquivos.
4. Use os dois arquivos desta mesma execução. Se houver conflito de assinatura com versões anteriores, exporte as notas antes de desinstalar. Desinstalar apaga dados locais.

Se o link não iniciar o download, abra a [execução](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024) e toque em **Artifacts → JARVIS-Voz-A-APK** e **JARVIS-Mobile-3.1-APK**. No Chrome, a opção **Para computador** pode facilitar a exibição da seção. Nenhum caminho `/workspace/...` é necessário.

## Arquivos e verificação

Fonte da correção de entrega: `bf3737d816bc72223c2a5c3ef516f9917ffee94f`. A comparação com `6e0d506490add14f08482c4d13ec77e593e4adb6` contém apenas o workflow e o script de verificação dos downloads. Alterações posteriores de Markdown só registram esta entrega.

Os dois APKs foram recompilados, verificados com apksigner/aapt/zipalign e publicados separadamente. Assinaturas válidas, mesmo certificado, CRC íntegro, ABIs arm64-v8a/x86_64 e bibliotecas alinhadas a 16 KB. Versões preservadas: Mobile 3.1.0/código 8; Voz A 1.0.0/código 1. Motor sem permissões Android solicitadas e com o modelo Faber aprovado incluído.

| APK | SHA-256 |
| --- | --- |
| JARVIS-Voz-A-1.0.0-debug.apk | `fb617256ce003217858cb52a426e1770687d886272eef7b1e97f526b27be3f45` |
| JARVIS-Mobile-3.1.0-debug.apk | `a39ca717f4dd8751eb5447eb38e73adff636cfbc4487c862567d9e39bc8de38a` |

Certificado comum SHA-256: `2b0ff50a78c34d41db107f0fe84bcb6f6d8ae66809c550e2fb56fd0e0e9dfdb9`. A assinatura é de teste, temporária por execução. Esta recompilação usa um certificado diferente da entrega anterior; use sempre o novo par completo.

O job **Verificar downloads publicados** baixa os dois artifacts do servidor, compara tamanho/SHA-256 com os APKs assinados, verifica ZIP/estrutura Android/modelo e igualdade dos certificados, e registra os links no resumo da execução. O comprovante fica em **JARVIS-comprovante-downloads**, com DOWNLOADS.md, downloads.json e SHA256SUMS. Os checks de assinatura anteriores são aplicáveis aos downloads porque os hashes dos arquivos são os mesmos.

## Disponibilidade e regeneração

A API confirmou `expired: false` para ambos. Expiração efetiva: **26/01/2027 às 15:32:53 UTC**, após retenção de 90 dias. Os downloads permanecem disponíveis depois do término da execução; podem expirar ou ser removidos pelo proprietário. Artifacts não são hospedagem permanente.

Após expirar, abra Actions → **Android - APK de teste → Run workflow → main**. Aguarde os três jobs verdes. O resumo da nova execução gera automaticamente os dois links. [Guia completo](APK-PELO-CELULAR.md).

[Fontes correspondentes do motor GPL](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40717061024/artifacts/12829950268) com a mesma retenção. Nenhum serviço pago, conta adicional ou publicação em loja/release foi solicitado. O download pelo navegador físico do Samsung ainda deve ser realizado pelo usuário; a disponibilidade foi verificada pela API após o término da execução e pelo próprio CI.
