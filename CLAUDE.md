# Cryo — guia geral do projeto

Este arquivo reúne tudo o que é preciso para continuar o Cryo. O Claude Code lê este arquivo sozinho ao abrir a pasta.

> **Regra principal: o PRD manda.** Tudo o que o app faz e como ele é feito e mantido está em **[docs/PRD.md](docs/PRD.md)**,
> com um código para cada item (RF-, RNF-, DS-, TXT-, PRC-...). Toda mudança segue o ciclo do PRD (PRC-01) e a definição de
> pronto (PRC-04): primeiro o PRD, depois o código, os testes e a revisão. `PrdConformanceTest` confere sozinho as regras que
> dá para conferir (licença, permissões, cores fixas, catálogo de telas, schema do banco, changelogs). Este CLAUDE.md é o guia
> de trabalho no código; se ele disser outra coisa que o PRD, vale o PRD.
Para mexer em telas, leia também **[docs/VISUAL.md](docs/VISUAL.md)**. Para a importação de extratos, **[docs/IMPORTACAO.md](docs/IMPORTACAO.md)**.

Este arquivo é **público** (vai para o GitHub junto com o código): nunca escrever aqui senhas, chaves ou dados pessoais.

Situação descrita aqui: **10/11/2026**.

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
| Idioma | 10 línguas a partir da 1.4 (base pt-BR; en, es, fr, de, it, ja, ko, zh-CN, hi), na moeda escolhida. Até a 1.3: só português e reais |
| Assinatura | Chave própria do Hanry. SHA-256 do certificado: `6e13e284c2f00b72473589391776ed2f28dbad5506f515de3fac99a364a3a2e7` |

## 3. Onde estamos

| Versão | versionCode | Situação |
|---|---|---|
| 1.0 | 1 | Publicada (GitHub, tag `v1.0`) |
| 1.1 | 2 | Publicada (GitHub) |
| 1.2 | 3 | Publicada no GitHub e no F-Droid (commit `a9b3d209bc51ee7531afda8174192a94f1e36f81`). |
| 1.3 | 4 | **Publicada no GitHub em 06/11/2026** (Release `v1.3` com `Cryo-1.3.apk`; etiqueta no commit `5377d7a`, branch `versao-1.3`). |
| 1.4 | 5 | **Publicada no GitHub em 06/11/2026** (Release `v1.4` com `Cryo-1.4.apk`, SHA-256 `4208d49e…82b5`; etiqueta no commit `f8a402b`, branches `main` e `versao-1.4`). F-Droid: merge request **!56186** (licença AGPL + build da 1.4) **aceito em 07/11/2026** (por @linsui); a 1.4 entra na loja quando o servidor do F-Droid compilar e conferir com o APK da Release (alguns dias). |
| 1.8 | 9 | **Pronta** (10/11/2026, PRD 6.12b e 6.12c), sem push, aguardando o teste do Hanry: auditoria do código com as correções, **biblioteca de erros como lei** (`docs/ERROS.md`) e **previsor novo, feito do zero** (mês, categorias, investimentos) com as telas que explicam a conta. Branch `versao-1.8` (a partir da 1.7). |
| 1.7 | 8 | **Pronta** (09/11/2026, PRD 6.19c), sem push, aguardando o teste do Hanry: avisos inteligentes estilo Duolingo, sons, momentos animados, painel do mês, botão de voz embaixo (One UI), modo compras ao vivo. Branch `versao-1.7` (a partir da 1.6). |
| 1.6 | 7 | **Testada e aprovada pelo Hanry** (09/11/2026: "gostei pra caramba"; ~5 s no A55), sem push: **voz no aparelho** com o Whisper (RF-VOZ). Branch `versao-1.6` (a partir da 1.5). Lançamento depois da 1.5. |
| 1.5 | 6 | **Pronta e guardada** (07/11/2026), sem push: ícone G4 + profundidade + **motor financeiro** + **proteção dos dados** (o Hanry pediu para juntar tudo numa versão só, já que a 1.5 ainda não tinha saído). APK `Cryo-1.5.apk` (SHA-256 `a575908b…8941`) entregue. **O Hanry pediu para lançar só daqui a um mês (por volta de 07/12/2026).** O código está no branch `versao-1.5` e numa cópia (`git bundle`) entregue a ele; só publicar com o "pode publicar" dele (seção 11). |

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

### O que a 1.5 traz (branch `versao-1.5`)

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

### O que a 1.6 traz (branch `versao-1.6`, a partir da 1.5)

- **Voz no aparelho** (RF-VOZ, PRD 6.2a): o Whisper (whisper.cpp v1.9.5, MIT, cópia em `app/src/main/cpp/whisper/`) compilado
  pelo NDK numa biblioteca só (`libcryo_whisper.so`, 1,6 MB, só arm64 com fp16+dotprod; nos outros, uma biblioteca vazia) e o
  modelo "base" q5_1 (57 MB) em `assets/whisper/`, lido direto do APK. O áudio só passa pela memória.
- **Tela de eco** (`ui/tx/VoiceSheet.kt`): o protótipo aprovado virou a tela de verdade (palavras ao vivo, anéis por sílaba,
  cartõezinhos com mola). Microfone pedido com explicação (`MicPermissionSheet`).
- **Português falado** (`domain/Spoken.kt`): números por extenso, "18 e 90" = 18,90, correções ("32, não, 35"), apelidos
  ("roxinho"), vários lançamentos numa fala, frases que o Whisper inventa no silêncio descartadas.
- Escolha do ouvido em Ajustes (Whisper ou o reconhecedor do celular só no aparelho); sem nenhum dos dois, a tela de voz do sistema.
- Permissão nova `RECORD_AUDIO`. APK de ~6 MB para ~65 MB. Banco de dados continua na versão 3.

### O que a 1.7 traz (branch `versao-1.7`, a partir da 1.6)

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
- **Voz**: o som de "pode falar" não entra na gravação; fim da fala em 1,2 s (como na 1.6); tempo na tela de voz e chave
  experimental "Atenção rápida no Whisper" no modo desenvolvedor. Estudo de outras IAs de fala no PRD (RF-VOZ-14).
- **Banco de dados versão 4** (`budget_adjustments`, `no_spend_days`, `goals.art`; migração 3→4 automática com `Migration3To4`).

### O que a 1.8 traz (branch `versao-1.8`, a partir da 1.7)

- **Auditoria do código inteiro** (RF-AUD, PRD 6.12c): backup com senha nunca sai aberto (falha fechada), backup lido com limite e
  conferido campo a campo, planilha sem fórmulas, erro do sensor só libera aparelho sem trava, `FLAG_SECURE` no Android 8–12 com o
  bloqueio ligado, valores enormes por `Money.centsOrNull`, "Já paguei" em transação, apagar categoria leva os remanejamentos,
  cada aviso com o seu `PendingIntent`. Testes `Audit18Test`, `Audit18DataTest`.
- **Biblioteca de erros = lei** (`docs/ERROS.md`, PRC-08): cada erro já corrigido, a regra e o teste que guarda; `KnownIssuesTest`.
- **Previsor novo, do zero** (RF-PRV, PRD 6.12b, `domain/predict/`): série diária sem os extremos (`Series.kt`), 7 modelos clássicos
  (`Models.kt`: perfil da semana, médias de 3 e 6 meses, o mesmo trecho do mês puro e no nível de agora, Holt amortecido, Theta),
  conjunto pesado pelo erro de cada modelo no passado da própria pessoa, com o **erro encolhido para a média** quando há pouco
  passado (`Ensemble.kt`), época do ano só confirmada em dois anos (`Season.kt`), previsão do mês com a faixa **conferida no
  passado** (calibração conforme, `Forecaster.kt`), **categorias** com a chance de passar do limite (`CategoryForecast.kt`, também
  usada no aviso de ritmo) e **investimentos** "se continuar como foi" (`InvestForecast.kt`: Dietz modificado, sobe e desce, maior
  queda, 1/3/5 anos). O motor da 1.5 (`Outlook.month`, `Pacing.projected`) fica como régua dos testes.
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
  recusado com mensagem; num mês fora do padrão que nunca se repetiu, a 1.5 reagia mais depressa (o novo ganha no conjunto).

### Continuar: a 1.9 (roteiro; a especificação completa está no PRD 6.19d)

Pedido do Hanry (10/11/2026). Tudo no aparelho, **sem permissão nova e sem mudar o banco** (versão 4). Nesta ordem:

1. **RF-CFX — contas fixas × extrato** (conserta número errado: fazer primeiro). Causa conferida: a importação não conhece contas
   fixas (`importer/ImportPlan.kt` só compara valor + conta + 3 dias); "paga" = movimentação com `billId`/`billYm`. A linha
   importada entra sem vínculo → conta fixa segue pendente, a previsão conta em dobro (`pendingBillsExpense` em
   `domain/Analytics.kt`) e "Já paguei" depois cria uma segunda movimentação.
   - Começar pela regra pura `BillMatcher` (em `importer/`, com testes JVM) e por RF-CFX-07 (previsão sem dobro: mexer em
     `Ledger.billStatuses`/`forecast` com cuidado: o motor, as categorias e os avisos leem daí). Depois a conferência
     (`ImportRow` ganha o casamento; `ImportModel` grava com `billId`), "Já paguei" (`Repository.markBillPaid`, mantendo a
     transação do ERR-018), a tela "Organizar contas fixas" (`BillReconciler` puro + desfazer) e os apelidos por HMAC no `EngineStore`.
   - Nomes: normalizar com `TxKey.of` (já tira números e pontuação). Parcelas (`installmentTotal > 1`) e `CARD_PAYMENT` não casam.
2. **RF-PRF — perfil**: `HomeHeader` (`ui/components/Sections.kt`) troca a engrenagem pela bolinha clicável; rota nova `profile`
   (entrar no catálogo de telas do PRD 5.2, senão `PrdConformanceTest.todaTelaEstaNoCatalogo` falha); ajustes novos `avatar` e
   `avatarColor` no DataStore (`SettingsRepository`). Emoji = dado da pessoa (um grafema, `BreakIterator`), nunca texto do app.
   Folha de editar com campo de texto: usar `ModalBottomSheet` (o `AlertDialog` com campo trava o Robolectric).
3. **RF-GUI — guia "Aprender o Cryo"**: `GuidePlanner` puro (molde: `AlertPlanner` + `AlertPlannerTest`), `GuideState` (molde:
   `AlertState`), anfitrião no molde do `MomentHost`. **O `TestApp` precisa desligar o guia** (como faz com `reminderAsked`), senão
   as telas de teste ganham um cartão por cima. Um destaque por vez; animação finita.
4. Fechar: textos nas 10 línguas, imagens 86+, `versionCode 10` / `"1.9"`, changelogs (3 arquivos + 10 da loja), PRD (marcar feitos,
   catálogo), `docs/ERROS.md` (os defeitos do item 1 viram `ERR-039+` com guarda), suíte inteira, APK de clone limpo. **Sem push.**

### Próximos passos imediatos

1. **1.5**: guardada para lançar por volta de 07/12/2026 (pedido do Hanry). Se esta sessão não existir mais, recuperar o código do
   `git bundle` que o Hanry guardou (`git fetch cryo-1.5.bundle versao-1.5:versao-1.5`, sobre o `main` do GitHub). Na data, com o
   "pode publicar": push de `versao-1.5` e de `main`, Release `v1.5` (seção 11). O F-Droid pega sozinho pela etiqueta.
2. **F-Droid**: o merge request !56186 (licença AGPL e build da 1.4) foi aceito em 07/11/2026; a cópia em `docs/fdroid/` é igual à
   receita oficial. Conferir em https://f-droid.org/packages/io.github.h3yk0.cryo/ que a 1.4 apareceu (alguns dias). Se não
   aparecer em ~1 semana, olhar o registro de builds do F-Droid (https://monitor.f-droid.org/builds) à procura de falha.
3. As traduções foram feitas pelo Claude: pedir revisão de quem fala cada língua (RF-LNG-08, por issue).
4. Importação: perguntar ao Hanry quais bancos ele usa e aprender os formatos (com relatório anônimo).
5. **1.6** (branch `versao-1.6`): testada e aprovada pelo Hanry no A55 (09/11/2026; ~5 s até a transcrição). **No lançamento**:
   a receita do F-Droid precisa da linha do NDK (merge request no fdroiddata; rascunho em `docs/fdroid/1.6-receita.md`).
6. **1.8** (branch `versao-1.8`, a partir da 1.7): pronta em 10/11/2026, APK entregue para o teste do Hanry. **Conferir no celular**:
   os cartões da previsão em Análises, Orçamentos e no investimento com os dados de verdade dele; se algum número parecer estranho,
   pedir o relatório anônimo (nunca o dado real) e medir no laboratório antes de mudar.
7. **1.7** (branch `versao-1.7`, a partir da 1.6): pronta em 09/11/2026, APK entregue para o teste do Hanry. **Conferir no A55**:
   a figurinha anima na cortina (RF-AVI-09)? o modo compras aparece na Now Bar (RF-AVI-10)? o tempo da voz com e sem a chave
   "Atenção rápida no Whisper" (modo desenvolvedor › Voz; RF-VOZ-13: se ficar estável e mais rápida, ligar por padrão).

---

## 4. Regras de ouro (não negociáveis)

**Privacidade**
- O app **não tem permissão de INTERNET** e não pode ganhar. Nada de bibliotecas de analytics, anúncios, crash reporting
  online ou qualquer coisa que fale com servidores. Permissões atuais: `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, (1.6)
  `RECORD_AUDIO` e (1.7) `POST_PROMOTED_NOTIFICATIONS` (notificação ao vivo do modo compras), só. O áudio da voz nunca vira arquivo e é apagado da memória ao terminar (RF-VOZ-02).
- Dados ficam só no aparelho. Nuvem só pelo seletor de arquivos do Android (o app da nuvem é quem envia).
- `android:allowBackup="false"`; `data_extraction_rules.xml` só permite transferência direta entre aparelhos e exclui as
  permissões do backup automático e tudo do modo de teste.
- **Nunca** colocar extratos reais de banco no repositório (nem em testes). Testes usam arquivos inventados.
- O motor financeiro **não grava resultados**; o pouco que guarda é **HMAC** (RF-SEG-02). A senha dos backups **nunca** é
  guardada (só a chave derivada, trancada pelo cofre do Android). Nada de `Log`, `println` ou `printStackTrace` (RF-SEG-07).

**Processo**
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
  `fastlane/metadata/android/<língua>/changelogs/<versionCode>.txt` nas 10 línguas (máximo 500 caracteres cada).
- **O que aparece na página inicial do GitHub é em inglês** (pedido do Hanry, 06/11/2026): README, CHANGELOG, notas das Releases
  (inglês primeiro, depois português) e **mensagens de commit**. Comentários do código e documentos de trabalho continuam em português.
- Mudança no banco de dados = nova versão do Room com migração e teste em `MigrationTest` (os schemas ficam em `app/schemas/`).
  Backups antigos precisam continuar abrindo (`Backup.import` lê todas as versões).

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
│                       Vault.kt (cofre: Keystore, HMAC, AES), BackupCrypto.kt (backup com senha), EngineStore.kt
├── domain/             Regras sem Android: Ledger.kt (saldos, faturas, contas fixas, orçamentos), Analytics.kt (previsão,
│                       calendário, fluxo), Debts.kt (dívidas), PhraseParser.kt + PhraseLang.kt (registro por frase e as
│                       palavras de cada língua), Format.kt (valores e datas), I18n.kt (língua, moeda, Res),
│                       engine/ (o motor financeiro: Engine.kt junta tudo; Stats, Recurrences, Outlook, Projection,
│                       Signals, Health, Wealth, CategoryModel, TxKey),
│                       predict/ (1.8, o previsor: Series, Models, Season, Ensemble, Forecaster, CategoryForecast,
│                       InvestForecast)
├── importer/           Leitura de extratos e conferência (veja docs/IMPORTACAO.md)
├── voice/            Voz no aparelho (1.6): WhisperNative (JNI), WhisperEngine, Listener (WhisperListener e o medidor de
│                       volume/sílabas), SystemListener (plano B), AudioInput/MicInput, VoiceSettings e VoicePrompt
├── notify/             Avisos (1.7): AlertPlanner (regras), Alerts (canais e notificação), AlertWork (manhã, noite, botões),
│                       AlertState, Shopping (modo compras ao vivo), Sounds; Reminders (contas e dívidas); backup automático
└── ui/                 Nav.kt (rotas, barra de abas, transições), components/ (peças reutilizáveis; Motion.kt = movimento,
                        Depth.kt = profundidade e vibração), screens/ (telas; Tour.kt = passeio), tx/ (lançamento),
                        theme/ (Theme.kt: cores, CryoTheme.colors, CryoTheme.depth)
```

Código nativo (1.6): `app/src/main/cpp/` = `CMakeLists.txt` (opções da compilação reproduzível), `cryo_whisper.cpp` (a ponte
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

**Room** (`cryo.db`, versão 4; teste: `cryo-teste.db`). Tabelas: `accounts`, `cards`, `categories`, `transactions`,
`investments`, `investment_yields`, `goals`, `bills`, `budgets`, `debts`, `debt_adjustments` e (1.7) `budget_adjustments`
(remanejar só no mês; `moveGroup` liga as duas pontas) e `no_spend_days`.
Migrações automáticas: 1→2 (dívidas, com `Migration1To2`), 2→3 (`transactions.importKey` + índice) e 3→4 (tabelas novas e
`goals.art`, preenchido a partir do emoji por `Migration3To4`). Schemas em `app/schemas/`.

**Tipos de movimentação** (`TxType`): `EXPENSE`, `INCOME` (no cartão = estorno), `TRANSFER` (conta → conta),
`CARD_PAYMENT` (conta → cartão, com `invoiceYm`), `INVEST_IN`/`INVEST_OUT`, `GOAL_IN`/`GOAL_OUT` (caixinhas),
`DEBT_IN` (dinheiro emprestado que entrou: não conta como receita).

**Cartão**: cada compra guarda `invoiceYm` (fatura em que cai). Compra no dia do fechamento ou depois vai para a fatura seguinte
(`CardMath.invoiceFor`). Parcelado = uma movimentação por parcela, ligadas por `installmentGroup`.

**Backup** (`data/Backup.kt`): JSON `{"app":"Cryo","version":2,"exportedAt":…, accounts, cards, categories, transactions, investments,
yields, goals, bills, budgets, debts, debtAdjustments}`. Backups da 1.0 (versão 1) continuam abrindo. Cópia feita no modo de
teste leva `"testMode": true` e se chama `cryo-teste-backup-<data>.json`: não pode ser restaurada por cima dos dados de verdade.
Exportação para planilha: CSV com `;`.

**Backup automático** (`data/AutoBackup.kt`): pasta do celular (substituir sempre `cryo-backup.json` ou guardar cópias
`cryo-backup-AAAA-MM-DD-HHMMSS.json`, mantendo 5/10/30/todas) e/ou um arquivo na nuvem. Frequência diária, semanal ou mensal.
Proteções: só substitui ou apaga arquivos que este celular gravou; se outro aparelho mexeu no arquivo ou se muitas movimentações
sumirem de repente, pausa e avisa; se nada mudou, não grava cópia igual.

**Ajustes**: DataStore `settings` (tema, cores do papel de parede, bloqueio, esconder valores, nome, lembretes, conta principal,
apresentação vista); `settings_test` no modo de teste; `auto_backup`; SharedPreferences `cryo_dev` (modo desenvolvedor).

**Importação**: cada movimentação importada guarda `importKey` = `a<conta>|<formato>.id:<FITID>` ou `c<cartão>|<formato>.h:<hash>`
(ex.: `a3|ofx.id:123`, `c7|csv.h:9f3a…`; + `#n` para linhas repetidas). Detalhes em `docs/IMPORTACAO.md`.

## 9. Funcionalidades (onde estão)

| Área | Arquivos principais |
|---|---|
| Início (saldo, patrimônio, resumo do mês, alertas, previsão) | `ui/screens/HomeScreen.kt` |
| Registro rápido por frase/voz | `ui/tx/QuickEntry.kt`, `domain/PhraseParser.kt`; voz (1.6): `ui/tx/VoiceSheet.kt`, `voice/`, `domain/Spoken.kt`, `src/main/cpp/` |
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
| Lembretes e avisos (1.7) | `notify/` (`AlertPlanner.kt`, `Alerts.kt`, `AlertWork.kt`, `Shopping.kt`, `Sounds.kt`), `ui/screens/AlertScreens.kt` |
| Momentos, ilustrações | `ui/components/Moments.kt`, `ui/components/Art.kt` |
| Painel do mês (orçamento) | `ui/screens/BudgetMonthScreen.kt` |
| Motor financeiro (previsão com faixa, recorrências, próximos meses, saúde, dívidas, rendimento) | `domain/engine/`, `ui/screens/EngineScreens.kt`, cartões em `InsightsScreen.kt` |
| Proteção dos dados (cofre, HMAC, backup com senha) | `data/Vault.kt`, `data/BackupCrypto.kt`, `data/EngineStore.kt`, `ui/screens/SecurityUi.kt` |

## 10. Testes

Robolectric 4.16.1 (JVM, sem celular) + Roborazzi (imagens das telas). Cerca de 790 testes:

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
| `SpokenTest`, `VoiceListenerTest`, `VoiceFlowTest` | Voz (1.6): português falado; a escuta com microfone e Whisper falsos (início, fim, cancelar, nada ouvido); a tela de eco (imagens 58–62) |
| `WhisperHostTest` | A ponte JNI e o modelo de verdade, compilados para o computador (só com `-PwhisperHost=true`, depois de `app/src/test/cpp/build-host.sh`) |
| `AlertPlannerTest`, `AlertActionsTest` | Avisos (1.7): cada regra com dados inventados (um por dia, dois horários, pessoa nova, tipo desligado, dispensado 3 vezes, ritmo, lembrete e sequência, resumo de domingo), exemplos, botões da notificação, desbloqueio, tela de bloqueio, figurinha no `MessagingStyle`, sons no silencioso |
| `MomentsTest`, `BudgetMonthTest`, `Screens17Test` | Momentos depois de salvar e o modo compras; remanejar só no mês e backup; imagens 71–81 (painel, avisos, momentos, compras, botão de voz, fonte em 200%) |
| `ForecastTest`, `ForecastLabTest`, `Audit18Test`, `Audit18DataTest`, `KnownIssuesTest`, `Screens18Test` | 1.8: o previsor contra o motor da 1.5 em mundos inventados (`ForecastWorld`), a faixa de 80%, a época do ano, categorias e investimentos; o laboratório completo (só com `-PforecastLab=true`); as correções da auditoria; a biblioteca de erros (`docs/ERROS.md`) inteira e com as guardas ligadas; imagens 82–85 (previsão conferida, orçamentos, investimento, carteira) |
| `EngineStabilityTest`, `EngineScalabilityTest`, `EnginePerformanceTest`, `EngineResourcesTest`, `EngineSecurityTest` | Os **500 testes de garantia** (RF-MOT-15), 100 casos cada; o de segurança roda no Robolectric (`ParameterizedRobolectricTestRunner`) |

**Armadilhas já descobertas (economizam horas):**
- O DataStore é um singleton por processo e o Robolectric **não zera entre testes**: testes que usam a Activity começam com
  `settings.clear()` (veja `DevModeFlowTest.realData()` e `AppFlowTest`).
- `AlertDialog` com campo de texto **trava** o Robolectric ("Compose did not get idle"). Por isso a senha do PDF é uma tela, não um diálogo.
  `ModalBottomSheet` com campo de texto funciona.
- ViewModel com `viewModelScope`: esperar com `rule.waitUntil { ShadowLooper.idleMainLooper(); condição }` (veja `ImportFlowTest.waitFor`).
- Itens de `LazyColumn` fora da tela não existem na árvore: rolar antes (`performScrollToNode`) ou esperar por textos do topo.
- Animações infinitas (spinner) impedem o "idle": não deixar `CircularProgressIndicator` na tela enquanto o teste espera.
- Numeração das imagens: 01–36 telas e fluxos (inclui backup), 37–43 importação, 44–46 modo desenvolvedor, 47–52 passeio, 53–57 motor, 58–62 voz, 63–68 proposta da 1.7, 69–70 ilustrações, 71–81 telas da 1.7, 82–85 previsor da 1.8.
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
- **Compilação nativa** (1.6): precisa de `ANDROID_HOME` (o Gradle não acha o SDK sem ele aqui), do NDK `27.2.12479018` e do
  CMake `3.22.1` do SDK (`sdkmanager "cmake;3.22.1"`). A primeira compilação do Whisper leva ~3 min.
- **Whisper e silêncio**: com áudio vazio ele inventa frases ("O que é isso", "Legendas pela comunidade Amara.org"). Por isso
  ele só é chamado depois de voz de verdade, áudio sem energia nem chega a ele e `Spoken.clean` descarta essas frases.
- **Janela curta do Whisper** (`audio_ctx`): bem mais rápida, mas às vezes entra em repetição. Só nas palavras ao vivo (com
  limite de 48 pedaços); o resultado final usa a janela inteira.
- **Microfone falso nos testes**: o `FakeMic` despeja o áudio na hora; para testar "Pronto" e "Cancelar" use `paced = true`.
- Claude Code na nuvem: o Maven Central às vezes responde 429 (excesso de pedidos). Um init script local em
  `~/.gradle/init.d/` trocando o Maven Central pelo espelho do Google resolve; ele fica fora do projeto e não muda o APK.

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
- **1.8** (pronta em 10/11/2026, aguardando o teste do Hanry): auditoria do código com as correções, biblioteca de erros como lei,
  previsor novo do zero (mês, categorias, investimentos) com a faixa conferida no passado e as telas que explicam a conta.
- **1.7** (pronta em 09/11/2026, aguardando o teste do Hanry): avisos inteligentes com figurinhas animadas, modo compras ao vivo,
  sons próprios, momentos, ilustrações no lugar de emoji, painel do mês e remanejar só no mês, botão de voz no meio da barra.
- **1.6** (aprovada em 09/11/2026): voz no aparelho com o Whisper e a animação de eco.
- **1.5** (pronta em 07/11/2026, aguardando o teste do Hanry): ícone novo G4 (C dourado com volume), profundidade em todo o app,
  motor financeiro no aparelho, backup com senha e proteção dos dados, 500 testes de garantia.

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
- **1.5 unida**: o Hanry gostou da 1.5 (ícone e profundidade) e pediu o motor; como ela não tinha sido publicada, saiu tudo junto
  como 1.5 (versionCode 6). Widgets, notificações especiais e sons ficaram para a 1.6.
- **Voz com o Whisper e regras, sem modelo de linguagem** (1.6): o ouvido (voz → texto) é IA local; o cérebro (texto →
  lançamento) continua com regras exatas e testáveis. Modelos de conversa locais pesam de 300 MB a 1 GB, demoram e podem
  inventar números. Modelo "base" q5_1 (57 MB): o "tiny" erra mais em português e o "small" pesa 190 MB.
- **Um app só, não um Cryo por língua** (análise pedida pelo Hanry, 09/11/2026): o modelo multilíngue tem o mesmo tamanho para
  qualquer língua e não há modelo oficial menor só de português; 10 apps dividiriam pessoas, backups e atualizações.
- **Whisper só em arm64 com fp16+dotprod**: compilar uma versão só deixa a biblioteca com 1,6 MB; celulares sem essas instruções
  (anteriores a ~2018) seriam lentos demais para o Whisper e usam o reconhecedor do celular. A conferência é feita lendo o
  `/proc/cpuinfo` **antes** de carregar a biblioteca (carregar num processador sem elas derrubaria o app).
- **Bibliotecas vazias** para armeabi-v7a, x86 e x86_64: um APK com biblioteca nativa só instala nos processadores que têm
  versão dela.
- **Avisos só em dois horários** (1.7): de manhã e no horário do lembrete do dia, no máximo um aviso comum por dia. Por isso
  não existe "horário silencioso" separado: a madrugada já fica livre e há menos ajuste para a pessoa entender.
- **Notificação animada pelo estilo de conversa** (1.7): estudo do código do SystemUI (AOSP): só o `MessagingStyle` dá "play"
  sozinho numa imagem animada (com a cortina aberta ou o aviso por cima); `BigPictureStyle` carrega mas não anima;
  `RemoteViews` com `ViewFlipper` é desaconselhado e cada fabricante desenha diferente.
- **"Ao vivo" só para o modo compras** (1.7): o Google só deixa promover o que a pessoa começou, está acontecendo e tem fim;
  avisos e lembretes não podem.
- **Voz: o resultado final continua com a janela inteira do Whisper** (1.7): a janela do tamanho da fala era ~4 vezes mais
  rápida, mas no teste com o modelo de verdade ouviu "32,40" no lugar de "32,50", com qualquer folga até 10 s. Valor errado
  não compensa. A "atenção rápida" (flash attention) foi ~20% mais rápida no computador com o mesmo texto, mas estava desligada
  na 1.6 aprovada e não dá para testar no celular daqui: fica como chave do modo desenvolvedor até o Hanry medir no A55.
- **Rótulos curtos com a fonte limitada a 1,3x** (abas e atalhos, `CappedFontScale`): com a fonte em 200% eles quebravam no meio
  da palavra; o resto do app cresce sem limite (RF-ALC-04).
- **Previsor por conjunto de modelos simples, pesados pelo passado da própria pessoa** (1.8): sem rede neural (pouco dado por
  pessoa, sem internet, tudo explicável). O que decide é o laboratório (`ForecastLabTest`): o motor da 1.5 fica no código como
  régua e o novo só entra onde erra menos. Faixa conferida contra o passado (calibração conforme): prometer 80% e cumprir.
- **Época do ano só com dois anos de confirmação** (1.8): com um ano, os alarmes falsos custavam mais do que ela rendia.
- **Encolher o erro, não o peso** (1.8): com pouco passado, o conjunto antes misturava os pesos com "todos iguais", e cada modelo
  ganhava uma fatia fixa mesmo errando feio (mercado gasto no começo do mês virava alarme, ERR-037). Agora o erro medido de cada
  modelo encolhe para o erro médio, tanto mais quanto menos dias o mediram: a cautela continua, sem dar voz a quem errou.
- **Duas rentabilidades na tela do investimento** (1.8): a TIR ("Rendimento real", o que a pessoa ganhou, conta quando ela aportou)
  e a do investimento em si (Dietz modificado, usada na projeção). Elas podem diferir bastante (aportes grandes em meses ruins);
  por isso o texto da segunda diz "o investimento em si ... (sem contar quando você aportou)".
- **Dados de exemplo escalados por moeda** com fatores inteiros (as contas dos exemplos continuam batendo); real, dólar, euro,
  libra e as outras de valor parecido ficam com os mesmos números.

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
2. **1.5** (ícone G4 + profundidade + motor financeiro + proteção dos dados): pronta e guardada; lançamento por volta de 07/12/2026.
3. Revisão das traduções por quem fala cada língua (RF-LNG-08).
4. **1.6**: **voz melhor e no aparelho** (feita e aprovada). **1.7**: avisos inteligentes, sons, momentos animados, alertas de
   orçamento, voz ao alcance do polegar (pronta, aguardando o teste). **1.8**: auditoria, biblioteca de erros e previsor novo
   (pronta, aguardando o teste). **1.9** (proposta, PRD 6.19d): contas fixas que se acertam com o extrato, perfil, guia
   "Aprender o Cryo". Depois: widgets na tela inicial; dívida quitada e última
   parcela como momentos (RF-MOM-04).
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
