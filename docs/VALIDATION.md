# Registro de validação — JARVIS Mobile 0.2.0

## Execução real

- Repositório: https://github.com/bassijosehugo29-stack/Jarvis---Mobile-, branch main.
- Commit de código validado: `8fe8c04b306f7e0507f2f5bd6f0a63b523a07427`.
- [Execução completa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171): **success** nos dois jobs.
- 14 testes de unidade e 8 instrumentados no emulador Android 35, sem falhas.
- Compilação `assembleDebug` e `assembleDebugAndroidTest`: sucesso. Android Lint: 0 erros e 9 avisos não impeditivos, preservados no relatório.
- [APK como artefato](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171/artifacts/12769968379). [Capturas e relatórios de interface](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171/artifacts/12769648745). Artefatos retidos por 7 dias.

## Conteúdo verificado

O APK contém manifesto, DEX, recursos, bloco de assinatura e bibliotecas ARM64, ARMv7, x86 e x86_64. O ZIP e o APK passaram na verificação de integridade. Isso não substitui uma verificação criptográfica independente da assinatura.

- Tamanho: 10368766 bytes.
- SHA-256 do APK extraído: `d714a6f376a45905983435902fab02b67df6d370526da42036bb01b1967df463`.
- Certificado debug SHA-256: `f1681de8732d4ea8066b45721f2bb4691c924bef4ce5bd65336fb80504f4b62c`.
- O certificado difere do APK 0.1.0: desinstalar a versão antiga antes de instalar.

## Validação da interface

A tela inicial da Activity real foi capturada pelo emulador e revista. Também há uma captura com fonte ampliada em 150%. Os testes verificam o cabeçalho fixo durante a rolagem, acesso aos atalhos, pausa de escuta, configurações e abertura das permissões.

A primeira execução da revisão passou nos 22 testes, mas a exportação das capturas falhou porque a pasta privada foi removida na limpeza automática do runner Android. A rotina de captura foi corrigida para usar MediaStore somente nos testes; nenhuma permissão ou gravação de imagens foi adicionada ao aplicativo entregue. A execução indicada acima confirmou a exportação e a conclusão completa.

## Preservação e limitações

O ZIP original de 37 arquivos e o commit inicial `7344899019a5e6fe3562617909d93fbe41e90b44` foram preservados. Não houve force push. A versão 0.1 havia sido validada na execução 40572705460. O redesign preserva os comandos, reconhecimento pt-BR, TTS e liberação do microfone em onStop.

As verificações de estrutura, XML, YAML e scripts passaram localmente (`python3 scripts/check_project.py`). A compilação e os testes Android foram executados no GitHub Actions.

A versão 0.2 ainda precisa de teste físico no Galaxy S25 FE, especialmente disponibilidade dos modelos de voz, One UI e confirmação no Samsung Clock. Não há escuta em segundo plano nem sincronização implementada. Nenhuma API paga, nova assinatura, publicação na Play Store ou chave de API foi utilizada.
