# Validação do JARVIS Mobile 0.5.0

## Resultado confirmado

- [Execução](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666): ambos os jobs com success; código `e0d7e8c411e398d6e18ea9cdc5a41681bde0d92c`.
- 47 testes de unidade e 15 instrumentados Android 35 aprovados, total 62, sem falhas.
- testDebugUnitTest, lintDebug, assembleDebug, assembleDebugAndroidTest e connectedDebugAndroidTest concluídos.
- Lint: 0 erros e 19 avisos não impeditivos; não foi usado baseline para esconder erros.
- [APK](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666/artifacts/12780696806); [relatórios, capturas, vídeo e transcrição da conversa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40604934666/artifacts/12779839416), retidos sete dias.

LocalConversationTest importou os pesos oficiais com SHA-256 conferido e enviou duas mensagens pela Activity real, verificando as respostas do modelo e o contexto do nome Rafael. Não é resposta estática: há inferência JNI/CPU com o modelo Qwen. A transcrição exportada usa apenas dados fictícios do teste, não histórico do usuário. O teste não usa a API do ChatGPT ou serviço de inferência remoto.

SpeechOutputTest usou um serviço exclusivo do APK de testes, em processo separado, para produzir PCM não nulo e completar os callbacks TTS, incluindo fallback quando pt-BR falta e falha explícita de síntese. O fixture produz um tom, não uma voz humana: o teste não comprova alto-falante, voz Samsung, Bluetooth, DND ou volume físico do Galaxy. A voz real precisa do aceite no aparelho. Os testes de UI incluem mensagem sem microfone, diagnósticos de voz, ausência de atalhos de apps, pausa, cabeçalho fixo e fonte ampliada. Agenda, launcher e alarmes seguem cobertos.

## APK conferido

- Tamanho: 17496508 bytes; ARM64-v8a e x86_64.
- SHA-256: `8ac0a1688bd700813161f365149ff11a711d7500e94d7faf8a24c5a1273a2a4f`.
- Certificado debug SHA-256: `40efc94ebaf048d62020f59ff43c34ea86c52b058fac32e035d60f11ab1c77a2`.
- Assinatura diferente da 0.4: desinstale a versão anterior antes de instalar.

ZIP/APK passaram pelo CRC. Manifesto, DEX, recursos, modelo JSON e bloco de assinatura presentes. O APK não inclui os pesos GGUF; são obtidos separadamente. As bibliotecas JNI ARM64/x86_64 foram conferidas quanto a segmentos ELF alinhados a 16 KB. Não foi feita verificação criptográfica independente além das instalações no fluxo Android do CI.

## Correções e limites

Na primeira execução da 0.5, a inferência direta com contexto passou; o serviço TTS auxiliar, escrito em Kotlin, não tinha a biblioteca Kotlin no processo separado do APK de testes e falhou. Foi substituído por Java/Android puro. Os cálculos nativos foram otimizados mesmo no APK debug; a versão inicial sem otimização demorou 3m55s no teste de duas respostas. O fluxo passou a coletar evidências mesmo após falha, preservando o resultado real do Gradle. A execução acima valida a revisão corrigida e o caminho pela Activity.

Compilação/testes Android ocorreram no GitHub Actions. A máquina local não tem Android SDK; XML, shell, fontes e links foram verificados localmente, sem confundir isso com compilação. Ainda é necessário ouvir a voz e testar pt-BR/RMS, saída de mídia, permissões, download por Wi-Fi, importação, Samsung Calendar/Clock e interrupção ao sair no Galaxy S25 FE físico. Modelo leve pode ser lento ou impreciso; sincronização de contas e dados atuais não foram comprovados.

Sem cobrança de API, assinatura, chave, publicação na Play Store ou integração paga.
