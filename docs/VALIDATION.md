# Validação do JARVIS Mobile 0.4.0

Nova revisão em desenvolvimento: testes, compilação e APK ainda não confirmados. Os resultados da 0.3 abaixo são históricos.

# Validação do JARVIS Mobile 0.3.0

## Resultado confirmado

- [Execução completa](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439): success nos dois jobs.
- Commit: `5870ac9ee8246486554600c332a41cd89ca72046`; branch main no repositório bassijosehugo29-stack/Jarvis---Mobile-.
- 14 testes de unidade e 8 instrumentados Android 35 aprovados, sem falhas.
- assembleDebug e assembleDebugAndroidTest concluídos; Android Lint com 0 erros e 9 avisos não impeditivos.
- [APK de teste](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439/artifacts/12771677174); [capturas e vídeo](https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/actions/runs/40586959439/artifacts/12771093811), retidos por 7 dias.

O APK tem 10401534 bytes e inclui ARM64, ARMv7, x86 e x86_64. O ZIP e o APK foram conferidos quanto à integridade; o manifesto, DEX, recursos e bloco de assinatura estão presentes.

- SHA-256 do APK: `49a90f913e33d16ea6fd26640cf543766769af5034c8978ad2bf916192cb5433`.
- Certificado debug SHA-256: `de602be7afdb53be45086b43117604b47f8bc8bba54435416e10b90dffe9e8db`.
- A assinatura difere da versão anterior; desinstalar antes de instalar. Não foi feita verificação criptográfica independente além da instalação usada pelo fluxo de testes.

## Conferência visual e movimento

A Activity real e a variante com fonte ampliada foram capturadas. O fluxo instalou o APK, abriu a Activity e gravou uma prévia silenciosa H.264, 720x1600, com animações do Android habilitadas. As capturas foram revistas para conferir o HUD e o vídeo foi inspecionado quanto ao movimento.

Na primeira captura do HUD, um aviso do Pixel Launcher do emulador ficou sobre a interface, embora os 22 testes tivessem passado. A preparação das capturas foi ajustada para encerrar somente esse processo externo no emulador, sem suprimir erros do aplicativo sob teste. A execução acima confirmou a nova exportação.

O nível visual de entrada é obtido do serviço Android por onRmsChanged; valores não finitos são descartados e o nível é zerado ao parar, falhar ou responder. Microfone, TTS e clock/animações respeitam o ciclo de vida. O comportamento de RMS, pt-BR, One UI e Samsung Clock ainda exige validação física no Galaxy S25 FE.

## Escopo e preservação

O usuário confirmou prioridade no visual holográfico e nas animações do celular. Conversa livre com IA e integração com notebook não fazem parte desta versão. Os comandos originais foram preservados, assim como o ZIP original de 37 arquivos e o histórico do GitHub. Não houve force push.

Fontes, XMLs, scripts e YAML foram conferidos localmente. Compilação/testes Android foram executados no GitHub Actions. Nenhuma API paga, assinatura adicional, chave de API ou publicação na Play Store foi utilizada.
