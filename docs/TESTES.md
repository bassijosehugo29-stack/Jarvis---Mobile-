# Plano de testes

**Resultado confirmado da 0.2.0:** 14 testes de unidade e 8 instrumentados aprovados; compilação e Lint sem erros (9 avisos). [Execução validada](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40582352171). Inclui abertura real da Activity, captura da tela, fonte ampliada, manutenção do cabeçalho ao rolar, atalhos e acesso às permissões. Voz e Samsung Clock ainda exigem teste físico.

## Automatizados incluídos

- `CommandParserTest`: palavra-chave completa/acentuação, ChatGPT explícito, formatos de horário, limites 00:00/23:59, horários inválidos, fallback para ambiguidade, ajuda e parada.
- `ConversationGateTest`: ignora antes da ativação, aceita dentro da janela, expira em 20 segundos, reinicia e estende após resposta.
- `JarvisScreenTest` (instrumentado): controles sem permissão de áudio, pausa, configurações, cabeçalho fixo e fontes ampliadas.
- `MainActivityVisualTest` (instrumentado): abertura da Activity real com microfone desligado e captura da tela, incluindo as barras do sistema.
- `AlarmIntentTest` (instrumentado): ação oficial, horário, interface obrigatória e ausência de horário quando não especificado.
- Android Lint para APIs, manifesto, recursos e problemas estáticos Android.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

O segundo comando precisa de emulador API 26+ ou dispositivo com depuração habilitada. Não testa voz com áudio real. O primeiro job do CI executa testes de unidade, lint e compilação. O segundo usa emulador Android 35 para executar os testes instrumentados. Nenhum desses testes foi executado neste ambiente local; confira os resultados reais no Actions.

## Aceitação manual no Galaxy S25 FE

Registre versão Android/One UI, provedor de voz, modelo pt-BR instalado e versão do APK.

| Caso | Resultado esperado |
| --- | --- |
| Abrir pela primeira vez | Tela preta/azul/dourada, microfone desligado, sem pedido de áudio até tocar em ativar |
| Negar microfone | Mensagem de orientação; botões ChatGPT e alarme continuam funcionando |
| Permitir microfone e ativar | Indicador Android de microfone; tela mostra escuta |
| Dizer “abrir ChatGPT” antes de “Jarvis” | Nenhum aplicativo abre |
| Dizer “Jarvis” | Texto e voz exatos: “Às suas ordens, senhor” |
| Após resposta, dizer “abrir ChatGPT” em até 20 s | Abre o ChatGPT oficial se instalado; escuta para ao sair |
| Esperar mais de 20 s e dizer comando sem Jarvis | Comando ignorado; nova palavra-chave necessária |
| “Jarvis, abrir ChatGPT” | Abre o aplicativo oficial numa frase |
| ChatGPT desinstalado | Mensagem informativa; não abre site nem instala automaticamente |
| “Jarvis, alarme para 7:30” | Relógio abre com 07:30; usuário confirma/salva |
| “Jarvis, alarme para 7 e 30” | Mesmo comportamento 07:30 |
| “Jarvis, alarme para 25:90” | Informa horário inválido; não agenda |
| “Jarvis, criar alarme” / “sete e trinta” | Relógio abre para escolher horário, sem assumir horário |
| Salvar alarme no relógio e fechar JARVIS | Alarme aparece e toca sob controle do relógio |
| Cancelar no relógio sem salvar | JARVIS não afirma que salvou alarme |
| “Jarvis, parar” | Escuta para e confirmação é falada |
| JARVIS falando | Microfone pausado; não executa comandos da própria fala |
| Botão desativar | Microfone liberado |
| Ir à tela inicial / bloquear / abrir outro app | Após `onStop`, microfone liberado, sem escuta em background |
| Voltar ao JARVIS / girar aparelho | Tela funcional; ativação novamente é necessária após parada |
| Revogar áudio nas configurações | Sem crash; próximo uso exige permissão |
| Modelo pt-BR local ausente / serviço desativado | Orientação visível; sem loop acelerado de tentativas |
| TTS indisponível | Resposta textual disponível, ação pode continuar após callback/timeout |
| Modo avião com modelos pt-BR instalados | Verificar disponibilidade real; não há garantia para todo provedor |
| Aumentar fonte / TalkBack / navegação por gestos | Tela rolável, botões acessíveis, conteúdo fora das barras do sistema |

Faça testes de alarmes próximos de um horário seguro e remova os alarmes criados ao terminar. O app só prepara a tela oficial; não consegue verificar se o usuário salvou o alarme.
