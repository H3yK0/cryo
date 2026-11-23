# Cryo — guia geral do projeto

Este arquivo reúne tudo o que é preciso para continuar o Cryo. O Claude Code lê este arquivo sozinho ao abrir a pasta.

> **Regra principal: o PRD manda.** Tudo o que o app faz e como ele é feito e mantido está em **[docs/PRD.md](docs/PRD.md)**,
> com um código para cada item (RF-, RNF-, DS-, TXT-, PRC-...). Toda mudança segue o ciclo do PRD (PRC-01) e a definição de
> pronto (PRC-04): primeiro o PRD, depois o código, os testes e a revisão. `PrdConformanceTest` confere sozinho as regras que
> dá para conferir (licença, permissões, cores fixas, catálogo de telas, schema do banco, changelogs). Este CLAUDE.md é o guia
> de trabalho no código; se ele disser outra coisa que o PRD, vale o PRD.
Para mexer em telas, leia também **[docs/VISUAL.md](docs/VISUAL.md)**. Para a importação de extratos, **[docs/IMPORTACAO.md](docs/IMPORTACAO.md)**.

Este arquivo é **público** (vai para o GitHub junto com o código): nunca escrever aqui senhas, chaves ou dados pessoais.

Situação descrita aqui: **23/11/2026**.

---

## 1. O que é o Cryo

App Android de finanças pessoais: simples de usar, completo, **offline** e de código aberto. Nasceu de uma necessidade real do
Hanry, com a intenção de ajudar outras pessoas a organizar o dinheiro. Tem gente usando de verdade: cuidado redobrado com os dados
de quem já usa (migrações do banco, backups antigos).

O que ele faz: registro de gastos por frase ou voz ("almoço 32,50 no débito", "tv 1200 em 10x no cartão"), previsão do fim do
mês, calendário de calor dos gastos, fluxo do dinheiro, cartões de crédito com faturas e parcelas, dívidas, contas fixas com
lembrete, orçamentos por categoria, metas e caixinhas, investimentos, backup automático (pasta ou nuvem) e, a partir da 1.3,
importação de extratos do banco (OFX, CSV, XLSX, PDF) com conferência antes de entrar.

## 2. Identidade

| Item | Valor |
|---|---|
| Nome | Cryo · slogan "Seu dinheiro, claro como gelo." |
| ID do app | `io.github.h3yk0.cryo` (debug: `io.github.h3yk0.cryo.debug`) |
| Autor público | Hanry Franco |
| Licença | AGPL-3.0-or-later a partir da 1.4 (até a 1.3: GPL-3.0-or-later). Cabeçalho SPDX em todo arquivo `.kt` |
| Código | https://github.com/H3yK0/cryo (branch `main`, tags `vX.Y`) |
| F-Droid | https://f-droid.org/packages/io.github.h3yk0.cryo/ (publicado; receita em `docs/fdroid/`) |
| Releases | https://github.com/H3yK0/cryo/releases (APK `Cryo-X.Y.apk` em cada uma) |
| Idioma | 14 línguas a partir da 1.6 (base pt-BR; en, es, fr, de, it, ja, ko, zh-CN, hi e, desde a 1.6, ru, uk, ar e he — pasta `values-iw`), na moeda escolhida. 10 línguas na 1.4 e na 1.5; até a 1.3: só português e reais |
| Assinatura | Chave própria do Hanry. SHA-256 do certificado: `6e13e284c2f00b72473589391776ed2f28dbad5506f515de3fac99a364a3a2e7` |

## 3. Onde estamos

| Versão | versionCode | Situação |
|---|---|---|
| 1.0 | 1 | Publicada (GitHub, tag `v1.0`) |
| 1.1 | 2 | Publicada (GitHub) |
| 1.2 | 3 | Publicada no GitHub e no F-Droid (commit `a9b3d209bc51ee7531afda8174192a94f1e36f81`). |
| 1.3 | 4 | **Publicada no GitHub em 06/11/2026** (Release `v1.3` com `Cryo-1.3.apk`; etiqueta no commit `5377d7a`, branch `versao-1.3`). |
| 1.4 | 5 | **Publicada no GitHub em 06/11/2026** (Release `v1.4` com `Cryo-1.4.apk`, SHA-256 `4208d49e…82b5`; etiqueta no commit `f8a402b`, branches `main` e `versao-1.4`). F-Droid: merge request **!56186** (licença AGPL + build da 1.4) **aceito em 07/11/2026** (por @linsui); a 1.4 entra na loja quando o servidor do F-Droid compilar e conferir com o APK da Release (alguns dias). |
| 1.5 | 10 | **Publicada no GitHub em 16/11/2026** (Release `v1.5`, etiqueta no commit `18444d8`, branches `main` e `versao-1.5`). **Não vai para o F-Droid** (decisão do Hanry, 16/11/2026): o modelo de voz ia dentro do APK (ERR-057). Junta as cinco etapas feitas depois da 1.4 (descritas abaixo). |
| 1.6 | 11 | **Publicada no GitHub em 22/11/2026** (o Hanry testou o APK e deu o "pode publicar" em 21/11; push de `main` e `versao-1.6` no commit `ddeb228`). As **Releases** foram criadas pelo Hanry na página do GitHub em 22/11/2026 (conferidas com `get_release_by_tag`): `v1.6` (etiqueta em `ddeb228`, `Cryo-1.6.apk` com SHA-256 `daebb287…3e86ee`, o mesmo testado) e `voice-model-1` (etiqueta no commit da 1.4, `f8a402b`; `ggml-base-q5_1.bin` com SHA-256 `422f1ae4…8898` e a licença MIT). Daqui não dá para criar etiqueta nem Release (o push de etiqueta é recusado pela rede, HTTP 403): elas nascem com a Release. Falta o **merge request do F-Droid**, que é do Hanry (`docs/fdroid/1.6-receita.md`). Modelo de voz fora do APK, o fim do mês real, orçamento pela renda e o Geral, 4 línguas novas, os lotes LA1–LA8 e o visual em blocos com a central de avisos (descrição abaixo). **Reconstruída em 20/11/2026**: o contêiner foi apagado com a 1.6 sem commit (veja "Salvar o trabalho" na seção 4). |
| 1.7 | 12 | **Aprovada pelo Hanry em 23/11/2026** ("pode salvar e publicar"): commit e push de `versao-1.7` e `main`; a Release `v1.7` (com `Cryo-1.7.apk`) é dele, como na 1.6. A DLA foi aprovada por ele em 23/11/2026 (PRD 11.1c: "tudo na 1.7", menos a LA5, que foi para a 1.8), depois da falha de processo de 22/11/2026 (seção 4). Branch `versao-1.7`, saído do `main` da 1.6. APK de teste `Cryo-1.7.apk` (SHA-256 `7b8fe0de…157af0`, compilado num clone limpo do `ddeb228` com o backup 11 por cima) entregue em 23/11/2026; compilado de novo em outra pasta, sem a chave, e com a assinatura copiada (`apksigcopier`), sai idêntico byte a byte (o mesmo teste do F-Droid); permissões iguais às da 1.6. Sem commit até o "pode salvar" do Hanry: o trabalho fica nos backups zip (`Cryo-1.7-backup-N.zip`, por cima do `ddeb228`). Não precisa de merge request no F-Droid (os mesmos passos de compilação). |

> **Numeração (decisão do Hanry, 12/11/2026).** A próxima versão pública é a **1.5** e ela junta tudo o que veio depois da 1.4.
> O trabalho foi feito em cinco etapas, que na época tinham nome de versão mas nunca saíram: **etapa 1** (ícone, profundidade,
> motor financeiro, proteção dos dados), **etapa 2** (voz no aparelho), **etapa 3** (avisos, sons, momentos), **etapa 4**
> (auditoria, biblioteca de erros, previsor novo) e **etapa 5** (contas fixas × extrato, perfil, guia, cartão). Onde o histórico
> do PRD, os comentários do código, os nomes de testes (`Screens17Test`, `Audit18Test`, `Screens18Test`, `Screens19Test`) ou os
> branches antigos (`versao-1.6` a `versao-1.9`, marcos de trabalho que não serão publicados) disserem 1.6, 1.7, 1.8 ou 1.9,
> leia etapas 2 a 5 da 1.5. A versão seguinte a esta será a 1.6 de verdade.

A inclusão no F-Droid foi aceita (merge request !50813 no fdroiddata, "New app: Cryo", aceito em 06/10/2026). A receita usa
compilação reproduzível com a chave do Hanry e atualização automática por tag (veja a seção 11).

### O que a 1.3 traz (branch `versao-1.3`)

- **Importar extrato do banco**: OFX, CSV, planilha XLSX (e o ".xls" que na verdade é HTML), PDF. Conferência antes de importar,
  categorias sugeridas, repetidas desmarcadas, fatura de cartão (escolha da fatura, sinais invertidos sozinhos), pagamento de
  fatura reconhecido e convertido em pagamento do cartão, desfazer. Detalhes em `docs/IMPORTACAO.md`.
- **Modo desenvolvedor** (7 toques na versão em Ajustes › Sobre): modo de teste com banco separado (`cryo-teste.db`) e dados de
  exemplo, extratos de exemplo de todos os formatos, JSON de teste, relatório da leitura (com versão anônima).
- **Correção de segurança**: o bloqueio agora aparece também quando o Android mata o app em segundo plano e restaura a tela
  (antes passava direto). Bloqueado, o app sai da tela com o estado guardado: ao desbloquear, a pessoa volta para onde estava.
  Com o bloqueio ligado, a miniatura em Recentes não mostra os dados (Android 13+).
- Lembretes e backup automático sempre usam os dados de verdade (mesmo com as telas no modo de teste).
- Banco de dados versão 3 (coluna `importKey` em `transactions`).

Uma revisão independente encontrou 15 problemas na 1.3, todos corrigidos. A **revisão final** (20/10 a 01/11/2026) confirmou as 15
correções no código e achou outros 17 problemas (lista na seção 13), **todos corrigidos**; os de dinheiro, dados e bloqueio têm testes que falham no
código anterior. Todos os testes passam.

### O que a 1.4 traz (branch `versao-1.4`)

- **10 línguas** (RF-LNG): todo texto em `res/values*/strings.xml` (base `values/` = pt-BR; `values-en`, `-es`, `-fr`, `-de`, `-it`,
  `-ja`, `-ko`, `-zh-rCN`, `-hi`). Segue a língua do celular (inglês se não for uma das 10); Ajustes › Idioma e moeda troca na hora.
- **Moeda do app** (RF-MOE): 20 moedas, padrão pelo país do celular; trocar não converte (só muda o símbolo e o formato).
- **Registro por frase nas 10 línguas** (`domain/PhraseLang.kt`), inclusive japonês e chinês (sem espaços), coreano (partículas) e hindi.
- **Importação de outros países**: colunas nas 10 línguas, datas mês/dia descobertas pelo arquivo, valores e moedas de cada país,
  Shift_JIS/CP949/GB18030, PDF em outros formatos, pagamento de fatura e categorias em outras línguas.
- **Visual 2.0** em todas as telas, **4 abas** (Carteira dentro do Início), **restaurar backup na apresentação**, licença **AGPL**,
  **PRD** (`docs/PRD.md`) com conformidade automática (`PrdConformanceTest`).
- **Passeio animado "Como usar o Cryo"** (RF-TOUR, `ui/screens/Tour.kt`): 5 páginas depois das boas-vindas e em Ajustes › Sobre,
  com ilustrações animadas feitas com os próprios componentes, na língua e na moeda de quem usa.
- **Movimento** (DS-MOV, `ui/components/Motion.kt`): telas deslizam (eixo X), abas com fade-through, barra de abas animada, saldo
  que conta, barras e gráficos que crescem, itens do Extrato animados, atalhos que encolhem ao toque. Respeita "Remover animações".
- **GitHub nas 10 línguas** (RF-LNG-09): `README.md` em inglês (principal) e `docs/readme/README.<língua>.md`; imagens das telas em
  cada língua no fastlane (RF-LNG-10, `StoreScreenshotsTest`), usadas pela loja e pelos READMEs.
- Dados de exemplo na escala de cada moeda (`SampleData.m()`): salário de R$ 4.200 vira ¥210.000, ₩2.520.000.
- Loja (fastlane) nas 10 línguas. Banco de dados continua na versão 3 (sem migração nova).

### O que a 1.5 traz — etapa 1: ícone, profundidade, motor financeiro e proteção dos dados

- **Ícone novo G4** (DS-ICO-02..05): o C quadrado agora **dourado, com espessura e fio de luz**, sem a estrela. Escolhido depois
  de uma pesquisa de psicologia da atenção (cor quente sobre o azul puxa o olho; continuidade com o C conhecido) e da comparação
  de 11 desenhos. Versões: adaptativo, monocromático, notificação, `ic_cryo_badge` (dentro do app) e `ic_cryo_mark` (banner).
- **Profundidade** em todo o app, "com peso de app de iPhone" (DS-PROF, `ui/components/Depth.kt`): fundo agrupado com
  cartões de sombra suave, listas agrupadas, cabeçalho do Início em degradê que pousa sobre a tela, atalhos e botão principal
  (`CryoButton`) com volume, barras elevadas, molas e **vibração ao toque** (sem permissão nova).
- Ajustes no estilo do iPhone (grupos em cartões, ícone em Sobre); ícone do app na apresentação e no bloqueio.
- **Motor financeiro** (RF-MOT, `domain/engine/`, puro Kotlin, tudo no aparelho): estatística robusta (mediana, desvio robusto,
  pesos pelo tempo, Theil–Sen), **recorrências** descobertas sozinhas (salário, aluguel, assinaturas, preço novo, atraso),
  **previsão do mês com faixa** (2.000 cenários; erra 38% menos que a da 1.4 no teste contra o passado), **próximos 3/6/12 meses**
  (400 cenários, faturas, parcelas, contas, dívidas, dia mais apertado), **fora do padrão** e cobranças repetidas, **saúde
  financeira** (0–100 em 6 partes), **ritmo das categorias** e limites sugeridos, **juros embutidos e plano para quitar**,
  **rendimento real** (TIR com datas) com inflação opcional, **metas** (quando chega lá), **categoria aprendida** (naive Bayes) na
  importação e no registro por frase, **percepções** com nota de importância. Telas: Saúde financeira, O que se repete, Plano
  para quitar, cartões novos em Análises, faixa no Início.
- **Proteção dos dados** (RF-SEG): cofre do Android (Keystore) com HMAC-SHA256 (o motor só guarda hashes) e AES-256-GCM;
  **backup com senha** opcional (PBKDF2 310 mil voltas, envelope DAD-11), restauração pedindo a senha, lembretes sem nome nem
  valor na tela de bloqueio, nada em logs. Rust e criptografia do banco inteiro avaliados e deixados para depois (seção 13).
- **500 testes de garantia** (RF-MOT-15): estabilidade, escalabilidade, desempenho, economia de recursos e segurança, 100 cada.
- Imagens da loja e do guia visual refeitas. Banco de dados continua na versão 3 (sem migração nova).

### O que a 1.5 traz — etapa 2: voz no aparelho

- **Voz no aparelho** (RF-VOZ, PRD 6.2a): o Whisper (whisper.cpp v1.9.5, MIT, cópia em `app/src/main/cpp/whisper/`) compilado
  pelo NDK numa biblioteca só (`libcryo_whisper.so`, 1,6 MB, só arm64 com fp16+dotprod; nos outros, uma biblioteca vazia) e o
  modelo "base" q5_1 (57 MB) em `assets/whisper/`, lido direto do APK. O áudio só passa pela memória.
- **Tela de eco** (`ui/tx/VoiceSheet.kt`): o protótipo aprovado virou a tela de verdade (palavras ao vivo, anéis por sílaba,
  cartõezinhos com mola). Microfone pedido com explicação (`MicPermissionSheet`).
- **Português falado** (`domain/Spoken.kt`): números por extenso, "18 e 90" = 18,90, correções ("32, não, 35"), apelidos
  ("roxinho"), vários lançamentos numa fala, frases que o Whisper inventa no silêncio descartadas.
- Escolha do ouvido em Ajustes (Whisper ou o reconhecedor do celular só no aparelho); sem nenhum dos dois, a tela de voz do sistema.
- Permissão nova `RECORD_AUDIO`. APK de ~6 MB para ~65 MB. Banco de dados continua na versão 3.

### O que a 1.5 traz — etapa 3: avisos, sons e momentos

- **Botão de voz no meio da barra de baixo** (RF-ALC, `ui/Nav.kt` `VoiceTabBar` + `ui/tx/VoiceLauncher.kt`): tocar = falar, segurar = escrever. O campo de frase do Início ficou só para escrever.
- **Avisos inteligentes** (RF-AVI, `notify/AlertPlanner.kt` = regras puras; `Alerts.kt` = canais e notificação; `AlertWork.kt` = os dois horários e
  os botões; `AlertState.kt` = só *quando* cada aviso saiu e as escolhas): no máximo 1 aviso comum por dia, só de manhã (~9h, worker
  `BillReminderWorker`, nome da 1.0) e no horário do lembrete do dia (`EveningAlertWorker`). Tom Amigo (3 textos) ou Discreto.
  Botões na notificação: Já paguei, Lembrar amanhã, Dia sem gastos, Registrar salário, Anotar (os que mudam dados pedem desbloqueio).
- **Notificação animada** (RF-AVI-09): o Android só anima imagem dentro do `MessagingStyle`; cada aviso é uma mensagem do Cryo com
  uma figurinha WebP animada (`res/raw/sticker_*.webp`, servida pelo `FileProvider` `${applicationId}.stickers`). O resumo da
  semana é um gráfico parado (`BigPictureStyle`).
- **Modo compras ao vivo** (RF-AVI-10, `notify/Shopping.kt`): `ProgressStyle` + `setRequestPromotedOngoing` no Android 16 (Now Bar
  da One UI 8); permissão nova `POST_PROMOTED_NOTIFICATIONS` (não pergunta nada).
- **Momentos** (RF-MOM, `ui/components/Moments.kt`, `MomentBus` no `AppContainer`): meta completa, orçamento em 80%/estourado com
  a sugestão de onde cobrir, sequência de dias, fechamento do mês. Calculados em `momentAfterSave` (`ui/tx/TxForm.kt`).
- **Ilustrações próprias** no lugar de emoji (RF-ILU, `ui/components/Art.kt`, cores em `ui/theme/ArtColors.kt`); metas com `GoalArt`.
- **Painel do mês** (`plan/budgets/month`, `ui/screens/BudgetMonthScreen.kt`) e **remanejar só no mês** (`budget_adjustments`).
- **Ajustes › Avisos e sons** (`settings/alerts`, `ui/screens/AlertScreens.kt`, com "Ver exemplo" = `AlertExamples`).
- **Sons** (RF-SOM, `notify/Sounds.kt`, `res/raw/som_*.ogg`, gerados por `tools/sons/make_sounds.py`): só com o celular no modo normal.
- **Voz**: o som de "pode falar" não entra na gravação; fim da fala em 1,2 s (como na 1.5); tempo na tela de voz e chave
  experimental "Atenção rápida no Whisper" no modo desenvolvedor. Estudo de outras IAs de fala no PRD (RF-VOZ-14).
- **Banco de dados versão 4** (`budget_adjustments`, `no_spend_days`, `goals.art`; migração 3→4 automática com `Migration3To4`).

### O que a 1.5 traz — etapa 4: auditoria, biblioteca de erros e previsor novo

- **Auditoria do código inteiro** (RF-AUD, PRD 6.12c): backup com senha nunca sai aberto (falha fechada), backup lido com limite e
  conferido campo a campo, planilha sem fórmulas, erro do sensor só libera aparelho sem trava, `FLAG_SECURE` no Android 8–12 com o
  bloqueio ligado, valores enormes por `Money.centsOrNull`, "Já paguei" em transação, apagar categoria leva os remanejamentos,
  cada aviso com o seu `PendingIntent`. Testes `Audit18Test`, `Audit18DataTest`.
- **Bloqueio** (achado pelo Hanry no teste, ERR-039): abrir do zero sempre pede o desbloqueio (`savedInstanceState == null` em
  `MainActivity.onCreate`), mesmo com o processo vivo; ligar a chave passa pelo desbloqueio uma vez (`confirmDeviceLock`).
- **Biblioteca de erros = lei** (`docs/ERROS.md`, PRC-08): cada erro já corrigido, a regra e o teste que guarda; `KnownIssuesTest`.
- **Previsor novo, do zero** (RF-PRV, PRD 6.12b, `domain/predict/`): série diária sem os extremos (`Series.kt`), 7 modelos clássicos
  (`Models.kt`: perfil da semana, médias de 3 e 6 meses, o mesmo trecho do mês puro e no nível de agora, Holt amortecido, Theta),
  conjunto pesado pelo erro de cada modelo no passado da própria pessoa, com o **erro encolhido para a média** quando há pouco
  passado (`Ensemble.kt`), época do ano só confirmada em dois anos (`Season.kt`), previsão do mês com a faixa **conferida no
  passado** (calibração conforme, `Forecaster.kt`), **categorias** com a chance de passar do limite (`CategoryForecast.kt`, também
  usada no aviso de ritmo) e **investimentos** "se continuar como foi" (`InvestForecast.kt`: Dietz modificado, sobe e desce, maior
  queda, 1/3/5 anos). O primeiro motor, o da etapa 1 (`Outlook.month`, `Pacing.projected`), fica como régua dos testes.
- **Telas** (RF-PRV-08): Análises (conferida no passado, o que mais pesou, época do ano), Planejar › Orçamentos (como cada categoria
  fecha o mês e a chance de passar), investimento e Carteira ("se continuar como foi"). 23 textos novos nas 10 línguas.
- **Laboratório** (`ForecastLabTest`, só com `-PforecastLab=true`; mundos inventados em `ForecastWorld`, 10 com `MONTHLY_SHOP`).
  Medir qualquer mudança no previsor antes de aceitar (várias ideias "óbvias" pioraram; lista no PRD 6.12b):
  ```bash
  ./gradlew testDebugUnitTest --tests '*ForecastLabTest' -PforecastLab=true     # tudo (~5 min); tabelas em app/build/forecast-*.txt
  ./gradlew testDebugUnitTest --tests '*ForecastLabTest.tabela' -PforecastLab=SEASONAL,STEADY:6   # só alguns mundos, 6 sementes
  ```
- Nenhuma permissão nova; banco de dados continua na versão 4.
- **Avisar o Hanry**: no Android 8–12, com o bloqueio ligado, não dá para tirar print do app (`FLAG_SECURE`); backup estragado agora é
  recusado com mensagem; num mês fora do padrão que nunca se repetiu, o primeiro motor reagia mais depressa (o novo ganha no conjunto).

### O que a 1.5 traz — etapa 5: contas fixas × extrato, perfil, guia e cartão

- **Contas fixas × extrato** (RF-CFX, PRD 6.19d): `domain/BillMatch.kt` = `BillMatcher` (regra pura: CERTA = nome + valor igual, ou
  apelido aprendido; PROVÁVEL = nome com outro valor, ou valor igual perto do vencimento) e `BillReconciler` (sugestões do mês e dos
  3 anteriores). A importação liga a linha à conta fixa (`ImportRow.bill`/`payBill`, `ImportPlanner.withBills`); o `Ledger` trata o
  casamento CERTO ainda não ligado como pago (`inferredBillPayments`, `BillStatus.inferred`), para a previsão e os avisos não
  contarem em dobro; "Já paguei" (tela e notificação) liga ao que já existe antes de gravar; desfazer só apaga o que foi feito à
  mão. `billYm = 0` sem `billId` = "a pessoa disse que não é conta fixa" (`BillMatcher.NOT_A_BILL`). Apelidos por HMAC no
  `EngineStore` (`billAlias`, `learnBill`). Criar a conta fixa depois pergunta pelos lançamentos que já existem (`c.billReview`).
- **Perfil** (RF-PRF): `ui/components/Avatar.kt` (letra, desenho, emoji ou foto; `Avatars.savePhoto` guarda 256 px em
  `filesDir/avatar.jpg`, fora do backup), `ui/screens/ProfileScreens.kt` (rota `profile`); a bolinha do `HomeHeader` abre o painel
  no lugar da engrenagem. Ajustes novos `avatar` e `avatarColor`.
- **Guia "Aprender o Cryo"** (RF-GUI): `domain/Guide.kt` (18 lições em 6 degraus, `GuideFacts` só com quantidades, `GuidePlanner`
  puro: no máximo 1 dica por dia e 3 por semana, a lição volta em 7 dias uma vez, 3 "Agora não" pausam 14 dias),
  `data/GuideStore.kt` (SharedPreferences `cryo_guide`: só contagens e dias), `ui/components/Guide.kt` (`GuideHost` = o cartão acima
  da barra, `GuideSpotlight` = o destaque, `Modifier.guideTarget`, `showLesson`), `ui/screens/LearnScreen.kt` (rota `learn`).
  Contadores pelo `AppContainer.guideCount` (o modo de teste não conta nem recebe dicas).
- **Cartão, a partir do issue #3** (RF-CAR-07 corrigido, RF-CAR-10, RF-CAR-11): a receita pode entrar num cartão (estorno, botão na
  tela do cartão); `TxDraft.invoiceYm` guarda a fatura do lançamento: ao editar ela **fica onde está** e só volta a seguir a data
  quando a data, o cartão ou o tipo mudam (os setters do rascunho zeram a escolha); "Fatura em que entra" e "Fatura que este
  pagamento quita" no formulário; `FinanceRepository.adjustInvoice` lança a diferença para o total do banco como um ajuste à vista.
- **Sons** (RF-SOM-01 revisto): kit refeito, abafado e grave (`tools/sons/make_sounds.py`, com `--medir` e `--ogg`).
- **Auditoria**: ERR-040 a ERR-045 em `docs/ERROS.md` (previsão em dobro, conta paga à mão × extrato do cartão, desfazer que apagava
  o importado, estorno que saía do cartão, compra que mudava de fatura ao editar, "Já paguei" da notificação sem olhar o extrato).
- **Pedidos de quem usa** (issue #3 do GitHub, 12 e 13/11/2026, aprovados pelo Hanry):
  - **Escolher qualquer fatura** do cartão (RF-CAR-10): os chips mostram as vizinhas e "Outra…" abre a lista inteira
    (`InvoicePickerSheet` em `ui/tx/TxForm.kt`), com o total de cada fatura, para pôr compras antigas no mês certo.
  - **A previsão responde em uma frase** se a conta fica negativa, a partir de quando, e qual o menor saldo
    (`Projection.firstNegativeWithin`, cartão em `ui/screens/EngineScreens.kt`).
  - **Contas fixas que não são mensais** (RF-FIX-06..10): ritmo (todo mês, 2, 3, 6 meses ou anual), primeiro vencimento,
    parcelas de cada ciclo com o preço à vista para mostrar os juros, prazo ("até quando") e **modelos por língua**
    (`data/BillTemplates.kt`: aluguel, luz, água, internet, celular, assinatura, plano de saúde, seguro do carro, e os
    impostos de cada país — IPVA/IPTU no Brasil, *Vehicle registration* em inglês, 自動車税 em japonês). As regras ficam em
    `BillCycle` (`domain/Ledger.kt`); o `Ledger`, a previsão, os avisos e o casador já seguem o ciclo. Ao criar, a chave
    "Já paguei o de ..." fecha o vencimento que já passou por `FinanceRepository.markBillPaidLinking` (liga ao pagamento
    que existe; só lança um novo se não houver) — o mesmo caminho do botão da tela e do botão do aviso.
  - **Apoiar o Cryo** (RF-AJU-12): Ajustes › Sobre, READMEs e o campo `Donate:` da receita do F-Droid apontam para o Buy
    Me a Coffee. O app continua sem permissão de internet: quem abre a página é o navegador.
  - **ERR-046** (a barra de desfazer cobria o botão "+") e **ERR-047** (botão flutuante sem nome para o leitor de tela).
- **Teste do Hanry (14/11/2026)**, tudo corrigido na mesma 1.5:
  - **Leitor de PDF, três defeitos achados com um extrato de verdade** (ERR-049/050/051, `importer/PdfStatement.kt`): o fim
    de um CNPJ colava no valor ("…/0001-49 512,30" virava R$ 49.512,30); o cabeçalho repetido de cada página
    ("01 de novembro de 2026 a 13 de novembro de 2026") virava a data de tudo o que vinha depois; e um traço do próprio
    texto ("LTDA - 87,50") invertia o sinal. Com os três consertos, o extrato do Hanry fecha **no centavo** com o banco.
    O extrato de verdade **não entra no repositório** (PRC-06): o teste-guarda usa um inventado no mesmo formato.
  - **Apagar uma importação inteira** (RF-IMP-23): `domain/ImportBatches.kt` agrupa por destino + hora da importação;
    tela `imports` (`ui/screens/ImportsScreen.kt`), com confirmação e desfazer.
  - **Estornar uma movimentação** (RF-LAN-09): botão na tela do lançamento, cria o contrário com desfazer.
  - **ERR-048**: Ajustes › Avisos só mudava depois de sair e voltar (a tela lia o contador de mudanças sem usar o valor).
  - **Revisão antes de publicar (15/11/2026)**: **ERR-052** (o conserto do ERR-051 deixava a seção mandar sempre: o
    resumo do topo, "Débitos 250,00", virava seção do arquivo inteiro e um salário marcado com "C" virava saída; agora a
    marca forte do banco vence e só o traço solto segue a seção) e **ERR-053** (o estorno levava a categoria de gasto
    para a receita; a regra virou a função pura `refundOf`, em `ui/tx/TxForm.kt`, com teste).
- **Saldo igual ao do banco, sozinho** (RF-IMP-24, ERR-054, teste do Hanry de 15 e 16/11/2026): o Cryo mostrava menos
  que o banco — era o que **já estava na conta antes do extrato** (o saldo inicial), não outro jeito de fazer a
  conta (só há soma e subtração: a ordem não muda o resultado). `importer/PdfStatement.kt` lê "Saldo inicial" e "Saldo
  final do período" (`Statement.opening`, `balance`, `extra`, `readGap`); `importer/BalanceCheck.kt` é a regra pura
  (`autoShift` = saldo do banco na véspera menos o do Cryo na véspera; `gap` = banco × Cryo no último dia do extrato);
  `ImportModel.import()` aplica o acerto no **saldo inicial da conta** (`FinanceRepository.shiftBalance`, nunca um
  lançamento) e guarda em `Step.Done.shift`; a tela final (`BalanceCard` em `ImportScreen.kt`) mostra as etapas e o
  "Desfazer o acerto". **ERR-055**: no celular o cabeçalho do PDF chega com rótulo e valor em linhas separadas, ou
  todos os rótulos e depois os valores (`readBalances` lê os três formatos com uma fila de rótulos). O OFX deduz o saldo
  de antes quando o `DTASOF` está dentro do período (`Statement.balanceDate`). Reabrir um extrato que já está inteiro no
  Cryo (toda linha `IMPORTED_BEFORE`) acerta o saldo na conferência (`ImportModel.syncBalance`, `reviewShift`).
- **Pedidos de 17/11/2026 feitos**: **RF-FIX-11**, conta fixa paga por fora (`MoneyActionSheet` ganhou `offLabel`/`onConfirmOff`;
  o pagamento nasce sem conta e sem cartão: conta no mês, não mexe em saldo) e **RF-EXT-07**, busca do Extrato com período
  (Tudo, Mês, Ano) e por valor (`domain/TxSearch.kt`, regra pura; `MonthSelector` aceita os rótulos das setas).
- 178 textos novos nas 10 línguas. Nenhuma permissão nova; **banco de dados versão 5** (migração 4→5 automática: a conta
  fixa de quem já usa continua mensal, sem parcelas e sem prazo).
- **Avisar o Hanry**: quem já usa pode ter compras importadas que mudaram de fatura ao serem editadas antes da 1.5: agora dá para
  devolvê-las pela "Fatura em que entra" ou acertar o mês com "Ajustar total". O issue #3 ainda está sem resposta no GitHub (há um
  rascunho de resposta na entrega da 1.5; só o Hanry posta).

### O que a 1.6 traz (versionCode 11; lançada em 21/11/2026, branch `versao-1.6`, etiqueta `v1.6`)

- **Modelo de voz fora do APK** (RF-VOZ-15, ERR-057, `docs/VOZ-MODELOS.md`): `voice/WhisperModels.kt` (os 6 modelos oficiais, com
  tamanho e SHA-256), `voice/WhisperModelStore.kt` (copia para `noBackupFilesDir/whisper/` conferindo no caminho; confere de
  novo antes de usar), `ui/screens/VoiceModelUi.kt` (a folha "Baixe a voz do Cryo uma vez" e Ajustes › Modelo de voz). A ponte
  nativa abre arquivo (`nativeOpenFile`); `assets/whisper/` e os shims de teste saíram; `SourceTreeTest` barra binários. APK de
  ~65 MB para ~9 MB. O teste no computador (`build-host.sh`) baixa o modelo (ou `CRYO_WHISPER_MODEL=/caminho`).
- **O fim do mês real** (RF-PRV-10 reescrito em 21/11/2026, ERR-056 e ERR-064 com a lei nova): `domain/predict/MonthEnd.kt`
  (`MonthEnds.of(l)`, sem o motor) é a conta única do Início, de Análises e da central: **renda − despesas**, só o que já
  aconteceu e o que é fixo, nada estimado. Linhas (`EndLine`): renda fixa (contas fixas de entrada, agendados e o que entra
  todo mês há 3 meses, `MonthEnds.monthlyIncome`), outras entradas, contas fixas, metas e investimentos (com o que falta
  guardar no mês para as metas com prazo, `goalLeftThisMonth`), dívidas e parcelas, despesas variáveis e saídas agendadas.
  "Com o dia a dia / Só o certo" e "Contar entradas avulsas" saíram da tela (a escolha das avulsas ficou só para o motor).
- **Orçamento pela renda** (RF-ORC-07/08): `domain/IncomePlan.kt`, `data/IncomePlanStore.kt`, `ui/screens/BudgetIncomeScreen.kt`
  (rota `plan/budgets/income`). O plano nunca sugere limite menor que as contas fixas mensais (RF-ORC-09).
- **Russo, ucraniano, árabe e hebraico** (RF-LNG-11, ERR-058): textos tirados exatos do APK que o Hanry testou; `PhraseLang`
  ganhou `prefix` (artigo e preposições grudados no árabe e no hebraico); palavras-chave em radical no russo e no ucraniano
  ("зарплат"); `LocaleSettings.latinDigits` (árabe com 0–9) e `Res.s/Res.p` formatando com `AppLocale.locale`; RTL nos
  deslizes (`Motion.kt`); colunas, meses e codificações dos extratos (windows-1251, KOI8, windows-1256, windows-1255).
- **Lotes LA1–LA8** (aprovados na DLA de 19/11/2026, PRD 3.14, PRC-11):
  - LA1 `ui/components/HealthRing.kt`: a nota da saúde ao lado do saldo (RF-INI-18).
  - LA2 e LA3 `domain/AccountStatement.kt`: extrato da conta por dia com o saldo depois de cada lançamento (RF-CON-08) e o
    filtro "Todas as contas" do Extrato (RF-EXT-09); a linha do lançamento com a bolinha da conta (RF-EXT-08, `TxRow`).
  - LA4 `EntryDefaults` (`ui/tx/TxEditorScreen.kt`): o último dia usado (`AppSettings.lastTxDate`, chip "Último:") e a fatura
    da tela do cartão (`Routes.txNew(..., invoice)`) (RF-LAN-10, RF-CAR-12).
  - LA5 agendados com fundo e reloginho (RF-EXT-10); "Previstas" no Extrato e **pular o mês** (RF-EXT-11, RF-FIX-12):
    tabela `bill_skips` (banco versão 6), `Ledger.billStatuses` tira o mês pulado de tudo; "Puladas neste mês".
  - LA6 `ui/components/Sheet.kt` (`CryoSheet`): a lista de idiomas tremia (ERR-059); toda folha passa por ela.
  - LA7 `domain/BudgetBills.kt` + `data/BudgetBillStore.kt`: o orçamento conta com as contas fixas (RF-ORC-09).
  - LA8 `domain/predict/BudgetPlan.kt` (o orçamento no fim do mês, RF-PRV-11), `domain/engine/YearOutlook.kt` ("Como vou fechar
    o ano?", RF-MOT-17: projeção até 31/12, sinais, zoom, arrastar e pinçar; `YearChart` em `Charts.kt`) e
    `domain/engine/GoalForecast.kt` (previsão de metas, RF-MOT-16), no lugar de "Próximos meses".
  - A chave "nunca mexer no saldo" na conta fixa **não será feita** (decisão do Hanry, 19/11/2026).
- Banco de dados **versão 6** (migração 5→6 automática, só cria `bill_skips`). 53 textos novos dos lotes nas 13 línguas.
- **Reforma visual** (pedido do Hanry de 21/11/2026, lotes LA9 a LA13, DLA no PRD 11.1b, PRD 3.17):
  - LA9 **central de avisos** (RF-INI-19/20): `domain/Inbox.kt` (regras puras: grupos, ordem, o número do sino, o destaque;
    a percepção que repete um aviso fica de fora pelo `topic` "alert:…" de `Insight`), `data/InboxStore.kt` (o "visto" é só o
    HMAC da chave, SharedPreferences `cryo_inbox`/`cryo_inbox_teste`, esquece depois de 90 dias), `ui/screens/InboxScreen.kt`
    (rota `inbox`); o sino entra no `HomeHeader(bell = …)`. No Início, no máximo um destaque (`ToneNotice`), nunca o fim do mês.
  - LA10 **tons e blocos**: `CryoTheme.tones` (`ui/theme/Theme.kt`: alerta rosa, atenção pêssego, bom menta, informação
    azul-gelo, com o par escuro) e `ui/components/Tiles.kt` (`CryoTile` com `leading`/`trailing`, `TileRow`, `TileButton`
    com `stacked`, `Expandable`, `ToneNotice`, `GroupTitle`, `ProgressRing`). Início em blocos (`homeTiles` em `HomeScreen.kt`).
  - LA11 **Análises** (RF-ANA-07): o fim do mês real com Renda × Despesas e as linhas fechadas; os 6 meses logo abaixo; o
    ano (a explicação fechada); a saúde; o fluxo em blocos (`FlowBlocks`, o desenho do Sankey num `Expandable`); as 4 maiores
    categorias. Saíram as percepções, a previsão de metas (foi para Planejar › Metas) e "O que se repete" (central e Contas fixas).
  - LA12 **Planejar**: o orçamento **Geral** (RF-ORC-10, `domain/GeneralBudget.kt`; DAD-17: é o `Budget` de código de
    categoria 0, `GeneralBudget.ID`, sem migração), orçamentos em blocos (`BudgetTile`), botões-bloco para a renda e o painel,
    as sugestões das contas fixas num cartão só; contas fixas com três blocos (`BillStat`), as pagas e as puladas fechadas e
    "O que se repete" no fim (RF-FIX-13); metas em blocos com a folha das quatro ações (`GoalTile`, `GoalSheet`, RF-MET-07).
  - LA13 **Ajustes por tipo** (RF-AJU-13; a conta principal numa folha, a senha dos backups em Segurança) e **Ajustes › Voz**
    (RF-VOZ-16, `VoiceSettingsScreen` em `ui/screens/VoiceModelUi.kt`, rota `settings/voice`).
  - 73 textos novos nas 14 línguas (`tr_visual.py` no rascunho da sessão); 48 textos que ficaram sem uso saíram das 14 línguas.
- **Diferenças conhecidas da 1.6 original** (a reconstrução refez a lógica a partir do APK): a tela do orçamento pela renda da
  1.6 original tinha uma folha a mais (26 `CryoSheet` contra 25 agora); o registro por frase não lê milhar com espaço
  ("25 000"), como na 1.6 — anotado como ideia.
- **Auditoria independente** (21/11/2026: relatório do GPT-6 conferido no código, com testes; correções aprovadas pelo Hanry;
  PRD 3.16, ERR-060 a ERR-066):
  - `Outlook.expectedItems`: um agendado tira só a ocorrência que ele cobre (até um período dela), nunca a recorrência
    inteira (ERR-060: um salário agendado apagava os outros 11 meses da projeção). `RecurrenceScheduledTest`.
  - `Projection` (passo 4): pagamento de dívida agendado só conta dentro do período e cobre a parcela de vencimento mais
    perto; o que sobra vai para as seguintes e depois para as anteriores (ERR-061). `DebtProjectionTest`.
  - `ImportPlanner.plan`: a mesma fatura só casa a **parcela** do Cryo; o pareamento é global, pela mesma fatura e pela data
    mais perto, nunca pela ordem do arquivo (ERR-062). Testes novos em `StatementImportTest`.
  - `BackupCrypto.isEncrypted` olha a chave `"cipher":` do envelope (ERR-063). `BackupEnvelopeTest`.
  - `MonthEnds.of` sem o motor fica sem a linha dos orçamentos (ERR-064). `MonthEndTest`.
  - `WhisperEngine` conta as chamadas em andamento: fechar pede para parar, espera e solta uma vez só; a ponte é trocável nos
    testes (`WhisperBridge`, ERR-065). `WhisperEngineGuardTest`, `WhisperHostTest.fecharNoMeioDeUmaContaEhSeguro`.
  - `WhisperModelStore`: SHA-256 no primeiro uso de cada abertura (`usableFile`), temporário largado apagado, `openWith` não
    apaga um modelo bom que não abriu (ERR-066). Testes novos em `WhisperModelStoreTest`.
  - Conferidos sem defeito: a criptografia do backup, a compilação reproduzível (o APK sai idêntico compilado em outra
    pasta), o alinhamento de 16 KB e a escolha do processador.

### O que a 1.7 traz (versionCode 12; pronta para o teste do Hanry, branch `versao-1.7`)

Pedido do Hanry (22/11/2026): "melhorias pequenas e resolução de bugs como travamentos"; em 23/11/2026, as issues #5, #6 e #7
e quatro ideias novas. Lotes LA1–LA12 e a DLA aprovada no PRD 11.1c (a LA5, importar o banco do xpenc, ficou para a 1.8).

- **Caça a travamentos** (LA1, RNF-EST-01): `CrashSmokeTest` abre ~60 telas (inclusive as de detalhe com um id que não existe
  e as da 1.7: Ajustes com o bloqueio, telas com blocos escondidos, a folha de organizar e a tela de editar o widget) em seis
  situações: sem dado nenhum, com os dados de exemplo, com os valores escondidos, no escuro, em árabe (RTL) e com a fonte em
  200%, rolando cada uma; falha em qualquer exceção. Nenhuma tela quebrou. Na leitura do código saíram três defeitos:
  **ERR-067** (as iniciais da semana do calendário fixas em português; agora `weekInitials(AppLocale.locale)` em `Charts.kt`),
  **ERR-068** (com "Esconder valores", o desenho do fluxo mostrava o total e o leitor de tela lia os valores) e **ERR-069**
  (a voz fechava o app com U+2028, U+2029 ou U+0085 no texto ouvido: `Spoken.BLANKS` e `TRAILING_PUNCT` com `DOT_MATCHES_ALL`,
  sem `!!`).
- **Bloqueio por tempo e modo privacidade** (LA2 e LA6, issues #6 e #7, RF-BLQ-02, 05, 07 e 08, **ERR-070**):
  `domain/LockRules.kt` é a regra pura (`relock`, `stopByOwnLaunch`, `secure`; `CHOICES` = 0, 60, 300 e 900 s, padrão 60).
  `AppSettings.lockAfter` e `privacyMode` (`setLockAfter`, `setPrivacyMode`). Em `MainActivity`, o `onStop` guarda a hora da
  saída (menos ao girar a tela e durante a pergunta da digital) e o `onStart` pergunta ao `LockRules.relock`; `startActivity`,
  `startActivityForResult` e `startIntentSenderForResult` marcam `CryoApp.ownLaunchAt` (5 minutos de folga para voltar do que o
  próprio Cryo abriu); `applyPrivacy` põe o `FLAG_SECURE` (no Android 12 ou anterior, sempre que o bloqueio está ligado).
  Ajustes › Privacidade e segurança: "Bloquear de novo" (folha com as quatro escolhas, `lockAfterLabel`) e a chave do modo
  privacidade (ligada e travada no Android ≤ 12). `unlock()` encerra a pergunta da digital em andamento (`authenticating`).
- **Toque no fluxo** (LA3, RF-ANA-10, RF-EXT-12): `FlowNode.categoryId`; `openFlowNode` leva a categoria ao Extrato filtrado no
  mês (`Routes.txs(id, ym.key())`), Investimentos à Carteira, Metas a Planejar › Metas, Dinheiro emprestado a Dívidas e
  "Outras categorias" às saídas do mês (`Routes.txs(0, ym, out = true)`). A rota do Extrato agora é
  `txs?category={category}&month={month}&out={out}` (`TransactionsScreen(categoryFilter, initialMonth, onlyOut)`).
- **O funil de verdade** (LA4, RF-ANA-09 revisto: "cara de funil mesmo, saltado e clicável"): `FlowFunnel` em `Charts.kt`
  refeito. `funnelBands` monta as faixas: a boca é o dinheiro do mês (`insights_funnel_top`) e cada destino estreita a faixa
  pelo que saiu, até "Sobrou" (boca mínima de 24% da largura). Faixas de 46 dp com sombra (`CryoTheme.depth.shadow`), o lado
  mais escuro, degradê e um fio de luz (o "saltado"); cada uma afunda um pouco ao toque. De onde o dinheiro veio fica em
  pílulas (`FlowRow`) acima; o nome e o valor de cada faixa ao lado. `canOpen` (= `flowTarget`) diz o que é tocável.
- **Arrastar entre as abas** (LA7, RF-GES-01..03): `ui/components/TabSwipe.kt`. `Modifier.tabSwipe` fica no `Box` do `NavHost`
  (`Nav.kt`): um detector no passe `Initial` que só toma o gesto claramente de lado (|dx| > 1,5 × |dy|) e larga no vertical;
  troca com 80 dp ou 900 dp/s. Bordas de 48 dp livres (`TAB_SWIPE_EDGE`, o voltar do Android); `Modifier.noTabSwipe()` marca o
  que arrasta de lado (o gráfico do ano, as fileiras de atalhos e de filtros); `TabSwipeInner` deixa Planejar passar pelas três
  partes antes de trocar de aba; em RTL o lado espelha. A tela nova entra pelo lado do dedo (`Motion.swipeEnter`/`swipeExit`).
- **Organizar os blocos** (LA8, RF-ORG-01..05): `domain/Layouts.kt` (`ScreenLayout`: a ordem e os escondidos, em texto
  "a,-b,c"; `arrange`, `visible` nunca vazio, `moved`, `toggled`; as telas em `Layouts`: `HOME`, `HOME_TILES`, `INSIGHTS`,
  `BUDGETS`, `BILLS`, `GOALS`, `WALLET`) e `ui/components/Arrange.kt` (`Block`, `LazyListScope.arranged`, `OrganizeButton`,
  `ArrangeSheet`: segurar e arrastar, a alça, o olho e as ações "Subir"/"Descer" do leitor de tela). Guardado em
  `AppSettings.layouts` (chaves `layout_<tela>` do DataStore; `setLayout`, `setLayouts`) e no backup (`"layout"`). Início (as
  partes e os blocos da grade), Análises, Planejar (as três partes) e Carteira.
- **Widget** (LA9, RF-WID-01..07): `domain/WidgetContent.kt` (`WidgetMain`, `WidgetLine`, `WidgetConfig` em texto "MAIN|A,B",
  `WidgetContents.of(ledger, config)`: os números do `MonthEnds.of` e do `Ledger`, as próximas contas, orçamentos e metas),
  `data/WidgetPrefs.kt` (SharedPreferences `cryo_widgets`, uma escolha por widget), `ui/widget/CryoWidget.kt` (o
  `GlanceAppWidget` com `SizeMode.Responsive` em 5 tamanhos; `widgetFit` é a conta de espaço que decide o que cabe;
  `WidgetTheme` com as cores do papel de parede no Android 12+ e `CryoPalette` antes; no Android 8–11 os cantos vêm da forma
  pronta `widget_bg`; "Lançar" abre a `MainActivity` com `EXTRA_OPEN = "entry"`, que abre o campo de escrever),
  `ui/widget/WidgetConfigActivity.kt` (a tela de editar, sem dados) e `res/xml/cryo_widget_info.xml`. `CryoApp` atualiza os
  widgets 2 s depois de cada mudança nos dados de verdade (`WidgetUpdates.refresh`); o Android também chama de hora em hora.
- **Saúde no topo** (LA10, RF-ANA-08): o primeiro item de Análises (`item(key = "saude")`), acima do seletor do mês, em
  qualquer mês; `HealthPlaceholder` guarda o lugar enquanto o motor calcula.
- **Fluxo numa caixa só, Barras | Funil** (LA11, RF-ANA-09): `FlowBox` em `InsightsScreen.kt` (o `Segmented`, as três
  caixinhas `FlowStat` no tom `CryoTheme.depth.grouped`, a dica do toque e a vista escolhida). Barras = `FlowBars`; Funil =
  `FlowFunnel`. A escolha fica em `AppSettings.flowView` (0 = Barras, 1 = Funil; `setFlowView`).
- **Calendário de temperatura** (LA12, RF-ANA-02, DS-COR-06): `CryoTheme.heat` (`CryoHeat` em `Theme.kt`: 5 cores + a cor do
  número, no claro e no escuro, sem seguir o papel de parede) e `heatLevels` (6 níveis pela mediana dos dias com gasto do mês:
  ≤ ½ azul-claro, ≤ 1× azul, ≤ 1,5× amarelo, ≤ 2,5× laranja, acima vermelho).
- 50 textos novos nas 14 línguas (`tr_17.py` e `tr_17b.py` no rascunho da sessão); 3 saíram (`insights_flow_drawing`,
  `insights_onde_dinheiro_veio_cima` e `widget_label`: o widget usa o `app_name`). Nenhuma permissão nova (conferido com o
  `aapt2`); banco de dados continua na versão 6; uma biblioteca nova, o Jetpack Glance 1.2.0 (seção 13).

### Para a próxima sessão

1. **1.7**: o Hanry testa o APK (o que conferir está no relatório de 23/11/2026: o bloqueio "Na hora", o modo privacidade, o
   widget nos três tamanhos e o "Editar o widget", arrastar entre as abas, "Organizar esta tela", o funil novo); commit no
   "pode salvar", push no "pode publicar"; as Releases são dele. A 1.7 não precisa de merge request no F-Droid.
2. **1.8** (PRD, roteiro): a LA5 (importar o banco do xpenc, issue #5: o Hanry vai conversar com quem pediu) e a proposta
   antiga (contas a receber, corrigir o saldo de uma conta, escolher vários lançamentos, calculadora no campo de valor).
3. Registro por frase: aceitar milhar separado por espaço ("25 000", comum em russo, ucraniano e francês) — ideia, não pedida.
4. Revisão das traduções por quem fala cada língua (RF-LNG-08), principalmente as 4 novas.

### Os pedidos de 17/11/2026 (feitos na 1.6)

1. **Orçamento a partir da renda** (requisito novo em RF-ORC, PRD primeiro): perguntar a renda fixa e a média das variáveis
   (o motor já acha o salário em `Recurrences` e tem a mediana das entradas avulsas), uma chave "contar a renda variável"
   com o aviso de que ela oscila, e sugerir limites por categoria (já existe "limites sugeridos" no ritmo das categorias)
   e a separação da renda (necessidades, desejos, guardar e investir). Sugestão de investimento só por **tipo** (reserva
   com liquidez diária, renda fixa, renda variável), nunca produto. Tudo editável (PRI-09) e explicável (PRI-05).
2. **Quatro línguas novas: russo, ucraniano, árabe e hebraico**, com as moedas (RUB, UAH, ILS e as árabes mais usadas:
   SAR, AED, EGP). É grande: ~1.920 textos por língua; plurais próprios (ru/uk: one/few/many/other; ar: as seis formas;
   he: one/two/many/other — o `addstrings.py` do rascunho só conhece one/other); `PhraseLang` e as colunas da importação;
   loja (fastlane), READMEs, `PrdConformanceTest` e `StoreScreenshotsTest` (a lista de línguas passa de 10 para 14).
   Árabe e hebraico são **da direita para a esquerda**: conferir os desenhos próprios (`Charts.kt`, `Art.kt`, `Depth.kt`,
   `Tour.kt`), os ícones que precisam espelhar e os valores em dinheiro dentro de frases.
3. **Conta fixa**: uma chave na própria conta fixa para "nunca mexer no saldo" — **não será feita** (Hanry, 19/11/2026: o
   previsto só vira lançamento quando é pago, e na hora já existe a escolha de não mexer no saldo, RF-FIX-11).

### Próximos passos imediatos

0. **1.7** (`versionCode` 12, branch `versao-1.7`): os lotes LA1–LA12 da 1.7, menos a LA5 (DLA aprovada em 23/11/2026). APK
   `Cryo-1.7.apk` (SHA-256 `7b8fe0de4d0bd956ad47b29f8e638c3f226f2dcd352a7f649b251d374d157af0`) entregue para ele testar em 23/11/2026; nada de commit nem push antes do
   "pode salvar"/"pode publicar". Suíte inteira: 1.177 testes, nenhuma falha (32 só rodam sob pedido).
1. **1.6** (`versionCode` 11): pronta em 20/11/2026, com as correções da auditoria e a reforma visual em 21/11/2026;
   `Cryo-1.6.apk` (SHA-256 `daebb287…3e86ee`) **testado pelo Hanry e aprovado como lançável** em 21/11/2026. O que foi
   conferido no teste: o Início em blocos, o
   sino e a central de avisos (o destaque some no ×; abrir a central zera o número); o fim do mês real (Início e Análises, com
   "Como cheguei nesse número"); o Geral em Planejar › Orçamentos (a sugestão e o dividido); contas fixas e metas em blocos;
   Ajustes por tipo e Ajustes › Voz; a folha "Baixe a voz do Cryo uma vez" (o modelo que ele já baixou serve); árabe e hebraico
   (tela espelhada, algarismos 0–9 no árabe); o extrato da conta; "Todas as contas" e as "Previstas" com "Pular este mês";
   "Como vou fechar o ano?" (zoom na primeira vista, arrastar e pinçar).
2. **1.6 publicada**: push de `versao-1.6` e de `main` (`ddeb228`, 21/11/2026); o APK compilado de um clone limpo desse commit
   é idêntico ao que o Hanry testou (`daebb287…3e86ee`). As Releases `v1.6` e `voice-model-1` (que criam as etiquetas) foram
   feitas pelo Hanry em 22/11/2026, com os arquivos certos; a `voice-model-1` aponta para o commit da 1.4 (`f8a402b`), para o
   robô do F-Droid não tomá-la por versão.
3. **F-Droid**: o Hanry abriu o merge request **!58632** (a 1.6, com o NDK) e os 9 jobs do CI passaram em 23/11/2026, com a
   receita de `docs/fdroid/io.github.h3yk0.cryo.yml`; falta os mantenedores aceitarem. Acompanhar a 1.6 entrar na loja. Para as
   próximas versões, o passo a passo dele: `docs/fdroid/COMO-ATUALIZAR.md`.
4. As versões seguem com nome só de números (`v1.7`, `v1.8`): a receita do F-Droid só olha etiquetas `v` + números.
5. **Issue #3** do GitHub: responder agora que a 1.6 saiu (só o Hanry posta).
6. As traduções foram feitas pelo Claude: pedir revisão de quem fala cada língua (RF-LNG-08, por issue).
7. Voz: medir no A55 o tempo com e sem a chave "Atenção rápida no Whisper" (modo desenvolvedor › Voz; RF-VOZ-13).

---

## 4. Regras de ouro (não negociáveis)

**Privacidade**
- O app **não tem permissão de INTERNET** e não pode ganhar. Nada de bibliotecas de analytics, anúncios, crash reporting
  online ou qualquer coisa que fale com servidores. Permissões atuais: `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, (1.5)
  `RECORD_AUDIO` e (1.5) `POST_PROMOTED_NOTIFICATIONS` (notificação ao vivo do modo compras), só. O áudio da voz nunca vira arquivo e é apagado da memória ao terminar (RF-VOZ-02).
- Dados ficam só no aparelho. Nuvem só pelo seletor de arquivos do Android (o app da nuvem é quem envia).
- `android:allowBackup="false"`; `data_extraction_rules.xml` só permite transferência direta entre aparelhos e exclui as
  permissões do backup automático e tudo do modo de teste.
- **Nunca** colocar extratos reais de banco no repositório (nem em testes). Testes usam arquivos inventados.
- O motor financeiro **não grava resultados**; o pouco que guarda é **HMAC** (RF-SEG-02). A senha dos backups **nunca** é
  guardada (só a chave derivada, trancada pelo cofre do Android). Nada de `Log`, `println` ou `printStackTrace` (RF-SEG-07).

**Processo**
- **LA → DLA → código: cada passo só com o ok explícito do Hanry** (PRC-11; **falha crítica de 22/11/2026**). Uma lista de
  LAs serve para **anotar**, nunca para desenvolver: anotar no PRD, perguntar se a lista está fechada e se pode ir para a
  DLA (ele pode ter mais itens), apresentar a DLA com as dúvidas e só escrever código depois que ele aprovar. Em 22/11/2026
  o Claude pulou as duas portas na 1.7 (fez a DLA sozinho e desenvolveu tudo), e o Hanry ainda tinha informações novas:
  esforço e tokens gastos sem necessidade. Na dúvida, perguntar e esperar.
- **Salvar o trabalho sem commit** (lição de 19–20/11/2026: o contêiner da nuvem é apagado quando a sessão fica parada, e a 1.6
  inteira se perdeu duas vezes): enquanto o Hanry não autoriza commit, a cada etapa concluída o Claude gera um backup zip (os
  arquivos mudados e novos por cima do commit-base, mais `APAGADOS.txt` e um LEIA-ME) e entrega ao Hanry pelo envio de arquivos.
  Se a sessão cair, um backup restaura tudo num clone limpo. O registro da sessão anterior também serve (`list_events`).
- **Biblioteca de erros = lei** (pedido do Hanry, 10/11/2026): **[docs/ERROS.md](docs/ERROS.md)** lista cada erro já corrigido, a
  lei ("o que nunca mais pode acontecer") e o teste que guarda. Antes de mexer numa área, ler as linhas dela. Nenhum
  teste-guarda pode ser apagado, desligado ou afrouxado. Todo defeito novo corrigido entra lá no mesmo commit, com teste que
  falha no código anterior. `KnownIssuesTest` confere; a suíte inteira passa antes de qualquer entrega.
- **O Hanry quer baixar e testar o APK antes de qualquer push** (GitHub, GitLab, F-Droid). Nunca dar push, criar tag ou release
  sem a aprovação explícita dele.
- Commits com a identidade pública: `Hanry Franco <102742894+H3yK0@users.noreply.github.com>` (nunca o e-mail pessoal).
- A **chave de assinatura nunca vai para o git**. Ela fica em `keystore/` (no `.gitignore`): `keystore/cryo-release.jks` +
  `keystore/keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Se a chave se perder, não dá mais
  para atualizar o app (nem no F-Droid, nem por cima da instalação das pessoas).
- Compilação reproduzível: o APK do GitHub precisa ser idêntico ao que o F-Droid compila. Não mexer em `vcsInfo`,
  `dependenciesInfo` nem reativar as tarefas de ArtProfile em `app/build.gradle.kts` sem entender o efeito (seção 11).
- Toda versão nova: subir `versionCode` e `versionName`, escrever `CHANGELOG.md` (em inglês) e `docs/CHANGELOG.pt-BR.md`, e
  `fastlane/metadata/android/<língua>/changelogs/<versionCode>.txt` nas 14 línguas (máximo 500 caracteres cada).
- **O que aparece na página inicial do GitHub é em inglês** (pedido do Hanry, 06/11/2026): README, CHANGELOG, notas das Releases
  (inglês primeiro, depois português) e **mensagens de commit**. Comentários do código e documentos de trabalho continuam em português.
- Mudança no banco de dados = nova versão do Room com migração e teste em `MigrationTest` (os schemas ficam em `app/schemas/`).
  Backups antigos precisam continuar abrindo (`Backup.import` lê todas as versões).

**Produto**
- **Flexibilidade é lei** (pedido do Hanry, 13/11/2026, PRI-09): toda regra automática do app (a fatura pela data, o ritmo
  da conta fixa, a categoria sugerida) precisa ter um jeito à vista de a pessoa mudar o resultado — respeitando a lógica:
  a escolha dela é guardada e os cálculos a seguem.

**Código**
- Botão principal é sempre `CryoButton` (nunca `Button(` direto nas telas: `PrdConformanceTest` confere). Cartões e listas usam
  as peças de `ui/components/Depth.kt` (`depthCard`, `groupedRow`) e as cores de `CryoTheme.depth`, nunca cores soltas.
- Kotlin + Jetpack Compose + Material 3. Texto da interface simples, no "você" (guia em `docs/VISUAL.md`), **sempre** em
  `res/values/strings.xml` (base pt-BR) e traduzido nas outras 9 línguas no mesmo commit: `PrdConformanceTest` falha se faltar
  chave, se os marcadores (`%1$s`) não baterem ou se houver texto escrito direto nas telas e notificações. Regras sem Android
  usam `Res.s(R.string.x)` / `Res.p(R.plurals.x, n)` (`domain/I18n.kt`).
- Comentários do código em português, explicando o porquê.
- Dinheiro sempre em **centavos (`Long`)**; datas em `LocalDate`; mês de fatura como `Int` `AAAAMM` (`YearMonth.key()`).
- Nada pode impedir o app de abrir (lembretes e backup ficam em `runCatching`).

## 5. Como trabalhar com o Hanry

- Responder em português do Brasil.
- Explicar de forma **visual, clara e progressiva**, com exemplos, comparações e analogias: sem jargão, passo a passo, sem
  deixar espaço para dúvida.
- Antes de começar algo grande, buscar o máximo de contexto (este arquivo, o código, os testes) para não dar instruções incompletas.
- Ele aprova o que vai para o público. Mostrar o resultado (APK, imagens das telas) antes de publicar.
- Ficar no assunto do pedido.

---

## 6. Comandos

Requisitos: **JDK 21** (o mesmo do F-Droid) e Android SDK com a plataforma 36. Gradle 8.14.3 vem pelo wrapper.

```bash
./gradlew assembleDebug                 # APK de teste (ID .debug, instala ao lado do oficial)
./gradlew assembleRelease               # APK de lançamento (assinado se existir keystore/keystore.properties)
./gradlew testDebugUnitTest             # todos os testes (regras, banco, backup, importação, telas)
./gradlew testDebugUnitTest --tests '*StatementImportTest'     # um conjunto só
./gradlew testDebugUnitTest --tests '*StoreScreenshotsTest' --tests '*StoreAssetsTest' -PstoreAssets=true   # imagens da loja e dos READMEs, nas 10 línguas
./gradlew testDebugUnitTest --tests '*TourTest' -PtourGif=true   # quadros das animações do passeio (app/screenshots/passeio_gif/), para montar GIFs
app/src/test/cpp/build-host.sh && ./gradlew testDebugUnitTest --tests '*WhisperHostTest' -PwhisperHost=true   # a ponte do Whisper com o modelo de verdade, no computador
```

- Os testes de tela salvam imagens em `app/screenshots/` (fora do git). É assim que se confere o visual sem celular.
- Saída do APK: `app/build/outputs/apk/release/app-release.apk` → renomear para `Cryo-<versão>.apk`.

## 7. Arquitetura

Um único módulo (`:app`), uma única Activity, tudo em Compose.

```
app/src/main/java/io/github/h3yk0/cryo/
├── CryoApp.kt          Application: guarda o AppContainer de verdade e o de teste; troca entre eles (modo de teste)
├── MainActivity.kt     Tema, bloqueio (camada por cima do app), apresentação inicial ou CryoRoot
├── data/               Room (db/), Repository.kt (FinanceRepository + SettingsRepository), Backup.kt (JSON e CSV),
│                       AutoBackup.kt (backup automático), DefaultData.kt (categorias, paleta, ícones),
│                       SampleData.kt (dados de exemplo), Dev.kt (modo desenvolvedor), LocaleSettings.kt (língua e moeda),
│                       Vault.kt (cofre: Keystore, HMAC, AES), BackupCrypto.kt (backup com senha), EngineStore.kt,
│                       WidgetPrefs.kt (1.7, a escolha de cada widget)
├── domain/             Regras sem Android: Ledger.kt (saldos, faturas, contas fixas, orçamentos), Analytics.kt (previsão,
│                       calendário, fluxo), Debts.kt (dívidas), PhraseParser.kt + PhraseLang.kt (registro por frase e as
│                       palavras de cada língua), Format.kt (valores e datas), I18n.kt (língua, moeda, Res),
│                       (1.7) LockRules.kt (quando o bloqueio volta), Layouts.kt (a arrumação das telas),
│                       WidgetContent.kt (o que o widget mostra),
│                       engine/ (o motor financeiro: Engine.kt junta tudo; Stats, Recurrences, Outlook, Projection,
│                       Signals, Health, Wealth, CategoryModel, TxKey),
│                       predict/ (1.5, o previsor: Series, Models, Season, Ensemble, Forecaster, CategoryForecast,
│                       InvestForecast)
├── importer/           Leitura de extratos e conferência (veja docs/IMPORTACAO.md)
├── voice/            Voz no aparelho (1.5): WhisperNative (JNI), WhisperEngine, Listener (WhisperListener e o medidor de
│                       volume/sílabas), SystemListener (plano B), AudioInput/MicInput, VoiceSettings e VoicePrompt
├── notify/             Avisos (1.5): AlertPlanner (regras), Alerts (canais e notificação), AlertWork (manhã, noite, botões),
│                       AlertState, Shopping (modo compras ao vivo), Sounds; Reminders (contas e dívidas); backup automático
└── ui/                 Nav.kt (rotas, barra de abas, transições), components/ (peças reutilizáveis; Motion.kt = movimento,
                        Depth.kt = profundidade e vibração; 1.7: TabSwipe.kt = arrastar entre as abas, Arrange.kt =
                        organizar a tela), screens/ (telas; Tour.kt = passeio), tx/ (lançamento),
                        theme/ (Theme.kt: cores, CryoTheme.colors, CryoTheme.depth), widget/ (1.7: o widget, com o Glance)
```

Código nativo (1.5): `app/src/main/cpp/` = `CMakeLists.txt` (opções da compilação reproduzível), `cryo_whisper.cpp` (a ponte
JNI), `cryo_whisper_stub.cpp` (32 bits e x86) e `whisper/` (cópia do whisper.cpp v1.9.5; única mudança: o commit do ggml vem
do Cryo, não do git). O modelo fica em `app/src/main/assets/whisper/ggml-base-q5_1.bin` (SHA-256 `422f1ae4…8898`).

**Como os dados andam**
1. `AppContainer` (em `CryoApp.kt`) cria o banco Room, o `FinanceRepository`, os ajustes (DataStore), o backup e o modo desenvolvedor.
   As telas pegam o container por `LocalContainer.current`.
2. `FinanceRepository.snapshot` é um `StateFlow<Snapshot?>` com **todos** os dados (combina os fluxos do Room).
3. `rememberLedger()` transforma o `Snapshot` num `Ledger` (recalculado quando os dados mudam ou o dia vira). Todas as contas
   (saldo, fatura, previsão, dívidas) saem do `Ledger`, nunca de consultas soltas.
4. Para gravar: métodos do repositório, chamados em `c.launch { }` (escopo do app, sobrevive à troca de tela).
5. Mensagens rápidas: `c.message("texto", "Desfazer") { ... }` mostra a snackbar.
6. **Motor**: `rememberEngine()` devolve o `EngineReport` (calculado fora da linha principal por `c.engine`, um `EngineRunner`
   com cache do último resultado; enquanto calcula, a tela mostra o anterior). O motor usa um `Ledger` próprio (os mapas de
   cache do `Ledger` não são feitos para duas threads). Nas imagens de teste, `EngineRunner.inline = true` faz rodar na hora.

**Pontos importantes**
- `CryoApp.real` = dados de verdade (lembretes e backup **sempre** usam este). `CryoApp.container` = o que as telas mostram
  (o de teste quando o modo de teste está ligado). Trocar = `switchData()` + recriar a Activity (`switchTestMode()` em `DevScreens.kt`).
- O bloqueio (`lockSettings`) vem sempre dos ajustes de verdade. `CryoApp.unlocked` vale por processo; volta a bloquear depois
  de 60 s em segundo plano ou quando o Android mata o processo. Bloqueado, o conteúdo **não é composto** (fica num
  `SaveableStateHolder` em `MainActivity`): diálogos, menus e folhas são janelas próprias e ficariam por cima de qualquer camada.
- Um aviso de conta ou dívida (dados de verdade) tocado com o modo de teste ligado sai do modo de teste antes de abrir a tela.
- Navegação: rotas em `ui/Nav.kt` (`Routes`), `LocalNav` com `go()`, `back()`, `tab()`, `open()`.
- Abas (1.4): Início, Extrato, Planejar (Orçamentos / Contas fixas / Metas), Análises. A Carteira abre pelo Início; Ajustes pela
  engrenagem do Início.
- Língua e moeda: `LocaleSettings` (SharedPreferences `cryo_locale`) guarda a escolha; `CryoApp`/`MainActivity` embrulham o
  contexto (`wrap`) e `install` define `AppLocale` (locale, moeda) e o `Res` das regras. Valores sempre em centavos, inclusive
  em iene e won (múltiplos de 100). Trocar a língua recria a Activity.

## 8. Banco de dados e formatos

**Room** (`cryo.db`, versão 6; teste: `cryo-teste.db`). O orçamento **Geral** (1.6, RF-ORC-10, DAD-17) é a linha de `budgets`
com `categoryId = 0` (`GeneralBudget.ID`): nenhuma categoria tem esse número, e toda conta por categoria já o ignora. Tabelas: `accounts`, `cards`, `categories`, `transactions`,
`investments`, `investment_yields`, `goals`, `bills`, `budgets`, `debts`, `debt_adjustments` e (1.5) `budget_adjustments`
(remanejar só no mês; `moveGroup` liga as duas pontas) e `no_spend_days`.
Migrações automáticas: 1→2 (dívidas, com `Migration1To2`), 2→3 (`transactions.importKey` + índice), 3→4 (tabelas novas e
`goals.art`, preenchido a partir do emoji por `Migration3To4`) e 4→5 (`bills.every`, `installments`, `cashAmount`, `endYm`,
todas com valor padrão: quem vem da 1.4 continua com a conta mensal de sempre) e 5→6 (1.6: `bill_skips`, os meses pulados das
contas fixas, chave `billId` + `ym`; vai no backup como `billSkips`). Schemas em `app/schemas/`.

**Tipos de movimentação** (`TxType`): `EXPENSE`, `INCOME` (no cartão = estorno), `TRANSFER` (conta → conta),
`CARD_PAYMENT` (conta → cartão, com `invoiceYm`), `INVEST_IN`/`INVEST_OUT`, `GOAL_IN`/`GOAL_OUT` (caixinhas),
`DEBT_IN` (dinheiro emprestado que entrou: não conta como receita).

**Cartão**: cada compra guarda `invoiceYm` (fatura em que cai). Compra no dia do fechamento ou depois vai para a fatura seguinte
(`CardMath.invoiceFor`). Parcelado = uma movimentação por parcela, ligadas por `installmentGroup`.

**Backup** (`data/Backup.kt`): JSON `{"app":"Cryo","version":2,"exportedAt":…, accounts, cards, categories, transactions, investments,
yields, goals, bills, budgets, debts, debtAdjustments}` (e, desde a 1.7, `"layout"`, a arrumação das telas, opcional: os
backups antigos abrem sem ela e as versões antigas a ignoram). Backups da 1.0 (versão 1) continuam abrindo. Cópia feita no modo de
teste leva `"testMode": true` e se chama `cryo-teste-backup-<data>.json`: não pode ser restaurada por cima dos dados de verdade.
Exportação para planilha: CSV com `;`.

**Backup automático** (`data/AutoBackup.kt`): pasta do celular (substituir sempre `cryo-backup.json` ou guardar cópias
`cryo-backup-AAAA-MM-DD-HHMMSS.json`, mantendo 5/10/30/todas) e/ou um arquivo na nuvem. Frequência diária, semanal ou mensal.
Proteções: só substitui ou apaga arquivos que este celular gravou; se outro aparelho mexeu no arquivo ou se muitas movimentações
sumirem de repente, pausa e avisa; se nada mudou, não grava cópia igual.

**Ajustes**: DataStore `settings` (tema, cores do papel de parede, bloqueio, esconder valores, nome, lembretes, conta principal,
apresentação vista; desde a 1.7, a vista do fluxo do dinheiro `flowView`, o tempo do bloqueio `lock_after`, o modo privacidade
`privacy` e a arrumação de cada tela `layout_<tela>`); `settings_test` no modo de teste; `auto_backup`; SharedPreferences
`cryo_dev` (modo desenvolvedor) e (1.7) `cryo_widgets` (a escolha de cada widget, `w<id>`; sai quando o widget é tirado).

**Importação**: cada movimentação importada guarda `importKey` = `a<conta>|<formato>.id:<FITID>` ou `c<cartão>|<formato>.h:<hash>`
(ex.: `a3|ofx.id:123`, `c7|csv.h:9f3a…`; + `#n` para linhas repetidas). Detalhes em `docs/IMPORTACAO.md`.

## 9. Funcionalidades (onde estão)

| Área | Arquivos principais |
|---|---|
| Início (saldo, patrimônio, resumo do mês, alertas, previsão) | `ui/screens/HomeScreen.kt` |
| Registro rápido por frase/voz | `ui/tx/QuickEntry.kt`, `domain/PhraseParser.kt`; voz (1.5): `ui/tx/VoiceSheet.kt`, `voice/`, `domain/Spoken.kt`, `src/main/cpp/` |
| Lançamento completo | `ui/tx/TxEditorScreen.kt`, `ui/tx/TxForm.kt` |
| Extrato (movimentações) | `ui/screens/TransactionsScreen.kt` |
| Planejar: orçamentos, contas fixas, metas | `ui/screens/PlanScreens.kt` |
| Carteira: contas, cartões, investimentos, arquivados | `ui/screens/WalletScreens.kt` |
| Análises: previsão, calendário de calor, fluxo | `ui/screens/InsightsScreen.kt`, `ui/components/Charts.kt`, `domain/Analytics.kt` |
| Dívidas | `ui/screens/DebtScreens.kt`, `domain/Debts.kt` |
| Ajustes, categorias | `ui/screens/SettingsScreens.kt` |
| Backup automático | `ui/screens/BackupScreens.kt`, `data/AutoBackup.kt`, `notify/AutoBackupWork.kt` |
| Importar extrato | `ui/screens/ImportScreen.kt`, `ui/screens/ImportModel.kt`, `importer/` |
| Modo desenvolvedor / modo de teste | `ui/screens/DevScreens.kt`, `data/Dev.kt`, `data/SampleData.kt`, `CryoApp.kt` |
| Apresentação inicial, tela de bloqueio | `ui/screens/Onboarding.kt` |
| Passeio "Como usar o Cryo" | `ui/screens/Tour.kt` (primeiro uso e rota `tour` pelos Ajustes) |
| Movimento (transições, contagem, aparecer) | `ui/components/Motion.kt`, `ui/Nav.kt` |
| Profundidade (cartões, volume, sombras, vibração) | `ui/components/Depth.kt`, `ui/theme/Theme.kt` (`CryoDepth`) |
| Ícone do app e marca | `res/drawable/ic_launcher_*.xml`, `ic_notification.xml`, `ic_cryo_badge.xml`, `ic_cryo_mark.xml` |
| Lembretes e avisos (1.5) | `notify/` (`AlertPlanner.kt`, `Alerts.kt`, `AlertWork.kt`, `Shopping.kt`, `Sounds.kt`), `ui/screens/AlertScreens.kt` |
| Momentos, ilustrações | `ui/components/Moments.kt`, `ui/components/Art.kt` |
| Painel do mês (orçamento) | `ui/screens/BudgetMonthScreen.kt` |
| Motor financeiro (previsão com faixa, recorrências, próximos meses, saúde, dívidas, rendimento) | `domain/engine/`, `ui/screens/EngineScreens.kt`, cartões em `InsightsScreen.kt` |
| Contas fixas: ritmo, parcelas, juros, prazo e modelos (1.5) | `domain/Ledger.kt` (`BillCycle`), `data/BillTemplates.kt`, `ui/screens/PlanScreens.kt` (`BillCycleFields`) |
| Importações feitas, apagar uma inteira (1.5) | `domain/ImportBatches.kt`, `ui/screens/ImportsScreen.kt` |
| Contas fixas × extrato (1.5) | `domain/BillMatch.kt`, `importer/ImportPlan.kt` (`withBills`), `ui/screens/PlanScreens.kt` (`BillSuggestionsSheet`) |
| Perfil (1.5) | `ui/components/Avatar.kt`, `ui/screens/ProfileScreens.kt` |
| Guia "Aprender o Cryo" (1.5) | `domain/Guide.kt`, `data/GuideStore.kt`, `ui/components/Guide.kt`, `ui/screens/LearnScreen.kt` |
| Proteção dos dados (cofre, HMAC, backup com senha) | `data/Vault.kt`, `data/BackupCrypto.kt`, `data/EngineStore.kt`, `ui/screens/SecurityUi.kt` |
| Modelo de voz baixado pela pessoa (1.6) | `voice/WhisperModels.kt`, `voice/WhisperModelStore.kt`, `ui/screens/VoiceModelUi.kt`, `docs/VOZ-MODELOS.md` |
| Fim do mês que fecha, orçamento no fim do mês (1.6) | `domain/predict/MonthEnd.kt`, `domain/predict/BudgetPlan.kt`, `ForecastCard` em `InsightsScreen.kt` |
| Orçamento pela renda e com as contas fixas (1.6) | `domain/IncomePlan.kt`, `domain/BudgetBills.kt`, `data/IncomePlanStore.kt`, `data/BudgetBillStore.kt`, `ui/screens/BudgetIncomeScreen.kt` |
| Extrato da conta, filtro por conta, previstas e pular o mês (1.6) | `domain/AccountStatement.kt`, `AccountDetailScreen` em `WalletScreens.kt`, `TransactionsScreen.kt`, `Ledger.billStatuses` |
| "Como vou fechar o ano?" e previsão de metas (1.6) | `domain/engine/YearOutlook.kt`, `domain/engine/GoalForecast.kt`, `YearChart` em `Charts.kt`, cartões em `EngineScreens.kt` |
| Saúde no Início, folhas (1.6) | `ui/components/HealthRing.kt`, `ui/components/Sheet.kt` (`CryoSheet`) |
| Central de avisos e o sino (1.6, LA9) | `domain/Inbox.kt`, `data/InboxStore.kt`, `ui/screens/InboxScreen.kt`, `HomeHeader(bell)` em `ui/components/Sections.kt` |
| Blocos, resumo com setinha, tons por prioridade (1.6, LA10) | `ui/components/Tiles.kt`, `CryoTheme.tones` em `ui/theme/Theme.kt` |
| Orçamento Geral (1.6, LA12) | `domain/GeneralBudget.kt`, `GeneralBudgetCard`/`GeneralBudgetSheet` em `ui/screens/PlanScreens.kt` |
| Ajustes › Voz (1.6, LA13) | `VoiceSettingsScreen` em `ui/screens/VoiceModelUi.kt`, `voiceSummary` em `SettingsScreens.kt` |
| Fluxo do dinheiro: caixa, Barras e Funil, toque (1.7) | `FlowBox`, `FlowBars`, `openFlowNode` em `InsightsScreen.kt`; `FlowFunnel` em `Charts.kt`; `FlowNode.categoryId` em `domain/Analytics.kt` |
| Calendário de calor na temperatura (1.7) | `heatLevels`, `weekInitials`, `HeatmapCalendar` em `Charts.kt`; `CryoTheme.heat` em `ui/theme/Theme.kt` |
| Bloqueio por tempo e modo privacidade (1.7) | `domain/LockRules.kt`, `MainActivity.kt` (`onStop`/`onStart`, `applyPrivacy`, `markOwnLaunch`), "Bloquear de novo" em `SettingsScreens.kt` |
| Arrastar entre as abas (1.7) | `ui/components/TabSwipe.kt`, `Box` do `NavHost` em `ui/Nav.kt`, `TabSwipeInner` em `PlanScreens.kt` |
| Organizar esta tela (1.7) | `domain/Layouts.kt`, `ui/components/Arrange.kt`, os `Block` de `HomeScreen.kt`, `InsightsScreen.kt`, `PlanScreens.kt` e `WalletScreens.kt` |
| Widget (1.7) | `domain/WidgetContent.kt`, `data/WidgetPrefs.kt`, `ui/widget/CryoWidget.kt`, `ui/widget/WidgetConfigActivity.kt`, `res/xml/cryo_widget_info.xml` |

## 10. Testes

Robolectric 4.16.1 (JVM, sem celular) + Roborazzi (imagens das telas). Cerca de 1.180 testes:

| Arquivo | O que cobre |
|---|---|
| `LedgerTest`, `DebtsTest`, `PhraseParserTest` | Regras de dinheiro, faturas, previsão, dívidas, frases (JVM puro, com `JvmStrings`) |
| `PhraseLanguagesTest`, `I18nTest` | Frases nas 9 outras línguas; valores, moedas e datas de cada língua; troca de língua e moeda |
| `RepositoryTest`, `MigrationTest` | Banco de dados e migrações (1→3 sem perder nada) |
| `AutoBackupTest`, `BackupFlowTest` | Backup automático (regras e fluxo de tela com um provedor de arquivos falso, `TestDocsProvider`) |
| `StatementImportTest`, `ImportFlowTest` | Leitores de extrato, conferência, importação de ponta a ponta |
| `ImportCountriesTest` | Extratos inventados de outros países (EUA, Reino Unido, Alemanha, França, Japão, Coreia, China, Índia) |
| `PrdConformanceTest` | Regras do PRD conferidas sozinhas (PRC-09): licença, permissões, cores fixas, catálogo de telas, IDs, schema, changelogs, 10 línguas completas, nenhum texto no código, loja nas 10 línguas |
| `DevModeFlowTest`, `AppFlowTest`, `DebtFlowTest` | App de verdade (Activity): modo de teste, bloqueio, primeiro uso |
| `ScreensTest`, `LanguageScreensTest` | Imagens das telas; Início, Análises, apresentação e passeio em cada língua (`app/screenshots/linguas/`) |
| `TourTest`, `TourPhrasesTest` | Passeio: 5 páginas com imagens, Ajustes › Como usar, frases do passeio entendidas nas 10 línguas, quadros para GIF |
| `MotionTest` | Saldo que conta até o valor exato; "Remover animações" mostra o final |
| `StoreScreenshotsTest`, `StoreAssetsTest` | Imagens da loja e dos READMEs nas 10 línguas; ícone e banner (só com `-PstoreAssets=true`) |
| `EngineTest` | Motor: um teste por requisito RF-MOT, inclusive o **teste contra o passado** (backtest) da previsão; dados de `EngineFixtures` (`Personas`) |
| `SpokenTest`, `VoiceListenerTest`, `VoiceFlowTest` | Voz (1.5): português falado; a escuta com microfone e Whisper falsos (início, fim, cancelar, nada ouvido); a tela de eco (imagens 58–62) |
| `WhisperHostTest` | A ponte JNI e o modelo de verdade, compilados para o computador (só com `-PwhisperHost=true`, depois de `app/src/test/cpp/build-host.sh`) |
| `AlertPlannerTest`, `AlertActionsTest` | Avisos (1.5): cada regra com dados inventados (um por dia, dois horários, pessoa nova, tipo desligado, dispensado 3 vezes, ritmo, lembrete e sequência, resumo de domingo), exemplos, botões da notificação, desbloqueio, tela de bloqueio, figurinha no `MessagingStyle`, sons no silencioso |
| `MomentsTest`, `BudgetMonthTest`, `Screens17Test` | Momentos depois de salvar e o modo compras; remanejar só no mês e backup; imagens 71–81 (painel, avisos, momentos, compras, botão de voz, fonte em 200%) |
| `ForecastTest`, `ForecastLabTest`, `Audit18Test`, `Audit18DataTest`, `KnownIssuesTest`, `Screens18Test` | Etapa 4: o previsor contra o primeiro motor em mundos inventados (`ForecastWorld`), a faixa de 80%, a época do ano, categorias e investimentos; o laboratório completo (só com `-PforecastLab=true`); as correções da auditoria; a biblioteca de erros (`docs/ERROS.md`) inteira e com as guardas ligadas; imagens 82–85 (previsão conferida, orçamentos, investimento, carteira) |
| `BillCycleTest` | Contas fixas não mensais: ritmo, parcelas, juros, prazo, os modelos de cada língua e o `Ledger` seguindo o ciclo (JVM puro) |
| `BillMatchTest`, `BillFlowTest`, `ProfileTest`, `GuidePlannerTest`, `GuideFlowTest`, `CardFixTest`, `Screens19Test` | Etapa 5: o casador de contas fixas e a previsão sem dobro; importar, ligar, desligar, juntar e criar a conta fixa depois; o perfil (imagens 86–88); as regras do guia com datas inventadas e as telas dele (89–91); estorno, fatura mantida ao editar, escolha da fatura e ajustar total (92–94) |
| `MonthEndTest`, `IncomePlanTest`, `BudgetBillsTest`, `WhisperModelStoreTest`, `SourceTreeTest` | 1.6: o fim do mês (com os orçamentos), o orçamento pela renda e com as contas fixas, o modelo de voz baixado, nenhum binário no repositório |
| `AccountStatementTest`, `StatementTest`, `EntryDefaultsTest`, `BillSkipTest`, `YearOutlookTest`, `GoalForecastTest`, `Screens16Test` | 1.6 (lotes LA): extrato da conta, filtro por conta, último dia e fatura da tela, pular o mês (com o backup), o ano, as metas e as imagens 106–114 |
| `InboxTest`, `GeneralBudgetTest`, `MonthEndTest`, `ScreensVisualTest` | Reforma visual da 1.6 (LA9–LA13): a central (grupos, ordem, o sino, o destaque, nada repetido, o "visto" só em HMAC), o Geral (o gasto que conta, a sugestão, o dividido, nunca vira categoria), o fim do mês real (a soma das linhas, nada estimado) e as imagens 115–125 |
| `CrashSmokeTest`, `HeatLevelsTest`, `InsightsFlowTest` | 1.7: todas as telas em seis situações difíceis (RNF-EST-01); os seis níveis do calendário e as iniciais da semana (JVM puro); a saúde no topo, Barras e Funil com a escolha guardada, o toque que abre o Extrato (categoria no mês, "Outras categorias" nas saídas), o funil sem valores escondidos e as imagens 126–132 |
| `LockRulesTest`, `LockFlowTest` | 1.7 (ERR-070): o tempo escolhido manda na volta, a folga do que o Cryo abriu, a janela protegida (JVM puro); no app de verdade: "Na hora" a cada volta, 1 minuto, girar a tela, voltar do seletor de arquivos, o modo privacidade, o Android antigo e a imagem 133 |
| `TabSwipeTest`, `ArrangeTest` | 1.7: arrastar troca de aba, a borda livre, rolar não troca, as partes de Planejar, o gráfico do ano, árabe; a arrumação (blocos novos no lugar de sempre, nunca tudo escondido, o texto guardado, a folha, o Início, o backup) e a imagem 134 |
| `WidgetTest`, `WidgetFlowTest` | 1.7: a escolha guardada, os mesmos números do app, só o que foi escolhido, os tamanhos e o Android 11 desenhados de verdade (imagens 135–137c, pelo `GlanceRemoteViews`), a tela de editar (138); "Lançar" abre o campo de escrever, passa pelo bloqueio e sai do modo de teste |
| `RecurrenceScheduledTest`, `DebtProjectionTest`, `BackupEnvelopeTest`, `WhisperEngineGuardTest` | Auditoria da 1.6 (ERR-060 a ERR-066): agendado × recorrência, parcelas de dívida na projeção, backup aberto com "cipher", fechar o Whisper com contas em andamento (mais os testes novos em `StatementImportTest`, `MonthEndTest`, `WhisperModelStoreTest` e `WhisperHostTest`) |
| `EngineStabilityTest`, `EngineScalabilityTest`, `EnginePerformanceTest`, `EngineResourcesTest`, `EngineSecurityTest` | Os **500 testes de garantia** (RF-MOT-15), 100 casos cada; o de segurança roda no Robolectric (`ParameterizedRobolectricTestRunner`) |

**Armadilhas já descobertas (economizam horas):**
- O DataStore é um singleton por processo e o Robolectric **não zera entre testes**: testes que usam a Activity começam com
  `settings.clear()` (veja `DevModeFlowTest.realData()` e `AppFlowTest`).
- `AlertDialog` com campo de texto **trava** o Robolectric ("Compose did not get idle"). Por isso a senha do PDF é uma tela, não um diálogo.
  `ModalBottomSheet` com campo de texto funciona.
- ViewModel com `viewModelScope`: esperar com `rule.waitUntil { ShadowLooper.idleMainLooper(); condição }` (veja `ImportFlowTest.waitFor`).
- Itens de `LazyColumn` fora da tela não existem na árvore: rolar antes (`performScrollToNode`) ou esperar por textos do topo.
- Animações infinitas (spinner) impedem o "idle": não deixar `CircularProgressIndicator` na tela enquanto o teste espera.
- Numeração das imagens: 01–36 telas e fluxos (inclui backup), 37–43 importação, 44–46 modo desenvolvedor, 47–52 passeio, 53–57 motor, 58–62 voz, 63–68 protótipos dos avisos, 69–70 ilustrações, 71–81 avisos, momentos e painel do mês, 82–85 previsor, 86–88 perfil, 89–91 guia, 92–94 cartão, 100 importações feitas, 101–103 saldo depois de importar, 104 conta fixa paga por fora, 105 busca com período, 95–99 (pedidos do issue #3: lista de faturas, desfazer sem cobrir o botão, saldo mínimo, conta fixa anual), 106–114 lotes LA1–LA8, 115–125 reforma visual (`ScreensVisualTest`), 126–132 fluxo e calendário da 1.7 (`InsightsFlowTest`), 133 Ajustes do bloqueio (`LockFlowTest`), 134 organizar Análises
  (`ArrangeTest`), 135–137c o widget (pequeno, médio, grande, patrimônio e metas, Android 11) e 138 editar o widget (`WidgetTest`).
- **`CryoRoot("insights")` num teste com `createAndroidComposeRule`** dá "You must call setGraph() before calling getGraph()":
  ali o `LaunchedEffect` da abertura roda antes do `NavHost` (que fica dentro do `Scaffold`, num subcompose) pôr o grafo. No
  celular a ordem é outra (o NavHost compõe no mesmo quadro). Nos testes, abra `CryoRoot(null)` e toque na aba (veja
  `InsightsFlowTest.app`).
- **Imagem com um item da `LazyColumn` no alto**: `performScrollToKey("chave")` (põe o item no topo); `performScrollToNode`
  só rola o mínimo e deixa a caixa cortada embaixo.
- **`Navigator.tab(Routes.HOME)` a partir de uma tela empilhada não vai para o Início**: ele guarda as telas fechadas e as reabre
  na hora (é assim que a aba "lembra onde a pessoa estava"). Para ir mesmo ao Início use `Navigator.home()` (veja `showLesson`).
- **Guia nos testes**: o `TestApp` desliga (`GuideStore.enabled = false`) e os fluxos com o `CryoApp` também (`guideOff`). Para
  testar o guia, ligue a chave e ponha a primeira abertura no passado (`GuideFlowTest.guideOn`): nos 2 primeiros minutos ele não fala.
- **`TxDraft`**: mudar `date`, `source` ou `type` zera `invoiceYm` (a fatura volta a seguir a data). Para testar a escolha da
  fatura, escolha-a **depois** de mexer nesses campos.
- **O `Ledger` é refeito para vários meses do passado** (o previsor confere a faixa em ~12 meses): nada do que ele calcula pode
  ordenar ou copiar todas as movimentações. Um `sortedBy { it.amount }` no casador de contas fixas (cada comparação encaixota dois
  `Long`) somou 8 MB e derrubou o `EngineResourcesTest`; a correção foi uma passada só, sem ordenar (`BillMatcher.certain`).
- **Suíte inteira em lotes**: aqui o comando tem limite de 10 minutos; rode por letra inicial da classe
  (`--tests 'io.github.h3yk0.cryo.E*'` etc.). O Gradle apaga os resultados a cada rodada: some os lotes à mão.
- **Extrato de verdade nunca entra no repositório** (PRC-06): para diagnosticar um PDF do Hanry, extraia o texto numa
  pasta fora do projeto, chame `PdfStatement.parse` por um teste temporário e **apague o teste**
  depois; o teste-guarda que fica no repositório usa um extrato inventado no mesmo formato. **Extraia com
  `pdftotext -raw`**, que é o mais parecido com o leitor do Android (`PdfRenderer.textContents`: a ordem em que o texto
  está no arquivo, rótulo e valor em linhas separadas); `-layout` junta tudo na mesma linha e esconde defeitos (ERR-055).
  Em dúvida sobre o que o celular entrega, peça o **relatório anônimo** do modo desenvolvedor.
- **Botão estendido (`ExtendedFloatingActionButton`)**: o Material esconde o texto dele da árvore mesclada; nos testes,
  procure pelo `contentDescription` (ERR-047), não pelo texto.
- **`performScrollToNode` quer exatamente um rolável**: na tela de lançamento há vários; use `performScrollTo()` no
  próprio nó, ou `onAllNodes(hasScrollToNodeAction()).onLast()` quando a lista está numa folha por cima.
- **Teste que adultera dados precisa mudar de verdade**: o `EngineSecurityTest` trocava um byte do backup trancado pelo
  valor de *outro* byte; 1 vez em 256 dava o mesmo valor e o teste falhava sem defeito (o texto cifrado muda a cada
  rodada, então a falha ia e vinha). Adulterar = xor com valor diferente de zero no mesmo byte. Uma falha que some ao
  rodar de novo não é "instabilidade": é para achar a causa.
- **Conferir um extrato é conferir três coisas**: entradas, saídas **e o saldo de partida**. Em 14/11 as movimentações
  do extrato do Hanry fechavam no centavo e o saldo continuava errado: faltava o saldo inicial (ERR-054). E o
  `ImportModel` ignora toques enquanto está ocupado (`busy`): nos testes, espere o `step` mudar antes do próximo passo.
- **Recebedor do manifesto** (`AlertActionReceiver`) não recebe `sendBroadcast` no Robolectric: os testes chamam `handle()` direto.
- `FileProvider` funciona no Robolectric (a figurinha da notificação dá para conferir); `renderArt` precisa de `@GraphicsMode(NATIVE)`.
- **Chaves de `LazyColumn`** com ids de tabelas diferentes colidem (categoria 1 e remanejamento 1): use prefixo (`"b$id"`, `"a$id"`).
- Fonte grande nas imagens: `@Config(fontScale = 2.0f)`. Testes com a Activity de verdade marcam `AlertState.reminderAsked = true`
  (senão a pergunta do horário do lembrete abre por cima); o `TestApp` já faz isso.
- Imagens de tela com `CryoRoot` e um dia fixo: `LocalFixedToday`.
- Animações: o ambiente de teste do Compose **ignora a escala de animação do sistema** (por isso `animationsOn()` lê
  `Settings.Global.ANIMATOR_DURATION_SCALE` sozinho). Com `mainClock.autoAdvance = false`, mudar um estado de fora pede
  `rule.runOnUiThread { ...; Snapshot.sendApplyNotifications() }`. Animações finitas terminam antes das imagens (inclusive no
  `captureRoboImage { }`); infinitas travam o "idle" e são proibidas (DS-MOV-10).
- O `BiometricPrompt` não roda no Robolectric: desbloquear com `MainActivity.unlock()` (`@VisibleForTesting`). Para simular
  mais de 1 minuto em segundo plano: `scenario.moveToState(CREATED)`, `ShadowSystemClock.advanceBy(...)`, `moveToState(RESUMED)`
  (veja `DevModeFlowTest.bloqueioTiraOAppDaTelaEVoltaParaOndeEstava`).
- Os testes rodam em pt-BR (`src/test/resources/robolectric.properties`, `qualifiers=pt-rBR`): todo `@Config(qualifiers)` começa
  com "+" para não perder a língua. Testes JVM puros chamam `JvmStrings.install()` (lê `values/strings.xml`).
- Expressões regulares com grupos de nome (`groups["x"]`) dependem de uma função do Android 13+: use grupos numerados (veja `tpl`
  em `PhraseParser`). E o `\b` do Java e o do Android tratam letras fora do latim de jeitos diferentes: use `PhraseParser.wordRegex`.
- **Vetores com `<clip-path>`**: o leitor de vetores do Compose (`painterResource`) desloca os desenhos que vêm depois de um
  `clip-path` (o C saía dobrado). Desenhe o contorno como um caminho em vez de recortar (veja `ic_cryo_badge.xml`).
- Sombras (`Modifier.dropShadow`) aparecem nas imagens do Robolectric; a vibração (`HapticFeedback`) não faz nada lá, sem erro.
- O Robolectric **não tem o cofre do Android** (AndroidKeyStore): o `Vault` cai na chave da área privada para o HMAC, e o `wrap`
  só funciona nos testes com `Vault.softwareWrapForTests = true`. O caminho do Keystore de verdade só roda no celular.
- `org.json` nos testes JVM puros devolve valores vazios (`unitTests.isReturnDefaultValues`): o que usa JSON (backup, criptografia)
  é testado no Robolectric. `java.lang.management` não está na biblioteca do Android que compila os testes: use reflexão.
- PBKDF2 com 310 mil voltas leva ~0,3 s na JVM e ~1 s no celular: nos testes, passe menos voltas (`iterations = 1_000`).
- Imagens das telas do motor: sem `EngineRunner.inline = true` no `@Before`, a primeira composição sai sem o resultado do motor.
- **Compilação nativa** (1.5): precisa de `ANDROID_HOME` (o Gradle não acha o SDK sem ele aqui), do NDK `27.2.12479018` e do
  CMake `3.22.1` do SDK (`sdkmanager "cmake;3.22.1"`). A primeira compilação do Whisper leva ~3 min.
- **Whisper e silêncio**: com áudio vazio ele inventa frases ("O que é isso", "Legendas pela comunidade Amara.org"). Por isso
  ele só é chamado depois de voz de verdade, áudio sem energia nem chega a ele e `Spoken.clean` descarta essas frases.
- **Janela curta do Whisper** (`audio_ctx`): bem mais rápida, mas às vezes entra em repetição. Só nas palavras ao vivo (com
  limite de 48 pedaços); o resultado final usa a janela inteira.
- **Microfone falso nos testes**: o `FakeMic` despeja o áudio na hora; para testar "Pronto" e "Cancelar" use `paced = true`.
- Claude Code na nuvem: o Maven Central às vezes responde 429 (excesso de pedidos) ou redireciona arquivos grandes para um
  espelho que a rede não libera. Um init script local em `~/.gradle/init.d/` trocando o Maven Central pelo espelho do Google
  (`maven-central.storage-download.googleapis.com`) resolve, inclusive para o Robolectric (`robolectric.dependency.repo.url`);
  ele fica fora do projeto e não muda o APK. O SDK do Android vem de `dl.google.com` (o ambiente "cryo" libera esse endereço).
- **Folhas que sobem de baixo**: sempre `CryoSheet` (ERR-059; `PrdConformanceTest.folhasPelaCryoSheet`).
- **Imagens 106–114** (1.6): `Screens16Test`. Nas imagens da loja, as sugestões de orçamento × contas fixas ficam dispensadas.
- **`Expandable` fechado não está na árvore** (DS-EXP): para achar um texto de dentro, toque antes no título (veja
  `Screens16Test.orcamentoComAsContasFixas`). Em Análises há dois "Como cheguei nesse número" (o do mês e o do ano): use
  `onAllNodesWithText(...)[0]`.
- **O Geral é um orçamento de código 0**: teste que pega "o primeiro orçamento" dos dados de exemplo precisa pular o
  `GeneralBudget.ID` (o Room devolve a tabela pela chave, e o 0 vem primeiro).
- **Whisper de verdade no computador sem internet para o Hugging Face**: o modelo recomendado está no histórico do git (a 1.5
  o levava dentro do APK): `git show 18444d8:app/src/main/assets/whisper/ggml-base-q5_1.bin > /fora/do/projeto/ggml-base-q5_1.bin`
  e `CRYO_WHISPER_MODEL=/fora/do/projeto/ggml-base-q5_1.bin app/src/test/cpp/build-host.sh` (o script confere o SHA-256).
  Nunca devolva o arquivo para dentro do projeto (`SourceTreeTest`).
- **Nada chama a ponte do Whisper por fora do `WhisperEngine`** (ERR-065): é ele que garante que nenhuma chamada chega a um
  modelo já solto. Para testar a ordem das chamadas entre threads, use uma `WhisperBridge` falsa (`WhisperEngineGuardTest`).
- **Bloqueio nos testes** (1.7): a `LockScreen` chama a digital sozinha ao aparecer; desbloquear com `MainActivity.unlock()`
  encerra essa pergunta (sem isso, a segunda volta não bloqueava). Depois de mudar o tempo, espere a tela ler o ajuste
  (`rule.activity.lockAfterNow == segundos`): a leitura é por fluxo, fora da linha principal.
- **Ajustes mudados no meio do teste chegam por fluxo**: nas esperas, chame `ShadowLooper.idleMainLooper()` (como em
  `ArrangeTest`). E uma `LazyColumn` guarda a âncora da rolagem quando a arrumação chega depois da primeira composição: para
  uma imagem estável, comece o teste já com os ajustes lidos (`remember { runBlocking { c.settings.current() } }`).
- **Arrumação parcial não move nada**: `ScreenLayout` só reordena o que está na ordem guardada; o app sempre guarda a ordem
  inteira (`moved`). Teste com a ordem inteira.
- **Imagens do widget**: `GlanceRemoteViews().compose(ctx, DpSize)` (`@OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)`)
  devolve o `RemoteViews` de verdade; `apply` numa `FrameLayout`, medir, desenhar num `Bitmap` (veja `WidgetTest.render`).
  O `cornerRadius` do Glance só funciona no Android 12+: antes, o fundo é a forma pronta (`widget_bg`).
- **Nada de `Button` do Glance**: o `PrdConformanceTest` procura `Button(` nas telas; o "Lançar" do widget é uma `Box` clicável.
- **Abrir a `MainActivity` com um `Intent` próprio** (o "Lançar" do widget, os avisos): `createEmptyComposeRule()` e
  `ActivityScenario.launch<MainActivity>(intent)` (veja `WidgetFlowTest`); o `createAndroidComposeRule` já abre a dele.

## 11. Publicar uma versão

1. Subir `versionCode`/`versionName` em `app/build.gradle.kts`; `CHANGELOG.md` (inglês) e `docs/CHANGELOG.pt-BR.md`; changelogs do
   fastlane nas 10 línguas (≤ 500 caracteres).
2. Testes passando. Commit com a identidade noreply.
3. **APK a partir de um clone limpo** (o F-Droid compila do commit exato; arquivos soltos na pasta mudam o resultado):
   ```bash
   git clone <repo> /tmp/cryo-build && cd /tmp/cryo-build && git checkout <commit>
   cp -r <pasta-com-a-chave>/keystore .
   ./gradlew assembleRelease --no-daemon
   cp app/build/outputs/apk/release/app-release.apk Cryo-<versão>.apk
   ```
   Conferir a assinatura: `apksigner verify --print-certs Cryo-<versão>.apk` (SHA-256 da seção 2).
4. **Entregar o APK ao Hanry e esperar a aprovação.**
5. Push do commit em `main` **antes** de criar a Release. Criar a **Release `v<versão>`** no GitHub (ela cria a tag; "Target" =
   o branch da versão, nunca um `main` ainda sem o código novo) com o arquivo **exatamente** `Cryo-<versão>.apk` e as notas em
   inglês e português. (Em 04/11/2026 as Releases v1.3 e v1.4 foram criadas antes do push e prenderam as etiquetas no código da
   1.2; foram refeitas em 06/11/2026.) O nome importa: o F-Droid baixa
   `https://github.com/H3yK0/cryo/releases/download/v%v/Cryo-%v.apk` para comparar com o que ele compilou.
6. F-Droid: **não precisa abrir merge request** para versões novas. Se a receita mudar (ex.: licença), o merge request precisa
   **incluir também o build da versão nova** quando a etiqueta já existe: o teste `checkupdates` do fdroiddata roda o robô e
   reprova se ele ainda acharia algo para acrescentar (aconteceu no !56186, 07/11/2026). A receita (`docs/fdroid/io.github.h3yk0.cryo.yml`, cópia da
   que está no fdroiddata) tem `UpdateCheckMode: Tags` e `AutoUpdateMode: Version`: o robô do F-Droid acha a tag nova, compila,
   compara com o APK da Release e publica (pode levar alguns dias). Só é preciso MR se a receita mudar (ex.: novo passo de build).

Por que a compilação é reproduzível: `vcsInfo.include = false`, `dependenciesInfo` desligado e as tarefas `*ArtProfile*`
desligadas (o perfil de inicialização saía diferente em cada máquina). JDK 21 nos dois lados.

## 12. Histórico de versões

- **1.0** (01/10/2026): primeira versão pública. Registro por frase e voz, visão geral, previsão do mês, calendário de calor,
  fluxo do dinheiro, cartões e parcelas, contas fixas com lembrete, orçamentos, metas, investimentos, bloqueio, backup em arquivo.
- **1.1** (02/10/2026): área de Dívidas, arquivo de metas e de contas/cartões/investimentos, compilação reproduzível (pedido do revisor do F-Droid).
- **1.2** (03/10/2026): backup automático em pasta e/ou nuvem, com proteções contra sobrescrever o que não é dele.
- **1.3** (06/11/2026): importar extrato, modo desenvolvedor, correção do bloqueio.
- **1.4** (06/11/2026): 10 línguas e moeda do app, frases e extratos de outros países, visual 2.0 com 4 abas, passeio animado,
  animações, restaurar na apresentação, licença AGPL, PRD, GitHub em 10 línguas.
- **1.5** (16/11/2026, só no GitHub; junta as cinco etapas feitas depois da 1.4): ícone novo G4 e
  profundidade; motor financeiro e previsor conferido no passado; backup com senha e proteção dos dados; voz no aparelho com o
  Whisper; avisos inteligentes, sons, momentos e ilustrações próprias; painel do mês; auditoria e biblioteca de erros como lei;
  contas fixas que se acertam com o extrato; perfil; guia "Aprender o Cryo"; no cartão, estorno, escolha da fatura e ajustar total.
- **1.6** (21/11/2026; Release no GitHub em 22/11/2026; no F-Droid depois do merge request da receita): modelo de voz fora do APK (F-Droid), o fim do mês real (renda −
  despesas, nada estimado), orçamento pela renda, com as contas fixas e o Geral, russo, ucraniano, árabe e hebraico, os lotes
  LA1–LA8 (saúde no Início, extrato da conta, filtro por conta, previstas e pular o mês, último dia e fatura da tela, o ano e as
  metas, a lista que tremia) e a reforma visual LA9–LA13 (Início em blocos, central de avisos com o sino, tons pastel por
  prioridade, Análises e Planejar resumidos, Ajustes por tipo e a voz numa tela própria).
- **1.7** (pronta para o teste em 23/11/2026): o bloqueio por tempo e o modo privacidade (issues #6 e #7), o widget na tela
  inicial (Jetpack Glance), arrastar entre as abas, organizar os blocos de cada tela, o funil de verdade, a saúde no topo de
  Análises, o fluxo numa caixa só com Barras e Funil e o toque que abre as movimentações, o calendário de calor nas cores da
  temperatura e a caça a travamentos (teste de fumaça em todas as telas; a voz que fechava o app com um separador de linha
  incomum, as iniciais da semana em todas as línguas, o fluxo sem valores quando escondidos).

Detalhes de cada uma em `CHANGELOG.md` (inglês) e `docs/CHANGELOG.pt-BR.md`.

## 13. Decisões e porquês

- **PDF pelo leitor do próprio Android** (`PdfRenderer`, texto no Android 15+ ou 12–14 com extensão S ≥ 13), e não PdfBox:
  sem biblioteca de vários MB e sem BouncyCastle. Em aparelhos antigos, o app explica e sugere OFX/CSV.
- **Open Finance**: só instituições autorizadas pelo Banco Central recebem dados; o caminho pessoal seria o Meu Pluggy com as
  chaves do próprio usuário, o que exige internet. Como o Cryo não tem internet por princípio, **ficou decidido importar arquivos**.
  Se um dia voltar ao assunto, teria que ser algo separado (outro app ou variante), nunca no app principal.
- **JSON de teste só no modo de teste**; extratos de exemplo são reconhecidos (`SampleFiles.isSample`) e recusados com os dados
  de verdade. Nada do modo de teste chega aos dados de verdade.
- **Chave de importação por conta/cartão**: o mesmo identificador de banco em contas diferentes não é "já importado". Importações
  anteriores entram na busca de repetidas quando vieram de outro extrato (ex.: o pagamento da fatura importado pelo cartão
  aparece depois no extrato da conta) ou do mesmo extrato em outro formato (OFX e depois CSV).
- **Datas sem ano** ("05/12") ganham o ano da data completa mais recente do arquivo, voltando um ano se cairiam mais de 31 dias depois.
- **Bloqueio tira o app da tela** (e não uma camada por cima): uma camada deixava diálogos, menus e folhas por cima do bloqueio e o
  Voltar e o teclado chegando ao app escondido. O estado fica guardado (`SaveableStateHolder`), como ao girar a tela.
- **Chave de importação com o formato** e, no cartão, "já importada" só na mesma fatura: o mesmo período em outro formato passa
  pela busca de repetidas, e a parcela seguinte (mesma data, descrição e valor) não é escondida.
- **Exemplos reconhecidos por descrição e valor**: só a descrição pegava faturas de verdade (Uber, Spotify, iFood).
- **Sem perfil de inicialização (ART profile)** no APK: quebra a reprodutibilidade.
- **Uma moeda para o app todo, sem conversão**: sem internet não há cotação; trocar a moeda só muda símbolo e formato (aviso antes).
- **Língua fora das 10 → inglês**; moeda padrão pelo país do celular (en-GB → libra, es-MX → peso), senão a da língua.
- **Categorias padrão na língua de quem instala**, com palavras-chave do país; as de quem já usa não são traduzidas (são dados da pessoa).
- **Extratos de exemplo continuam brasileiros** (servem para testar a importação do Brasil); os rótulos deles são traduzidos.
- **Traduções feitas pelo Claude**, conferidas por `PrdConformanceTest` (chaves e marcadores) e por imagens; revisão humana pendente (RF-LNG-08).
- **Passeio desenhado com os componentes do app** (sem Lottie, vídeo ou arquivos de animação): não pesa no APK, segue tema, língua
  e moeda, e não traz biblioteca nova (PRC-07). Cada página tem uma linha do tempo finita (2,8 s).
- **Movimento sempre pelas APIs do Compose** (nunca `delay()`): elas seguem "Remover animações". O bloqueio nunca anima.
- **README em inglês na raiz** (é o que o GitHub mostra) e as outras 9 línguas em `docs/readme/`; os documentos de trabalho
  (PRD, CLAUDE.md, VISUAL.md, IMPORTACAO.md) continuam em português. As imagens de cada README são as da loja daquela língua.
- **Ícone G4** (07/11/2026): o Hanry achou o ícone antigo com "cara de feito com IA". Pesquisa de psicologia da atenção
  (contraste de cor quente sobre fundo frio, forma simples e reconhecível, continuidade com o que já existe) → 11 desenhos
  comparados → o C quadrado dourado com espessura. A estrela saiu (é o símbolo comum de "recurso de IA").
- **Dourado só na marca**: é a cor do símbolo, nunca de entrada ou saída de dinheiro (verde/vermelho continuam com esse papel).
- **Profundidade por peças centrais** (`Depth.kt` + `CryoTheme.depth`) e não sombra por tela: o mesmo peso em todo o app, nos dois
  temas e com Material You. Sombras largas e claras (nunca bordas escuras), luz vinda de cima, volume só na ação principal.
- **Vibração pelo `HapticFeedback` do Compose**: segue o ajuste do celular e não precisa da permissão `VIBRATE` (RNF-PRIV-03).
- **Motor sem rede neural** (07/11/2026): cada pessoa tem poucos dados (centenas de movimentações), o app não tem internet e
  cada número precisa ser explicável (PRI-05). Por isso: estatística robusta, simulação de cenários com semente tirada dos
  dados (mesmo dado, mesmo resultado), métodos numéricos com plano B (Newton com bissecção) e teste contra o passado.
- **Previsão por cenários e não por média**: a média se deixa levar por uma compra grande e dá um número só. Os cenários sorteiam
  dias parecidos do histórico (mesmo dia da semana, peso maior no recente) e dão a faixa de 80%. Entradas avulsas (freelas)
  também são sorteadas: sem isso, quem tem renda irregular via a faixa errar em 83% dos meses.
- **Juros embutidos pelo contrato**: o `DebtMath` conta as parcelas que faltam como saldo ÷ parcela; o motor usa as parcelas que
  faltam no contrato (total − pagas), que é o que mostra os juros quando a pessoa cadastrou o saldo para quitar do banco.
- **Hash + criptografia, só com o Android** (pedido do Hanry, 07/11/2026): o motor guarda só HMAC-SHA256 (chave no Keystore);
  backup com senha em AES-256-GCM com PBKDF2. Sem biblioteca nova. A senha nunca é guardada: só a chave derivada, trancada pelo
  cofre (para o backup automático funcionar sozinho).
- **Sem SQLCipher** (criptografia do banco inteiro): biblioteca nativa de vários MB que o F-Droid não compila do código-fonte,
  o que quebraria a compilação reproduzível; o banco já fica na área privada, protegido pela criptografia do próprio Android.
- **Sem Rust por enquanto** (o Hanry pediu para avaliar): o Kotlin já protege a memória e o motor é rápido (50 mil movimentações
  em ~85 ms na JVM); Rust não impede vazamento de dados, e código nativo aumentaria o APK (uma cópia por processador) e
  complicaria a compilação reproduzível. Faz sentido se um dia houver Cryo para computador ou iPhone: um motor só para todos.
- **Uma 1.5 só** (decisão do Hanry, 12/11/2026): nada do que veio depois da 1.4 tinha sido publicado, então tudo sai junto como
  1.5, para a numeração pública seguir sem buracos (1.4 → 1.5). O `versionCode` é 10 e não 6: o celular do Hanry já tinha APKs de
  teste com os números 6 a 9, e o Android não instala um número menor por cima. Quem usa só vê "1.5".
- **Voz com o Whisper e regras, sem modelo de linguagem** (1.5): o ouvido (voz → texto) é IA local; o cérebro (texto →
  lançamento) continua com regras exatas e testáveis. Modelos de conversa locais pesam de 300 MB a 1 GB, demoram e podem
  inventar números. Modelo "base" q5_1 (57 MB): o "tiny" erra mais em português e o "small" pesa 190 MB.
- **Um app só, não um Cryo por língua** (análise pedida pelo Hanry, 09/11/2026): o modelo multilíngue tem o mesmo tamanho para
  qualquer língua e não há modelo oficial menor só de português; 10 apps dividiriam pessoas, backups e atualizações.
- **Whisper só em arm64 com fp16+dotprod**: compilar uma versão só deixa a biblioteca com 1,6 MB; celulares sem essas instruções
  (anteriores a ~2018) seriam lentos demais para o Whisper e usam o reconhecedor do celular. A conferência é feita lendo o
  `/proc/cpuinfo` **antes** de carregar a biblioteca (carregar num processador sem elas derrubaria o app).
- **Bibliotecas vazias** para armeabi-v7a, x86 e x86_64: um APK com biblioteca nativa só instala nos processadores que têm
  versão dela.
- **Avisos só em dois horários** (1.5): de manhã e no horário do lembrete do dia, no máximo um aviso comum por dia. Por isso
  não existe "horário silencioso" separado: a madrugada já fica livre e há menos ajuste para a pessoa entender.
- **Notificação animada pelo estilo de conversa** (1.5): estudo do código do SystemUI (AOSP): só o `MessagingStyle` dá "play"
  sozinho numa imagem animada (com a cortina aberta ou o aviso por cima); `BigPictureStyle` carrega mas não anima;
  `RemoteViews` com `ViewFlipper` é desaconselhado e cada fabricante desenha diferente.
- **"Ao vivo" só para o modo compras** (1.5): o Google só deixa promover o que a pessoa começou, está acontecendo e tem fim;
  avisos e lembretes não podem.
- **Voz: o resultado final continua com a janela inteira do Whisper** (1.5): a janela do tamanho da fala era ~4 vezes mais
  rápida, mas no teste com o modelo de verdade ouviu "32,40" no lugar de "32,50", com qualquer folga até 10 s. Valor errado
  não compensa. A "atenção rápida" (flash attention) foi ~20% mais rápida no computador com o mesmo texto, mas estava desligada
  na 1.5 aprovada e não dá para testar no celular daqui: fica como chave do modo desenvolvedor até o Hanry medir no A55.
- **Rótulos curtos com a fonte limitada a 1,3x** (abas e atalhos, `CappedFontScale`): com a fonte em 200% eles quebravam no meio
  da palavra; o resto do app cresce sem limite (RF-ALC-04).
- **Previsor por conjunto de modelos simples, pesados pelo passado da própria pessoa** (1.5): sem rede neural (pouco dado por
  pessoa, sem internet, tudo explicável). O que decide é o laboratório (`ForecastLabTest`): o primeiro motor fica no código como
  régua e o novo só entra onde erra menos. Faixa conferida contra o passado (calibração conforme): prometer 80% e cumprir.
- **Época do ano só com dois anos de confirmação** (1.5): com um ano, os alarmes falsos custavam mais do que ela rendia.
- **Encolher o erro, não o peso** (1.5): com pouco passado, o conjunto antes misturava os pesos com "todos iguais", e cada modelo
  ganhava uma fatia fixa mesmo errando feio (mercado gasto no começo do mês virava alarme, ERR-037). Agora o erro medido de cada
  modelo encolhe para o erro médio, tanto mais quanto menos dias o mediram: a cautela continua, sem dar voz a quem errou.
- **Duas rentabilidades na tela do investimento** (1.5): a TIR ("Rendimento real", o que a pessoa ganhou, conta quando ela aportou)
  e a do investimento em si (Dietz modificado, usada na projeção). Elas podem diferir bastante (aportes grandes em meses ruins);
  por isso o texto da segunda diz "o investimento em si ... (sem contar quando você aportou)".
- **Ritmo da conta fixa por "a cada N meses" + parcelas** (1.5, issue #3): em vez de inventar um calendário completo, a
  conta fixa tem um ciclo (`every`) e um número de parcelas mensais seguidas (`installments`). Isso cobre mensal, bimestral,
  trimestral, semestral, anual e o IPVA em 3x com quatro números simples, sem tabela nova e com uma regra que cabe numa
  linha (`BillCycle.partOf`). O valor guardado é o **de cada parcela** (é o que a pessoa paga e o que aparece no extrato).
- **Juros da conta fixa pelo preço à vista** (1.5): o app não calcula taxa nenhuma; a pessoa informa quanto custaria à
  vista e o Cryo mostra a diferença. É um número que ela conhece e dá para conferir.
- **Modelos de conta fixa por língua** (1.5): a lista é a mesma, o nome vem dos textos. Assim o Brasil vê "IPVA", o Japão
  自動車税 e os Estados Unidos "Vehicle registration" sem código diferente por país.
- **Casamento de conta fixa em dois níveis** (1.5): só o CERTO (nome + valor igual, ou apelido confirmado) age sozinho; o PROVÁVEL
  sempre pergunta. Ligar errado sozinho custaria mais confiança do que perguntar.
- **"Não é conta fixa" guardado no próprio lançamento** (`billYm = 0`): não pede migração do banco, vai junto no backup e some
  quando o lançamento some.
- **Guia por regras, sem IA** (1.5): cada lição tem uma condição que dá para ler e testar; mesmo estado, mesma dica. O guia só
  guarda contagens e dias, num arquivo à parte, e só conta o que nenhuma lição conseguiria deduzir dos dados.
- **A fatura guardada manda** (1.5, issue #3): o banco fecha em dias que variam, então a regra da data é só uma sugestão. Editar não
  recalcula; a pessoa escolhe a fatura quando precisa.
- **"Ajustar total" é um lançamento à vista**, não um número escondido: quem olha a fatura vê o ajuste e pode apagá-lo; os totais
  continuam sendo sempre a soma do que está lançado.
- **O fim do mês real, sem cenários** (21/11/2026, pedido do Hanry): o número do mês passou a ser renda − despesas com só o
  que já aconteceu e o que é fixo. A previsão por cenários continua no motor (categorias, o ano, as metas), mas o número do
  mês não estima nada: é uma conta que a pessoa confere linha por linha e que só cai conforme as despesas entram.
- **O Geral é uma linha de `budgets`** (código de categoria 0) e não uma tabela ou um arquivo à parte: vai e volta no backup,
  fica separado no modo de teste e sai com "Apagar todos os dados" sem migração; as versões antigas simplesmente não o mostram.
- **Central de avisos com um destaque só** (DS-PSI): a lista de avisos e as percepções saíram do meio do Início e de
  Análises; o sino conta o que é importante e novo, e o Início mostra no máximo um destaque. O "visto" é só o HMAC da chave do
  aviso (RF-SEG-02), como no motor.
- **Cor pastel só onde há prioridade** (efeito de isolamento): se tudo é colorido, nada chama o olho; o tom nunca é o único
  sinal (sempre há ícone ou texto), e a profundidade continua.
- **Dados de exemplo escalados por moeda** com fatores inteiros (as contas dos exemplos continuam batendo); real, dólar, euro,
  libra e as outras de valor parecido ficam com os mesmos números.
- **Widget com o Jetpack Glance** (1.7, PRC-07, aprovado pelo Hanry na DLA de 23/11/2026 "só se o F-Droid não impedir"):
  escrever o widget à mão (RemoteViews e um layout XML por tamanho) seria muito mais código para manter, e o Glance é do
  próprio Android (AndroidX, Apache-2.0, sem rede, sem código nativo). Conferido em 23/11/2026 com o fdroidserver 2.4.5: o
  verificador do código (`scanner.scan_source`, o mesmo que o servidor do F-Droid roda antes de compilar; com o Firebase
  posto de propósito numa cópia ele reclama, então a conferência é de verdade) e o do APK (`fdroid scanner`) sem nenhum
  problema; as mesmas permissões e as mesmas bibliotecas nativas da 1.6; 6 módulos novos, todos `androidx`. O APK cresce
  ~0,8 MB (o Glance traz ~960 layouts prontos). A lista de assinaturas mais nova (`-r`, em fdroid.gitlab.io) não abre
  daqui: o verificador usou a que vem com ele. Repetir a conferência ao trocar a versão do Glance (`docs/fdroid/COMO-ATUALIZAR.md`).
- **O widget mostra os valores mesmo com o bloqueio e com "Esconder valores"** (decisão do Hanry, 23/11/2026): pôr um widget
  na tela é escolher a conveniência. Por isso ele nunca abre nada por fora do bloqueio (tocar abre o app, que pede o
  desbloqueio) e a tela de editar só mostra escolhas, nunca dados. Sempre os dados de verdade, como os avisos.
- **Bloqueio por tempo** (1.7, issues #6 e #7): Na hora, 1, 5 ou 15 minutos; o padrão continua 1 minuto (o de sempre). Voltar
  do que o próprio Cryo abriu (escolher um arquivo, o navegador do modelo de voz, compartilhar) tem 5 minutos de folga, senão
  "Na hora" pediria o desbloqueio no meio de uma importação. O **modo privacidade** é o `FLAG_SECURE` (print e gravação
  saem pretos); no Android 12 ou anterior ele vem ligado com o bloqueio, porque ali não existe outro jeito de esconder a
  miniatura de Recentes (`setRecentsScreenshotEnabled` é do Android 13).
- **Arrastar entre as abas** (1.7): só o gesto claramente de lado (1,5 vez mais de lado que para cima) e nunca nos 48 dp de
  cada borda, que são do gesto de voltar do Android; listas e gráficos que arrastam de lado ficam fora (`noTabSwipe`).
- **A arrumação de cada tela é um texto curto** ("a,-b,c": a ordem e os escondidos) numa chave do DataStore por tela e no
  backup (`"layout"`), sem migração do banco. Um bloco novo de uma versão futura entra logo depois do vizinho de sempre, e
  pelo menos um bloco fica à vista.
- **O funil escorre** (1.7, LA4: "mais com cara de funil, saltado e clicável"): a boca é o dinheiro do mês e cada faixa
  estreita pelo que saiu para um destino, até o que sobrou; de onde o dinheiro veio fica em pílulas acima. Faixas com
  sombra, lado mais escuro e brilho (o "saltado"), e cada uma afunda um pouco ao toque.

Correções da primeira revisão da 1.3 (todas feitas e conferidas na revisão final): pagamento de fatura importado duas vezes; chaves
globais escondendo movimentações; ano errado em datas sem ano; vazamentos no relatório anônimo (cabeçalhos, nome do arquivo);
exemplos e backups de teste chegando aos dados de verdade; marcação de linha que sobrevivia à troca de conta; sinais e tipo de
extrato errados em PDF sem sinal; erro ao recriar dados de exemplo duas vezes; categoria de despesa salva em linha de receita;
toques seguidos recalculando a partir de estado velho; desfazer sem tratamento de erro; relatório grande demais para compartilhar;
bloqueio que jogava a pessoa de volta ao Início; falta de aviso de modo de teste fora das abas; textos ("1 toque", "Entrou").

Correções da revisão final (01/11/2026): diálogos e menus abertos por cima do
bloqueio; Voltar e teclado chegando ao app bloqueado; miniatura de Recentes com os dados; "Marcar todas" marcando pagamento de
fatura sem cartão (agora avisa); PDF que marca só as entradas; "-" do saldo negativo colado no valor do PDF; saldo negativo
decidindo os sinais de um PDF sem sinais; mesmo período em outro formato entrando em dobro; parcela seguinte escondida como já
importada; fatura com valor em dólar e nome do titular (C6); OFX com linhas de saldo e valor zero; fatura real tomada por
exemplo; aviso de conta abrindo os dados de teste; troca de dados com a tela fora de vista; cópia do relatório completo sem
confirmação; caixa de marcar da conferência sem descrição para leitores de tela; plural de "arquivos de exemplo salvos".

## 14. Roadmap (prioridades do Hanry)

1. **1.3** e **1.4**: publicadas no GitHub (06/11/2026); receita do F-Droid atualizada (!56186, 07/11/2026): acompanhar a 1.4 na loja.
2. **1.5**: publicada no GitHub (16/11/2026); não vai para o F-Droid. **1.6**: publicada no GitHub (Releases de 22/11/2026); entra no F-Droid quando o merge request da receita for aceito. **1.7**: pronta para o teste do Hanry (23/11/2026); não precisa de merge request no F-Droid.
3. Revisão das traduções por quem fala cada língua (RF-LNG-08).
4. Depois da 1.7 (a 1.8, proposta no PRD): importar o banco do xpenc (LA5, issue #5); contas a receber e as outras ideias da olhada no Tally; dívida quitada e última parcela como momentos (RF-MOM-04);
   as lições que ficaram fora do guia (esconder valores, transferir, calendário, backup com senha).
5. Importação: aprender os formatos dos bancos que o Hanry usa (com relatório anônimo, nunca o arquivo real).

## 15. Arquivos de referência

| Arquivo | Para quê |
|---|---|
| `docs/PRD.md` | **Documento de requisitos: manda em tudo** (catálogo de telas e requisitos, design system, processo) |
| `docs/ERROS.md` | **Biblioteca de erros já corrigidos (lei)**: ler antes de mexer; cada linha tem o teste que guarda |
| `README.md` | Apresentação pública (GitHub), em inglês; as outras 9 línguas em `docs/readme/` |
| `CHANGELOG.md` | Novidades de cada versão, em inglês (em português: `docs/CHANGELOG.pt-BR.md`) |
| `docs/VISUAL.md` | Guia visual: cores, tipografia, componentes, telas, textos |
| `docs/IMPORTACAO.md` | Importação de extratos e modo desenvolvedor (para usuários e programadores) |
| `docs/visual/` | Imagens atuais das telas (as do guia visual) |
| `docs/fdroid/io.github.h3yk0.cryo.yml` | Cópia da receita publicada no F-Droid |
| `fastlane/metadata/android/` | Textos e imagens da página do F-Droid, nas 10 línguas |
| `app/schemas/` | Schemas do Room (não apagar: as migrações dependem deles) |
