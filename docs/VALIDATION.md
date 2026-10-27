# Registro de validação — JARVIS Mobile 0.1.0

Data: 27/10/2026. Repositório: https://github.com/bassijosehugo29-stack/Jarvis---Mobile-. Branch: `main`.

## Compilação confirmada

- Commit de código validado: `456f5a33efd1c830d49e7d86e7f8d1e13d8bf409`.
- Execução: [https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460).
- Job `debug-apk`: **success**.
- Tarefas: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
- Resultado do Gradle: **BUILD SUCCESSFUL**.
- APK: [artefato JARVIS-Mobile-debug](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40572705460/artifacts/12766140769), disponível por 7 dias após a geração.
- 14 testes de unidade: 0 falhas, 0 ignorados.
- Android Lint: 0 erros e 9 avisos. Os avisos são sobre target/dependências mais recentes, regras de transferência/backup e uma sugestão de extensão KTX; estão preservados no relatório, sem baseline para ocultar erros.
- 4 testes instrumentados Android 35: aprovados, sem falhas. O log registra `Starting 4 tests`, `Finished 4 tests` e `BUILD SUCCESSFUL`. A execução completa concluiu com **success** em 27/10/2026 às 19:56 UTC.

## Integridade do APK

O ZIP de artefato foi baixado, sua integridade foi conferida e o APK foi extraído. O APK contém `AndroidManifest.xml`, `classes.dex`, `resources.arsc`, bibliotecas ARM64, ARMv7, x86 e x86_64 e um bloco de assinatura APK.

- Tamanho do APK: 10.335.998 bytes.
- SHA-256 do APK: `e9a589a5bfa73eae0c20e8885dc8574741800d599d555278aec956f4b409f8c5`.
- O hash se refere ao APK extraído, não ao ZIP de artefato.
- A checagem de estrutura e presença do bloco de assinatura não é uma verificação criptográfica independente nem um teste físico no Samsung.

## Correções feitas com base nas execuções reais

1. O pacote antigo `tools` deixou de estar disponível no SDK. O workflow agora solicita apenas `platform-tools` na preparação, além da plataforma Android 35 e Build Tools 35.0.0.
2. O Lint identificou `windowLightNavigationBar` em recurso usado desde API 26, embora o atributo exija API 27. O atributo foi removido do tema base; a Activity configura barras escuras com `SystemBarStyle`, compatível com a faixa suportada.
3. A criação do reconhecedor local contém uma condição explícita para Android 12+.

## Preservação e verificações locais

- O ZIP original de 37 arquivos foi preservado, SHA-256 `ac608e5ea957978a3bcedabf0766d0ba06f1db16bf82edc89694790acbdd5ab5`.
- O commit inicial remoto `7344899019a5e6fe3562617909d93fbe41e90b44` foi mantido no histórico. Nenhum force push foi usado.
- Os XMLs, a estrutura de pacotes, o YAML do workflow e a sintaxe dos launchers POSIX passaram nas verificações locais. Reproduzir com `python3 scripts/check_project.py`.
- O bloqueio anterior de escrita foi resolvido após a instalação do GitHub App pelo proprietário. A compilação ocorreu no runner do Actions; o contêiner local não tem o SDK Android.

## Validação ainda necessária no Galaxy S25 FE

Executar a lista em `TESTES.md`, especialmente pt-BR offline, síntese de voz, permissões, desligamento do microfone ao sair, abertura do ChatGPT e confirmação no Samsung Clock. Nenhum teste físico no aparelho foi realizado por esta entrega. Os testes automatizados não validam disponibilidade do modelo de voz nem todos os comportamentos da One UI.

Nenhum aplicativo foi publicado na Play Store nem foi habilitado serviço pago. A release antiga criada pelo proprietário aponta para o commit inicial; o download correto desta versão é o artefato indicado acima.
