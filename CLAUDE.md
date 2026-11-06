# Cryo — guia geral do projeto

Este arquivo reúne tudo o que é preciso para continuar o Cryo. O Claude Code lê este arquivo sozinho ao abrir a pasta.

> **Regra principal: o PRD manda.** Tudo o que o app faz e como ele é feito e mantido está em **[docs/PRD.md](docs/PRD.md)**,
> com um código para cada item (RF-, RNF-, DS-, TXT-, PRC-...). Toda mudança segue o ciclo do PRD (PRC-01) e a definição de
> pronto (PRC-04): primeiro o PRD, depois o código, os testes e a revisão. `PrdConformanceTest` confere sozinho as regras que
> dá para conferir (licença, permissões, cores fixas, catálogo de telas, schema do banco, changelogs). Este CLAUDE.md é o guia
> de trabalho no código; se ele disser outra coisa que o PRD, vale o PRD.
Para mexer em telas, leia também **[docs/VISUAL.md](docs/VISUAL.md)**. Para a importação de extratos, **[docs/IMPORTACAO.md](docs/IMPORTACAO.md)**.

Este arquivo é **público** (vai para o GitHub junto com o código): nunca escrever aqui senhas, chaves ou dados pessoais.

Situação descrita aqui: **06/11/2026**.

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
| 1.4 | 5 | **Publicada no GitHub em 06/11/2026** (Release `v1.4` com `Cryo-1.4.apk`, SHA-256 `4208d49e…82b5`; etiqueta no commit `f8a402b`, branches `main` e `versao-1.4`). F-Droid: entra sozinha pela etiqueta `v1.4` (alguns dias). |

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

### Próximos passos imediatos

1. **F-Droid**: acompanhar a entrada da 1.4 (o robô acha a etiqueta `v1.4`, compila e compara com o APK da Release). A licença
   mudou para AGPL: o Hanry abre o merge request no fdroiddata trocando `License: GPL-3.0-or-later` por `AGPL-3.0-or-later`
   (a cópia em `docs/fdroid/` já está com AGPL).
2. As traduções foram feitas pelo Claude: pedir revisão de quem fala cada língua (RF-LNG-08, por issue).
3. Importação: perguntar ao Hanry quais bancos ele usa e aprender os formatos (com relatório anônimo).
4. 1.5: detalhar com o Hanry (widgets, notificações especiais, sons).

---

## 4. Regras de ouro (não negociáveis)

**Privacidade**
- O app **não tem permissão de INTERNET** e não pode ganhar. Nada de bibliotecas de analytics, anúncios, crash reporting
  online ou qualquer coisa que fale com servidores. Permissões atuais: `POST_NOTIFICATIONS` e `USE_BIOMETRIC`, só.
- Dados ficam só no aparelho. Nuvem só pelo seletor de arquivos do Android (o app da nuvem é quem envia).
- `android:allowBackup="false"`; `data_extraction_rules.xml` só permite transferência direta entre aparelhos e exclui as
  permissões do backup automático e tudo do modo de teste.
- **Nunca** colocar extratos reais de banco no repositório (nem em testes). Testes usam arquivos inventados.

**Processo**
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
│                       SampleData.kt (dados de exemplo), Dev.kt (modo desenvolvedor), LocaleSettings.kt (língua e moeda)
├── domain/             Regras sem Android: Ledger.kt (saldos, faturas, contas fixas, orçamentos), Analytics.kt (previsão,
│                       calendário, fluxo), Debts.kt (dívidas), PhraseParser.kt + PhraseLang.kt (registro por frase e as
│                       palavras de cada língua), Format.kt (valores e datas), I18n.kt (língua, moeda, Res)
├── importer/           Leitura de extratos e conferência (veja docs/IMPORTACAO.md)
├── notify/             Lembretes de contas e parcelas (diário, ~9h) e worker do backup automático (a cada 6h), via WorkManager
└── ui/                 Nav.kt (rotas, barra de abas, transições), components/ (peças reutilizáveis; Motion.kt = movimento),
                        screens/ (telas; Tour.kt = passeio), tx/ (lançamento), theme/
```

**Como os dados andam**
1. `AppContainer` (em `CryoApp.kt`) cria o banco Room, o `FinanceRepository`, os ajustes (DataStore), o backup e o modo desenvolvedor.
   As telas pegam o container por `LocalContainer.current`.
2. `FinanceRepository.snapshot` é um `StateFlow<Snapshot?>` com **todos** os dados (combina os fluxos do Room).
3. `rememberLedger()` transforma o `Snapshot` num `Ledger` (recalculado quando os dados mudam ou o dia vira). Todas as contas
   (saldo, fatura, previsão, dívidas) saem do `Ledger`, nunca de consultas soltas.
4. Para gravar: métodos do repositório, chamados em `c.launch { }` (escopo do app, sobrevive à troca de tela).
5. Mensagens rápidas: `c.message("texto", "Desfazer") { ... }` mostra a snackbar.

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

**Room** (`cryo.db`, versão 3; teste: `cryo-teste.db`). Tabelas: `accounts`, `cards`, `categories`, `transactions`,
`investments`, `investment_yields`, `goals`, `bills`, `budgets`, `debts`, `debt_adjustments`.
Migrações automáticas: 1→2 (dívidas, com `Migration1To2`) e 2→3 (`transactions.importKey` + índice). Schemas em `app/schemas/`.

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
| Registro rápido por frase/voz | `ui/tx/QuickEntry.kt`, `domain/PhraseParser.kt` (voz = `RecognizerIntent` do próprio celular) |
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
| Lembretes | `notify/Reminders.kt` |

## 10. Testes

Robolectric 4.16.1 (JVM, sem celular) + Roborazzi (imagens das telas). Cerca de 255 testes:

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

**Armadilhas já descobertas (economizam horas):**
- O DataStore é um singleton por processo e o Robolectric **não zera entre testes**: testes que usam a Activity começam com
  `settings.clear()` (veja `DevModeFlowTest.realData()` e `AppFlowTest`).
- `AlertDialog` com campo de texto **trava** o Robolectric ("Compose did not get idle"). Por isso a senha do PDF é uma tela, não um diálogo.
  `ModalBottomSheet` com campo de texto funciona.
- ViewModel com `viewModelScope`: esperar com `rule.waitUntil { ShadowLooper.idleMainLooper(); condição }` (veja `ImportFlowTest.waitFor`).
- Itens de `LazyColumn` fora da tela não existem na árvore: rolar antes (`performScrollToNode`) ou esperar por textos do topo.
- Animações infinitas (spinner) impedem o "idle": não deixar `CircularProgressIndicator` na tela enquanto o teste espera.
- Numeração das imagens: 01–36 telas e fluxos (inclui backup), 37–43 importação, 44–46 modo desenvolvedor, 47–52 passeio.
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
6. F-Droid: **não precisa abrir merge request** para versões novas. A receita (`docs/fdroid/io.github.h3yk0.cryo.yml`, cópia da
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

1. **1.3** e **1.4**: publicadas no GitHub (06/11/2026); acompanhar o F-Droid e o merge request da licença.
2. Revisão das traduções por quem fala cada língua (RF-LNG-08).
3. **1.5**: widgets na tela inicial; notificações especiais (popups); sons personalizados tocados de forma aleatória.
   Detalhar com o Hanry antes de começar (quais widgets, quais momentos, quais sons).
4. Importação: aprender os formatos dos bancos que o Hanry usa (com relatório anônimo, nunca o arquivo real).

## 15. Arquivos de referência

| Arquivo | Para quê |
|---|---|
| `docs/PRD.md` | **Documento de requisitos: manda em tudo** (catálogo de telas e requisitos, design system, processo) |
| `README.md` | Apresentação pública (GitHub), em inglês; as outras 9 línguas em `docs/readme/` |
| `CHANGELOG.md` | Novidades de cada versão, em inglês (em português: `docs/CHANGELOG.pt-BR.md`) |
| `docs/VISUAL.md` | Guia visual: cores, tipografia, componentes, telas, textos |
| `docs/IMPORTACAO.md` | Importação de extratos e modo desenvolvedor (para usuários e programadores) |
| `docs/visual/` | Imagens atuais das telas (as do guia visual) |
| `docs/fdroid/io.github.h3yk0.cryo.yml` | Cópia da receita publicada no F-Droid |
| `fastlane/metadata/android/` | Textos e imagens da página do F-Droid, nas 10 línguas |
| `app/schemas/` | Schemas do Room (não apagar: as migrações dependem deles) |
