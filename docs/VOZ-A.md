# Voz A aprovada — Executivo clássico

O usuário aprovou a opção A. Esta integração está em validação; resultados anteriores da 3.0 não comprovam o motor novo. Nada foi alterado nos módulos de Gmail, agenda, notícias ou memória nesta etapa.

## Instalar no celular

A entrega contém dois APKs: JARVIS Voz A (motor) e JARVIS Mobile (assistente). Instale o motor primeiro e depois o aplicativo, ambos da mesma execução. Os certificados devem coincidir: o JARVIS confere assinatura, identidade do modelo e ausência de permissões de rede/captura no motor antes de usá-lo.

O motor já inclui o Faber pt-BR aprovado, sem download no celular, conta, chave ou assinatura. A internet só é necessária para obter os APKs. O processo de síntese não tem INTERNET nem RECORD_AUDIO. Não se altera o mecanismo padrão do Android para outros aplicativos.

No JARVIS, perfil A é padrão. Use Assistente → ajustes → Testar voz. Silencioso bloqueia a fala. Se o motor estiver ausente/incompatível, a resposta fica em texto com orientação; nenhuma voz de outro fabricante é rotulada como A. “Voz do Android” é uma alternativa explicitamente selecionável e continua exigindo voz local para dados pessoais.

## Identidade e privacidade

Modelo pt_BR-faber-medium FP32, revisão 5041a7a49ae5b3a0f30e6837769eb03fdbb231bd, SHA-256 original 858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7. O preparador mantém o grafo/pesos originais e adiciona metadados para o carregador. Essa equivalência é checada antes da compilação. O dataset é declarado CC0 no cartão anexado à audição; não há clonagem de uma pessoa da referência.

Length scale 1,08, noise scale 0,55, noise W 0,70 e pausa de 240 ms por sentença. Pitch natural fixo, sem abaixar artificialmente o tom. Filtro passa-altas de 65 Hz, realce discreto em 170 Hz, compressão suave e normalização aproximada a -20 LUFS com limitação de pico. A velocidade na interface é relativa ao perfil. O tratamento reproduz a intenção da amostra; diferenças de runtime/phonemizer e saída do aparelho podem mudar detalhes e precisam de aceite físico.

Textos são sintetizados no próprio motor, inclusive frases novas. Não são uma coleção de gravações. Texto/PCM existem em RAM durante a solicitação; não há histórico, arquivos de fala pessoal ou logs de conteúdo. O motor só guarda dados estáticos do fonemizador. Cancelamento interrompe entrega de PCM e invalida callbacks. Voz, foco, duck e modos continuam coordenados pelo SpeechOutput do JARVIS; não existe serviço de microfone em segundo plano.

## Arquitetura e licenças

O motor é um aplicativo separado, comunicando pelo protocolo Android TextToSpeech. Seu código é GPL-3.0-or-later, com sherpa-onnx v1.13.8/Apache-2.0 e fonemizador eSpeak NG/GPL. Isso não muda a licença do APK separado JARVIS Mobile. Fontes correspondentes e receitas de compilação são fornecidas junto aos artefatos. [Avisos do motor](../voice-engine/THIRD-PARTY.md).

O Gradle compila os bindings Kotlin a partir da versão exata da biblioteca nativa. Arquivos binários/modelos são obtidos somente no build por scripts/prepare_voice_a.py, com SHA-256 fixado e validação de ELF 16 KB. Não são baixados como código executável no celular. Para compilar, instale Python/onnx e execute o preparador antes do Gradle.

## Verificação prevista

Testes do DSP verificam sinal finito/limitado, silêncio, normalização e preservação da frequência fundamental. Testes Android verificam assinatura/metadata/permissões, as quatro frases aprovadas e uma frase inédita, PCM real, seleção pelo SpeechOutput e migração das preferências preservando o modo. As quatro frases sintetizadas no Android ficam no artefato de testes, com conteúdo público de comparação. Nenhum texto pessoal real entra no CI.

Não afirmar aprovação física no Galaxy a partir do emulador. Não há serviço pago obrigatório nem publicação em loja nesta entrega. Consulte o resultado confirmado em [VALIDATION.md](VALIDATION.md).
