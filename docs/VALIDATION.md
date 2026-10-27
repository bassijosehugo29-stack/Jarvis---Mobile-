# Validação da 0.5 ainda em andamento

Não há confirmação de compilação/áudio/conversa da revisão 0.5 neste momento. A seção abaixo é histórica, da 0.4.

# Validação do JARVIS Mobile 0.4.0

## Resultado real

- [Execução completa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094): ambos os jobs com success.
- Commit de código: `8a3b9e0ed1752d4460d880d9be6758caafb57770`; branch main.
- 39 testes de unidade e 11 instrumentados Android 35 aprovados, total 50, sem falhas.
- testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest e connectedDebugAndroidTest concluídos.
- Android Lint com 0 erros e 10 avisos não impeditivos: versões mais recentes de dependências/target, DataExtractionRules e sugestões de KTX. Não foi aplicado baseline para esconder erros.
- [APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094/artifacts/12776313560) e [capturas/vídeo](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40598031094/artifacts/12777163969), disponíveis por sete dias no Actions.

O teste CalendarProviderTest criou uma agenda LOCAL isolada no emulador, gravou um compromisso, leu título/início/fim/fuso/agenda e confirmou ausência de alarme, removendo os dados no finally. Não usa conta externa. InstalledAppsTest consultou atividades reais do launcher e validou o intent explícito para o componente selecionado. Os testes de UI confirmaram a ausência dos atalhos e a acessibilidade do núcleo/configurações. A prévia mostra a Activity real e uma variante com fonte ampliada; o vídeo é silencioso, com o microfone desligado.

## Capturas da interface

A imagem estática jarvis-home.png ficou incompleta no teste do emulador: não mostrou o núcleo central. A causa dessa captura incompleta não foi determinada. A captura com fonte ampliada e o vídeo gravado após abrir novamente a Activity exibiram o núcleo e os controles, e o movimento foi conferido em dois quadros. O arquivo original permanece no artefato para revisão. A prévia PNG distribuída é um quadro extraído do vídeo, sem redesenho da interface. Conferir a renderização no Galaxy no aceite físico.

## Arquivo conferido

- Tamanho: 10434378 bytes; ABIs ARM64-v8a, armeabi-v7a, x86 e x86_64.
- SHA-256 APK: `1d0aab0754edc2b6c9414d2aea998f120f4653c3d908fa60de43256a2c51b5c0`.
- Certificado debug SHA-256: `01bc22399d3c8c8fff6c59011550f4e6da4114f3cf3064736d14ee15024e1ffe`.
- Certificado diferente da 0.3: desinstale a versão anterior para instalar este APK.

ZIP/APK passaram pela conferência CRC; manifesto, DEX, recursos e bloco de assinatura estão presentes. Não foi realizada verificação criptográfica independente da assinatura além das instalações feitas pelo fluxo Android do CI. O aparelho físico do usuário não foi acessado.

## Limites da evidência

Compilação/testes Android foram executados no GitHub Actions; o ambiente local não possui Android SDK. A estrutura, XMLs, shell e links da documentação foram conferidos localmente por scripts, o que não substitui compilação.

O emulador não reproduz áudio de comandos reais. Ainda é necessário testar no Galaxy S25 FE: serviço/modelo pt-BR, TTS/RMS, diálogo de permissões, nomes dos apps, Samsung Calendar/Clock e comportamento ao sair/bloquear. O pedido de calendário é iniciado pelo comando de voz; o teste de provedor concede permissões no emulador para testar a gravação isolada. A sincronização da conta não foi testada, e o alarme não possui confirmação verificável pelo JARVIS.

Sem serviços pagos, API keys, publicação na Play Store, automação de telas privadas ou escuta oculta. O ZIP original e o histórico foram preservados.
