# Cryo — PRD (Documento de Requisitos do Produto)

> **Este documento manda.** Tudo o que o Cryo faz, como ele se parece e como ele é feito e mantido está aqui, com um código
> para cada item. Código, telas, textos e testes seguem o PRD; se algo no app não está aqui, ou está diferente daqui, um dos
> dois está errado e precisa ser corrigido **antes** de publicar (veja [PRC-01](#11-processo-de-criação-e-manutenção)).
>
> Público: este arquivo vai para o GitHub. Nunca escrever aqui senhas, chaves ou dados pessoais.

## 0. Controle do documento

| Item | Valor |
|---|---|
| Produto | Cryo · "Seu dinheiro, claro como gelo." |
| Dono do produto | Hanry Franco (aprova tudo o que vai para o público) |
| Versão do PRD | **2.1** (04/11/2026) |
| Versão publicada do app | 1.2 (GitHub e F-Droid) · 1.3 pronta, aguardando teste · 1.4 em preparação |
| Documentos anexos | [`docs/VISUAL.md`](VISUAL.md) (detalhe visual), [`docs/IMPORTACAO.md`](IMPORTACAO.md) (detalhe da importação), [`CLAUDE.md`](../CLAUDE.md) (como trabalhar no código), [`CHANGELOG.md`](../CHANGELOG.md) |

**Hierarquia**: PRD → anexos → código. Os anexos detalham o que o PRD define; se algum anexo disser outra coisa, vale o PRD e o
anexo é corrigido no mesmo commit.

### 0.1 Como ler os códigos

| Prefixo | O que é | Exemplo |
|---|---|---|
| `PRI-` | Princípio do produto | PRI-01 Privacidade em primeiro lugar |
| `PER-` / `JOR-` | Persona / jornada | JOR-02 Registrar um gasto em 5 segundos |
| `TELA-` | Tela do app (com a rota) | TELA-INICIO |
| `RF-<MÓDULO>-nn` | Requisito funcional | RF-IMP-07 |
| `RNF-<ÁREA>-nn` | Requisito não funcional | RNF-PRIV-01 |
| `DAD-` | Regra de dados | DAD-01 Dinheiro em centavos |
| `DS-` | Design system (cor, letra, forma, componente, padrão) | DS-COMP-14 Seção plana |
| `TXT-` / `ACS-` | Regra de texto / de acessibilidade | TXT-03 Erro = o que houve + o que fazer |
| `PRC-` | Processo de criação e manutenção | PRC-04 Definição de pronto |

**Status** de cada requisito: ✅ feito (com a versão em que entrou) · 🟡 em andamento (na versão indicada) ·
🔵 proposto (aguarda aprovação do Hanry) · ⛔ fora do escopo (decidido que não entra).

### 0.2 Histórico do documento

| Versão | Data | Mudança |
|---|---|---|
| 1.0 | 19/10/2026 | Regras espalhadas em CLAUDE.md, VISUAL.md e IMPORTACAO.md (base desta versão). |
| 2.0 | 04/11/2026 | PRD único com catálogo e códigos; processo de criação e manutenção; conformidade automática; design system 2.0 (visual de banco, cores do Cryo); licença AGPL. |
| 2.1 | 04/11/2026 | Aprovados: piloto do Início, direção 2.0 em todas as telas, 4 abas, restaurar backup na apresentação. Novos: 10 línguas (RF-LNG) e moeda do app (RF-MOE), frases nas 10 línguas, importação de outros países; tudo na 1.4. |

---

## 1. Visão

**Problema.** Organizar o dinheiro costuma exigir planilha, cadastro em app de banco ou entregar os dados para uma empresa.
Quem só quer saber "quanto posso gastar até o fim do mês" desiste no meio.

**Proposta.** Um app de finanças pessoais que:
- registra um gasto em segundos, por frase ou voz ("almoço 32,50 no débito");
- responde as perguntas do dia a dia na primeira tela: quanto tenho, quanto devo no cartão, como está o mês;
- funciona **sem internet**, sem cadastro, sem anúncios, com os dados só no celular;
- é **software livre** (AGPL v3 ou posterior) e publicado no F-Droid com compilação reproduzível.

**Objetivos da 1.4** (veja a [seção 12](#12-roadmap)): aparência organizada de app de banco (referência: Nubank), mantendo as
cores do Cryo; app mais fácil e intuitivo (4 abas, atalhos); **10 línguas e a moeda de cada país**; licença AGPL; este PRD como
regra para tudo.

## 2. Princípios (PRI)

Toda decisão é conferida contra estes princípios, nesta ordem de prioridade.

| ID | Princípio | Na prática |
|---|---|---|
| PRI-01 | **Privacidade em primeiro lugar** | Sem permissão de internet, sem analytics, sem cadastro. Nada sai do celular sem a pessoa escolher o arquivo. |
| PRI-02 | **Nunca perder dados** | Migração testada a cada mudança no banco; backups antigos sempre abrem; nada apaga sem confirmação e, quando dá, desfazer. |
| PRI-03 | **Simples de usar** | A ação mais comum leva um toque; termos do dia a dia; uma ação principal por tela. |
| PRI-04 | **O número em primeiro lugar** | O valor que importa é o maior elemento da tela. |
| PRI-05 | **Explicar em vez de esconder** | Todo aviso diz o que aconteceu e o que fazer. |
| PRI-06 | **Calmo e organizado** | Visual limpo, cor com propósito, nada piscando; seções em vez de blocos empilhados (DS 2.0). |
| PRI-07 | **Para todo mundo** | Acessível (leitor de tela, fonte grande, contraste), Android 8 em diante. |
| PRI-08 | **Livre e verificável** | Código aberto (AGPL), compilação reproduzível, F-Droid. |

## 3. Pessoas e jornadas

### 3.1 Personas (PER)

| ID | Quem | Precisa de |
|---|---|---|
| PER-01 | **Hanry**, o criador: usa todo dia, conta corrente + cartão, quer ver o mês e importar o extrato do banco. | Rapidez, previsão do mês, importação confiável. |
| PER-02 | **Quem quer sair do vermelho**: tem dívidas, cartão no limite. | Ver quanto deve, parcelas, quando fica livre, quanto pode gastar por dia. |
| PER-03 | **Quem preza privacidade**: chegou pelo F-Droid. | Nada de internet, código aberto, backup no próprio controle. |
| PER-04 | **Quem tem pouca familiaridade com tecnologia**: usa o celular para WhatsApp e banco. | Textos simples, botões grandes, poucos passos, voz. |
| PER-05 | **Quem não fala português**: chegou pelo F-Droid em outro país (EUA, Espanha, Coreia, Itália, Alemanha, Índia, China, Japão, França...). | O app na sua língua, com a sua moeda e o jeito de escrever datas e valores do seu país; registrar por frase na sua língua. |

### 3.2 Jornadas principais (JOR)

| ID | Jornada | Passos ideais | Requisitos |
|---|---|---|---|
| JOR-01 | Primeiro uso | Abrir → boas-vindas (nome, conta principal, cartão opcional) → Início pronto. Máximo 4 telas. | RF-BOA-01..05 |
| JOR-02 | Registrar um gasto em 5 segundos | Início → falar/escrever "almoço 25 no débito" → conferir → Salvar. | RF-REG-01..08 |
| JOR-03 | Saber como está o mês | Abrir o app: o Início mostra entrou, saiu e quanto sobra no fim do mês. | RF-INI-05, RF-ANA-01 |
| JOR-04 | Pagar a fatura do cartão | Início › Cartão › Pagar fatura (ou atalho) → valor sugerido → Salvar. | RF-CAR-06 |
| JOR-05 | Importar o extrato do banco | Atalho Importar → escolher arquivo → conferir → Importar. | RF-IMP-01..20 |
| JOR-06 | Trocar de celular | Backup automático na nuvem → celular novo → Ajustes › Backup automático → escolher a mesma nuvem → o Cryo mostra o backup encontrado → Restaurar. | RF-BKP-05..10, RF-BOA-06 |
| JOR-07 | Sair do vermelho | Dívidas → ver quanto falta e a próxima parcela → registrar pagamento. | RF-DIV-01..08 |

## 4. Escopo

**Dentro**: finanças pessoais de uma pessoa, numa moeda (a do seu país, RF-MOE), em 10 línguas (RF-LNG), num celular Android.

**Fora (⛔, decidido)**:

| ID | Fora do escopo | Por quê |
|---|---|---|
| ESC-01 | Internet, sincronização própria, conta online | PRI-01. A nuvem só entra pelo seletor de arquivos do Android (o app da nuvem envia). |
| ESC-02 | Open Finance / conexão com bancos | Exige internet e instituição autorizada; decidido importar arquivos (CLAUDE.md, seção 13). Se um dia voltar, será outro app, nunca o principal. |
| ESC-03 | Anúncios, analytics, crash reporting online | PRI-01. |
| ESC-04 | Várias moedas ao mesmo tempo (conversão entre moedas) | Decidido (04/11/2026): **uma moeda para o app todo** (RF-MOE-01). Sem internet não há cotação; compras internacionais entram pelo valor na moeda da fatura. |
| ESC-05 | Uso por empresa / várias pessoas | Escopo pessoal. |

## 5. Arquitetura de informação

### 5.1 Navegação

- **Abas** (barra inferior, **4** a partir da 1.4, RF-INI-14): Início · Extrato · Planejar · Análises. Ajustes pela engrenagem
  do Início. Até a 1.3 eram 5 (com Carteira).
- **Início é o painel** (DS 2.0, 1.4): responde "quanto tenho, quanto devo, como está o mês" e leva a tudo pelos atalhos e pelas
  seções com ›. As seções de contas, cartões, investimentos e dívidas ficam no Início; a Carteira completa (TELA-CARTEIRA:
  patrimônio detalhado, todas as contas, cartões, investimentos e arquivados) abre pelo patrimônio e pelas seções.
- Telas internas abrem por cima, com seta de voltar.

### 5.2 Catálogo de telas (TELA)

Toda rota de `ui/Nav.kt` (`Routes`) está aqui. Tela nova = linha nova nesta tabela no mesmo commit (conferido por
`PrdConformanceTest`).

| ID | Tela | Rota | Arquivo |
|---|---|---|---|
| TELA-INICIO | Início (painel) | `home` | `ui/screens/HomeScreen.kt` |
| TELA-EXTRATO | Extrato (movimentações por mês, busca, calendário) | `txs?category={category}` | `ui/screens/TransactionsScreen.kt` |
| TELA-PLANEJAR | Planejar (abas Orçamentos, Contas fixas, Metas) | `plan?tab={tab}` | `ui/screens/PlanScreens.kt` |
| TELA-CARTEIRA | Carteira completa (patrimônio, contas, cartões, dívidas, investimentos); aba até a 1.3, tela interna na 1.4 | `wallet` | `ui/screens/WalletScreens.kt` |
| TELA-ANALISES | Análises (previsão, calendário de calor, fluxo, categorias, 6 meses) | `insights` | `ui/screens/InsightsScreen.kt` |
| TELA-LANCAR | Novo lançamento | `tx/new?type={type}&account={account}&card={card}` | `ui/tx/TxEditorScreen.kt`, `ui/tx/TxForm.kt` |
| TELA-LANC-EDITAR | Editar lançamento | `tx/edit/{id}` | `ui/tx/TxEditorScreen.kt` |
| TELA-CONTA | Conta (saldo, movimentações) | `account/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-CONTA-EDITAR | Nova/editar conta | `account/edit/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-CARTAO | Cartão e faturas | `card/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-CARTAO-EDITAR | Novo/editar cartão | `card/edit/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-INVEST | Investimento (valor, rendimento, histórico) | `investment/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-INVEST-EDITAR | Novo/editar investimento | `investment/edit/{id}` | `ui/screens/WalletScreens.kt` |
| TELA-ARQUIVADOS | Contas, cartões e investimentos arquivados | `wallet/archive` | `ui/screens/WalletScreens.kt` |
| TELA-META-EDITAR | Nova/editar meta | `goal/edit/{id}` | `ui/screens/PlanScreens.kt` |
| TELA-METAS-ARQUIVO | Metas arquivadas | `goals/archive` | `ui/screens/PlanScreens.kt` |
| TELA-FIXA-EDITAR | Nova/editar conta fixa (a pagar ou a receber) | `bill/edit/{id}?kind={kind}` | `ui/screens/PlanScreens.kt` |
| TELA-DIVIDAS | Dívidas | `debts` | `ui/screens/DebtScreens.kt` |
| TELA-DIVIDA | Detalhe da dívida | `debt/{id}` | `ui/screens/DebtScreens.kt` |
| TELA-DIVIDA-EDITAR | Nova/editar dívida | `debt/edit/{id}` | `ui/screens/DebtScreens.kt` |
| TELA-AJUSTES | Ajustes | `settings` | `ui/screens/SettingsScreens.kt` |
| TELA-BACKUP | Backup automático | `settings/backup` | `ui/screens/BackupScreens.kt` |
| TELA-IMPORTAR | Importar extrato (passo a passo) | `import?account={account}&card={card}` | `ui/screens/ImportScreen.kt`, `ui/screens/ImportModel.kt` |
| TELA-RELATORIO | Relatório da leitura (modo desenvolvedor) | `import/report` | `ui/screens/DevScreens.kt` |
| TELA-DEV | Modo desenvolvedor | `settings/dev` | `ui/screens/DevScreens.kt` |
| TELA-CATEGORIAS | Categorias | `categories` | `ui/screens/SettingsScreens.kt` |
| TELA-CATEGORIA-EDITAR | Nova/editar categoria | `category/edit/{id}?kind={kind}` | `ui/screens/SettingsScreens.kt` |
| TELA-BOAS-VINDAS | Apresentação inicial (sem rota: antes do app) | — | `ui/screens/Onboarding.kt` |
| TELA-BLOQUEIO | Cryo bloqueado (sem rota: no lugar do app) | — | `ui/screens/Onboarding.kt` (`LockScreen`) |

Folhas e diálogos que fazem parte de uma tela (registro rápido, pagar dívida, conferir linha da importação) seguem o padrão
DS-PAD-03 e são citados no requisito da tela.

---

## 6. Requisitos funcionais (RF)

Formato: **ID · requisito · critério de aceite · status**. Os testes citados ficam em `app/src/test/java/io/github/h3yk0/cryo/`.

### 6.1 Início — RF-INI (TELA-INICIO)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-INI-01 | Saudação com o nome | "Olá, Nome" ou "Olá!" sem nome; data por extenso. | ✅ 1.0 |
| RF-INI-02 | Saldo em contas em destaque | Soma das contas marcadas "somar no saldo total"; maior número da tela (PRI-04). | ✅ 1.0 |
| RF-INI-03 | Patrimônio | Contas + investimentos + caixinhas − cartões − dívidas, com a explicação da conta. | ✅ 1.1 |
| RF-INI-04 | Esconder valores | O olho troca todos os valores do app por `R$ ••••` e é lembrado. | ✅ 1.0 |
| RF-INI-05 | Resumo do mês | Entrou, saiu, fim do mês previsto e quanto dá para gastar por dia (frase pronta). | ✅ 1.0 |
| RF-INI-06 | Avisos | Contas fixas vencidas/vencendo, parcelas de dívida, faturas fechadas (≤ 5 dias) ou vencidas, orçamentos ≥ 80 %. Cada aviso leva à tela certa. | ✅ 1.1 |
| RF-INI-07 | Registro rápido no Início | Campo em pílula com microfone (RF-REG). | ✅ 1.0 |
| RF-INI-08 | Cartões | Por cartão: fatura em foco (status e valor) e limite disponível; toque abre o cartão. | ✅ 1.0 |
| RF-INI-09 | Dívidas, orçamentos, metas | Resumos que só aparecem quando existem; "›" leva à tela completa. | ✅ 1.1 |
| RF-INI-10 | Últimas movimentações | Até 6, sem as futuras; estado vazio ensina o registro rápido. | ✅ 1.0 |
| RF-INI-11 | **Cabeçalho na cor do Cryo** | Faixa no topo com saudação, olho, engrenagem e o saldo (DS-COMP-16). Segue o Material You. | 🟡 1.4 |
| RF-INI-12 | **Atalhos** | Fila de atalhos redondos logo abaixo do cabeçalho: Lançar, Importar, Transferir, Pagar fatura (se houver cartão), Contas fixas, Metas, Dívidas. Cada um abre a tela certa em 1 toque (DS-COMP-15). | 🟡 1.4 |
| RF-INI-13 | **Seções planas** | O resto do Início em seções separadas por linhas finas, cada uma com título e "›" (DS-COMP-14): Este mês, Cartão de crédito, Contas, Dívidas, Orçamentos, Metas e caixinhas, Últimas movimentações. Sem cartões empilhados. | 🟡 1.4 |
| RF-INI-14 | Carteira dentro do Início (4 abas) | Barra com Início, Extrato, Planejar e Análises; no Início, seções Contas, Cartão, Investimentos e Dívidas; patrimônio e seções levam à Carteira completa (5.1). | 🟡 1.4 |
| RF-INI-17 | Investimentos no Início | Seção com o total investido hoje e quanto rendeu; › abre a Carteira completa. Só aparece quando há investimento. | 🟡 1.4 |
| RF-INI-15 | Avisos discretos | Avisos como cartões neutros com ícone colorido (DS-COMP-17), não como faixas laranja/vermelhas cheias. | 🟡 1.4 |
| RF-INI-16 | Sem botão flutuante no Início | "Lançar" passa a ser o 1º atalho (o botão flutuante cobria conteúdo). | 🟡 1.4 |

Testes: `ScreensTest` (imagens 01–04), `AppFlowTest`, `DevModeFlowTest`.

### 6.2 Registro rápido por frase ou voz — RF-REG

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-REG-01 | Entender valor | "32,50", "32.50", "R$ 32", "1.200", "1200 reais". | ✅ 1.0 |
| RF-REG-02 | Entender categoria | Pelas palavras-chave de cada categoria e pelo que a pessoa já lançou. | ✅ 1.0 |
| RF-REG-03 | Entender conta ou cartão | "no débito", "no crédito", "no cartão", nome da conta ou do cartão; senão a conta principal. | ✅ 1.0 |
| RF-REG-04 | Entender data | "hoje", "ontem", "anteontem", "amanhã", "dia 5", "05/10", "5 de outubro", "segunda" ("segunda passada"). | ✅ 1.0 |
| RF-REG-05 | Entender parcelas | "em 10x" no cartão: uma movimentação por parcela, cada uma na fatura certa (DAD-05). | ✅ 1.0 |
| RF-REG-06 | Entender entradas, transferências e metas | "recebi 3000 de salário" vira receita; "transferi 200 para a poupança" vira transferência; o nome de uma meta guarda nela. | ✅ 1.0 |
| RF-REG-07 | Conferir antes de salvar | Folha com o que foi entendido; dá para corrigir; nada salva sozinho. | ✅ 1.0 |
| RF-REG-08 | Voz | Reconhecimento de fala do próprio celular (`RecognizerIntent`), na língua do app; sem ele, digitar sempre funciona. | ✅ 1.0 |
| RF-REG-09 | **Frases nas 10 línguas** | Valor, data relativa (ontem, dia 5, segunda...), parcelas ("10x", "in 10 installments", "10回払い"...), conta ou cartão, entrada, transferência e categoria pelas palavras-chave da língua do app. Em japonês, mandarim e coreano (sem espaços entre palavras) e em hindi, regras próprias de leitura. | 🟡 1.4 |

Código: `domain/PhraseParser.kt`, `ui/tx/QuickEntry.kt`. Testes: `PhraseParserTest`.

### 6.3 Lançamento completo — RF-LAN (TELA-LANCAR, TELA-LANC-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-LAN-01 | Tipos | Despesa, receita, transferência, pagamento de fatura, aplicar/resgatar investimento, guardar/retirar de meta, dinheiro emprestado (DAD-04). | ✅ 1.0–1.1 |
| RF-LAN-02 | Valor estilo caixa eletrônico | Só números, centavos se ajustam (DS-COMP-10). | ✅ 1.0 |
| RF-LAN-03 | Categoria em grade | Grade de 4 colunas com ícone (DS-COMP-11). | ✅ 1.0 |
| RF-LAN-04 | Conta ou cartão | Chips; no cartão, a fatura é calculada pela data (DAD-05). | ✅ 1.0 |
| RF-LAN-05 | Parcelado | Número de parcelas; apagar pergunta "só esta" ou o grupo todo. | ✅ 1.0 |
| RF-LAN-06 | Data | Hoje, Ontem, Outra data. | ✅ 1.0 |
| RF-LAN-07 | Apagar com desfazer | Snackbar "Desfazer". | ✅ 1.0 |
| RF-LAN-08 | Ligação com dívida | Pagamento de parcela de dívida, dinheiro emprestado que entrou. | ✅ 1.1 |

### 6.4 Extrato — RF-EXT (TELA-EXTRATO)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-EXT-01 | Movimentações do mês | Agrupadas por dia, com cabeçalho "Segunda-feira, 5 de outubro". | ✅ 1.0 |
| RF-EXT-02 | Trocar de mês | Seletor de mês. | ✅ 1.0 |
| RF-EXT-03 | Buscar | Busca em todos os meses. | ✅ 1.0 |
| RF-EXT-04 | Filtrar por categoria | Vindo de Análises ou do orçamento. | ✅ 1.0 |
| RF-EXT-05 | Calendário de gastos | Atalho para o calendário de calor. | ✅ 1.0 |
| RF-EXT-06 | Lançar | Botão "Lançar". | ✅ 1.0 |

### 6.5 Contas e transferências — RF-CON (TELA-CARTEIRA, TELA-CONTA, TELA-CONTA-EDITAR, TELA-ARQUIVADOS)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-CON-01 | Tipos de conta | Corrente, poupança, dinheiro, benefício (VR/VA), outra. | ✅ 1.0 |
| RF-CON-02 | Saldo inicial e atual | Saldo = inicial + movimentações; pode ser negativo (avisa). | ✅ 1.0 |
| RF-CON-03 | Somar no saldo total | Interruptor por conta. | ✅ 1.0 |
| RF-CON-04 | Conta principal | Usada quando a frase não diz a conta. | ✅ 1.0 |
| RF-CON-05 | Transferir | Entre contas, sem contar como gasto nem receita. | ✅ 1.0 |
| RF-CON-06 | Arquivar e restaurar | Conta arquivada some das listas e das escolhas, mas o histórico fica. | ✅ 1.1 |
| RF-CON-07 | Importar desta conta | Botão ↥ na tela da conta abre a importação já com ela (RF-IMP). | ✅ 1.3 |

### 6.6 Cartões e faturas — RF-CAR (TELA-CARTAO, TELA-CARTAO-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-CAR-01 | Cadastro | Nome, limite, dia de fechamento, dia de vencimento, cor, conta usada para pagar. | ✅ 1.0 |
| RF-CAR-02 | Fatura certa | Compra no dia do fechamento ou depois vai para a fatura seguinte (DAD-05). | ✅ 1.0 |
| RF-CAR-03 | Status da fatura | Aberta, fechada, paga, vencida, sem gastos. | ✅ 1.0 |
| RF-CAR-04 | Limite disponível | Limite − o que está em aberto. | ✅ 1.0 |
| RF-CAR-05 | Compras da fatura | Lista por fatura, trocar de fatura. | ✅ 1.0 |
| RF-CAR-06 | Pagar fatura | Pagamento da conta para o cartão (`CARD_PAYMENT`) com o valor da fatura sugerido. | ✅ 1.0 |
| RF-CAR-07 | Estorno | Receita no cartão diminui a fatura. | ✅ 1.0 |
| RF-CAR-08 | Arquivar | Igual a contas. | ✅ 1.1 |
| RF-CAR-09 | Importar fatura | Botão ↥ na tela do cartão (RF-IMP). | ✅ 1.3 |

Testes: `LedgerTest`.

### 6.7 Dívidas — RF-DIV (TELA-DIVIDAS, TELA-DIVIDA, TELA-DIVIDA-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-DIV-01 | Tipos | Empréstimo, financiamento, cartão/cheque especial, acordo, pessoa, outra. | ✅ 1.1 |
| RF-DIV-02 | Com ou sem parcelas | Parcelas por mês com valor; ou prazo para quitar. | ✅ 1.1 |
| RF-DIV-03 | Quanto falta | Falta pagar, já pagou %, livre em mês/ano. | ✅ 1.1 |
| RF-DIV-04 | Próxima parcela e atrasos | Estado: atrasada, vence logo, em dia, paga no mês. | ✅ 1.1 |
| RF-DIV-05 | Pagar | Folha de pagamento; diferença do valor da parcela vira juros/multa ou adiantamento. | ✅ 1.1 |
| RF-DIV-06 | Ajustes | "Peguei mais", "Atualizar saldo"; desfazer ajuste. | ✅ 1.1 |
| RF-DIV-07 | Renda comprometida | Quanto da renda vai para parcelas. | ✅ 1.1 |
| RF-DIV-08 | Quitadas | Arquivo de dívidas quitadas; restaurar. | ✅ 1.1 |
| RF-DIV-09 | Dinheiro emprestado | Entrada `DEBT_IN` não conta como receita (DAD-04). | ✅ 1.1 |

Código: `domain/Debts.kt`. Testes: `DebtsTest`, `DebtFlowTest`.

### 6.8 Contas fixas e lembretes — RF-FIX (TELA-PLANEJAR aba 2, TELA-FIXA-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-FIX-01 | A pagar e a receber | Aluguel, internet, salário; valor aproximado e dia. | ✅ 1.0 |
| RF-FIX-02 | Estado no mês | Paga, vencida, vence logo, a vencer. | ✅ 1.0 |
| RF-FIX-03 | "Paguei" | Marca como paga lançando a movimentação. | ✅ 1.0 |
| RF-FIX-04 | Lembrete | Notificação diária ~9h para o que vence (RF-NOT-01). | ✅ 1.0 |
| RF-FIX-05 | Entra na previsão | Contas fixas não pagas entram no fim do mês previsto. | ✅ 1.0 |

### 6.9 Orçamentos — RF-ORC (TELA-PLANEJAR aba 1)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-ORC-01 | Limite por categoria por mês | Total planejado, restante e quanto por dia. | ✅ 1.0 |
| RF-ORC-02 | Barra de uso | Cor normal, âmbar a partir de 80 %, vermelho a partir de 100 % (DS-COMP-08). | ✅ 1.0 |
| RF-ORC-03 | Sugestões | Pela média de gastos de cada categoria. | ✅ 1.0 |

### 6.10 Metas e caixinhas — RF-MET (TELA-PLANEJAR aba 3, TELA-META-EDITAR, TELA-METAS-ARQUIVO)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-MET-01 | Meta com valor e prazo | Emoji, cor, valor alvo, prazo opcional, valor inicial opcional. | ✅ 1.0 |
| RF-MET-02 | Guardar e retirar | Movimentações `GOAL_IN`/`GOAL_OUT` saindo/entrando numa conta. | ✅ 1.0 |
| RF-MET-03 | Valor mensal sugerido | Quanto guardar por mês para chegar no prazo. | ✅ 1.0 |
| RF-MET-04 | Arquivo | Concluídas e arquivadas vão para o arquivo; restaurar. | ✅ 1.1 |

### 6.11 Investimentos — RF-INV (TELA-INVEST, TELA-INVEST-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-INV-01 | Tipos | Poupança, renda fixa, Tesouro, ações, FIIs, fundos, cripto, previdência, outro. | ✅ 1.0 |
| RF-INV-02 | Aportar e resgatar | `INVEST_IN`/`INVEST_OUT` saindo/entrando numa conta. | ✅ 1.0 |
| RF-INV-03 | Atualizar valor | Registro do valor atual; rendimento = atual − aportado. | ✅ 1.0 |
| RF-INV-04 | Histórico e arquivo | Atualizações com remover; arquivar investimento. | ✅ 1.1 |

### 6.12 Análises — RF-ANA (TELA-ANALISES)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-ANA-01 | Previsão do fim do mês | Linha cheia até hoje e tracejada na projeção; "Como cheguei nesse número". | ✅ 1.0 |
| RF-ANA-02 | Calendário de calor | Um quadrado por dia, 5 níveis de cor. | ✅ 1.0 |
| RF-ANA-03 | Fluxo do dinheiro | De onde veio e para onde foi (Sankey). | ✅ 1.0 |
| RF-ANA-04 | Gastos por categoria | Rosca com legenda; toque abre o extrato filtrado. | ✅ 1.0 |
| RF-ANA-05 | Últimos 6 meses | Barras de entradas e saídas. | ✅ 1.0 |
| RF-ANA-06 | "O Cryo percebeu" | Observações automáticas sobre o mês. | ✅ 1.0 |

Código: `domain/Analytics.kt`, `ui/components/Charts.kt`.

### 6.13 Importação de extratos — RF-IMP (TELA-IMPORTAR, TELA-RELATORIO)

Detalhe completo em [`docs/IMPORTACAO.md`](IMPORTACAO.md).

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-IMP-01 | OFX (SGML e XML) | Identificador do banco (FITID) reconhece o que já veio; linhas de saldo e valor zero ficam de fora. | ✅ 1.3 |
| RF-IMP-02 | CSV | Separador, codificação e colunas descobertos sozinhos; cabeçalho depois do nome do banco; coluna C/D; entradas e saídas separadas. | ✅ 1.3 |
| RF-IMP-03 | Planilha XLSX e ".xls" HTML | Datas e números da planilha. ".xls" binário antigo explica como converter. | ✅ 1.3 |
| RF-IMP-04 | PDF | Pelo texto; sinais pelas marcas das movimentações (não de saldos e totais) ou pelas palavras; senha pedida e não guardada; aparelho sem leitor explica. | ✅ 1.3 |
| RF-IMP-05 | Escolher colunas | Quando não tem certeza, pergunta, com prévia. | ✅ 1.3 |
| RF-IMP-06 | Coluna de parcela | "Parcela 2/3" entra na descrição. | ✅ 1.3 |
| RF-IMP-07 | Valor em reais | Ignora valor em dólar, cotação, nome e final do cartão. | ✅ 1.3 |
| RF-IMP-08 | Datas sem ano | Ano da data completa mais recente do arquivo (DAD-08). | ✅ 1.3 |
| RF-IMP-09 | Conferência | Categoria sugerida; editar descrição/categoria; marcar/desmarcar; totais. | ✅ 1.3 |
| RF-IMP-10 | Já importada | Mesma chave (DAD-09) na mesma conta/cartão; no cartão, só na mesma fatura. Desmarcada. | ✅ 1.3 |
| RF-IMP-11 | Parece repetida | Mesmo valor, até 3 dias, lançada à mão ou vinda de outro extrato/formato. Desmarcada. | ✅ 1.3 |
| RF-IMP-12 | Pagamento de fatura | Na conta: vira pagamento do cartão escolhido. No cartão: crédito "Pagamento recebido" pede a conta. Marcado sem o outro lado: aviso. | ✅ 1.3 |
| RF-IMP-13 | Fatura do cartão | Fatura sugerida (a da compra mais recente) ou pela data de cada compra; sinais virados sozinhos com aviso. | ✅ 1.3 |
| RF-IMP-14 | Desfazer | Desfaz a importação inteira. | ✅ 1.3 |
| RF-IMP-15 | Saldo do extrato | Compara o saldo do arquivo com o do Cryo e explica a diferença. | ✅ 1.3 |
| RF-IMP-16 | Exemplos só no modo de teste | Reconhecidos por descrição **e** valor; recusados com os dados de verdade. | ✅ 1.3 |
| RF-IMP-17 | Limites | Arquivo de até 20 MB; planilha com proteção contra arquivo compactado gigante. | ✅ 1.3 |
| RF-IMP-18 | Relatório anônimo | Letras viram x, números viram 9, formato preservado; nome do arquivo e colunas também. | ✅ 1.3 |
| RF-IMP-19 | Tudo no celular | Nada sai do aparelho (PRI-01). | ✅ 1.3 |
| RF-IMP-20 | Bancos do Hanry | Formatos dos bancos que o Hanry usa testados com relatório anônimo. | 🔵 1.4 |
| RF-IMP-21 | **Extratos de outros países** | Nomes de coluna nas 10 línguas (Date/Amount, Fecha/Importe, Datum/Betrag, 日付/金額...); ordem da data (dia/mês, mês/dia como nos EUA, ano/mês/dia) descoberta pelo arquivo e, se não der, pelo país; símbolo da moeda do app e de outras; valores sem centavos (iene, won). | 🟡 1.4 |

Testes: `StatementImportTest`, `ImportFlowTest`.

### 6.14 Backup — RF-BKP (TELA-AJUSTES "Seus dados", TELA-BACKUP)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-BKP-01 | Salvar uma cópia | Arquivo `.json` com tudo, onde a pessoa escolher (DAD-10). | ✅ 1.0 |
| RF-BKP-02 | Restaurar | Mostra o que tem no arquivo e pede confirmação; backups de todas as versões abrem. | ✅ 1.0 |
| RF-BKP-03 | Exportar para planilha | CSV com `;`. | ✅ 1.0 |
| RF-BKP-04 | Apagar tudo | Confirmação "Apagar tudo?". | ✅ 1.0 |
| RF-BKP-05 | Backup automático em pasta | Substituir sempre ou guardar 5/10/30/todas as cópias. | ✅ 1.2 |
| RF-BKP-06 | Backup automático na nuvem | Um arquivo escolhido pelo seletor do Android (o app da nuvem envia). | ✅ 1.2 |
| RF-BKP-07 | Frequência | Diária, semanal ou mensal; "Fazer backup agora". | ✅ 1.2 |
| RF-BKP-08 | Não sobrescrever o que não é dele | Só substitui/apaga arquivos que este celular gravou; outro backup no lugar: mostra os dois e pergunta (e restaura dali). | ✅ 1.2 |
| RF-BKP-09 | Proteções | Pausa e avisa se outro aparelho mexeu no arquivo ou se muitas movimentações sumiram; não grava cópia igual. | ✅ 1.2 |
| RF-BKP-10 | Notificação de problema | Avisa quando um destino passa a falhar (sem repetir). | ✅ 1.2 |
| RF-BKP-11 | Modo de teste separado | Cópia do modo de teste leva `testMode` e não restaura por cima dos dados de verdade; o backup automático não roda no modo de teste. | ✅ 1.3 |

Testes: `AutoBackupTest`, `BackupFlowTest`, `RepositoryTest`.

### 6.15 Bloqueio e privacidade — RF-BLQ (TELA-BLOQUEIO)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-BLQ-01 | Bloquear com digital, rosto ou senha do celular | Ligável em Ajustes; exige trava no aparelho. | ✅ 1.0 |
| RF-BLQ-02 | Quando bloqueia | Ao abrir do zero, depois de 1 min em segundo plano e quando o Android fechou o app e restaura a tela. Girar a tela não bloqueia. | ✅ 1.3 |
| RF-BLQ-03 | Nada do app à vista | Bloqueado, o app sai da tela (nem diálogos, menus ou folhas; nem Voltar ou teclado chegam a ele). | ✅ 1.3 |
| RF-BLQ-04 | Volta para onde estava | O estado é guardado e volta ao desbloquear. | ✅ 1.3 |
| RF-BLQ-05 | Recentes | Com o bloqueio ligado, a miniatura não guarda a tela (Android 13+). | ✅ 1.3 |
| RF-BLQ-06 | Não prender a pessoa fora | Sem trava no aparelho ou sem biometria, libera. | ✅ 1.0 |

Testes: `DevModeFlowTest` (bloqueio).

### 6.16 Ajustes e categorias — RF-AJU (TELA-AJUSTES, TELA-CATEGORIAS, TELA-CATEGORIA-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-AJU-01 | Perfil | Nome (como quer ser chamado). | ✅ 1.0 |
| RF-AJU-02 | Aparência | Tema claro/escuro/do sistema; cores do papel de parede (Material You). | ✅ 1.0 |
| RF-AJU-03 | Privacidade | Bloqueio, esconder valores. | ✅ 1.0 |
| RF-AJU-04 | Lembretes | Ligar/desligar avisos de contas. | ✅ 1.0 |
| RF-AJU-05 | Registro rápido | Conta principal. | ✅ 1.0 |
| RF-AJU-06 | Categorias | Despesas e receitas: nome, ícone, cor, palavras-chave do registro rápido; arquivar. | ✅ 1.0 |
| RF-AJU-07 | Sobre | Versão, código-fonte, licença AGPL v3 ou posterior, aviso de garantia. | ✅ 1.0 · licença 🟡 1.4 |
| RF-AJU-08 | Modo desenvolvedor | 7 toques na versão (RF-DEV). | ✅ 1.3 |
| RF-AJU-09 | Idioma | Ajustes › Idioma: "Do celular" (padrão) ou uma das 10 línguas; muda na hora (RF-LNG-02). | 🟡 1.4 |
| RF-AJU-10 | Moeda | Ajustes › Moeda: lista de moedas; a do país do celular vem marcada (RF-MOE-02). | 🟡 1.4 |

### 6.17 Primeiro uso — RF-BOA (TELA-BOAS-VINDAS)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-BOA-01 | Boas-vindas | O que o Cryo faz e que os dados ficam no celular. | ✅ 1.0 |
| RF-BOA-02 | Nome | Opcional. | ✅ 1.0 |
| RF-BOA-03 | Conta principal | Nome e saldo de hoje (pode ser negativo). | ✅ 1.0 |
| RF-BOA-04 | Dinheiro vivo e cartão | Opcionais. | ✅ 1.0 |
| RF-BOA-05 | Notificações | Pedir permissão (Android 13+). | ✅ 1.0 |
| RF-BOA-06 | Restaurar no primeiro uso | Na primeira tela da apresentação, "Já uso o Cryo: restaurar um backup" abre o arquivo, mostra o que tem nele e restaura, pulando o resto da apresentação. | 🟡 1.4 |
| RF-BOA-07 | Língua e moeda no primeiro uso | A apresentação mostra a língua e a moeda escolhidas pelo celular e deixa trocar ali mesmo (RF-LNG-02, RF-MOE-02). | 🟡 1.4 |

### 6.18 Modo desenvolvedor e modo de teste — RF-DEV (TELA-DEV, TELA-RELATORIO)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-DEV-01 | Ligar | 7 toques na versão; contagem regressiva a partir do 3º. | ✅ 1.3 |
| RF-DEV-02 | Modo de teste | Banco `cryo-teste.db` e ajustes separados, com 4 meses de exemplo; faixa "Modo de teste" em todas as telas. | ✅ 1.3 |
| RF-DEV-03 | Dados de verdade intocados | Lembretes e backup sempre nos dados de verdade; aviso de conta/dívida sai do modo de teste. | ✅ 1.3 |
| RF-DEV-04 | Arquivos de exemplo | Todos os formatos; salvar numa pasta; abrir direto na importação. | ✅ 1.3 |
| RF-DEV-05 | Relatório da leitura | Completo e anônimo; copiar e compartilhar o completo pedem confirmação. | ✅ 1.3 |

### 6.19a Línguas — RF-LNG

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-LNG-01 | **10 línguas** | Português do Brasil (base), inglês, espanhol, coreano, italiano, alemão, hindi, mandarim (chinês simplificado), japonês e francês. | 🟡 1.4 |
| RF-LNG-02 | Qual língua | Segue a língua do celular; se ela não for uma das 10, inglês. Ajustes › Idioma troca na hora, sem fechar o app. No Android 13+, também pela tela de idioma do app do sistema. | 🟡 1.4 |
| RF-LNG-03 | Nenhum texto no código | Todo texto que aparece na tela, em notificações e em mensagens está em `res/values*/strings.xml` (base em `values/`, pt-BR). | 🟡 1.4 |
| RF-LNG-04 | Tradução completa | Toda chave existe nas 10 línguas, com os mesmos marcadores (`%1$s`...) e plurais pelas regras de cada língua. | 🟡 1.4 |
| RF-LNG-05 | Datas do país | Nomes de dias e meses e a ordem da data no jeito da língua e do país (5 de outubro · October 5 · 10月5日...). | 🟡 1.4 |
| RF-LNG-06 | Categorias na língua de quem instala | As categorias padrão (nomes e palavras-chave do registro rápido) são criadas na língua do app na primeira vez; as de quem já usa não mudam. | 🟡 1.4 |
| RF-LNG-07 | Loja nas 10 línguas | Título, descrições e novidades do F-Droid (`fastlane/metadata/android/<língua>/`). | 🟡 1.4 |
| RF-LNG-08 | Revisão por quem fala | Cada tradução revista por alguém que fala a língua (comunidade, por issue). | 🔵 contínuo |

### 6.19b Moeda — RF-MOE

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-MOE-01 | **Uma moeda para o app todo** | Todos os valores na moeda escolhida (decisão em ESC-04). | 🟡 1.4 |
| RF-MOE-02 | Qual moeda | Padrão: a do país do celular (en-US → dólar, en-GB → libra, es-MX → peso mexicano, es-ES → euro, ko → won, hi → rúpia, zh → yuan, ja → iene...); se o país não tiver uma moeda da lista, a do país principal da língua. Ajustes › Moeda troca. | 🟡 1.4 |
| RF-MOE-03 | Trocar não converte | Trocar a moeda muda só o símbolo e o formato: os números continuam os mesmos (sem internet não há cotação). Um aviso explica isso antes de trocar. | 🟡 1.4 |
| RF-MOE-04 | Formato do país | Símbolo, posição do símbolo, separadores e casas decimais pela língua e pela moeda (R$ 1.234,56 · $1,234.56 · 1.234,56 € · ¥1,235 · ₩1,235 · ₹1,234.56). Iene, won e outras sem centavos aparecem sem casas. | 🟡 1.4 |
| RF-MOE-05 | Digitar valor | O campo estilo caixa eletrônico respeita as casas da moeda (no iene, cada dígito é 1 iene). | 🟡 1.4 |
| RF-MOE-06 | Valores resumidos | Nos gráficos, no jeito de cada língua: R$ 1,2 mil · $1.2K · 1,2 Tsd. € · ¥1.2万 · ₹1.2 लाख. | 🟡 1.4 |
| RF-MOE-07 | Lista de moedas | Real, dólar americano, euro, libra, dólar canadense, dólar australiano, peso mexicano, peso argentino, peso chileno, peso colombiano, sol peruano, peso uruguaio, franco suíço, won, rúpia indiana, yuan, iene, novo dólar taiwanês, dólar de Hong Kong, dólar de Singapura. | 🟡 1.4 |

### 6.19 Notificações — RF-NOT

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-NOT-01 | Contas e parcelas | Diário ~9h; toque abre a tela certa nos dados de verdade. | ✅ 1.0 |
| RF-NOT-02 | Problema no backup | RF-BKP-10. | ✅ 1.2 |
| RF-NOT-03 | Pouca informação | A notificação mostra só o nome e o valor da conta ou parcela, nunca saldos; agendar lembretes nunca impede o app de abrir. | ✅ 1.0 |
| RF-NOT-04 | Notificações especiais e sons | Popups e sons personalizados aleatórios. | 🔵 1.5 (detalhar) |

### 6.20 Widgets — RF-WID

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-WID-01 | Widgets na tela inicial | Detalhar com o Hanry (quais, que dados, como respeitar "esconder valores" e o bloqueio). | 🔵 1.5 |

---

## 7. Requisitos não funcionais (RNF)

| ID | Requisito | Como se confere | Status |
|---|---|---|---|
| RNF-PRIV-01 | **Sem permissão de INTERNET.** Permissões declaradas: só `POST_NOTIFICATIONS` e `USE_BIOMETRIC` (as demais vêm das bibliotecas de biometria e de agendamento). | `PrdConformanceTest` | ✅ |
| RNF-PRIV-02 | Sem bibliotecas de analytics, anúncios ou crash reporting. | Revisão de dependências (PRC-07) | ✅ |
| RNF-PRIV-03 | `android:allowBackup="false"`; transferência entre aparelhos sem permissões do backup e sem o modo de teste. | `PrdConformanceTest` + revisão | ✅ |
| RNF-PRIV-04 | Nunca colocar extratos reais no repositório (nem em testes). | `PrdConformanceTest` (sem `.ofx/.csv/.xlsx/.pdf` versionados) | ✅ |
| RNF-SEG-01 | Chave de assinatura nunca no git (`keystore/` no `.gitignore`). | `PrdConformanceTest` | ✅ |
| RNF-SEG-02 | Bloqueio conforme RF-BLQ. | `DevModeFlowTest` | ✅ |
| RNF-REP-01 | **Compilação reproduzível**: duas compilações do mesmo commit dão o mesmo APK; JDK 21; sem `vcsInfo`, `dependenciesInfo` e ART profile. | Compilar duas vezes de clones limpos (PRC-06) | ✅ |
| RNF-LIC-01 | **Licença AGPL-3.0-or-later**: `LICENSE` com o texto oficial; cabeçalho SPDX em todo `.kt`. | `PrdConformanceTest` | 🟡 1.4 |
| RNF-COMP-01 | Android 8 (API 26) em diante; alvo API 36. | `app/build.gradle.kts` | ✅ |
| RNF-COMP-02 | Funciona sem serviços do Google. | Revisão de dependências | ✅ |
| RNF-DES-01 | O app abre mesmo se lembretes ou backup falharem (`runCatching`). | Revisão | ✅ |
| RNF-DES-02 | Listas longas sem travar (LazyColumn); importação de milhares de linhas fora da linha principal. | Testes de importação | ✅ |
| RNF-ACS-01 | Acessibilidade conforme ACS-01..07. | Revisão + testes de tela | ✅ |
| RNF-IDI-01 | Interface nas 10 línguas e na moeda escolhida (RF-LNG, RF-MOE); até a 1.3, só português e reais. | `PrdConformanceTest` (traduções completas, sem texto no código) | 🟡 1.4 |
| RNF-TAM-01 | APK pequeno: sem bibliotecas pesadas (PDF pelo leitor do Android). | Tamanho do APK no PRC-06 (~3 MB na 1.3) | ✅ |

## 8. Regras de dados (DAD)

| ID | Regra |
|---|---|
| DAD-01 | Dinheiro sempre em **centésimos da unidade da moeda (`Long`)**, em qualquer moeda; nunca `Double` para guardar valor. Em moedas sem centavos (iene, won), os valores são múltiplos de 100 e aparecem sem casas. Trocar a moeda não muda os números guardados (RF-MOE-03). |
| DAD-02 | Datas em `LocalDate`; mês de fatura como `Int` `AAAAMM` (`YearMonth.key()`). |
| DAD-03 | Mudança no banco = nova versão do Room, migração e teste em `MigrationTest`; schema exportado em `app/schemas/` (conferido por `PrdConformanceTest`). Banco atual: versão 3. |
| DAD-04 | Tipos de movimentação: `EXPENSE`, `INCOME` (no cartão = estorno), `TRANSFER`, `CARD_PAYMENT` (com `invoiceYm`), `INVEST_IN`/`INVEST_OUT`, `GOAL_IN`/`GOAL_OUT`, `DEBT_IN` (não é receita). |
| DAD-05 | Cartão: cada compra guarda `invoiceYm`; compra no dia do fechamento ou depois vai para a fatura seguinte (`CardMath.invoiceFor`); parcelado = uma movimentação por parcela, ligadas por `installmentGroup`. |
| DAD-06 | Todas as contas (saldo, fatura, previsão, dívidas) saem do `Ledger`, nunca de consultas soltas. |
| DAD-07 | Dados de verdade (`CryoApp.real`) e de teste nunca se misturam (RF-DEV-03, RF-BKP-11). |
| DAD-08 | Datas sem ano: ano da data completa mais recente do arquivo, voltando um ano se cairiam mais de 31 dias depois. |
| DAD-09 | Chave de importação: `a<conta>|<formato>.id:<FITID>` ou `c<cartão>|<formato>.h:<hash>`, `#n` para linhas repetidas. |
| DAD-10 | Backup JSON `{"app":"Cryo","version":2,...}`; backups de todas as versões abrem (`Backup.import`); campo novo só aparece quando usado. |

---

## 9. Design system (DS)

O detalhe visual (paletas completas, imagens das telas) fica em [`docs/VISUAL.md`](VISUAL.md). Aqui ficam as regras que todo
código segue.

### 9.1 Direção visual 2.0 (1.4): organizado como app de banco, com as cores do Cryo

Referência: a organização do Nubank (cabeçalho colorido, atalhos redondos, seções planas separadas por linhas finas, muito
espaço em branco, números grandes). **As cores são as do Cryo** (azul-gelo `#00677F`, gradiente da marca, Material You), nunca
roxo.

| ID | Regra | Status |
|---|---|---|
| DS-DIR-01 | **Seções, não blocos**: conteúdo em seções planas separadas por uma linha fina; cartões (fundo próprio) só para o que é destaque ou aviso. | 🟡 1.4 |
| DS-DIR-02 | **Cor com propósito**: a cor do Cryo no cabeçalho do Início, nos atalhos e nos destaques; verde e vermelho só para entrada, saída e alerta. | 🟡 1.4 |
| DS-DIR-03 | **Atalhos à mão**: as ações mais usadas em círculos no topo do Início. | 🟡 1.4 |
| DS-DIR-04 | **Uma pergunta por seção**: cada seção responde uma pergunta ("Como está o mês?") e o "›" leva ao detalhe. | 🟡 1.4 |
| DS-DIR-05 | Todas as telas na direção 2.0 (piloto do Início aprovado em 04/11/2026). | 🟡 1.4 |

### 9.2 Cores (DS-COR)

| ID | Regra |
|---|---|
| DS-COR-01 | Três fontes de cor: papel de parede (Material You, padrão no Android 12+), paleta "gelo" (`ui/theme/Theme.kt`) e cores de significado (`CryoTheme.colors`). Tabelas em VISUAL.md §2. |
| DS-COR-02 | **Nunca cor fixa (`Color(0xFF…)`) numa tela**: sempre `MaterialTheme.colorScheme.*` ou `CryoTheme.colors.*`. Exceção: cores que a pessoa escolhe (paleta de 20 em `DefaultData.Palette`). Conferido por `PrdConformanceTest`. |
| DS-COR-03 | Entrada verde com `+`; saída em listas na cor do texto com `−`, vermelha só nos resumos e quando estoura; nunca só a cor (sempre sinal, ícone ou texto). |
| DS-COR-04 | Cabeçalho do Início: claro → `primary`/`onPrimary`; escuro → `primaryContainer`/`onPrimaryContainer`. |
| DS-COR-05 | Modo de teste: `tertiaryContainer` + ícone de frasco, em qualquer lugar onde apareça. |

### 9.3 Letras (DS-TIPO)

| ID | Regra |
|---|---|
| DS-TIPO-01 | Fonte do sistema (Roboto); SemiBold em títulos; números tabulares (`tnum`). |
| DS-TIPO-02 | Saldo do Início e valor sendo digitado: `displaySmall`. Título de seção: `titleMedium`. Linha de lista: `bodyLarge` + subtítulo `bodySmall` em `onSurfaceVariant`. Tabela completa em VISUAL.md §3. |
| DS-TIPO-03 | Valores sempre com `MoneyText` (uma linha, auto-ajuste, "esconder valores"). |

### 9.4 Formas e espaços (DS-FORMA, DS-ESP)

| ID | Regra |
|---|---|
| DS-FORMA-01 | Cantos: 8 · 12 · **16** (avisos, campos) · **24** (cartões) · 32. Círculos nos atalhos e ícones. |
| DS-ESP-01 | Espaços múltiplos de 4. Margem lateral 16 dp em listas, **20 dp** em seções, formulários e telas de passo. |
| DS-ESP-02 | Botão principal 56 dp, largura total; secundário 52 dp; tudo clicável com pelo menos 48 dp. |

### 9.5 Componentes (DS-COMP)

Estão em `ui/components/`. **Antes de criar um componente, procure aqui**; componente novo = linha nova nesta tabela.

| ID | Componente | Para quê | Status |
|---|---|---|---|
| DS-COMP-01 | `CryoCard` | Destaque com fundo próprio (cantos 24). Na 2.0, só para destaques e avisos. | ✅ |
| DS-COMP-02 | `IconBadge` | Ícone de categoria/conta/cartão em círculo 40 dp com a cor da paleta. | ✅ |
| DS-COMP-03 | `MoneyText` | Qualquer valor em reais. | ✅ |
| DS-COMP-04 | `TxRow` | Linha de movimentação. | ✅ |
| DS-COMP-05 | `SectionTitle` | Título de seção com ação. | ✅ |
| DS-COMP-06 | `Banner` | Aviso, erro ou dica dentro de telas de passo. | ✅ |
| DS-COMP-07 | `Pill` | Status curto ("Paga", "Atrasada"). | ✅ |
| DS-COMP-08 | `UsageBar` | Uso de orçamento/limite/progresso. | ✅ |
| DS-COMP-09 | `EmptyState` | Lista vazia com ação. | ✅ |
| DS-COMP-10 | `AmountField` | Digitar valor (estilo caixa eletrônico). | ✅ |
| DS-COMP-11 | `CategoryGrid` | Escolher categoria. | ✅ |
| DS-COMP-12 | `CryoTopBar`, `ConfirmDialog`, `SwitchRow`, `ChoiceRow`, `DateChooser`, `Segmented`, `Stepper`, `SourceChips`, `FieldLabel`, choosers de cor/ícone/emoji | Peças de formulário e navegação. | ✅ |
| DS-COMP-13 | Gráficos (`Charts.kt`) | Previsão, calendário, fluxo, rosca, barras; desenhados à mão, sem biblioteca. | ✅ |
| DS-COMP-14 | **`CryoSection`** | Seção plana: título `titleMedium` + "›" (a seção inteira é tocável quando leva a outra tela), conteúdo, linha fina embaixo. Margem 20 dp. | 🟡 1.4 |
| DS-COMP-15 | **`Shortcuts` / `Shortcut`** | Atalho redondo: círculo 64 dp `surfaceContainerHigh` com ícone 24 dp e rótulo `labelMedium` embaixo (até 2 linhas); fila com rolagem lateral. | 🟡 1.4 |
| DS-COMP-16 | **`HomeHeader`** | Cabeçalho colorido do Início (DS-COR-04): saudação, olho, engrenagem, saldo em `displaySmall` e patrimônio. | 🟡 1.4 |
| DS-COMP-17 | **`InfoCard`** | Aviso discreto: fundo `surfaceContainerLow`, cantos 16, ícone colorido pelo tipo (vermelho atrasado, âmbar vencendo), texto e "›". | 🟡 1.4 |

### 9.6 Padrões de tela (DS-PAD)

| ID | Padrão |
|---|---|
| DS-PAD-01 | **Tela de aba**: título grande ou cabeçalho; conteúdo rolando; barra de abas. Início: DS-COMP-16 + DS-COMP-15 + seções DS-COMP-14. |
| DS-PAD-02 | **Tela interna**: `CryoTopBar` com voltar; ação principal fixa embaixo (`Surface` com `tonalElevation = 3.dp` + botão 56 dp, `navigationBarsPadding()` e `imePadding()`). |
| DS-PAD-03 | **Edição rápida**: `ModalBottomSheet` aberto por inteiro, título `titleLarge`, botão principal e "Cancelar" contornado. |
| DS-PAD-04 | **Passo a passo**: círculo 64 dp com ícone, título `headlineSmall`, texto, cartões, botão fixo. |
| DS-PAD-05 | **Confirmação**: título em pergunta, consequência, botão com o verbo. |
| DS-PAD-06 | **De ponta a ponta**: barras do sistema transparentes; `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))`. |
| DS-PAD-07 | **Toda tela nova tem imagem** em teste (`ScreensTest` ou o teste do fluxo), em claro, escuro e Material You quando for tela de aba. |

### 9.7 Ícones e marca (DS-ICO)

| ID | Regra |
|---|---|
| DS-ICO-01 | Material Icons estilo **Rounded** (`Icons.Rounded.*`; setas `Icons.AutoMirrored.Rounded.*`). |
| DS-ICO-02 | Ícone do app: gradiente da marca `#0A4F66 → #29B6D6`, hexágono aberto com estrela; versão monocromática. |

## 10. Conteúdo e acessibilidade

### 10.1 Textos (TXT)

| ID | Regra |
|---|---|
| TXT-01 | Português do Brasil, "você", frases curtas, palavras do dia a dia ("conta fixa", não "recorrência"). Sem termos técnicos fora do modo desenvolvedor. |
| TXT-02 | Botão = verbo ("Importar 11 movimentações", "Salvar"). |
| TXT-03 | Erro = o que aconteceu + o que fazer. |
| TXT-04 | Aviso explica a consequência. |
| TXT-05 | Confirmação: título pergunta, texto diz o que acontece, botão repete o verbo. |
| TXT-06 | Plural certo ("1 movimentação" / "2 movimentações"). |
| TXT-07 | Dinheiro no formato da língua e da moeda (RF-MOE-04); com sinal, `+` e `−` (menos de verdade) antes do valor, com espaço que não quebra; resumido (RF-MOE-06); escondido com o símbolo da moeda e `••••`. Tudo em `domain/Format.kt`. Exemplo em pt-BR: `R$ 1.234,56`, `− R$ 10,00`, `R$ 1,2 mil`, `R$ ••••`. |
| TXT-08 | Datas no jeito da língua (RF-LNG-05). Em pt-BR: "Segunda-feira, 5 de outubro", "05/10", "05/10/2025", "vence em 5 dias", "hoje", "amanhã". |
| TXT-09 | Tradução: a base é `values/strings.xml` (pt-BR). Mesmo tom em todas as línguas (próximo, simples, sem jargão; tratamento informal onde é o costume: "tú", "du", "tu"; formal-cortês no japonês, coreano e hindi). Nome "Cryo" e slogan traduzidos pelo sentido. |

### 10.2 Acessibilidade (ACS)

| ID | Regra |
|---|---|
| ACS-01 | Todo botão só com ícone tem `contentDescription`. |
| ACS-02 | Títulos de seção marcados como cabeçalho (`semantics { heading() }`). |
| ACS-03 | Interruptores e caixas: linha inteira tocável e estado descrito; caixa separada da linha tem descrição própria. |
| ACS-04 | Valor escondido é lido como "Valor escondido". |
| ACS-05 | Contraste das cores de significado no claro e no escuro; nunca só a cor. |
| ACS-06 | Textos crescem com a fonte do sistema; valores com auto-ajuste. |
| ACS-07 | Alvo de toque mínimo 48 dp. |

---

## 11. Processo de criação e manutenção

### 11.1 Ciclo de vida de uma mudança (PRC-01)

Toda mudança, de um texto a uma tela nova, passa por estes passos, nesta ordem:

```
1 Pedido ──▶ 2 PRD ──▶ 3 Aprovação ──▶ 4 Código ──▶ 5 Testes ──▶ 6 Revisão ──▶ 7 APK ──▶ 8 Teste do Hanry ──▶ 9 Publicação ──▶ 10 Fechamento
```

| Passo | O que acontece | Quem | Pronto quando |
|---|---|---|---|
| 1. Pedido | Ideia, problema ou relato (Hanry ou issue no GitHub). | Hanry / qualquer pessoa | Descrito em uma frase. |
| 2. PRD | Requisito novo (ID novo) ou alterado, com critério de aceite e status 🔵. Mudança visual: regra DS. | Claude | PRD atualizado no mesmo branch. |
| 3. Aprovação | O Hanry aprova o requisito (e, se visual, a imagem). | Hanry | Status passa a 🟡 com a versão. |
| 4. Código | Implementação seguindo o PRD; comentários em português explicando o porquê; cita o ID quando a regra não é óbvia. | Claude | Compila. |
| 5. Testes | Teste para cada critério de aceite que dá para testar; tela nova com imagem (DS-PAD-07); correção de defeito com teste que falha no código anterior. | Claude | `./gradlew testDebugUnitTest` passa, incluindo `PrdConformanceTest`. |
| 6. Revisão | Releitura do diff contra o PRD e a checklist PRC-04; para versões, revisão independente. | Claude | Nenhum achado aberto. |
| 7. APK | De clone limpo, assinado, conferido (PRC-06). | Claude | SHA-256 do certificado confere. |
| 8. Teste do Hanry | Instala por cima (com backup antes) e testa. | Hanry | "Pode publicar". |
| 9. Publicação | PRC-05. | Claude, com o "pode" do Hanry | Release no GitHub; F-Droid pega sozinho. |
| 10. Fechamento | Status ✅ com a versão; CHANGELOG; histórico do PRD; CLAUDE.md "Onde estamos". | Claude | Tudo atualizado. |

### 11.2 Rastreabilidade (PRC-02)

- **Commit**: primeira linha `<versão>: <resumo>`; no corpo, `PRD: RF-IMP-07, DS-COMP-14` com os IDs tocados.
- **Código**: comentário cita o ID quando a regra vem do PRD e não é óbvia (ex.: `// RF-BLQ-03: bloqueado, o app sai da tela`).
- **Teste**: KDoc ou comentário cita o ID que o teste garante.
- **PRD → código**: cada módulo lista arquivos e testes; caminhos citados existem (conferido por `PrdConformanceTest`).

### 11.3 Versões e branches (PRC-03)

| Item | Regra |
|---|---|
| Número | `X.Y` (`versionName`); `versionCode` +1 a cada versão publicada. Correção urgente de versão publicada: `X.Y.Z` com `versionCode` novo. |
| Branches | `main` = última publicada. Trabalho em `versao-X.Y`. Correção na versão anterior ainda não publicada: corrige no branch dela e leva para o da seguinte. |
| Tags | Só pela Release do GitHub, `vX.Y`, depois do "pode publicar". |
| Push | **Só com aprovação explícita do Hanry** (inclusive de branch). |
| Identidade | `Hanry Franco <102742894+H3yK0@users.noreply.github.com>`. |

### 11.4 Definição de pronto (PRC-04)

Uma mudança só está pronta quando **tudo** abaixo é verdade:

- [ ] Requisito no PRD com ID, critério de aceite e status certo.
- [ ] Código segue o PRD e as regras DS, TXT e ACS.
- [ ] Testes para os critérios de aceite; imagem de tela nova; teste que falha no código anterior quando é correção.
- [ ] `./gradlew testDebugUnitTest` passando (com `PrdConformanceTest`).
- [ ] Sem permissão nova, sem biblioteca nova sem decisão registrada (PRC-07).
- [ ] Banco mudou? Migração + `MigrationTest` + schema exportado + backup antigo abrindo.
- [ ] Textos revistos (plural, verbo no botão, sem jargão).
- [ ] CHANGELOG e, em versão nova, `fastlane/.../changelogs/<versionCode>.txt` (≤ 500 caracteres, pt-BR e en-US).
- [ ] VISUAL.md / IMPORTACAO.md / CLAUDE.md atualizados quando o assunto é deles.

### 11.5 Publicar uma versão (PRC-05)

1. Subir `versionCode`/`versionName`; CHANGELOG; changelogs do fastlane.
2. Testes passando; commit com a identidade pública.
3. APK de clone limpo (PRC-06) e entregar ao Hanry.
4. Com o "pode publicar": merge em `main`, push, Release `vX.Y` com o arquivo **exatamente** `Cryo-X.Y.apk`.
5. F-Droid: a receita (`docs/fdroid/io.github.h3yk0.cryo.yml`) acha a tag sozinha. **Mudou a receita** (ex.: licença na 1.4)?
   Abrir merge request no fdroiddata com a mudança.
6. Imagens da loja (`fastlane/`) geradas de novo quando o visual muda (`StoreAssetsTest -PstoreAssets=true`).
7. Fechamento (PRC-01, passo 10).

### 11.6 APK de teste e verificação (PRC-06)

1. Clone limpo do commit exato; copiar a pasta `keystore/` (fora do git); `./gradlew assembleRelease --no-daemon`.
2. `apksigner verify --print-certs`: SHA-256 `6e13e284c2f00b72473589391776ed2f28dbad5506f515de3fac99a364a3a2e7`.
3. `aapt2 dump badging`: versão certa, **sem `android.permission.INTERNET`**.
4. Reprodutibilidade: segunda compilação de outro clone limpo, **sem cache** (`--no-build-cache --rerun-tasks`), com o mesmo
   SHA-256 de arquivo.
5. Apagar as cópias da chave usadas na compilação.

### 11.7 Dependências e segurança (PRC-07)

- Biblioteca nova só com decisão registrada no CLAUDE.md (seção 13): por quê, tamanho, licença compatível com AGPL, sem rede.
- Atualizar dependências uma vez por versão, com todos os testes e a reprodutibilidade conferidos.
- Achado de segurança (bloqueio, privacidade, dados): severidade S1 (veja 11.8), corrige antes de qualquer outra coisa.

### 11.8 Defeitos (PRC-08)

| Severidade | Exemplo | Prazo |
|---|---|---|
| S1 | Perde dados, valor errado no saldo, dado à vista com o app bloqueado, permissão indevida | Antes de qualquer publicação; correção urgente `X.Y.Z` se já publicado. |
| S2 | Função principal não funciona (importar, lançar, backup) | Na próxima versão. |
| S3 | Texto errado, visual fora do DS, acessibilidade | Na próxima versão, se der. |
| S4 | Melhoria pequena | Entra no roadmap. |

Todo defeito corrigido ganha um teste que falha no código anterior e, se for regra nova, um ID no PRD.

### 11.9 Conformidade automática (PRC-09)

`app/src/test/java/io/github/h3yk0/cryo/PrdConformanceTest.kt` confere, a cada `testDebugUnitTest`, as regras que dá para
conferir sozinho:

| Confere | Regra |
|---|---|
| Todo `.kt` começa com `// SPDX-License-Identifier: AGPL-3.0-or-later`; `LICENSE` é a AGPL v3 | RNF-LIC-01 |
| Manifesto sem `INTERNET`; só `POST_NOTIFICATIONS` e `USE_BIOMETRIC` declaradas; `allowBackup="false"` | RNF-PRIV-01, RNF-PRIV-03 |
| Nenhum extrato (`.ofx`, `.csv`, `.xlsx`, `.xls`, `.pdf`) nem chave (`.jks`, `.keystore`) versionado; `keystore/` no `.gitignore` | RNF-PRIV-04, RNF-SEG-01 |
| Nenhuma cor fixa (`Color(0x…)`) em telas e componentes | DS-COR-02 |
| Toda rota de `Routes` está no catálogo de telas (5.2) | PRC-02 |
| Toda chave de texto existe nas 10 línguas, com os mesmos marcadores; nenhum `Text("...")` com texto escrito no código nas telas | RF-LNG-03, RF-LNG-04 |
| As línguas dos `values-*` são as 10 do RF-LNG-01, iguais às da loja (fastlane) | RF-LNG-01, RF-LNG-07 |
| Todo caminho de arquivo citado no PRD existe; IDs do PRD não se repetem | PRC-02 |
| Existe o schema do Room da versão atual do banco | DAD-03 |
| `CHANGELOG.md` tem a seção da versão atual; changelogs do fastlane com no máximo 500 caracteres | PRC-04 |

Regra nova que dá para conferir sozinha **entra nesse teste**.

### 11.10 Papéis (PRC-10)

| Papel | Quem | Faz |
|---|---|---|
| Dono do produto | Hanry | Decide o que entra, aprova requisitos, imagens, APK e publicação. |
| Desenvolvimento e revisão | Claude Code | Mantém o PRD, implementa, testa, revisa, gera o APK, publica com aprovação. |
| Comunidade | Quem usa | Issues e relatórios anônimos de importação. |

---

## 12. Roadmap

| Versão | Situação | Conteúdo |
|---|---|---|
| 1.0 | ✅ 01/10/2026 | Registro por frase e voz, visão geral, previsão, calendário, fluxo, cartões e parcelas, contas fixas, orçamentos, metas, investimentos, bloqueio, backup em arquivo. |
| 1.1 | ✅ 02/10/2026 | Dívidas, arquivo, compilação reproduzível. |
| 1.2 | ✅ 03/10/2026 | Backup automático (pasta e nuvem). |
| 1.3 | Pronta, aguardando teste do Hanry | Importar extrato, modo desenvolvedor, correções de bloqueio. |
| **1.4** | 🟡 Em preparação | **AGPL** (RNF-LIC-01) · **PRD e conformidade** (PRC-01..10) · **Direção visual 2.0** em todas as telas (DS-DIR-01..05, RF-INI-11..17) · **Mais fácil**: 4 abas (RF-INI-14), atalhos, restaurar na apresentação (RF-BOA-06) · **10 línguas** (RF-LNG) e **moeda do país** (RF-MOE), frases nas 10 línguas (RF-REG-09), extratos de outros países (RF-IMP-21) · Bancos do Hanry (RF-IMP-20). |
| 1.5 | 🔵 | Widgets (RF-WID-01), notificações especiais e sons (RF-NOT-04). |

## 13. Glossário

| Termo | Significado |
|---|---|
| Fatura em foco | A fatura que importa agora: a fechada ainda não paga ou, se não houver, a aberta. |
| Patrimônio | Contas + investimentos + caixinhas − cartões − dívidas. |
| Caixinha / meta | Dinheiro separado para um objetivo, que sai de uma conta. |
| Modo de teste | Banco de dados separado com dados de exemplo, para testar sem mexer nos dados de verdade. |
| Compilação reproduzível | Qualquer pessoa compila o mesmo código e chega ao mesmo APK, byte a byte. |
| AGPL | GNU Affero General Public License: licença livre; quem distribuir versões modificadas precisa liberar o código. |
| Moeda do app | A única moeda em que o Cryo mostra todos os valores (RF-MOE-01). |
