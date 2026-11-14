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
| Versão do PRD | **3.7** (12/11/2026) |
| Próxima versão do app | **1.5** (`versionCode` 10): tudo o que foi construído depois da 1.4, numa versão só (veja a numeração logo abaixo do histórico) |
| Versão publicada do app | **1.4** no GitHub (06/11/2026; a 1.3 também, no mesmo dia) · F-Droid: receita com a 1.4 e a licença AGPL aceita em 07/11/2026 (!56186); a 1.4 entra na loja depois da compilação do F-Droid |
| Documentos anexos | [`docs/VISUAL.md`](VISUAL.md) (detalhe visual), [`docs/IMPORTACAO.md`](IMPORTACAO.md) (detalhe da importação), [`CLAUDE.md`](../CLAUDE.md) (como trabalhar no código), [`CHANGELOG.md`](../CHANGELOG.md) (em inglês; em português: [`docs/CHANGELOG.pt-BR.md`](CHANGELOG.pt-BR.md)) |

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
| 2.2 | 04/11/2026 | 1.4 pronta para o teste do Hanry: requisitos da 1.4 marcados como feitos; componentes DS-COMP-18..20; conformidade das 10 línguas, de texto no código e da loja (PRC-09). |
| 2.5 | 07/11/2026 | Pedidos do Hanry para a 1.5: ícone novo (G4: o C quadrado dourado, com volume, sem a estrela) escolhido com base em pesquisa de psicologia da atenção (DS-ICO-02..05); **profundidade** em todo o app, "com peso de app de iPhone" (DS-PROF, seção 9.9); DS-DIR-01 revisto (seções em cartões). |
| 2.4 | 06/11/2026 | 1.3 e 1.4 publicadas no GitHub. Página inicial do repositório em inglês (pedido do Hanry): `CHANGELOG.md` em inglês (português em `docs/CHANGELOG.pt-BR.md`) e mensagens de commit em inglês (RF-LNG-09, PRC-02). |
| 3.8 | 13/11/2026 | Mais pedidos do issue #3 (fredericofqd), todos aprovados pelo Hanry para a 1.5: **escolher qualquer fatura do cartão** (RF-CAR-10 ampliado: "flexibilidade é lei, respeitando a lógica"), **a previsão diz em uma frase se a conta fica negativa e qual o menor saldo** (RF-MOT-04), **apoiar o Cryo** (RF-AJU-12) e a barra de desfazer que cobria o botão "Lançar" (ERR-046; na mesma auditoria, ERR-047: botão flutuante sem nome para o leitor de tela). Contas fixas mais completas (RF-FIX-06..10): ritmo (até anual), parcelas com juros, prazo e modelos por língua; **banco de dados versão 5** (DAD-15). |
| 3.7 | 12/11/2026 | **Numeração (decisão do Hanry)**: a próxima versão pública é a **1.5** e junta tudo o que foi construído depois da 1.4. As versões de trabalho 1.5, 1.6, 1.7, 1.8 e 1.9 nunca foram publicadas e viram **etapas da 1.5**; este documento passa a dizer 1.5 em todos os requisitos (só este histórico guarda os nomes antigos). `versionCode` 10. |
| 3.6 | 12/11/2026 | **1.9 construída**: contas fixas que se acertam com o extrato (RF-CFX), perfil (RF-PRF), guia "Aprender o Cryo" (RF-GUI) e sons mais abafados (RF-SOM-01 revisto) marcados como feitos, com o que mudou em relação à proposta. **Pedido de quem usa (issue #3 do GitHub, 12/11/2026)**: estorno no cartão lançado à mão e total da fatura que não batia; causa achada no código (ERR-043 e ERR-044) e requisitos novos RF-CAR-10 (escolher a fatura) e RF-CAR-11 (ajustar o total). Auditoria: ERR-040 a ERR-045 na biblioteca de erros. |
| 3.5 | 10/11/2026 | Retorno do Hanry no teste da 1.8: **o bloqueio não pedia a digital** ao fechar e abrir o app em menos de 1 minuto (RF-BLQ-02 revisto, ERR-039) e ligar a chave não confirmava nada (RF-BLQ-01 revisto); corrigido na própria 1.8. Decisões dele para a 1.9: foto no perfil (RF-PRF-01), pagamento na conta em que aconteceu e pergunta ao criar a conta fixa depois (RF-CFX-08), guia no padrão. |
| 3.4 | 10/11/2026 | Pedido do Hanry: **proposta da 1.9** (seção 6.19d): contas fixas que se acertam com o extrato importado (RF-CFX, com a causa dos "valores estranhos" conferida no código), perfil com a bolinha que abre os ajustes (RF-PRF) e guia local por regras (RF-GUI). |
| 3.3 | 10/11/2026 | **1.8 pronta**: a previsão nas telas (RF-PRV-08) e o aviso de ritmo com a previsão por categoria (RF-PRV-06); o conjunto passa a encolher o erro de cada modelo, não o peso (ERR-037, achado no caso do mercado do começo do mês); mundo `MONTHLY_SHOP` no laboratório; ERR-038. Versão 1.8 (versionCode 9). |
| 3.2 | 10/11/2026 | Pedido do Hanry: auditoria do código inteiro e **previsor novo, do zero** (RF-PRV, seção 6.12b), para a **1.8**. Auditoria feita e corrigida (RF-AUD); previsor do mês pronto e ligado ao motor, medido contra o da 1.5. Categorias e investimentos calculados (RF-PRV-06, 07); **falta só RF-PRV-08** (telas e textos). **Biblioteca de erros** `docs/ERROS.md` criada como lei (PRC-08), conferida por `KnownIssuesTest`. |
| 3.1 | 09/11/2026 | **1.7 construída**: requisitos marcados como feitos; avisos só em dois horários (RF-AVI-04, sem "horário silencioso" separado); telas TELA-ORCAMENTO-MES e TELA-AVISOS; voz mais rápida medida com o modelo de verdade (RF-VOZ-13: a janela curta erra valores e ficou de fora; atenção rápida como teste no modo desenvolvedor); fonte em 200% conferida (RF-ALC-04). |
| 3.0 | 09/11/2026 | **1.7 aprovada pelo Hanry**: barra com o botão de voz, tom "amigo", sons, **banco de dados mais flexível** (versão 4: remanejar o orçamento só no mês, dias sem gastos, ilustração das metas) com um **painel geral dos gastos**, sem mascote, lembrete do dia ligado **perguntando antes o horário**. Pedido novo: **nada de emoji de teclado**; ilustrações próprias no estilo do Cryo (RF-ILU). Estudo das notificações animadas e "ao vivo" do Android e da One UI (RF-AVI-09/10). |
| 2.9 | 09/11/2026 | Retorno do Hanry sobre a 1.6 ("gostei pra caramba", "lentinho, coisa de 5 segundos") e **proposta da 1.7** (seção 6.19c): avisos inteligentes no estilo Duolingo (RF-AVI), sons próprios (RF-SOM), momentos animados (RF-MOM), alertas de orçamento ao salvar e pelo ritmo (RF-ORC-04/05), botão de voz ao alcance do polegar como na One UI (RF-ALC) e voz mais rápida (RF-VOZ-13). |
| 2.8 | 09/11/2026 | 1.6 aprovada pelo Hanry ("aprovado; vamos testar em português primeiro"; permissão para compilar o Whisper) e feita: RF-VOZ-01..12 (Whisper no APK, microfone ao vivo com o eco, frases faladas em português, vários lançamentos, correções, escolha do ouvido em Ajustes). Permissão `RECORD_AUDIO` (RNF-PRIV-01). Análise de "um Cryo por língua" (seção 6.2a). |
| 2.7 | 09/11/2026 | Proposta da 1.6: voz no aparelho com Whisper e animação de eco (RF-VOZ, seção 6.2a), com as respostas do Hanry (Galaxy A55, aceita IA local se nada sair do celular). 1.5 guardada para lançar por volta de 07/12/2026. |
| 2.6 | 07/11/2026 | Pedido do Hanry depois de testar a 1.5: **motor financeiro** robusto e todo no aparelho (RF-MOT-01..15, seção 6.12a), com **500 testes de garantia**, e **proteção dos dados** com hash e criptografia (RF-SEG, seção 6.15a); Rust e criptografia do banco avaliados e deixados para depois. Aprovado e unido ao visual: tudo na **1.5**. |
| 2.3 | 06/11/2026 | Pedidos do Hanry depois de testar a 1.4: passeio animado "Como usar o Cryo" (RF-TOUR), movimento em todo o app (DS-MOV, ACS-08), página do GitHub nas 10 línguas com inglês como principal (RF-LNG-09, RF-LNG-10). Tudo na 1.4. |

> **Numeração da 1.5 (12/11/2026).** Depois da 1.4 (publicada), o trabalho foi feito em cinco etapas que tiveram nomes de versão
> mas **nunca foram publicadas**. Elas saem juntas como a **versão 1.5** (`versionCode` 10, para instalar por cima dos APKs de
> teste que já existiam). Onde o histórico acima, comentários do código, nomes de testes (`Screens17Test`, `Audit18Test`...) ou
> branches antigos disserem 1.6, 1.7, 1.8 ou 1.9, leia: **etapa 2** (voz no aparelho), **etapa 3** (avisos, sons, momentos),
> **etapa 4** (auditoria e previsor) e **etapa 5** (contas fixas, perfil, guia, cartão) da 1.5; a **etapa 1** foi o ícone, a
> profundidade, o motor financeiro e a proteção dos dados. A próxima versão depois desta será a 1.6 de verdade.

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
| PRI-09 | **Flexível, sem perder a lógica** (decisão do Hanry, 13/11/2026) | O Cryo sugere, a pessoa decide: toda regra automática (a fatura pela data, o ritmo da conta fixa, a categoria sugerida) tem um jeito de a pessoa mudar o resultado, à vista e sem gambiarra. A sugestão continua certa e explicada; a escolha dela manda. Nenhuma "flexibilidade" pode quebrar uma conta: o que ela escolhe é guardado e respeitado nos cálculos. | Toda tela com uma regra automática oferece a escolha (RF-CAR-10, RF-FIX-06, RF-CFX-02). |
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
| TELA-ANALISES | Análises (saúde financeira, previsão com faixa, percepções, próximos meses, recorrências, fora do padrão, fluxo, categorias, calendário de calor, 6 meses) | `insights` | `ui/screens/InsightsScreen.kt` |
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
| TELA-TOUR | Como usar o Cryo (passeio animado, aberto pelos Ajustes) | `tour` | `ui/screens/Tour.kt` |
| TELA-SAUDE | Saúde financeira: a nota de 0 a 100 e as 6 partes (RF-MOT-06) | `insights/health` | `ui/screens/EngineScreens.kt` |
| TELA-RECORRENCIAS | O que se repete: recorrências descobertas pelo motor (RF-MOT-02) | `insights/recurring` | `ui/screens/EngineScreens.kt` |
| TELA-PLANO-DIVIDAS | Plano para quitar as dívidas (RF-MOT-08) | `debts/plan` | `ui/screens/EngineScreens.kt` |
| TELA-ORCAMENTO-MES | Painel geral dos gastos do mês: categorias com o limite deste mês, sem orçamento, remanejar e desfazer (RF-ORC-06) | `plan/budgets/month` | `ui/screens/BudgetMonthScreen.kt` |
| TELA-AVISOS | Ajustes › Avisos e sons: tom, lembrete do dia, tipos com "Ver exemplo", sons (RF-AVI-08) | `settings/alerts` | `ui/screens/AlertScreens.kt` |
| TELA-PERFIL | Painel do perfil: a bolinha (letra, desenho, emoji ou foto), atalhos e a situação do backup, do bloqueio e dos avisos; abre pela bolinha do Início (RF-PRF-01..04) | `profile` | `ui/screens/ProfileScreens.kt` |
| TELA-APRENDER | "Aprender o Cryo": quanto a pessoa já conhece, as lições em seis degraus (tocar mostra onde fica) e a chave "Dicas do Cryo"; abre pelo perfil e por Ajustes › Sobre (RF-GUI-06) | `learn` | `ui/screens/LearnScreen.kt` |
| TELA-BOAS-VINDAS | Apresentação inicial (sem rota: antes do app); inclui o passeio (RF-TOUR-02) | — | `ui/screens/Onboarding.kt`, `ui/screens/Tour.kt` |
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
| RF-INI-11 | **Cabeçalho na cor do Cryo** | Faixa no topo com saudação, olho, engrenagem e o saldo (DS-COMP-16). Segue o Material You. | ✅ 1.4 |
| RF-INI-12 | **Atalhos** | Fila de atalhos redondos logo abaixo do cabeçalho: Lançar, Importar, Transferir, Pagar fatura (se houver cartão), Contas fixas, Metas, Dívidas. Cada um abre a tela certa em 1 toque (DS-COMP-15). | ✅ 1.4 |
| RF-INI-13 | **Seções planas** | O resto do Início em seções separadas por linhas finas, cada uma com título e "›" (DS-COMP-14): Este mês, Cartão de crédito, Contas, Dívidas, Orçamentos, Metas e caixinhas, Últimas movimentações. Sem cartões empilhados. | ✅ 1.4 |
| RF-INI-14 | Carteira dentro do Início (4 abas) | Barra com Início, Extrato, Planejar e Análises; no Início, seções Contas, Cartão, Investimentos e Dívidas; patrimônio e seções levam à Carteira completa (5.1). | ✅ 1.4 |
| RF-INI-17 | Investimentos no Início | Seção com o total investido hoje e quanto rendeu; › abre a Carteira completa. Só aparece quando há investimento. | ✅ 1.4 |
| RF-INI-15 | Avisos discretos | Avisos como cartões neutros com ícone colorido (DS-COMP-17), não como faixas laranja/vermelhas cheias. | ✅ 1.4 |
| RF-INI-16 | Sem botão flutuante no Início | "Lançar" passa a ser o 1º atalho (o botão flutuante cobria conteúdo). | ✅ 1.4 |

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
| RF-REG-08 | Voz | Reconhecimento de fala do próprio celular (`RecognizerIntent`), na língua do app; sem ele, digitar sempre funciona. A partir da 1.5, a voz é ouvida dentro do Cryo (RF-VOZ); a tela do sistema fica como último recurso. | ✅ 1.0 · no aparelho 1.5 |
| RF-REG-09 | **Frases nas 10 línguas** | Valor, data relativa (ontem, dia 5, segunda...), parcelas ("10x", "in 10 installments", "10回払い"...), conta ou cartão, entrada, transferência e categoria pelas palavras-chave da língua do app. Em japonês, mandarim e coreano (sem espaços entre palavras) e em hindi, regras próprias de leitura. | ✅ 1.4 |

Código: `domain/PhraseParser.kt` (regras), `domain/PhraseLang.kt` (palavras de cada língua), `ui/tx/QuickEntry.kt`. Testes: `PhraseParserTest`, `PhraseLanguagesTest`.

### 6.2a Voz no aparelho — RF-VOZ (1.5)

Pedido do Hanry (07 e 09/11/2026): melhorar o registro por voz com uma IA local; "se o Whisper não levar nenhum dado para fora,
eu aceito ter uma IA local dentro do app"; "uma animação daora enquanto a pessoa fala, como um eco, e o sistema pega exatamente
o que ela está dizendo". Celular dele: Samsung Galaxy A55, com serviços do Google.

**Por que mudar.** Hoje (RF-REG-08) o Cryo chama o reconhecedor do celular sem pedir que ele trabalhe só no aparelho: em muitos
celulares **o áudio vai para os servidores de quem fez o reconhecedor**, e em celulares sem Google o microfone não funciona. E o
entendimento de frases erra o jeito falado. Teste de 09/11/2026 com a regra atual: "uber 18 e 90" vira R$ 90,00; "trinta e dois e
cinquenta" e "cento e vinte reais" ficam sem valor; "com o roxinho" não acha o Cartão Roxo; "almoço 30 e uber 15" vira um
lançamento só.

**Duas partes**: o **ouvido** (voz → texto) ganha IA local (Whisper); o **cérebro** (texto → lançamento) continua com regras
exatas e testáveis, sem modelo de linguagem: modelos de conversa locais pesam de 300 MB a 1 GB, demoram em celular simples,
podem inventar números (inaceitável com dinheiro) e não explicam o que decidiram (PRI-05).

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-VOZ-01 | **Reconhecimento de fala no aparelho** com o Whisper (whisper.cpp v1.9.5 e o modelo "base" multilíngue q5_1, licença MIT, 57 MB dentro do APK: o app continua sem internet). Só em ARM de 64 bits com fp16 e dotprod (conferido nas informações do processador antes de carregar a biblioteca); nos outros, uma biblioteca vazia deixa o APK instalar. Plano B: o reconhecedor do celular **só no aparelho** (Android 12+); sem ele, a tela de voz do sistema, como na 1.5 (com "preferir offline"). | `WhisperHostTest` (a ponte e o modelo de verdade no computador); o app segue sem INTERNET (`PrdConformanceTest`). | ✅ 1.5 |
| RF-VOZ-02 | **Microfone**: permissão `RECORD_AUDIO` pedida só no primeiro toque no microfone, com a explicação ("o áudio fica só no celular; o Cryo nem tem internet"). O áudio fica só na memória e é apagado ao terminar: nunca vira arquivo. | `VoiceFlowTest` (tela de explicação), `VoiceListenerTest` (microfone sempre solto); RNF-PRIV-01 atualizado. | ✅ 1.5 |
| RF-VOZ-03 | **Ao vivo**: as palavras aparecem enquanto a pessoa fala (o áudio acumulado é relido a cada ~0,7 s); termina sozinho depois de ~1,2 s de silêncio; "Pronto" e "Cancelar" a qualquer momento. | `VoiceListenerTest` (início, fim, releituras, "Pronto", cancelar). **Medido pelo Hanry no A55 (09/11/2026): ~5 s** até a transcrição pronta; aprovado assim; a voz mais rápida veio na etapa seguinte (RF-VOZ-13). | 🟡 1.5 |
| RF-VOZ-04 | **Vocabulário da pessoa**: o Whisper recebe como dica os nomes das contas, cartões, categorias e metas, e as palavras de dinheiro da língua: acerta "Roxo", "Nubank", "Pet Shop". | `VoicePrompt`; `WhisperHostTest` (com a dica, "32,50" sai com algarismos). | ✅ 1.5 |
| RF-VOZ-05 | **Entender o jeito falado** nas 10 línguas: números por extenso ("trinta e dois e cinquenta", "cento e vinte", "três mil"), "18 e 90" = 18,90, "conto/pila", pontuação e "R$" do Whisper, apelidos e diminutivos ("roxinho" → Cartão Roxo). | `SpokenTest` com as frases do teste de 09/11. **Português primeiro** (pedido do Hanry); nas outras línguas o Whisper já escreve algarismos e fica a limpeza e a divisão. | ✅ 1.5 (pt) |
| RF-VOZ-06 | **Vários lançamentos numa frase**: "almoço 30 e uber 15" vira dois lançamentos para conferir. A data, o cartão ou a conta e o verbo ditos uma vez valem para todos ("mercado 120 e farmácia 30 no cartão"). | `SpokenTest`, `VoiceFlowTest`. | ✅ 1.5 |
| RF-VOZ-07 | **Correção falada**: "32, não, 35" fica 35, e a descrição sem o "não"; "no débito, não, na carteira" fica na carteira. | `SpokenTest`. | ✅ 1.5 |
| RF-VOZ-08 | **Animação de eco** (protótipo de 09/11/2026 para aprovação): o microfone "respira" com o volume da voz; cada sílaba solta um anel de eco que se abre e some; cada palavra reconhecida entra subindo com um brilho que se apaga; o que o Cryo entende (valor, categoria, conta ou cartão, data, parcelas) vira um cartãozinho que salta com mola; no fim, ✓ "Entendi" e o botão "Salvar". Anéis só enquanto há voz (nada infinito, DS-MOV-10); com "Remover animações", tudo aparece direto. | Imagens 58–62 (`VoiceFlowTest`); "Remover animações" pelo `animationsOn()` (ACS-08): sem anéis, palavras e cartõezinhos já no lugar. | ✅ 1.5 |
| RF-VOZ-09 | **Leve**: o modelo só é carregado ao abrir o microfone e é solto ao fechar; em celular lento, menos atualizações ao vivo (fica o resultado final). | `VoiceListenerTest` e `WhisperHostTest` (fechar solta tudo, fechar duas vezes é seguro). | ✅ 1.5 |
| RF-VOZ-10 | **Compilação reproduzível com código nativo**: NDK fixo (r27c), sem caminhos da máquina dentro do binário, o mesmo APK compilado em duas pastas diferentes. A receita do F-Droid ganha a linha do NDK (o lançamento da 1.5 precisa de merge request no fdroiddata). | Duas compilações limpas, em pastas diferentes, com o mesmo SHA-256; `PrdConformanceTest` confere as opções do CMake. | ✅ 1.5 |
| RF-VOZ-11 | **Escolha do ouvido** em Ajustes › Registro rápido › Reconhecimento de voz: "Whisper, dentro do Cryo" (padrão) ou "Reconhecedor do celular". Preferência do aparelho, fora do backup. Em celular que não roda o Whisper, a opção explica e fica no do celular. | Revisão. | ✅ 1.5 |
| RF-VOZ-13 | **Voz mais rápida** (pedido do Hanry depois do primeiro teste da voz: ~5 s no A55). Feito: o modo desenvolvedor mostra o tempo do fim da fala até o texto e tem a chave experimental **"Atenção rápida no Whisper"** (no computador, ~20% mais rápido com o mesmo texto; desligada até ser conferida no A55). O som de "pode falar" (RF-SOM) toca com o microfone já aberto: os 0,3 s em que ele toca **não vão para o Whisper** (a primeira versão de teste da 1.5 mandava "bipe + fala"). O fim da fala foi testado em 1,0 s e **voltou a 1,2 s** (o valor aprovado no primeiro teste), depois que o Hanry achou a captação pior no teste seguinte. **Medido e descartado**: o resultado final com a janela do tamanho da fala seria ~4 vezes mais rápido, mas ouviu "32,40" onde foi dito "32,50" (com qualquer folga até 10 s); o final continua com a janela inteira. Carregar o modelo antes do toque também ficou de fora (RF-VOZ-09). | `VoiceListenerTest` (o bipe não chega ao Whisper); `WhisperHostTest` (a janela inteira acerta "32,50" com e sem a atenção rápida; a janela curta erra). | 🟡 1.5 |
| RF-VOZ-14 | **Ouvido melhor: estudo de outras IAs locais** (pedido do Hanry, 09/11/2026: "o Whisper não está captando bem"). Bancada no computador com o mesmo uso do app: 60 gravações reais de brasileiros (FLEURS pt-BR) e 90 frases do Cryo em 3 vozes sintéticas, limpas e com ruído (10 dB), passando o texto pelo `Spoken` do app. Erro de palavras na fala real: Whisper base (o atual) 14,2%, Whisper small 6,8%, **Parakeet TDT 0.6B v3** (NVIDIA, CC-BY-4.0, 25 línguas europeias com português) **5,0%**, Vosk small 23,3%. Valor certo nas frases com ruído: Whisper base 33/90, small 53/90, **Parakeet 62/90**, Vosk 49/90. Tempo por frase no computador: Whisper base ~1,8 s, small ~5,5 s, **Parakeet ~0,4 s**, Vosk ~1,3 s. O modelo só de português da NVIDIA (FastConformer, 130 MB) ficou de fora pela licença (CC BY-NC, não comercial: o F-Droid não aceita). **Em aberto, para a decisão do Hanry**: o tamanho (Parakeet int8 ~670 MB; versões compactadas de 213–241 MB pelo parakeet.cpp, a conferir), o caminho no Android (parakeet.cpp, sobre o mesmo ggml do whisper.cpp, ou sherpa-onnx com onnxruntime, que o F-Droid teria de compilar), a conferência com gravações do próprio Hanry e no A55. | Relatório da bancada (09/11/2026); gravações do Hanry; medição no A55. | 🔵 proposta |
| RF-VOZ-12 | **Ruído e frases inventadas**: o Whisper só é chamado depois de ~160 ms de voz; áudio sem energia não chega a ele; "legendas de vídeo" que ele inventa no silêncio ("Legendas pela comunidade Amara.org", "Obrigado por assistir") e repetições ("meu e o meu e o meu") são descartadas; se nada sobra, "Não ouvi nada" e "Falar de novo". | `SpokenTest`, `VoiceListenerTest`, `WhisperHostTest`. | ✅ 1.5 |

**Um Cryo por língua? (análise pedida pelo Hanry, 09/11/2026.)** Não ajuda no peso: o modelo "base" multilíngue tem o
mesmo tamanho para qualquer língua; o "base" só de inglês tem o mesmo tamanho; não existe modelo oficial menor só de
português. Dez apps dividiriam quem usa, os backups e as atualizações (10 receitas no F-Droid, 10 páginas, 10 APKs para
conferir). O que de fato reduz o peso, se um dia precisar: o modelo "tiny" (31 MB, erra mais em português), um "pacote de
voz" opcional instalado à parte, ou APKs por processador (economiza só ~1–2 MB). Decisão: **um app só**.

Custos aceitos pelo Hanry: APK de ~5 MB para ~65 MB (o modelo); a permissão de microfone. Código previsto: whisper.cpp (cópia do
código-fonte, MIT) compilado pelo NDK, uma ponte JNI, a tela de "ouvindo" e as regras novas de frases faladas. O protótipo aprovado virou a tela de verdade
(`ui/tx/VoiceSheet.kt`); as imagens saem do `VoiceFlowTest`.

### 6.3 Lançamento completo — RF-LAN (TELA-LANCAR, TELA-LANC-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-LAN-01 | Tipos | Despesa, receita, transferência, pagamento de fatura, aplicar/resgatar investimento, guardar/retirar de meta, dinheiro emprestado (DAD-04). | ✅ 1.0–1.1 |
| RF-LAN-02 | Valor estilo caixa eletrônico | Só números, centavos se ajustam (DS-COMP-10). | ✅ 1.0 |
| RF-LAN-03 | Categoria em grade | Grade de 4 colunas com ícone (DS-COMP-11). | ✅ 1.0 |
| RF-LAN-04 | Conta ou cartão | Chips; no cartão, a fatura é sugerida pela data (DAD-05) e pode ser escolhida (RF-CAR-10). A receita também pode entrar num cartão: é o estorno (RF-CAR-07). | ✅ 1.0 · revisto 1.5 |
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
| RF-CAR-07 | Estorno | Receita no cartão diminui a fatura. **Dá para lançar à mão** (botão "Estorno" na tela do cartão, ou "Receita" escolhendo o cartão em "Recebido em") e **editar um estorno o mantém no cartão e na fatura dele** (ERR-043: até a 1.4 o formulário exigia uma conta, e o estorno importado saía do cartão ao ser editado). | ✅ 1.0 · corrigido 1.5 |
| RF-CAR-08 | Arquivar | Igual a contas. | ✅ 1.1 |
| RF-CAR-09 | Importar fatura | Botão ↥ na tela do cartão (RF-IMP). | ✅ 1.3 |
| RF-CAR-10 | **Escolher a fatura** (pedido de quem usa, issue #3) | O banco fecha a fatura em dias que variam; o Cryo sugere pela data e a pessoa pode mudar. No lançamento do cartão (compra ou estorno), "Fatura em que entra" mostra a sugerida e as vizinhas, e **"Outra…" abre a lista de todas as faturas do cartão** (de um ano atrás até três meses à frente, com o total de cada uma e as já pagas), para quem está pondo o histórico antigo no app; na compra parcelada, as parcelas seguem da escolhida em diante. No pagamento de fatura, "Fatura que este pagamento quita". **Editar não muda a fatura sozinho** (ERR-044): o lançamento fica na fatura em que está (a do arquivo importado, por exemplo) e só volta a seguir a data quando a pessoa muda a data ou o cartão. | Editar o nome de uma compra importada mantém a fatura; mudar a data a recalcula; a escolha vale para compra, parcelas, estorno e pagamento. | ✅ 1.5 |
| RF-CAR-11 | **Ajustar o total da fatura** (pedido de quem usa, issue #3) | Na tela do cartão, "Ajustar total": a pessoa digita o total que o banco mostra e o Cryo lança **a diferença** como um "Ajuste da fatura" nessa fatura (compra se faltava, crédito se sobrava), com desfazer. O ajuste é um lançamento como os outros, à vista e apagável: nada é escondido nem reescrito, e nenhuma conta é tocada. | Ajustar para mais e para menos chega ao total digitado; total igual não cria nada; apagar o ajuste desfaz. | ✅ 1.5 |

Testes: `LedgerTest`, `CardFixTest`, `Screens19Test`.

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
| RF-FIX-06 | **Nem toda conta é mensal** (pedido de quem usa, issue #3) | O ritmo da conta fixa é escolhido: todo mês, a cada 2, 3 ou 6 meses, ou **uma vez por ano** (IPVA, licenciamento, seguro, assinatura anual). Quando não é mensal, a pessoa também escolhe o **primeiro vencimento** (o IPVA é em janeiro, não "daqui a um mês"). Ela só aparece, só cobra na previsão e só lembra nos meses do ciclo (`BillCycle.partOf`). | Uma conta anual de janeiro aparece em janeiro e some de fevereiro a dezembro; a bimestral, de dois em dois meses. | ✅ 1.5 |
| RF-FIX-07 | **Pago em parcelas, com ou sem juros** | Cada ciclo pode ser pago em até 12 parcelas mensais seguidas (IPVA em 3x): o valor informado é **o de cada parcela** e a lista mostra "Parcela 2 de 3". Informando o **preço à vista**, o app diz quanto o parcelamento custa a mais ("Parcelando, você paga R$ 60,00 a mais") ou que não tem juros. Nada é estimado: é a conta que a pessoa informou. | Anual em 3 parcelas cobra em três meses seguidos e descansa nove; os juros aparecem no editor. | ✅ 1.5 |
| RF-FIX-08 | **Por quanto tempo** | "Até quando": sem prazo (como sempre) ou um último mês — o curso de um ano, o seguro que não vai ser renovado. Depois desse mês ela não aparece nem entra na previsão, e o histórico continua lá. | Conta com prazo até junho não aparece em julho. | ✅ 1.5 |
| RF-FIX-09 | **Modelos para começar** | Ao criar uma conta a pagar, uma fileira de modelos da **língua de quem usa** preenche nome, ritmo e dia: aluguel, luz, água, internet, celular, assinatura, plano de saúde, seguro do carro (anual) e os impostos de cada país (Brasil: IPVA e IPTU; inglês: *Vehicle registration* e *Property tax*; japonês: 自動車税 e 固定資産税, e assim por diante). O valor e a categoria continuam com a pessoa. | Os dez modelos existem nas 10 línguas; o imposto anual cai no próximo janeiro. | ✅ 1.5 |
| RF-FIX-10 | **Já paguei esta** | Ao criar a conta fixa, o Cryo procura nos lançamentos que já existem os que casam com ela e pergunta (RF-CFX-08), **olhando um ciclo inteiro para trás**: para a anual, os 12 meses anteriores. Assim o IPVA pago em janeiro não volta como "atrasado" quando a conta é criada em novembro. | Conta anual criada depois: o pagamento de janeiro é encontrado e ligado. | ✅ 1.5 |

### 6.9 Orçamentos — RF-ORC (TELA-PLANEJAR aba 1)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-ORC-01 | Limite por categoria por mês | Total planejado, restante e quanto por dia. | ✅ 1.0 |
| RF-ORC-02 | Barra de uso | Cor normal, âmbar a partir de 80 %, vermelho a partir de 100 % (DS-COMP-08). | ✅ 1.0 |
| RF-ORC-03 | Sugestões | Pela média de gastos de cada categoria. | ✅ 1.0 |
| RF-ORC-04 | **Alerta ao salvar** (1.5, RF-MOM-02): ao salvar um gasto que leva a categoria a 80% ou além do limite, o momento animado no lugar da mensagem de rodapé, com a **sugestão de onde cobrir** (uma categoria com folga neste mês: "Lazer ainda tem R$ 150 livres"). **Remanejar só neste mês** (decisão do Hanry: banco versão 4, DAD-12): passar um valor de uma categoria para outra, ou mudar o limite de uma categoria só neste mês, sem mexer no limite dos outros meses; desfazer pelo painel (RF-ORC-06). | Teste do alerta em 80%, 100% e sem orçamento. | ✅ 1.5 |
| RF-ORC-06 | **Painel geral dos gastos do mês** (decisão do Hanry): todas as categorias com orçamento, numa tela só, com o limite do mês (já com os remanejamentos), o gasto, o que sobra e a barra; tocar numa categoria mostra os remanejamentos do mês. É por ele que se escolhe de onde tirar ao remanejar (RF-ORC-04). Abre de Planejar › Orçamentos e do alerta ao salvar. | Imagem 71 (e 81 com a fonte em 200%); `BudgetMonthTest` (limite efetivo). | ✅ 1.5 |
| RF-ORC-05 | **Aviso pelo ritmo**: quando o motor (RF-MOT ritmo) prevê que a categoria passa do limite antes do fim do mês, um aviso por mês por categoria, com o dia previsto. | Teste. | ✅ 1.5 |

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

### 6.12a Motor financeiro — RF-MOT (TELA-ANALISES, TELA-DIVIDAS, TELA-INVEST, TELA-PLANEJAR)

Pedido do Hanry (07/11/2026): "um algoritmo pesado para a parte financeira, para o cálculo e as análises, tudo dentro do
próprio aplicativo; algo robusto". **Aprovado em 07/11/2026** e unido ao visual da 1.5: sai tudo numa versão só, a **1.5**
(a 1.5 ainda não tinha sido publicada).

**Princípios do motor**
- **Tudo no aparelho**, só Kotlin, sem biblioteca nova e sem internet (RNF-PRIV-01, PRC-07). Sem mudança no banco de dados.
- **Robusto de verdade**: estatística que não se deixa enganar por um gasto fora da curva (mediana em vez de média, cortes nos
  extremos, mais peso no que é recente), simulação de milhares de cenários para dar uma **faixa** e não um número mágico, e
  métodos numéricos com plano B (nunca "dá erro" nem número absurdo).
- **Comprovado**: cada parte é testada com históricos inventados de pessoas diferentes (estável, irregular, recém-chegada,
  endividada) e a previsão é **testada contra o passado** (backtest): ela precisa errar menos que a da 1.5.
- **Explicável**: cada número tem "Como cheguei nesse número" em linguagem simples (de onde veio, de quanto tempo, quanta
  certeza). Nada de caixa-preta e nada de "inteligência artificial" que não se explica (PRI-05).
- **Previsível**: os sorteios da simulação usam uma semente tirada dos próprios dados: os mesmos dados dão sempre o mesmo resultado.
- **Dinheiro em centavos** (`Long`) na entrada e na saída; números com vírgula só dentro das contas, arredondados no fim.
- **Honesto quando sabe pouco**: com menos de 60 dias de dados, mostra "Ainda conhecendo você" em vez de inventar.

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-MOT-01 | **Núcleo de estatística robusta**: mediana, quantis, desvio robusto (MAD), corte de extremos (winsorização), média com peso pelo tempo (meia-vida de 90 dias), tendência resistente a extremos (Theil–Sen), sorteio com semente (SplitMix64). | Testes com valores conhecidos; um gasto 100× maior não muda a mediana; mesma semente, mesmo sorteio. | ✅ 1.5 |
| RF-MOT-02 | **Recorrências**: descobre sozinho o que se repete (salário, aluguel, assinaturas, academia): descrição normalizada (sem números, datas, acentos e "parcela 3/10"), valor parecido, intervalo regular (semanal, quinzenal, mensal, anual), pelo menos 3 vezes (anual: 2). Mostra a próxima data e o valor esperado, "valor varia" (conta de luz), **ficou mais cara**, **atrasou** (salário que não veio) e **parou**. Sugere virar conta fixa; "Não é recorrente" esconde. | Histórico inventado com salário, Netflix, academia, luz e 300 compras soltas: acha todas as recorrentes e **nenhuma** compra solta; aumento de preço e atraso apontados no dia certo. | ✅ 1.5 |
| RF-MOT-03 | **Previsão do mês 2.0** (evolui RF-ANA-01): ritmo do dia a dia aprendido em até 12 meses (dia da semana, começo e fim do mês, peso maior no recente), misturado com o mês atual conforme a confiança; recorrências esperadas e ainda não lançadas entram (sem contar duas vezes as contas fixas); **2.000 cenários** simulados dão a **faixa de 80%** ("entre R$ X e R$ Y") e a **chance de fechar no vermelho**. | Backtest em 24 meses inventados de 4 perfis, nos dias 5, 10, 15, 20 e 25: erro médio menor que o da 1.5; a faixa de 80% contém o valor real em 70%–90% dos casos. | ✅ 1.5 (no app, a conta passou a ser a de RF-PRV-04; esta fica como régua dos testes) |
| RF-MOT-04 | **Próximos meses** (3, 6 e 12), com a resposta em uma frase logo abaixo do gráfico (pedido de quem usa, issue #3): **a conta fica negativa? a partir de quando?** e **qual o menor saldo previsto, e em que dia** — em verde quando não fica negativa, em vermelho quando fica, com a dica de se programar. Saldo das contas dia a dia com salários e recorrências, contas fixas, faturas (no vencimento, na conta que paga), parcelas futuras, dívidas e o gasto do dia a dia com faixa. Mostra o **dia mais apertado**, o saldo no fim de cada mês e a chance de ficar negativo. | Dados inventados: faturas e parcelas no dia certo; dia mais apertado igual ao calculado à mão; sem dados futuros, a linha segue o ritmo com a faixa abrindo. | ✅ 1.5 |
| RF-MOT-05 | **Fora do padrão**: gasto muito acima do normal da categoria (desvio robusto > 3,5 e mais que o dobro do comum, com pelo menos 8 gastos na categoria), **cobrança repetida** (mesma descrição e valor em até 2 dias, sem ser parcela), assinatura que subiu e categoria do mês que, até o mesmo dia, já passou de todos os meses anteriores e está pelo menos 60% acima do normal (com 6 meses para comparar). Os gastos são comparados na escala dos logaritmos (gastos têm "cauda longa"). | Dados inventados: aponta a compra fora da curva e a cobrança em dobro; **zero alarmes** num histórico normal de 12 meses. | ✅ 1.5 |
| RF-MOT-06 | **Saúde financeira**: nota de 0 a 100 com 6 partes, cada uma explicada e com uma sugestão do que fazer: **sobra do mês** (25 pontos: 0% → 0, 20% ou mais → 25), **reserva de emergência** (25: meses de gastos essenciais cobertos por contas, caixinhas e investimentos resgatáveis; 0 → 0, 6 meses → 25), **compromissos** (20: parcelas e dívidas ÷ renda; até 10% → 20, 50% ou mais → 0), **cartão** (10: uso do limite; até 30% → 10, 90% → 0), **em dia** (10: contas e dívidas sem atraso nos últimos 3 meses) e **estabilidade** (10: quanto os gastos variam de um mês para o outro). Faixas com palavras gentis: Atenção · Em construção · Bem · Ótima. | Contas da nota conferidas com perfis inventados (cada parte separada); com menos de 60 dias ou sem renda: "Ainda conhecendo você". Texto diz que não é conselho financeiro. | ✅ 1.5 |
| RF-MOT-07 | **Ritmo das categorias e orçamento inteligente**: aprende como cada categoria costuma andar no mês (mercado no começo, lazer no fim de semana) e avisa "no ritmo atual, Alimentação passa do limite por volta do dia 24"; sugere limites pelo histórico (60º percentil de 6 meses, com a tendência), arredondados. | Dados inventados: aviso no dia certo; sem alarme falso no começo do mês para categorias que gastam tudo no dia 1–5. | ✅ 1.5 |
| RF-MOT-08 | **Dívidas: juros e plano para quitar**: descobre os **juros embutidos** de cada dívida parcelada (a taxa que faz as parcelas restantes valerem o saldo: método de Newton com bissecção de reserva), ao mês e ao ano; compara **avalanche** (maior juro primeiro) e **bola de neve** (menor saldo primeiro) com um valor extra por mês: quando fica livre e quanto economiza; "antecipar parcelas" mostra o desconto justo. | Casos conhecidos (Tabela Price) com taxa exata até 0,01%; dívida sem juros dá 0%; simulação mês a mês confere com a conta fechada. | ✅ 1.5 |
| RF-MOT-09 | **Investimentos: rendimento real**: rentabilidade ao ano considerando cada aporte e resgate na sua data (TIR/XIRR, Newton com bissecção), no total da carteira e em cada investimento; comparação opcional com a inflação que a pessoa informar (sem internet não há índice). | Casos conhecidos (aporte único, aportes mensais, resgate no meio) com erro < 0,01%; menos de 30 dias de histórico: não mostra taxa ao ano. | ✅ 1.5 |
| RF-MOT-10 | **Metas: quando chega lá**: ritmo de depósitos (mediana dos últimos 6 meses), data provável, chance de bater o prazo (simulação) e quanto por mês precisaria, com rendimento opcional. | Casos conhecidos; meta sem depósitos: explica em vez de prever. | ✅ 1.5 |
| RF-MOT-11 | **"O Cryo percebeu" 2.0** (evolui RF-ANA-06): cada percepção ganha uma nota de importância (quanto dinheiro envolve × certeza × novidade); mostra as 3 a 5 mais importantes, sem repetir a mesma ideia. | Dados inventados: a ordem segue a nota; nenhuma frase repetida. | ✅ 1.5 |
| RF-MOT-12 | **Categoria aprendida**: aprende com as próprias movimentações da pessoa qual categoria cada descrição costuma ter (classificador bayesiano simples, com peso no recente e no que ela corrigiu); ajuda o registro por frase e a importação quando as palavras-chave não bastam. Só sugere com certeza de pelo menos 60%. | Dados inventados: acerta 85% ou mais em movimentações separadas para teste; nunca troca uma categoria escolhida pela pessoa. | ✅ 1.5 |
| RF-MOT-13 | **Rápido e sem travar**: tudo calculado fora da linha principal, com cache pela versão dos dados; 5 anos de dados (~10 mil movimentações) em menos de 300 ms num celular médio; a tela mostra o resultado anterior enquanto recalcula. | Teste de desempenho com 10 mil movimentações (limite folgado na JVM); nenhuma conta do motor na composição das telas. | ✅ 1.5 |
| RF-MOT-15 | **500 testes de garantia** (tabela abaixo): estabilidade, escalabilidade, desempenho, economia de recursos e segurança, 100 casos cada. | Os 500 passam em `./gradlew testDebugUnitTest`. | ✅ 1.5 |
| RF-MOT-14 | **Onde aparece**: Análises ganha, no topo, **Saúde financeira** e **Próximos meses**, e as seções **Recorrências** e **Fora do padrão**; a previsão do mês mostra a faixa; Dívidas ganha **Plano para quitar**; Investimento mostra o **rendimento ao ano**; Metas mostra **quando chega lá**; o Início mostra a faixa da previsão e o aviso mais importante. Telas novas: TELA-SAUDE, TELA-RECORRENCIAS e TELA-PLANO-DIVIDAS (os próximos meses ficam num cartão de Análises, com 3, 6 ou 12 meses). Planejar usa os limites sugeridos e o ritmo de cada categoria; a importação e o registro por frase usam a categoria aprendida quando nada mais acha a categoria. | Imagens das telas nos dois temas (DS-PAD-07); textos nas 10 línguas (RF-LNG). | ✅ 1.5 |

Ordem de construção: **(1)** núcleo, recorrências, previsão 2.0 e próximos meses (RF-MOT-01..04, 13); **(2)** fora do padrão,
saúde financeira, ritmo das categorias e percepções 2.0 (RF-MOT-05..07, 11); **(3)** dívidas, investimentos, metas e categoria
aprendida (RF-MOT-08..10, 12). As telas (RF-MOT-14) usam o visual da 1.5 (DS-PROF).
Código: `domain/engine/Engine.kt` (motor, cache e percepções), `domain/engine/Stats.kt`, `domain/engine/Recurrences.kt`,
`domain/engine/Outlook.kt`, `domain/engine/Projection.kt`, `domain/engine/Signals.kt`, `domain/engine/Health.kt`,
`domain/engine/Wealth.kt`, `domain/engine/CategoryModel.kt`; telas em `ui/screens/EngineScreens.kt`.
Testes: `EngineTest` (um ou mais por requisito, com o teste contra o passado), `EngineStabilityTest`, `EngineScalabilityTest`,
`EnginePerformanceTest`, `EngineResourcesTest`, `EngineSecurityTest` (os 500 de garantia), com os históricos inventados de `EngineFixtures`.

**Resultado do teste contra o passado** (450 casos, 3 perfis × 3 sementes × 10 meses × 5 dias): erro médio 38% menor que a
previsão da 1.4 (perfil estável −21%, renda irregular −42%, endividado −26%); a faixa de 80% conteve o valor real em 78% dos casos.
Desempenho medido na JVM: 50 mil movimentações em ~85 ms; ~9 mil em 56–90 ms; ~4 MB de memória por cálculo.

**Os 500 testes de garantia (RF-MOT-15)** rodam depois que o motor fica pronto: 5 blocos de 100 casos, cada bloco com um tipo
de teste, cada caso com dados diferentes (semente própria, perfil e tamanho):

| Bloco | Casos | O que cada caso confere |
|---|---|---|
| **Estabilidade** | 1–100 | Dados estranhos ou extremos (vazio, uma movimentação só, valores enormes, datas em ano bissexto, saldo negativo, sem renda, textos em outras escritas): o motor nunca quebra, nunca dá "NaN" nem infinito, as faixas ficam em ordem (mínimo ≤ provável ≤ máximo), notas entre 0 e 100, chances entre 0% e 100%, e o mesmo dado dá sempre o mesmo resultado. |
| **Escalabilidade** | 101–200 | De 100 a 50 mil movimentações, de 1 a 40 contas, de 1 a 10 anos: o tempo cresce no máximo na proporção dos dados (sem explodir), e os resultados continuam válidos. |
| **Desempenho** | 201–300 | Perfis realistas (até 5 anos, ~10 mil movimentações): o motor inteiro dentro do limite de tempo (RF-MOT-13), cada parte dentro do seu limite, e o recálculo com os mesmos dados vem do cache. |
| **Economia de recursos** | 301–400 | Memória usada por cálculo dentro do limite, nada fica preso na memória depois de muitos cálculos seguidos (sem vazamento), número de cenários fixo, nenhuma thread nova, cache que não cresce sem parar. |
| **Segurança** | 401–500 | Textos maliciosos (enormes, com caracteres de controle, de inversão de escrita, "%s", padrões que travam expressões regulares) terminam rápido e sem erro; o que o motor guarda é hash (HMAC-SHA256) e não contém nenhuma descrição, valor ou nome; backup com senha não deixa nada legível, recusa senha errada e arquivo adulterado; nada do motor fala com rede. |


### 6.12b Previsor — RF-PRV (1.5)

Pedido do Hanry (10/11/2026): "um algoritmo de previsão tão robusto e eficiente que se compara a IA, mas algoritmo puro".
O previsor é **puro Kotlin, sem biblioteca, sem rede e sem rede neural** (PRI-05: cada número se explica). A ideia central
é a que vence as competições de previsão com séries curtas: **vários métodos simples, combinados pelo desempenho de cada um
no passado da própria pessoa**, e uma faixa que é **conferida** contra esse passado antes de ser mostrada.

| Código | Requisito | Aceite | Situação |
|---|---|---|---|
| RF-PRV-01 | **Séries diárias** (`Series.kt`): cada fluxo (gasto do dia a dia, entradas avulsas, uma categoria) vira um valor por dia (até 3 anos). Quem prevê vê só o passado (`View`), **sem os extremos** (teto no percentil 97,5 dos dias vistos até ali). | Um modelo nunca lê um dia depois do dia da pergunta. | ✅ 1.5 |
| RF-PRV-02 | **Modelos** (`Models.kt`), todos respondendo "quanto soma nos próximos h dias?": perfil da semana com peso no recente; média de 3 e de 6 meses; o mesmo trecho do mês nos meses passados (puro e ajustado ao nível de agora); Holt amortecido e Theta sobre as semanas. Quem não tem o que dizer fica calado. | Cada modelo tem o seu erro medido no laboratório. | ✅ 1.5 |
| RF-PRV-03 | **Conjunto** (`Ensemble.kt`): para cada previsão, o Cryo refaz a pergunta em até 20 dias do passado (o mesmo dia do mês nos últimos 12 meses e algumas semanas atrás) e pesa cada modelo pelo erro (1/erro⁴); com pouco passado, o **erro** de cada modelo encolhe para o erro médio de todos (não o peso para "todos iguais": isso dava uma fatia fixa a quem errava feio, ERR-037). Os erros "de fora" (pesos refeitos sem aquele dia) dão o espalhamento. **Época do ano** (`Season.kt`): só multiplica a previsão quando os mesmos dias se destacaram **nos dois anos anteriores**, para o mesmo lado. | O conjunto erra menos que o melhor modelo sozinho na média dos mundos. | ✅ 1.5 |
| RF-PRV-04 | **Previsão do mês** (`Forecaster.month`, no lugar de RF-MOT-03): o que é certo (já aconteceu, agendados, contas fixas, parcelas, recorrências) entra como na 1.5; o incerto (gasto do dia a dia e entradas avulsas) vem do conjunto; 2.000 cenários dão a faixa. Mesmos dados, mesmo resultado. | `ForecastTest`: erra menos que `Outlook.month` (o primeiro motor). | ✅ 1.5 |
| RF-PRV-05 | **Conferência no passado** (calibração conforme): o Cryo refaz a previsão no mesmo dia de até 12 meses anteriores e alarga ou estreita a faixa até ela conter 80% do que aconteceu; corrige o centro só quando o erro para um lado é maior que o acaso. Guarda `Calibration(meses, acertos, erro típico, viés)` para a tela dizer "nos seus últimos N meses, a faixa acertou X". | A faixa de 80% contém o real em 68%–93% dos casos de `ForecastTest`. | ✅ 1.5 |
| RF-PRV-06 | **Categorias** (`CategoryForecast.kt`): como cada categoria fecha o mês = o que já saiu + o que tem dia certo (lançado, conta fixa, o que se repete) + o gasto do dia a dia previsto pelo conjunto; com limite, a **chance de passar**. Sai em `EngineReport.categories`; o aviso de ritmo (`Pacing.alerts`, RF-MOT-07) usa esse número, com a conta da 1.5 (`Pacing.projected`) só de reserva. | `ForecastTest`: erra menos que `Pacing.projected` e o mercado gasto todo no começo do mês não vira alarme. Laboratório (7.198 casos, 9 mundos): erro 109,77 contra 122,05 (−10%), melhor em 8 de 9 (perde no de compra semanal); no mundo `MONTHLY_SHOP`, 75,68 contra 84,66 (−11%). A faixa conteve o real em 75% (57% no de compras raras: estreita demais ali). | ✅ 1.5 |
| RF-PRV-07 | **Investimentos** (`InvestForecast.kt`): rentabilidade **do investimento** (Dietz modificado entre um rendimento lançado e o seguinte: não se deixa levar por quando a pessoa aportou), sobe e desce ao ano, maior queda, parte dos períodos no negativo, aporte típico, e projeção de 1, 3 e 5 anos em 1.000 cenários (sorteio dos rendimentos passados + a incerteza da média), com faixa de 80% e chance de chegar a um valor (`chanceOf`). Menos de 6 períodos: mostra o retrato, não projeta. A semente vem dos investimentos da conta: com um investimento só, a carteira e ele dão o mesmo número (ERR-038). | `ForecastTest`: renda fixa de 1% ao mês dá 1%, sem queda, e o valor futuro bate com a fórmula; aporte grande não muda a taxa. Texto sem promessa de ganho. | ✅ 1.5 |
| RF-PRV-08 | **A previsão se explica** nas telas. Análises (`InsightsScreen.kt`): "Conferida no seu passado: acertou em N de M meses (erro típico de X)", o que mais pesou na conta (`MonthOutlook.models`, nomes simples, só acima de 15%) e a época do ano (`MonthOutlook.season`, só com ±5%). Planejar › Orçamentos (`PlanScreens.kt`): "Deve fechar o mês perto de X · Y% de chance de passar do limite". Investimento (`WalletScreens.kt`): cartão "Se continuar como foi" ("o investimento em si rendeu X ao mês", para não confundir com o rendimento real da TIR logo acima; faixas de 1, 3 e 5 anos; chance de ter mais do que colocou; "não é promessa"); Carteira: onde a carteira pode estar em 5 anos. Textos nas 10 línguas. | Imagens 82–85 em `Screens18Test`. | ✅ 1.5 |
| RF-PRV-09 | **Laboratório** (`ForecastLabTest`, só com `-PforecastLab=true`) e **teste permanente** (`ForecastTest`): 9 "mundos" inventados e difíceis (`ForecastWorld`: estável, pós-pagamento, tendência, mudança de vida, sazonal, compra semanal, gastos raros, renda irregular, quem cortou gastos). | O previsor só substitui o primeiro motor onde ganha. | ✅ 1.5 |

**Resultado do laboratório** (900 casos: 9 mundos × 2 sementes × 10 meses × dias 5, 10, 15, 20 e 25; erro médio do resultado
do mês, na moeda; "cob." = em quantos casos a faixa de 80% conteve o real):

```
mundo       casos |  1º motor:  erro cob. largura | previsor:     erro cob. largura
STEADY        100 |    375.37  68.0   872.29 |    337.85  75.0  1021.17
PAYDAY        100 |    509.65  48.0  1068.52 |    353.74  74.0  1044.40
TREND         100 |    349.55  89.0  1277.84 |    306.67  88.0  1324.59
SHIFT         100 |    433.94  70.0  1133.58 |    398.65  79.0  1246.96
SEASONAL      100 |    418.57  72.0  1124.35 |    461.39  77.0  1407.52
WEEKLY        100 |    336.18  48.0   675.64 |    230.49  70.0   763.17
LUMPY         100 |   1448.94  70.0  3691.90 |   1378.86  84.0  5479.42
FREELANCER    100 |   2064.65  73.0  6514.44 |   2044.68  83.0  8102.10
SAVER         100 |    359.75  74.0  1030.99 |    348.98  90.0  1408.99
TODOS         900 |    699.62  68.0  1932.17 |    651.26  80.0  2422.03
```

**Mundo 10, `MONTHLY_SHOP`** (a compra do mês no mercado logo depois do pagamento; criado depois do ERR-037, porque os 9 mundos
sorteavam o dia de cada compra e não tinham categoria com dia certo): previsão do mês com erro 274,43 contra 638,87 da 1.5 (−57%),
faixa de 80% com 84% de acerto (1.5: 59%).

**Testado e descartado** (para ninguém repetir): somar a mediana dos erros passados ao centro (piorou em todos os mundos: era
ruído); modelos "nervosos" (média de 4 semanas, o mês esticado pela curva dos meses passados, o nível do mês misturado ao
histórico, que é o jeito da 1.5); o "pulso do mês" (aprender se um mês que começa caro continua caro: piorou em todos os
mundos); época do ano com um ano só de confirmação (alarmes falsos custavam mais do que ela rendia); nas categorias, comparar só com
o mesmo dia dos meses passados (menos dias para medir: piorou o caso do mercado).
**Limite conhecido**: num mês fora do padrão que nunca se repetiu (o primeiro dezembro caro), o motor da 1.5 reage mais
depressa; o novo ganha nos outros meses e no conjunto. Os mundos são inventados: a medida de verdade virá do uso.
Código: `domain/predict/` (`Series.kt`, `Models.kt`, `Season.kt`, `Ensemble.kt`, `Forecaster.kt`, `CategoryForecast.kt`, `InvestForecast.kt`). O motor da 1.5
(`Outlook.month`) continua no código como régua dos testes.

### 6.12c Auditoria da 1.5 — RF-AUD

| Código | Requisito | Situação |
|---|---|---|
| RF-AUD-01 | Backup com senha **nunca** sai aberto: se o cofre não responde, o backup falha e avisa (antes gravava sem proteção). | ✅ 1.5 |
| RF-AUD-02 | Backup lido com limite (64 MB) e conferido campo a campo (valores, datas, dias, contadores); arquivo estragado é recusado com mensagem clara, fora da linha principal. | ✅ 1.5 |
| RF-AUD-03 | Planilha (CSV) não executa fórmulas vindas de descrições (`=`, `+`, `-`, `@` ganham `'`). | ✅ 1.5 |
| RF-AUD-04 | Bloqueio: erro do sensor só libera em aparelho **sem** trava de tela; no Android 8–12 a tela fica protegida (`FLAG_SECURE`) com o bloqueio ligado. | ✅ 1.5 |
| RF-AUD-05 | Dinheiro: valores gigantes não "dão a volta" (`Money.centsOrNull`, teto de 13 algarismos) em frases, importação e JSON; parcelas nunca dividem por zero. | ✅ 1.5 |
| RF-AUD-06 | Banco: "Já paguei" duas vezes grava um pagamento só (transação); apagar categoria leva os remanejamentos. | ✅ 1.5 |
| RF-AUD-07 | Avisos: cada aviso abre a sua tela (`PendingIntent` com ação própria); figurinhas por tabela fixa. | ✅ 1.5 |

Testes: `Audit18Test`, `Audit18DataTest`. **Situação: todos os achados da auditoria estão corrigidos e com teste** (suíte inteira
passando); cada um está na biblioteca de erros (`docs/ERROS.md`, ERR-003..006, 011, 015..019) com a lei e a guarda.

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
| RF-IMP-21 | **Extratos de outros países** | Nomes de coluna nas 10 línguas (Date/Amount, Fecha/Importe, Datum/Betrag, 日付/金額...); ordem da data (dia/mês, mês/dia como nos EUA, ano/mês/dia) descoberta pelo arquivo e, se não der, pelo país; símbolo da moeda do app e de outras; valores sem centavos (iene, won). | ✅ 1.4 |

Testes: `StatementImportTest`, `ImportFlowTest`, `ImportCountriesTest`.

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
| RF-BLQ-01 | Bloquear com digital, rosto ou senha do celular | Ligável em Ajustes; exige trava no aparelho. **Ligar passa pelo desbloqueio uma vez** (a mesma pergunta de quando o app abre): a chave só liga se ele for aceito (1.5, ERR-039). | ✅ 1.0 · revisto 1.5 |
| RF-BLQ-02 | Quando bloqueia | **Toda vez que o app abre do zero** (tela criada sem estado salvo: inclusive quando a pessoa tira o app dos Recentes e abre de novo antes de 1 minuto, com o processo ainda vivo; 1.5, ERR-039), depois de 1 min em segundo plano e quando o Android fechou o app e restaura a tela. Girar a tela e as recriações do próprio app (modo de teste, língua) não pedem de novo. | ✅ 1.3 · revisto 1.5 |
| RF-BLQ-03 | Nada do app à vista | Bloqueado, o app sai da tela (nem diálogos, menus ou folhas; nem Voltar ou teclado chegam a ele). | ✅ 1.3 |
| RF-BLQ-04 | Volta para onde estava | O estado é guardado e volta ao desbloquear. | ✅ 1.3 |
| RF-BLQ-05 | Recentes | Com o bloqueio ligado, a miniatura não guarda a tela (Android 13+). | ✅ 1.3 |
| RF-BLQ-06 | Não prender a pessoa fora | Sem trava no aparelho ou sem biometria, libera. | ✅ 1.0 |

Testes: `DevModeFlowTest` (bloqueio).

### 6.15a Proteção dos dados — RF-SEG (hash + criptografia)

Pedido do Hanry (07/11/2026): "segurança de dados (o que está sendo armazenado)... não quero nenhum dado sensível sendo
vazado". Decidido: **hash + criptografia**, só com o que o próprio Android já traz (sem biblioteca nova).

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-SEG-01 | **Cofre do aparelho**: as chaves do Cryo ficam no cofre do Android (Android Keystore), que não deixa a chave sair do aparelho (nem para o próprio app). Se o cofre falhar (aparelho com defeito), as funções que dependem dele avisam e não guardam nada sem proteção. | Revisão; teste com cofre falso. | ✅ 1.5 |
| RF-SEG-02 | **Motor sem rastro**: os resultados do motor ficam só na memória; o pouco que ele guarda (recorrências que a pessoa escondeu) vira **HMAC-SHA256** com a chave do cofre: um código que não revela a descrição e não dá para desfazer. | Bloco de segurança dos 500 testes: o que fica guardado não contém descrição, valor nem nome. | ✅ 1.5 |
| RF-SEG-03 | **Backup com senha** (opcional, em Backup automático e em "Salvar uma cópia agora"): **AES-256-GCM** com chave tirada da senha por **PBKDF2-HMAC-SHA256** (310 mil voltas, sal aleatório); só o começo do arquivo fica legível (app, versão, data), o resto é ilegível; arquivo adulterado é recusado. A senha nunca é guardada: para o backup automático, guarda-se a chave já derivada, trancada pelo cofre (RF-SEG-01). Sem a senha não há como abrir: o app avisa antes de ligar. | Testes: ida e volta, senha errada, arquivo adulterado, nada legível no arquivo, backups antigos (sem senha) continuam abrindo. | ✅ 1.5 |
| RF-SEG-04 | **Restaurar com senha**: Ajustes e apresentação (RF-BOA-06) pedem a senha quando o arquivo é protegido (neste aparelho, abre sozinho com a chave guardada); senha errada explica e deixa tentar de novo. No "backup encontrado" do backup automático, um arquivo de outra senha é explicado ("restaure por Ajustes") em vez de oferecido. | `EngineSecurityTest`, revisão. | ✅ 1.5 |
| RF-SEG-05 | **Tela de bloqueio do celular**: lembretes aparecem só como "Você tem uma conta para pagar" quando o celular está bloqueado (sem nome nem valor); o texto completo só com o celular desbloqueado. | Teste da notificação (versão pública sem dados). | ✅ 1.5 |
| RF-SEG-06 | **Área de transferência**: o relatório completo copiado no modo desenvolvedor é marcado como sensível (Android 13+ não mostra a prévia); o anônimo não tem dados. | Revisão. | ✅ 1.5 |
| RF-SEG-07 | **Nada em logs**: o app não escreve dados no registro do sistema (sem `Log`, `println` nem `printStackTrace`). | `PrdConformanceTest`. | ✅ 1.5 |
Código: `data/Vault.kt` (cofre), `data/BackupCrypto.kt` (backup com senha e `BackupProtection`), `data/EngineStore.kt`
(o que o motor guarda), `ui/screens/SecurityUi.kt`. Testes: `EngineSecurityTest`, `PrdConformanceTest`.

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-SEG-08 | **Banco de dados**: fica na área privada do app, protegida pela criptografia do próprio Android e fora do backup do Android (RNF-PRIV-03). Criptografia extra do banco inteiro (SQLCipher) avaliada e **deixada de fora** por enquanto: biblioteca nativa de vários MB que o F-Droid não compila do código-fonte, o que quebraria a compilação reproduzível (RNF-REP-01). | Decisão registrada (CLAUDE.md, seção 13). | ⛔ |
| RF-SEG-09 | **Rust**: avaliado a pedido do Hanry (07/11/2026). Não entra agora: o Kotlin já protege a memória, Rust não impede vazamento de dados, e código nativo aumentaria o APK (uma cópia por tipo de processador) e complicaria a compilação reproduzível. Faz sentido **no futuro**, se o Cryo ganhar versões para outros sistemas (computador, iPhone): o motor em Rust seria um só para todos. | Decisão registrada (CLAUDE.md, seção 13). | ⛔ |

### 6.16 Ajustes e categorias — RF-AJU (TELA-AJUSTES, TELA-CATEGORIAS, TELA-CATEGORIA-EDITAR)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-AJU-01 | Perfil | Nome (como quer ser chamado). | ✅ 1.0 |
| RF-AJU-02 | Aparência | Tema claro/escuro/do sistema; cores do papel de parede (Material You). | ✅ 1.0 |
| RF-AJU-03 | Privacidade | Bloqueio, esconder valores. | ✅ 1.0 |
| RF-AJU-04 | Lembretes | Ligar/desligar avisos de contas. | ✅ 1.0 |
| RF-AJU-05 | Registro rápido | Conta principal. | ✅ 1.0 |
| RF-AJU-06 | Categorias | Despesas e receitas: nome, ícone, cor, palavras-chave do registro rápido; arquivar. | ✅ 1.0 |
| RF-AJU-07 | Sobre | Versão, código-fonte, licença AGPL v3 ou posterior, aviso de garantia. | ✅ 1.0 · licença ✅ 1.4 |
| RF-AJU-08 | Modo desenvolvedor | 7 toques na versão (RF-DEV). | ✅ 1.3 |
| RF-AJU-09 | Idioma | Ajustes › Idioma: "Do celular" (padrão) ou uma das 10 línguas; muda na hora (RF-LNG-02). | ✅ 1.4 |
| RF-AJU-10 | Moeda | Ajustes › Moeda: lista de moedas; a do país do celular vem marcada (RF-MOE-02). | ✅ 1.4 |
| RF-AJU-12 | **Apoiar o Cryo** | Ajustes › Sobre › "Apoiar o Cryo" abre o Buy Me a Coffee no navegador (o app continua **sem permissão de internet**: quem abre a página é o navegador). Nada é cobrado, nada é pedido dentro do app e não há lembrete: a linha fica quieta no fim dos Ajustes. Também no fim dos READMEs (10 línguas) e no campo `Donate:` da receita do F-Droid. | Tocar abre o navegador; o APK continua sem `INTERNET`. | ✅ 1.5 |
| RF-AJU-11 | Como usar o Cryo | Ajustes › Sobre › "Como usar o Cryo" abre o passeio animado (RF-TOUR-03). | ✅ 1.4 |

### 6.17 Primeiro uso — RF-BOA (TELA-BOAS-VINDAS)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-BOA-01 | Boas-vindas | O que o Cryo faz e que os dados ficam no celular. Na 1.4, a primeira tela mostra a marca (com uma animação curta), o slogan, a língua e a moeda; o que o Cryo faz fica no passeio (RF-TOUR). | ✅ 1.0 · ✅ 1.4 |
| RF-BOA-02 | Nome | Opcional. | ✅ 1.0 |
| RF-BOA-03 | Conta principal | Nome e saldo de hoje (pode ser negativo). | ✅ 1.0 |
| RF-BOA-04 | Dinheiro vivo e cartão | Opcionais. | ✅ 1.0 |
| RF-BOA-05 | Notificações | Pedir permissão (Android 13+). | ✅ 1.0 |
| RF-BOA-06 | Restaurar no primeiro uso | Na primeira tela da apresentação, "Já uso o Cryo: restaurar um backup" abre o arquivo, mostra o que tem nele e restaura, pulando o resto da apresentação. | ✅ 1.4 |
| RF-BOA-07 | Língua e moeda no primeiro uso | A apresentação mostra a língua e a moeda escolhidas pelo celular e deixa trocar ali mesmo (RF-LNG-02, RF-MOE-02). | ✅ 1.4 |
| RF-BOA-08 | Ordem da apresentação | Boas-vindas (nome, língua, moeda, restaurar) › passeio (RF-TOUR-02) › conta principal › cartão › tudo pronto. A barra de progresso no topo anda com suavidade (DS-MOV-09). | ✅ 1.4 |

### 6.17a Como usar o Cryo — RF-TOUR (TELA-TOUR, TELA-BOAS-VINDAS)

Pedido do Hanry (05/11/2026): uma explicação animada de como usar o app, para quem nunca ouviu falar do Cryo.

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-TOUR-01 | **Passeio animado** | 5 páginas, uma ideia por página, cada uma com título, texto curto e uma animação que mostra a função funcionando: (1) registrar por frase (a frase é digitada e vira um lançamento); (2) previsão do mês (a linha se desenha até o fim do mês); (3) cartões e parcelas (uma compra em 10x se espalha pelas faturas); (4) contas fixas, orçamentos e metas (o lembrete toca, a barra e a meta enchem); (5) privacidade (sem internet, só no celular, bloqueio). | ✅ 1.4 |
| RF-TOUR-02 | No primeiro uso | Depois das boas-vindas (já na língua e na moeda escolhidas) e antes do cadastro da conta. "Pular" vai direto ao cadastro. Quem restaura um backup não passa pelo passeio. | ✅ 1.4 |
| RF-TOUR-03 | De novo quando quiser | Ajustes › Sobre › "Como usar o Cryo" abre o passeio (TELA-TOUR); na última página, "Entendi" volta aos Ajustes. | ✅ 1.4 |
| RF-TOUR-04 | Na língua e na moeda da pessoa | Frases, nomes e valores na língua e na moeda do app, em valores típicos de cada moeda (um almoço: R$ 32,50 · $12.50 · 12,50 € · ¥1,200 · ₩12,000 · ₹250). As frases de exemplo são entendidas pelo registro rápido daquela língua (conferido em teste). | ✅ 1.4 |
| RF-TOUR-05 | Navegar | Deslizar para os lados ou tocar em "Próximo"; pontos mostram a página (o leitor de tela lê "Página 2 de 5"); o Voltar do celular volta uma página (na primeira, volta às boas-vindas ou aos Ajustes). | ✅ 1.4 |
| RF-TOUR-06 | Animação que termina | Cada animação dura até cerca de 3 s, começa quando a página aparece, recomeça ao voltar a ela e para no quadro final: nada fica rodando sem parar. Com "Remover animações" do Android, aparece direto o quadro final (ACS-08). A ilustração é decorativa para o leitor de tela: o título e o texto dizem tudo. | ✅ 1.4 |


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
| RF-LNG-01 | **10 línguas** | Português do Brasil (base), inglês, espanhol, coreano, italiano, alemão, hindi, mandarim (chinês simplificado), japonês e francês. | ✅ 1.4 |
| RF-LNG-02 | Qual língua | Segue a língua do celular; se ela não for uma das 10, inglês. Ajustes › Idioma troca na hora, sem fechar o app. No Android 13+, também pela tela de idioma do app do sistema. | ✅ 1.4 |
| RF-LNG-03 | Nenhum texto no código | Todo texto que aparece na tela, em notificações e em mensagens está em `res/values*/strings.xml` (base em `values/`, pt-BR). | ✅ 1.4 |
| RF-LNG-04 | Tradução completa | Toda chave existe nas 10 línguas, com os mesmos marcadores (`%1$s`...) e plurais pelas regras de cada língua. | ✅ 1.4 |
| RF-LNG-05 | Datas do país | Nomes de dias e meses e a ordem da data no jeito da língua e do país (5 de outubro · October 5 · 10月5日...). | ✅ 1.4 |
| RF-LNG-06 | Categorias na língua de quem instala | As categorias padrão (nomes e palavras-chave do registro rápido) são criadas na língua do app na primeira vez; as de quem já usa não mudam. | ✅ 1.4 |
| RF-LNG-07 | Loja nas 10 línguas | Título, descrições e novidades do F-Droid (`fastlane/metadata/android/<língua>/`). | ✅ 1.4 |
| RF-LNG-08 | Revisão por quem fala | Cada tradução revista por alguém que fala a língua (comunidade, por issue). | 🔵 contínuo |
| RF-LNG-09 | **Página do GitHub nas 10 línguas** | `README.md` em **inglês** (a página principal do repositório), com uma linha de línguas no topo levando às outras 9: `docs/readme/README.<língua>.md` (pt-BR, es, fr, de, it, ja, ko, zh-CN, hi). Todas com as mesmas seções e as imagens das telas na própria língua (RF-LNG-10). Os documentos de trabalho (PRD, CLAUDE.md, VISUAL.md, IMPORTACAO.md) continuam em português, e o README avisa isso. As notas de cada Release no GitHub saem em inglês e em português (PRC-05). Pedido do Hanry (05/11/2026). **Tudo o que aparece na página inicial é em inglês** (06/11/2026): `CHANGELOG.md` em inglês, com a cópia em português em `docs/CHANGELOG.pt-BR.md` (as duas com a seção da versão, conferido), e as mensagens de commit (PRC-02). | ✅ 1.4 |
| RF-LNG-10 | Imagens das telas em cada língua | `fastlane/metadata/android/<língua>/images/phoneScreenshots/` nas 10 línguas, com o visual da versão, geradas por `StoreAssetsTest` (`-PstoreAssets=true`) com os dados de exemplo na língua e na moeda de cada país. Servem à loja (F-Droid) e ao README da língua. | ✅ 1.4 |

### 6.19b Moeda — RF-MOE

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-MOE-01 | **Uma moeda para o app todo** | Todos os valores na moeda escolhida (decisão em ESC-04). | ✅ 1.4 |
| RF-MOE-02 | Qual moeda | Padrão: a do país do celular (en-US → dólar, en-GB → libra, es-MX → peso mexicano, es-ES → euro, ko → won, hi → rúpia, zh → yuan, ja → iene...); se o país não tiver uma moeda da lista, a do país principal da língua. Ajustes › Moeda troca. | ✅ 1.4 |
| RF-MOE-03 | Trocar não converte | Trocar a moeda muda só o símbolo e o formato: os números continuam os mesmos (sem internet não há cotação). Um aviso explica isso antes de trocar. | ✅ 1.4 |
| RF-MOE-04 | Formato do país | Símbolo, posição do símbolo, separadores e casas decimais pela língua e pela moeda (R$ 1.234,56 · $1,234.56 · 1.234,56 € · ¥1,235 · ₩1,235 · ₹1,234.56). Iene, won e outras sem centavos aparecem sem casas. | ✅ 1.4 |
| RF-MOE-05 | Digitar valor | O campo estilo caixa eletrônico respeita as casas da moeda (no iene, cada dígito é 1 iene). | ✅ 1.4 |
| RF-MOE-06 | Valores resumidos | Nos gráficos, no jeito de cada língua: R$ 1,2 mil · $1.2K · 1,2 Tsd. € · ¥1.2万 · ₹1.2 लाख. | ✅ 1.4 |
| RF-MOE-07 | Lista de moedas | Real, dólar americano, euro, libra, dólar canadense, dólar australiano, peso mexicano, peso argentino, peso chileno, peso colombiano, sol peruano, peso uruguaio, franco suíço, won, rúpia indiana, yuan, iene, novo dólar taiwanês, dólar de Hong Kong, dólar de Singapura. | ✅ 1.4 |

### 6.19 Notificações — RF-NOT

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-NOT-01 | Contas e parcelas | Diário ~9h; toque abre a tela certa nos dados de verdade. | ✅ 1.0 |
| RF-NOT-02 | Problema no backup | RF-BKP-10. | ✅ 1.2 |
| RF-NOT-03 | Pouca informação | A notificação mostra só o nome e o valor da conta ou parcela, nunca saldos; agendar lembretes nunca impede o app de abrir. | ✅ 1.0 |
| RF-NOT-04 | Notificações especiais e sons | Detalhado na seção 6.19c (RF-AVI, RF-SOM, RF-MOM, RF-ILU). | ✅ 1.5 |

### 6.19c Avisos, sons, momentos e alcance — 1.5

Pedido do Hanry (09/11/2026): "sons, notificações, alertas de passando do limite (orçamento), notificações push animadas do tipo
Duolingo onde a cada situação ele manda um alerta diferente"; e "usar uma técnica adotada pela One UI, colocar o botão de voz em
baixo, alcançável pelos dedos... deixar tudo mais acessível e cômodo". Protótipos para aprovação em `Proposal17Test` (imagens
63–68, quadros com `-PtourGif=true`) e o kit de sons para ouvir.

**Decisões do Hanry (09/11/2026):** barra com o botão de voz aprovada; tom **Amigo**; sons aprovados; "remodele o banco de
dados para ter dinamismo" e pode haver um **painel geral de gastos** (RF-ORC-04/06); **sem mascote**; lembrete do dia **ligado,
mas o app pergunta antes o horário** (RF-AVI-11); **nenhum emoji de teclado** no app: imagens próprias, bonitas e no estilo do
Cryo (RF-ILU); estudar a fundo as notificações animadas e "ao vivo" e aplicar o que der (RF-AVI-09/10).

**O que o Duolingo faz bem, e como fica no Cryo:** (1) cada situação tem um aviso diferente e com personalidade, nunca o mesmo
texto; (2) o aviso chega na hora certa (perto do que importa, não ao acaso); (3) dá para resolver na própria notificação; (4)
sequências e conquistas viram comemoração. **O que o Cryo não copia:** culpa e insistência. Dinheiro já é assunto sensível: o
tom é de amigo que ajuda, nunca de cobrança, e o app avisa pouco (regras anti-chatice abaixo). **"Push" sem internet:** o Cryo
não tem servidor; todo aviso é montado e agendado no próprio celular (WorkManager), com os dados que só existem ali.

**Avisos inteligentes — RF-AVI**

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-AVI-01 | **Um aviso para cada situação**, todos calculados no aparelho: orçamento em 80% e passou do limite (RF-ORC-04/05); **ritmo** ("no ritmo atual, Transporte passa do limite por volta do dia 24"); **fatura fecha amanhã** ("compras a partir de amanhã só vencem no mês seguinte: bom dia para a compra grande"); conta vence e venceu (RF-NOT-01, com texto novo); **dia de salário** (recorrência do motor: "hoje costuma cair o salário; já entrou?"); **salário atrasado**; **assinatura mais cara** e **cobrança repetida** (RF-MOT); **gasto fora do padrão**; **saldo vai ficar negativo** (projeção do motor); **meta quase lá** e concluída; **última parcela** de uma compra; **resumo da semana** (domingo à noite) e **fechamento do mês** (dia 1); **nota de saúde** que subiu; **lembrete do dia** se nada foi anotado (com "dia sem gastos"). | Um teste por situação (quando dispara e quando não). | ✅ 1.5 |
| RF-AVI-02 | **Vários textos por situação** (3 no tom Amigo, em cada língua), sorteados sem repetir o último; **tom** em Ajustes: **Amigo** (padrão, escolha do Hanry: "Ainda é dia 18. Segurando o delivery até sexta, você fecha o mês no azul") ou Discreto ("Alimentação: 80% do limite"). Sem emoji (RF-ILU): a imagem do aviso é uma ilustração própria. Nunca culpa, nunca ironia com dinheiro. | Textos nas 10 línguas; `PrdConformanceTest` (chaves, marcadores, nenhum emoji). | ✅ 1.5 |
| RF-AVI-03 | **Resolver na notificação**: "Já paguei" (marca a conta como paga), "Lembrar amanhã", "Dia sem gastos ✓", "Registrar salário" (com o valor de sempre) e "Anotar" (abre a voz). Ações que mudam dados pedem desbloqueio quando o bloqueio está ligado. | `AlertActionsTest` (Já paguei uma vez só, Dia sem gastos, Registrar salário, dispensar; "Já paguei" pede o desbloqueio e "Lembrar amanhã" não). | ✅ 1.5 |
| RF-AVI-04 | **Regras anti-chatice**: no máximo **1 aviso comum por dia** (contas e dívidas não contam), escolhido pela importância (RF-MOT-11); os avisos só chegam em **dois horários**: de manhã (~9h: contas e o aviso comum do dia) e no horário do lembrete do dia (o lembrete ou o resumo de domingo); por isso não há "horário silencioso" separado (decisão da construção: dois horários fixos já deixam a madrugada livre); o mesmo tipo no máximo 1 vez por semana (orçamento: 1 vez por faixa por mês); tipo dispensado 3 vezes seguidas passa a esperar o dobro; nos primeiros dias de uso (sem movimentações de 7 dias atrás), só as contas. | `AlertPlannerTest` (regras com dados inventados e um relógio fixo). | ✅ 1.5 |
| RF-AVI-05 | **Privacidade**: na tela de bloqueio só "Cryo · 3 avisos novos / Desbloqueie para ver", sem nome nem valor (RF-SEG-05); nada sai do celular; nada novo guardado além de quando cada tipo foi mostrado e dos dias sem gastos confirmados (estes entram no backup). | `AlertActionsTest` (a versão da tela de bloqueio não tem nome nem valor). | ✅ 1.5 |
| RF-AVI-06 | **Canais do Android**: Contas e dívidas, Orçamento, Dia a dia, Conquistas, Resumos, Lembrete do dia e Compras ao vivo, cada um com o seu som (RF-SOM) e importância; a pessoa pode mudar cada canal nos ajustes do próprio Android. O canal antigo de lembretes (1.0) é trocado pelos novos. | Revisão. | ✅ 1.5 |
| RF-AVI-07 | **Gráfico no aviso**: o resumo da semana traz o gráfico desenhado no aparelho com os dados da pessoa (barras dos 7 dias e a linha do normal), como imagem grande (estilo "BigPicture"). Fica parado: o Android não anima esse estilo (RF-AVI-09). O fechamento do mês vem com a figurinha animada (calendário com estrela), como os outros avisos. | `AlertPlannerTest` (os 7 dias do resumo de domingo). | ✅ 1.5 |
| RF-AVI-09 | **Aviso animado** (estudo de 09/11/2026 no código do Android): o SystemUI só dá "play" sozinho em imagem animada dentro de uma notificação **no estilo de conversa** (`MessagingStyle`, como uma mensagem com foto), quando a cortina está aberta ou o aviso aparece por cima da tela, e para quando fecha (não gasta bateria); na imagem grande (`BigPictureStyle`) ele carrega a animação mas não dá "play". Por isso os avisos com momento (meta, sequência, limite, fatura, salário, fechamento do mês...) vêm como **uma mensagem do Cryo com uma figurinha animada** (WebP animado da ilustração da situação, RF-ILU), servida ao sistema por um `FileProvider` do próprio app (o Android dá a permissão de leitura sozinho). Android 8–11 e celulares que não animarem mostram a figurinha parada, já bonita no primeiro quadro. Não usamos `RemoteViews` com `ViewFlipper` (o Google desaconselha e cada fabricante desenha diferente). | `AlertActionsTest` (a notificação usa `MessagingStyle` com a imagem `image/webp` e o endereço do `FileProvider`). Falta a conferência no A55 (o que a Samsung faz com a animação). | 🟡 1.5 |
| RF-AVI-10 | **Ao vivo: modo compras** (Live Update do Android 16 e "Now Bar" da One UI 8): o Google só deixa "ao vivo" o que a pessoa começou, que está acontecendo agora e tem fim (navegação, corrida, entrega); avisos, lembretes e propaganda **não podem**. O uso que cabe no Cryo: **"Vou às compras: até R$ 300"**. Enquanto a sessão dura, uma notificação fixa mostra a barra do que já foi gasto (verde até 80%, âmbar, vermelho; marcas em 80% e 100%), o carrinho andando na barra, e o quanto falta no chip da barra de status e na Now Bar; botões "Anotar" (abre a voz) e "Encerrar". Termina ao tocar em "Encerrar", à meia-noite ou depois de 6 h. No Android 16+: `ProgressStyle` com pedido de promoção (`setRequestPromotedOngoing`, permissão `POST_PROMOTED_NOTIFICATIONS`, que não pede nada à pessoa); antes disso: notificação fixa com barra de progresso. | `MomentsTest` (soma só o que veio depois, termina à meia-noite ou em 6 h); imagens 78 e 79. Falta a conferência no A55 (Now Bar da One UI 8). | 🟡 1.5 |
| RF-AVI-11 | **Lembrete do dia**: ligado, mas o Cryo **pergunta antes** o horário ("Quando eu te lembro de anotar?": 12h, 18h, 20h30, 22h ou outro), uma vez, depois da apresentação ou na primeira abertura depois de atualizar; dá para mudar ou desligar em Ajustes › Avisos. Só avisa se nada foi anotado no dia, com "Dia sem gastos" e "Anotar". | `AlertPlannerTest` (o lembrete só sem nada anotado; com a sequência; desligar; o horário novo é guardado); imagem 73. | ✅ 1.5 |
| RF-AVI-08 | **Ajustes › Avisos e sons** (TELA-AVISOS): o tom (Amigo ou Discreto), o horário do lembrete do dia (ou desligado), cada tipo de aviso com liga/desliga e **"Ver exemplo"** (mostra a notificação de verdade, com dados de mentira, na hora), os sons e a nota dos dois horários (RF-AVI-04). Contas e dívidas continuam no mesmo interruptor da 1.0. | Imagem; teste dos exemplos (um por tipo, nada guardado como mostrado). | ✅ 1.5 |

**Sons — RF-SOM**

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-SOM-01 | **Kit de sons próprio**, timbre de "cristal de gelo" (a marca): salvar (um "tim"), entrou dinheiro (duas notas subindo), atenção 80% (duas notas descendo), limite estourado (gelo trincando + nota grave), conquista (arpejo de cristal), sequência (três notas rápidas), notificação (sininho), voz começou/terminou (bip que sobe/desce). Todos **sintetizados do zero** por um script no repositório (nada gravado nem copiado; mesma licença do app); ~80 KB no total. **Revisto antes de sair** (pedido do Hanry, 11/11/2026: "menos cintilante, mais cômodo e discreto, como uma fechadura abafada, sem perder o cristalino"): a base de cada som passou a ser um toque grave e abafado (um corpo que cai de tom e um estalo curto e escuro) e o cristal ficou só como um fio por cima, uma oitava abaixo, sem os modos agudos do vidro. Medido pelo próprio script (`--medir`): o centro do som caiu para cerca da metade e quase nada da energia fica acima de 2 kHz (a faixa que soava estridente). | O script gera os arquivos e a comparação; demonstração enviada ao Hanry. | ✅ 1.5 |
| RF-SOM-02 | **Respeita o celular**: no silencioso ou no vibrar, sem som; volume baixo (a vibração do DS-PROF-07 continua); **Ajustes › Sons do Cryo** liga/desliga; nada toca em repetição; o som nunca é a única informação (texto e vibração junto, ACS). Sem permissão nova. | `AlertActionsTest` (no silencioso e no vibrar nada toca; com o ajuste desligado nada toca). | ✅ 1.5 |

**Momentos animados no app — RF-MOM** (os "popups" do pedido: dentro do app, onde a animação é livre)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-MOM-01 | **Meta concluída**: tela cheia; o anel completa e fica dourado, o ícone da meta salta, cristais de gelo explodem e caem; som "conquista" e vibração; "Ver minhas metas". | Imagem 75; `MomentsTest` (a meta completa ao guardar, e não ao editar). | ✅ 1.5 |
| RF-MOM-02 | **Orçamento ao salvar** (RF-ORC-04): em 80%, um cartão âmbar embaixo com a barra enchendo; ao passar do limite, a barra passa da marca dos 100%, fica vermelha e o cartão treme; som "limite"; sugestão de onde cobrir. | Imagem 74; `MomentsTest` (80%, estourado com a sugestão de onde cobrir, limite remanejado). | ✅ 1.5 |
| RF-MOM-03 | **Sequência de dias anotando** (marcos de 3, 7, 14, 30, 60 e 100 dias): "7 dias seguidos", com a chama de cristal (RF-ILU-02); um **dia sem gastos** confirmado também conta. Perder a sequência não gera bronca: "Recomeçar é parte". | `MomentsTest` (marcos, dia sem gastos conta, o segundo lançamento do dia não repete); imagem 76. | ✅ 1.5 |
| RF-MOM-04 | **Fechamento do mês** (ao abrir o app do dia 1 ao dia 3, uma vez por mês, só com os dados de verdade): "Outubro fechou no azul" com o que sobrou e a ilustração do calendário com estrela; se fechou no vermelho, tom calmo e a ilustração da linha abaixo do zero. **Dívida quitada** e **última parcela** ficam para depois (por enquanto, a mensagem de sempre e o aviso da última parcela). | Imagem 77. | ✅ 1.5 |
| RF-MOM-05 | **Regras**: um momento por vez (o que chega enquanto outro está na tela é ignorado); só depois de salvar ou na abertura do app, nunca no meio da voz, de um formulário ou da importação; tudo finito (≤ 2,6 s, DS-MOV-10); com "Remover animações", aparece direto no quadro final; fecha com um toque ou com o Voltar; o leitor de tela lê o texto. | `MomentsTest` (um por vez); `Screens17Test` (o Voltar fecha). | ✅ 1.5 |

**Orçamento** (complementa a seção 6.9): RF-ORC-04 e RF-ORC-05.

**Ilustrações próprias — RF-ILU** (pedido do Hanry: "nada de usar emojis de teclado, crie do zero as imagens")

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-ILU-01 | **Nenhum emoji de teclado** no app: nem nos textos (`strings.xml` das 10 línguas), nem no código, nem nas notificações. Onde antes havia emoji (metas, "Meta alcançada! 🎉", "Dívida quitada 🎉"), entra uma ilustração própria ou só o texto. | `PrdConformanceTest` falha se achar emoji. | ✅ 1.5 |
| RF-ILU-02 | **Conjunto de ilustrações do Cryo**, desenhadas em código (Compose, sem arquivo de imagem): azulejo com o degradê azul do ícone, fio de luz em cima, sombra suave, desenho em branco e azul-gelo com toque dourado (DS-ICO). **Situações**: orçamento (medidor), limite estourado (gelo trincado), fatura (cartão com calendário), conta (recibo com sino), salário (moedas entrando), dia sem gastos (carteira fechada), sequência (chama de cristal), resumo da semana (barras), fechamento do mês (calendário com estrela), assinatura mais cara (etiqueta com seta), cobrança repetida (dois recibos), fora do padrão (lupa no pico), saldo negativo (linha abaixo do zero), última parcela (blocos com o último marcado), saúde financeira (anel com seta), meta quase lá (alvo), dívida quitada (cadeado aberto), comemoração (estouro de cristais), compras (carrinho). | Imagem com todas, em claro e escuro. | ✅ 1.5 |
| RF-ILU-03 | **Ilustrações das metas** no lugar dos emojis: alvo, casa, avião, carro, formatura, aliança, escudo (reserva), celular, notebook, videogame, praia, bebê, pet, violão, moto, cofre, presente, igreja, academia, livros. As metas que já existem ganham a ilustração do emoji que tinham (DAD-14). | Teste da migração (emoji → ilustração). | ✅ 1.5 |
| RF-ILU-04 | **Figurinhas animadas**: cada ilustração de situação tem um movimento curto que se repete (a chama tremula, as moedas caem, o medidor balança, os cristais brilham), exportado como WebP animado para as notificações (RF-AVI-09). O primeiro quadro já é a ilustração completa. Geradas a partir do mesmo código por um teste (`-Pstickers=true` e `tools/stickers/make_stickers.sh`) e guardadas no app (~25 KB cada). | Os arquivos existem e batem com o código; imagem dos quadros. | ✅ 1.5 |

**Alcance com uma mão — RF-ALC** (o princípio da One UI: em cima é para olhar, embaixo é para tocar)

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-ALC-01 | **Botão de voz no centro da barra de baixo**, em todas as abas: 68 dp, com volume (DS-PROF-05), acima da linha da barra; tocar = falar (abre a tela de eco); **segurar = escrever** (abre a frase com o teclado). As abas ficam 2 de cada lado. | Imagem 79; `Screens17Test` (segurar abre o campo de escrever; o leitor de tela lê "Registrar falando"). | ✅ 1.5 |
| RF-ALC-02 | O microfone sai do campo de frase do Início (fica só para escrever, que é o caso de quem prefere digitar); o atalho "Lançar" continua. | Revisão. | ✅ 1.5 |
| RF-ALC-03 | **Ação principal sempre embaixo**: salvar, confirmar e escolher ficam na faixa de baixo ou em folhas que sobem de baixo (DS-PAD-02), nunca só no topo; o que fica no topo é para ler. Revisão de todas as telas atrás de botão importante na zona difícil. | Revisão (09/11/2026): no topo das telas só ficam ações secundárias ou de cuidado (Editar, Apagar, Importar, Buscar, Mais opções na conferência da importação); salvar, confirmar, remanejar e escolher ficam embaixo (barra de baixo ou folha). As telas novas da 1.5 seguem a regra. | ✅ 1.5 |
| RF-ALC-04 | **Acessível e cômodo**: alvos de toque ≥ 48 dp (o de voz, 68 dp); com a fonte do sistema em 200% nada importante some (os rótulos curtos das abas e dos atalhos crescem até 1,3x, como na One UI, para não quebrar no meio da palavra; todo o resto cresce sem limite); descrições para o leitor de tela em tudo o que é novo. | Imagens 80 e 81 (fonte em 200%); `PrdConformanceTest`. | ✅ 1.5 |

### 6.19d A 1.5 (construída em 12/11/2026) — contas fixas que se acertam com o extrato, perfil e guia

Pedido do Hanry (10/11/2026), em três pontos: (1) "quando importo o extrato, alguns valores ficam estranhos: o Cryo distingue
a Netflix do extrato da Netflix que já é conta fixa?"; (2) "a bolinha do perfil abre os ajustes, mais bonita, com um emoji
escolhido por mim (como a Google Play)"; (3) "o app é cru: e se a pessoa não souber usar? uma assistente local, por algoritmo,
que ensina aos poucos". Tudo continua no aparelho e sem permissão nova. (O banco foi para a versão 5 na reforma das contas fixas, RF-FIX-06..10, que veio depois.)

**O que o código faz hoje (conferido em 10/11/2026), a causa dos "valores estranhos":**

- A conferência da importação (`importer/ImportPlan.kt`, função `plan`) só marca uma linha como repetida quando já existe uma
  movimentação com o **mesmo valor, na mesma conta ou cartão, a até 3 dias** (ou na mesma fatura). **Nada na importação conhece
  contas fixas** (`importer/` e `ui/screens/ImportModel.kt` não citam `bills`).
- "Paga" para o Cryo = existir uma movimentação com `billId` e `billYm` (`data/Repository.kt`, `markBillPaid`; `domain/Ledger.kt`,
  `billStatuses`). A linha importada entra **sem** esse vínculo.
- Por isso, com a conta fixa "Netflix 59,99" e a linha "NETFLIX.COM 59,99" importada: (a) a conta fixa continua **pendente ou
  atrasada** e o lembrete toca; (b) a previsão do mês **conta duas vezes** (o gasto importado e a conta "a pagar":
  `domain/Analytics.kt`, `pendingBillsExpense`); (c) se a pessoa tocar "Já paguei" depois, nasce uma **segunda** movimentação e o
  saldo fica errado. Se ela marcou "Já paguei" **antes** de importar, a linha só é reconhecida se cair na mesma conta/cartão e a
  até 3 dias; conta fixa cadastrada na conta e cobrada no cartão passa em dobro.

**A. Contas fixas e extrato — RF-CFX** (prioridade 1: conserta número errado)

| Código | Requisito | Aceite | Situação |
|---|---|---|---|
| RF-CFX-01 | **Casador** (regra pura, `BillMatcher`, em `domain/BillMatch.kt`: a previsão e os avisos também usam): para cada linha do extrato e cada conta fixa ativa **sem pagamento** no mês (a linha entre 15 dias antes e 25 depois do vencimento, mesmo tipo: saída ou entrada), dá uma nota com três sinais: **nome** (as palavras do nome da conta fixa, normalizadas como em `domain/engine/TxKey.kt`, estão na descrição: "netflix" em "NETFLIX.COM"; ou um apelido aprendido, RF-CFX-04), **valor** (igual até 1% = forte; até 30% de diferença = "a conta varia", como luz e água) e **data** (perto do vencimento). Resultado: CERTA (nome + valor igual; ou apelido aprendido + valor até 30%), PROVÁVEL (nome com valor diferente; ou valor igual e data a até 5 dias, sem nome) ou nada. Cada conta-mês casa com uma linha só (a de maior nota) e cada linha com uma conta só. Parcelas e pagamentos de fatura nunca casam. | Testes do casador: Netflix; luz que varia; duas contas de mesmo valor (o nome decide); aluguel pago 2 dias depois; salário como conta fixa de entrada; parcela "3/10" não casa. | ✅ 1.5 |
| RF-CFX-02 | **Na conferência** (TELA-IMPORTAR): a linha casada mostra "Conta fixa: Netflix (nov)". CERTA já vem marcada como pagamento; PROVÁVEL vem desmarcada, com "Pode ser: Netflix". Tocar na linha abre o editor com a caixa "É o pagamento desta conta fixa". No topo: "3 linhas parecem pagamentos das suas contas fixas". (Trocar por **outra** conta fixa ficou para depois: faz-se em Planejar › Contas fixas.) Ao importar, a movimentação nasce **com** `billId` e `billYm` (nenhuma movimentação a mais) e, sem categoria na linha, herda a da conta fixa. O valor que vale é o do extrato, e **o pagamento fica na conta ou no cartão em que aconteceu** (a do extrato), não na conta padrão da conta fixa (decisão do Hanry, 10/11/2026: "cada Já paguei vale para a sua conta"). | `BillFlowTest`: importar a Netflix deixa a conta fixa paga, uma movimentação só; `BillMatchTest`: a previsão conta uma vez. | ✅ 1.5 |
| RF-CFX-03 | **Já marcada como paga**: se a conta-mês já tem pagamento feito à mão e uma linha casa com ele (valor até 1%, até 7 dias, **em qualquer conta ou cartão**), a linha vem desmarcada, como "parece repetida", com "Você já marcou como paga". Para ficar com a do extrato, a pessoa marca a linha e depois toca "Juntar" em Contas fixas (RF-CFX-05: fica a do extrato, sai a feita à mão). Fecha a brecha de conta fixa na conta e cobrança no cartão. | Teste: paga à mão na conta, extrato do cartão: não entra em dobro. | ✅ 1.5 |
| RF-CFX-04 | **Aprende os apelidos**: quando a pessoa confirma "esta linha é a conta fixa X", o Cryo guarda só o **HMAC** da descrição normalizada (RF-SEG-02, em `data/EngineStore.kt`, como as recorrências escondidas) apontando para a conta fixa. Na próxima vez, casa sozinho mesmo com nome diferente ("DEB AUT CIA ENERG" = "Luz"). "Não é" também é lembrado, no próprio lançamento (`billYm = 0`, DAD-05), para o Cryo não reconhecer sozinho depois. | Teste: confirmar uma vez, importar o mês seguinte: CERTA. Nada legível guardado. | ✅ 1.5 |
| RF-CFX-05 | **Arrumar o que já está no app** ("Organizar contas fixas", regra pura nova `BillReconciler`, olhando o mês atual e os 3 anteriores), no jeito de sugestões de um app de fotos: **Pagamentos encontrados** (conta-mês sem pagamento + movimentação sem vínculo que casa: "É este" ou "Não é", um por um) e **Pagas em dobro** (pagamento feito à mão + outra movimentação igual a até 7 dias: "Juntar", fica a do extrato). Sempre com desfazer; nada muda sem um toque. Entrada: o cartão "Conferir" em Planejar › Contas fixas quando há sugestões (a regra pura mora com o casador, em `domain/BillMatch.kt`). Recorrência sem conta fixa continua em "O que se repete" (RF-MOT-02). | Teste com dados já "sujos" (importados antes da 1.5): as sugestões aparecem, ligar e desfazer voltam ao estado exato. | ✅ 1.5 |
| RF-CFX-06 | **"Já paguei" olha antes de gravar**: ao tocar "Já paguei" (no app), se existe movimentação sem vínculo que casa como CERTA ou PROVÁVEL, o Cryo **liga** em vez de criar ("Achei o pagamento de 05/11 no extrato e liguei a ele", com desfazer). Na notificação (sem tela), liga sozinho só se for CERTA (ERR-045). Desfazer um pagamento que veio do extrato só desliga a conta fixa; apagar o lançamento só é oferecido para o que foi feito à mão (ERR-042). Continua valendo ERR-018 (uma gravação só, em transação). | `BillFlowTest` e `AlertActionsTest`: importar sem ligar, tocar "Já paguei" (na tela ou no aviso): continua uma movimentação. | ✅ 1.5 |
| RF-CFX-07 | **A previsão não conta em dobro nem antes de a pessoa confirmar**: uma conta-mês com casamento CERTO ainda não ligado é tratada como paga nas previsões (mês, categorias, próximos meses) e nos avisos de vencimento. | Teste: Netflix importada sem ligar + conta fixa pendente = a previsão conta 59,99 uma vez; nenhum aviso de "vence amanhã". | ✅ 1.5 |
| RF-CFX-08 | **Criar a conta fixa depois** (decisão do Hanry, 10/11/2026): ao criar ou editar uma conta fixa, o Cryo procura nos lançamentos que **já existem** (mês atual e 3 anteriores, em qualquer conta ou cartão) os que casam e pergunta, um por mês: "Netflix 79,99 em 05/11, na conta Nubank (débito): é esta conta fixa?". Dizer que sim liga o lançamento como o pagamento daquele mês (nada é criado nem movido de conta); dizer que não é lembrado (RF-CFX-04). | Teste: lançamento importado em outubro e novembro, conta fixa criada depois: as duas perguntas aparecem, os dois meses ficam pagos, o saldo não muda. | ✅ 1.5 |

**B. Perfil — RF-PRF**

| Código | Requisito | Aceite | Situação |
|---|---|---|---|
| RF-PRF-01 | **A bolinha é sua**: letra do nome (como hoje), um **desenho do Cryo** (os das metas, `GoalArt`), **um emoji do teclado escolhido pela pessoa** ou **uma foto dela** (decisão do Hanry, 10/11/2026), sobre uma cor da paleta. A foto vem do seletor de fotos do Android (**sem permissão nova**: o seletor entrega só a foto escolhida), é cortada em círculo, guardada pequena (256 px) na área privada do app, fica **só neste aparelho** (fora do backup) e pode ser removida. O emoji aqui é **dado da pessoa** (como o nome de uma categoria): o app continua sem emoji nas suas próprias telas e textos (RF-ILU; `PrdConformanceTest` olha o código e os textos, não os dados). Vale um caractere só (um "grafema"). Guardado nos ajustes (`avatar`: vazio, desenho, emoji ou foto; `avatarColor`). | Imagem do Início com cada um dos quatro; sem a foto (aparelho novo, backup restaurado) volta à letra; emoji de pele e bandeira (vários códigos) aparece inteiro. | ✅ 1.5 |
| RF-PRF-02 | **A bolinha abre o perfil**: no cabeçalho do Início (`ui/components/Sections.kt`, `HomeHeader`) a engrenagem sai e a bolinha vira botão (área de toque de 48 dp, descrição "Perfil e ajustes"). O olho de esconder valores continua. | Teste: tocar a bolinha abre o perfil; leitor de tela lê a descrição. | ✅ 1.5 |
| RF-PRF-03 | **Painel do perfil** (tela nova, rota `profile`, TELA-PERFIL), no jeito do menu de conta de uma loja de apps: no alto, a bolinha grande, "Olá, nome" e "Editar perfil"; uma linha de atalhos (esconder valores, tema claro/escuro/do sistema); logo abaixo, **Todos os ajustes** (a tela de sempre, `ui/screens/SettingsScreens.kt`, inteira: nada some; fica à vista sem rolar, porque antes os Ajustes estavam a um toque do Início); depois cartões agrupados (DS-PROF) com ícone, título e **a situação em uma linha**: Backup automático (o resumo dele, ou "desligado"; em âmbar se há problema), Bloqueio, Avisos e sons, Importar extrato; e por fim **Aprender o Cryo ("7 de 18")** e Como usar o Cryo. Idioma e moeda e Sobre continuam em Todos os ajustes. | Imagens claro e escuro (86 a 88); a bolinha do Início abre o painel; cada linha abre a tela certa. | ✅ 1.5 |
| RF-PRF-04 | **Editar perfil** (folha de baixo): nome e a bolinha (Letra · Desenhos · Emoji · Foto) com as cores. | Teste: trocar e ver no Início na hora; nome vazio volta à pessoa anônima. | ✅ 1.5 |

**C. Guia — RF-GUI ("Aprender o Cryo": dicas no ritmo de quem usa, por regras, sem IA)**

| Código | Requisito | Aceite | Situação |
|---|---|---|---|
| RF-GUI-01 | **O que o guia sabe**: quase tudo sai dos próprios dados e ajustes, sem vigiar nada (tem orçamento? conta fixa? meta? já importou? backup ligado? bloqueio?). Só o que não dá para deduzir é contado, e só o que alguma lição precisa: **11 contadores** (`GuideCount`: usou a frase, usou a voz, abriu a edição, as categorias, o painel do mês, as dívidas, Análises, a saúde financeira, o que se repete, a importação, o modo compras) e, por lição, quantas vezes apareceu, em que dia e se foi vista. Fica num arquivo só do aparelho (`GuideStore`, em `data/GuideStore.kt`, no molde de `notify/AlertState.kt`): nunca valores, nomes ou descrições; fora do backup e da troca de celular; nada em logs. Os fatos (`GuideFacts`) são só quantidades e sim/não. O modo de teste não conta nem recebe dicas. | Teste: o arquivo só tem chaves de lição/tela, números e datas. | ✅ 1.5 |
| RF-GUI-02 | **Lições** (regra pura `GuidePlanner`, em `domain/Guide.kt`, no molde de `notify/AlertPlanner.kt`): **18**, em 6 degraus: **Começar** (frase; voz: tocar fala, segurar escreve; editar e apagar no Extrato), **Organizar** (categorias; cartão: a fatura e o pagamento; contas fixas com lembrete), **Planejar** (orçamentos; painel do mês e remanejar; metas; dívidas e plano), **Entender** (previsão com faixa; saúde financeira; o que se repete), **Proteger** (backup automático; bloqueio), **Ir além** (importar extrato; modo compras; investimentos e "Atualizar valor"). Esconder valores, transferir, calendário e backup com senha ficaram de fora desta primeira lista (cabem depois, uma linha cada). Cada lição tem: como saber que a pessoa **já sabe** (aí é dada como feita, sem aparecer), quando ela é **útil agora**, a tela e o ponto que ela mostra, e o texto (título e uma ou duas frases). | Um teste por lição: aparece na hora certa e some quando a pessoa já faz sozinha. | ✅ 1.5 |
| RF-GUI-03 | **Nunca chato** (o espírito de RF-AVI-02): no máximo **1 dica por dia e 3 por semana**; nada antes do primeiro lançamento nem nos 2 primeiros minutos (o dia zero é do passeio, RF-TOUR); nunca por cima de uma folha, de um momento (RF-MOM), do bloqueio, da apresentação ou no mesmo dia de um momento; a lição que apareceu e não foi vista ("Agora não", Voltar ou o app fechado) volta 7 dias depois, uma vez; na segunda, só fica na lista; três "Agora não" seguidos pausam o guia 14 dias ("Me mostra" zera a conta); a dica conta para o dia **na hora em que aparece**; chave **Dicas do Cryo** na tela Aprender o Cryo (ligada no começo). As dicas só aparecem nas quatro abas. | Testes de cada limite, com datas inventadas. | ✅ 1.5 |
| RF-GUI-04 | **Qual lição vem agora**: a de menor degrau ainda não feita, com pontos a mais para o que os dados pedem (mais de 30 lançamentos e backup desligado: **Proteger passa na frente**; 10 lançamentos sem categoria; cartão cadastrado sem fatura paga pelo app; 3 meses de histórico: previsão; recorrências sem conta fixa; investimento sem atualização há 60 dias) e para a lição cujo ponto está **na tela em que a pessoa está**. Mesmo estado, mesma lição (sem sorteio). | Teste: `next(fatos, estado, hoje, tela)` é pura e determinística. | ✅ 1.5 |
| RF-GUI-05 | **Como aparece**: um cartão que sobe acima da barra de abas (não é diálogo, não trava a tela), com o ícone do assunto, título, texto curto e os botões **Me mostra** e **Agora não**; Voltar fecha. "Me mostra" abre a tela certa e, quando o ponto é uma peça fixa da tela (o campo de frase, o botão de voz: `Modifier.guideTarget`), destaca-o: o resto escurece e um anel o contorna; um toque ou Voltar fecha. Se o ponto não está à vista, o destaque desiste em 1,5 s (nunca aparece depois, do nada). Movimento finito (DS-MOV-10) e respeitando "Remover animações"; o leitor de tela lê o cartão sem roubar o foco. O anfitrião segue o molde do `MomentHost` (`ui/components/Moments.kt`). | Imagens 89 (cartão) e 90 (destaque); `GuideFlowTest`: Voltar fecha; bloqueado, o app nem é composto (ERR-002). | ✅ 1.5 |
| RF-GUI-06 | **Aprender o Cryo** (tela nova, rota `learn`, TELA-APRENDER), pelo perfil e por Ajustes › Sobre: um anel "Você já conhece 7 de 18", os 6 degraus com o que já foi feito, e tocar numa lição a mostra de novo. | Imagem 91; tocar numa lição a mostra de novo e a dá como vista; a aba Início não reabre a lista depois. | ✅ 1.5 |
| RF-GUI-07 | **Nos testes o guia nasce desligado** (como a pergunta do lembrete: o `TestApp` desliga, e os fluxos com o app de verdade, `AppFlowTest` e `DevModeFlowTest`, também), para as outras ~900 provas não ganharem um cartão por cima. Textos nas 10 línguas; nunca se apresenta como "IA". | A suíte inteira continua passando sem mudar os testes antigos. | ✅ 1.5 |

**Ordem de construção**: (1) RF-CFX-01, 07 e 02 (o número certo primeiro), depois 03, 06, 08, 05 e 04; (2) RF-PRF; (3) RF-GUI
(por último, porque ensina também as telas novas). Versão 1.5, `versionCode` 10. Cada defeito fechado no caminho entra em
`docs/ERROS.md` com a sua guarda (previsão em dobro; "Já paguei" depois de importar; conta na conta e cobrança no cartão).
**Decisões do Hanry (10/11/2026)**: a bolinha pode ser letra, desenho, emoji **ou foto própria**; o pagamento vale para a conta em que
aconteceu, e criar a conta fixa depois pergunta se os lançamentos já existentes são ela (RF-CFX-08); o guia segue o padrão
(no máximo 1 dica por dia).

### 6.20 Widgets — RF-WID

| ID | Requisito | Critério de aceite | Status |
|---|---|---|---|
| RF-WID-01 | Widgets na tela inicial | Detalhar com o Hanry (quais, que dados, como respeitar "esconder valores" e o bloqueio). | 🔵 depois da 1.5 |

---

## 7. Requisitos não funcionais (RNF)

| ID | Requisito | Como se confere | Status |
|---|---|---|---|
| RNF-PRIV-01 | **Sem permissão de INTERNET.** Permissões declaradas: só `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, a partir da 1.5 `RECORD_AUDIO` (registro por voz no aparelho, RF-VOZ-02; pedida só no primeiro toque no microfone, com explicação) e, a partir da 1.5, `POST_PROMOTED_NOTIFICATIONS` (o modo compras "ao vivo", RF-AVI-10; permissão comum, que não pergunta nada e não tem relação com rede). As demais vêm das bibliotecas de biometria e de agendamento. | `PrdConformanceTest` | ✅ |
| RNF-PRIV-02 | Sem bibliotecas de analytics, anúncios ou crash reporting. | Revisão de dependências (PRC-07) | ✅ |
| RNF-PRIV-03 | `android:allowBackup="false"`; transferência entre aparelhos sem permissões do backup e sem o modo de teste. | `PrdConformanceTest` + revisão | ✅ |
| RNF-PRIV-04 | Nunca colocar extratos reais no repositório (nem em testes). | `PrdConformanceTest` (sem `.ofx/.csv/.xlsx/.pdf` versionados) | ✅ |
| RNF-SEG-01 | Chave de assinatura nunca no git (`keystore/` no `.gitignore`). | `PrdConformanceTest` | ✅ |
| RNF-SEG-02 | Bloqueio conforme RF-BLQ. | `DevModeFlowTest` | ✅ |
| RNF-REP-01 | **Compilação reproduzível**: duas compilações do mesmo commit dão o mesmo APK; JDK 21; sem `vcsInfo`, `dependenciesInfo` e ART profile. | Compilar duas vezes de clones limpos (PRC-06) | ✅ |
| RNF-LIC-01 | **Licença AGPL-3.0-or-later**: `LICENSE` com o texto oficial; cabeçalho SPDX em todo `.kt`. | `PrdConformanceTest` | ✅ 1.4 |
| RNF-COMP-01 | Android 8 (API 26) em diante; alvo API 36. | `app/build.gradle.kts` | ✅ |
| RNF-COMP-02 | Funciona sem serviços do Google. | Revisão de dependências | ✅ |
| RNF-DES-01 | O app abre mesmo se lembretes ou backup falharem (`runCatching`). | Revisão | ✅ |
| RNF-DES-02 | Listas longas sem travar (LazyColumn); importação de milhares de linhas fora da linha principal. | Testes de importação | ✅ |
| RNF-ACS-01 | Acessibilidade conforme ACS-01..07. | Revisão + testes de tela | ✅ |
| RNF-IDI-01 | Interface nas 10 línguas e na moeda escolhida (RF-LNG, RF-MOE); até a 1.3, só português e reais. | `PrdConformanceTest` (traduções completas, sem texto no código) | ✅ 1.4 |
| RNF-TAM-01 | APK pequeno: sem bibliotecas pesadas (PDF pelo leitor do Android). | Tamanho do APK no PRC-06 (~3 MB na 1.3) | ✅ |

## 8. Regras de dados (DAD)

| ID | Regra |
|---|---|
| DAD-01 | Dinheiro sempre em **centésimos da unidade da moeda (`Long`)**, em qualquer moeda; nunca `Double` para guardar valor. Em moedas sem centavos (iene, won), os valores são múltiplos de 100 e aparecem sem casas. Trocar a moeda não muda os números guardados (RF-MOE-03). |
| DAD-02 | Datas em `LocalDate`; mês de fatura como `Int` `AAAAMM` (`YearMonth.key()`). |
| DAD-03 | Mudança no banco = nova versão do Room, migração e teste em `MigrationTest`; schema exportado em `app/schemas/` (conferido por `PrdConformanceTest`). Banco atual: versão 4 (1.5). |
| DAD-04 | Tipos de movimentação: `EXPENSE`, `INCOME` (no cartão = estorno), `TRANSFER`, `CARD_PAYMENT` (com `invoiceYm`), `INVEST_IN`/`INVEST_OUT`, `GOAL_IN`/`GOAL_OUT`, `DEBT_IN` (não é receita). |
| DAD-15 | Conta fixa: `every` (de quantos em quantos meses vence), `installments` (parcelas mensais de cada ciclo, com `amount` sendo o valor de **cada** uma), `cashAmount` (o preço à vista, para mostrar os juros) e `endYm` (último mês, 0 = sem prazo). Quem vem da 1.4 fica com 1, 1, 0 e 0: a conta mensal de sempre. As regras ficam em `BillCycle` (`domain/Ledger.kt`). |
| DAD-05 | Cartão: cada compra guarda `invoiceYm`; compra no dia do fechamento ou depois vai para a fatura seguinte (`CardMath.invoiceFor`); parcelado = uma movimentação por parcela, ligadas por `installmentGroup`. O `invoiceYm` guardado **manda**: a regra da data só vale para lançamento novo ou quando a data ou o cartão mudam (RF-CAR-10); estorno é `INCOME` com `cardId` e `invoiceYm`, sem conta. Conta fixa: "paga" = movimentação com `billId` e `billYm`; `billYm = 0` sem `billId` quer dizer "a pessoa disse que não é conta fixa" (RF-CFX-04; sem migração, vai no backup). |
| DAD-06 | Todas as contas (saldo, fatura, previsão, dívidas) saem do `Ledger`, nunca de consultas soltas. |
| DAD-07 | Dados de verdade (`CryoApp.real`) e de teste nunca se misturam (RF-DEV-03, RF-BKP-11). |
| DAD-08 | Datas sem ano: ano da data completa mais recente do arquivo, voltando um ano se cairiam mais de 31 dias depois. |
| DAD-09 | Chave de importação: `a<conta>|<formato>.id:<FITID>` ou `c<cartão>|<formato>.h:<hash>`, `#n` para linhas repetidas. |
| DAD-10 | Backup JSON `{"app":"Cryo","version":2,...}`; backups de todas as versões abrem (`Backup.import`); campo novo só aparece quando usado. |
| DAD-12 | **Remanejamento do orçamento só no mês** (1.5): tabela `budget_adjustments` (`id`, `ym` AAAAMM, `categoryId`, `amount` em centavos, positivo ou negativo, `moveGroup` ligando a saída e a entrada de um remanejamento). Limite do mês = limite da categoria + soma dos ajustes do mês (nunca menos que zero). Entra no backup como `budgetAdjustments`. |
| DAD-13 | **Dias sem gastos** (1.5): tabela `no_spend_days` (`date` como chave). Contam na sequência (RF-MOM-03); um gasto anotado depois naquele dia vale mais que a marca. Entra no backup como `noSpendDays`. |
| DAD-14 | **Ilustração da meta** (1.5): coluna `goals.art` (chave da ilustração, ex.: `beach`); a migração 3→4 preenche pelo emoji antigo, que continua guardado só para backups lidos por versões antigas. |
| DAD-11 | Backup protegido (RF-SEG-03): envelope `{"app":"Cryo","version":3,"exportedAt":…,"cipher":"AES-256-GCM","kdf":"PBKDF2-HMAC-SHA256","iterations":…,"salt":…,"iv":…,"data":…}`; `data` = JSON do backup compactado (gzip) e trancado; o começo legível entra no GCM como dado associado (mexer nele também é recusado). A versão 3 faz a 1.4 dizer "backup de uma versão mais nova". |

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
| DS-DIR-01 | **Seções, não blocos**: conteúdo em seções planas separadas por uma linha fina; cartões (fundo próprio) só para o que é destaque ou aviso. **Na 1.5**: as seções viram cartões com profundidade sobre um fundo agrupado (DS-PROF-02). | ✅ 1.4 · revisto 1.5 |
| DS-DIR-02 | **Cor com propósito**: a cor do Cryo no cabeçalho do Início, nos atalhos e nos destaques; verde e vermelho só para entrada, saída e alerta. | ✅ 1.4 |
| DS-DIR-03 | **Atalhos à mão**: as ações mais usadas em círculos no topo do Início. | ✅ 1.4 |
| DS-DIR-04 | **Uma pergunta por seção**: cada seção responde uma pergunta ("Como está o mês?") e o "›" leva ao detalhe. | ✅ 1.4 |
| DS-DIR-05 | Todas as telas na direção 2.0 (piloto do Início aprovado em 04/11/2026). | ✅ 1.4 |

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
| DS-COMP-03 | `MoneyText` | Qualquer valor na moeda do app. Com `animate = true`, conta até o novo valor quando ele muda (DS-MOV-05). | ✅ · conta ✅ 1.4 |
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
| DS-COMP-14 | **`CryoSection`** | Seção plana: título `titleMedium` + "›" (a seção inteira é tocável quando leva a outra tela), conteúdo, linha fina embaixo. Margem 20 dp. | ✅ 1.4 |
| DS-COMP-15 | **`Shortcuts` / `Shortcut`** | Atalho redondo: círculo 64 dp `surfaceContainerHigh` com ícone 24 dp e rótulo `labelMedium` embaixo (até 2 linhas); fila com rolagem lateral. | ✅ 1.4 |
| DS-COMP-16 | **`HomeHeader`** | Cabeçalho colorido do Início (DS-COR-04): saudação, olho, engrenagem, saldo em `displaySmall` e patrimônio. | ✅ 1.4 |
| DS-COMP-17 | **`InfoCard`** | Aviso discreto: fundo `surfaceContainerLow`, cantos 16, ícone colorido pelo tipo (vermelho atrasado, âmbar vencendo), texto e "›". | ✅ 1.4 |
| DS-COMP-18 | **`CryoListRow`** | Linha de lista plana: ícone à esquerda, título e subtítulo, valor ou ação à direita, toque opcional; conteúdo extra embaixo (barra de uso). Margem 20 dp. | ✅ 1.4 |
| DS-COMP-19 | **`HeroBlock`** | Destaque plano no topo das telas de detalhe (conta, cartão, dívida, investimento): valor principal em `displaySmall` e informações abaixo, sem cartão em volta. | ✅ 1.4 |
| DS-COMP-20 | **`FlatItem`** | Item plano tocável das listas de Planejar, Dívidas e Análises, com opção de aparecer apagado (pago, arquivado). | ✅ 1.4 |
| DS-COMP-21 | **`PageDots`** | Pontos de página (passeio): o da página atual vira uma pílula na cor do Cryo, com a largura animada; lido como "Página 2 de 5". | ✅ 1.4 |
| DS-COMP-23 | **`Depth`** (`ui/components/Depth.kt`) | Peças da profundidade (DS-PROF): `Modifier.depthCard()` (cartão com sombra suave e fio de luz), `Modifier.groupedRow()` (linha de lista agrupada: só a primeira e a última com cantos), `Modifier.raised()` (volume de círculos e botões), `CryoBadge` (o ícone dentro do app), `BottomBarSurface` (barra de ação fixa elevada), `rememberHaptics()` (vibrações com nome: confirmar, recusar, ligar/desligar, tique, toque). As cores do fundo agrupado, dos cartões, das sombras e do cabeçalho ficam em `CryoTheme.depth` (`ui/theme/Theme.kt`). | ✅ 1.5 |
| DS-COMP-24 | **`CryoButton`** | Botão principal com volume (DS-PROF-05); mesmos parâmetros do `Button`. | ✅ 1.5 |
| DS-COMP-25 | **Gráficos do motor** (`ui/components/Charts.kt`) | `ForecastChart` com a faixa (P10–P90) e a linha do mais provável; `ProjectionChart` (saldo dos próximos meses, faixa, dia mais apertado marcado); `ScoreRing` (anel da nota de 0 a 100, cor pela faixa). Todos aparecem com o movimento DS-MOV e têm descrição para leitor de tela. | ✅ 1.5 |
| DS-COMP-26 | **Tela de eco** (`ui/tx/VoiceSheet.kt`) | Folha de voz (RF-VOZ-08): estado e nome do ouvido no topo; palavras que entram subindo com brilho; microfone com volume que cresce com a voz e solta um anel por sílaba (finito); cartõezinhos do que foi entendido, com mola; com vários lançamentos, um cartão por lançamento. O eco encolhe depois de "Entendi". | ✅ 1.5 |
| DS-COMP-22 | **`Motion`** (`ui/components/Motion.kt`) | Tempos, curvas e transições do DS-MOV num lugar só: transições de tela e de aba, `animateMoney`, `rememberAppear` (0 → 1 uma vez, ao aparecer), `pressScale`. Animação nova usa estas peças. | ✅ 1.4 |

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
| DS-ICO-02 | **Ícone do app (G4, 1.5)**: o mesmo fundo em degradê `#0A4F66 → #29B6D6`; o **C quadrado** (hexágono aberto) agora **dourado** (`#FFD866 → #F0A81E`), com **espessura** (a parte de baixo em `#B9780A`) e um **fio de luz** nas arestas de cima; **sem a estrela** (ela lembra o símbolo de "recurso de IA"). Escolhido em 07/11/2026 depois de comparar 11 desenhos com a pesquisa de psicologia da atenção (cor quente no azul = ponto que puxa o olho; continuidade com o C que as pessoas já conhecem). Até a 1.4: C branco com estrela. |
| DS-ICO-03 | **Versões do ícone**: ícone adaptativo (`ic_launcher_background` + `ic_launcher_foreground`); **monocromático** só com o C (ícones temáticos do Android 13+); **notificação** só com o C, branco; **`ic_cryo_badge`**: o ícone completo (fundo + C dourado), usado dentro do app onde a marca aparece (apresentação, bloqueio, Sobre); **`ic_cryo_mark`**: só o C dourado com volume (banner da loja). |
| DS-ICO-04 | **Loja**: `icon.png` 512 × 512 e `featureGraphic.png` (en-US e pt-BR) gerados por `StoreAssetsTest` a partir dos mesmos desenhos. |
| DS-ICO-05 | O dourado do ícone é a cor da **marca** (`CryoTheme.colors.gold`): aparece no símbolo e em celebrações (meta concluída), **nunca** como cor de entrada ou saída de dinheiro (DS-COR-03). |

### 9.8 Movimento (DS-MOV)

Pedido do Hanry (05/11/2026): animações suaves ao navegar e usar o Cryo. **Direção**: o movimento é suave e tem propósito:
mostra de onde a tela veio, para onde foi e o que mudou; nunca enfeita, nunca faz a pessoa esperar.

| ID | Regra | Status |
|---|---|---|
| DS-MOV-01 | **Tempos e curvas**: curta 150 ms (toque, algo pequeno que aparece ou some), média 300 ms (trocar de tela, abrir uma parte), longa 600 ms (números e gráficos). Curvas "emphasized" do Material 3. Tudo vem de `Motion` (DS-COMP-22). | ✅ 1.4 |
| DS-MOV-02 | **Abrir e voltar telas**: a tela interna entra deslizando um pouco da direita com fade; ao voltar, sai para a direita e a anterior volta da esquerda (eixo X do Material). | ✅ 1.4 |
| DS-MOV-03 | **Trocar de aba**: "fade through": a aba atual some rápido e a nova aparece com fade, crescendo de leve (94% → 100%). | ✅ 1.4 |
| DS-MOV-04 | **Barra de abas**: some e volta deslizando, junto com a troca de tela, quando se entra e sai de uma tela interna. | ✅ 1.4 |
| DS-MOV-05 | **Números que mudam**: o saldo e o patrimônio do Início contam até o novo valor quando ele muda (depois de um lançamento, por exemplo). Na primeira vez aparecem prontos. O número que fica é sempre o exato. | ✅ 1.4 |
| DS-MOV-06 | **Barras e gráficos**: ao aparecer, as barras de uso crescem até o valor, a linha da previsão se desenha da esquerda para a direita, a rosca gira até as fatias e as barras dos meses sobem. Quando o valor muda, vão até o novo valor. | ✅ 1.4 |
| DS-MOV-07 | **Listas**: itens que entram, saem ou mudam de lugar no Extrato deslizam e aparecem com fade. | ✅ 1.4 |
| DS-MOV-08 | **Toque**: os atalhos do Início encolhem de leve (92%) ao toque e voltam. | ✅ 1.4 |
| DS-MOV-09 | **Passos e partes**: a apresentação desliza para a frente ao avançar e para trás ao voltar; a barra de progresso anda suave; partes de um formulário que aparecem (ex.: dados do cartão na apresentação) se abrem com a altura animada. Da apresentação para o app, fade. | ✅ 1.4 |
| DS-MOV-10 | **Limites**: nenhuma animação infinita (só o indicador de "carregando", que já é do sistema); animação só com as APIs de animação do Compose, que seguem "Remover animações" (nunca `delay()` para animar); o **bloqueio aparece e some sem animação** (o app bloqueado não pode ficar à vista nem por um instante, RF-BLQ). | ✅ 1.4 |

### 9.9 Profundidade (DS-PROF)

Pedido do Hanry (07/11/2026): o mesmo "volume" do ícone dentro do app, para a pessoa sentir que usa um app **bem construído,
com peso de app de iPhone**. **Direção**: profundidade discreta e consistente, com a mesma luz do ícone (vinda de cima): o que é
importante parece um objeto que se pode tocar; nada de brilho exagerado, reflexo, textura ou imitação de material.

| ID | Regra | Status |
|---|---|---|
| DS-PROF-01 | **Luz de cima**: superfícies de destaque têm degradê sutil (mais claro em cima), um **fio de luz** de 1 dp no topo e **sombra suave** embaixo (larga e clara, nunca uma borda escura). Tudo pelas peças de `Depth` (DS-COMP-23). | ✅ 1.5 |
| DS-PROF-02 | **Fundo agrupado e cartões**: a tela tem um fundo "agrupado" (cinza-gelo no claro, quase preto no escuro) e o conteúdo fica em **cartões** (branco no claro, cinza-grafite no escuro), cantos 24, margem 16 dp, sombra suave, como as listas agrupadas do iPhone. Seções (`CryoSection`), itens (`FlatItem`), destaques (`HeroBlock`), avisos (`InfoCard`) e listas de movimentações viram cartões. Funciona também com as cores do papel de parede (Material You). | ✅ 1.5 |
| DS-PROF-03 | **Cabeçalho do Início** com o degradê do ícone (`#0A4F66 → #168FAD`, no escuro mais fundo), um brilho suave atrás do saldo e cantos de baixo arredondados (28 dp) com sombra: o cabeçalho "pousa" sobre a tela. Com Material You, o degradê vem das cores do papel de parede. | ✅ 1.5 |
| DS-PROF-04 | **Atalhos com volume**: círculo com degradê, fio de luz e sombra suave; encolhem ao toque com **mola** e vibração leve. | ✅ 1.5 |
| DS-PROF-05 | **Botão principal** (`CryoButton`): degradê da cor principal (mais claro em cima), fio de luz, sombra colorida suave, encolhe ao toque com mola e vibra ao confirmar. Botões de apagar e secundários continuam sem volume (o volume chama para a ação principal). | ✅ 1.5 |
| DS-PROF-06 | **Barra de abas** elevada: cor dos cartões, fio de luz no topo e sombra para cima; troca de aba com vibração de "tique". | ✅ 1.5 |
| DS-PROF-07 | **Vibração ao toque** (como no iPhone): confirmar (salvar, importar), ligar/desligar (interruptores), tique (abas, segmentos, passos do passeio), toque nos atalhos. Pelo `HapticFeedback` do sistema, que segue o ajuste "vibração ao toque" do celular; **sem permissão nova** (RNF-PRIV-03). | ✅ 1.5 |
| DS-PROF-08 | **Molas**: toques e pequenas mudanças (encolher, crescer) usam mola, não tempo fixo; com "Remover animações", nada se move (ACS-08). | ✅ 1.5 |
| DS-PROF-09 | **Números com peso**: saldos e totais em `SemiBold` com algarismos de largura igual (`tnum`) e espaçamento levemente fechado nos números grandes. | ✅ 1.5 |

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
| ACS-08 | **Remover animações**: com a opção do Android ligada (Acessibilidade › Remover animações, ou escala de animação 0), tudo aparece direto no estado final, inclusive o passeio (DS-MOV-10, RF-TOUR-06). |

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

- **Commit**: primeira linha `<versão>: <resumo>`; no corpo, `PRD: RF-IMP-07, DS-COMP-14` com os IDs tocados. A partir de 06/11/2026, **em inglês** (a mensagem do último commit aparece na página inicial do repositório, RF-LNG-09).
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
- [ ] `CHANGELOG.md` (inglês) e `docs/CHANGELOG.pt-BR.md` e, em versão nova, `fastlane/.../changelogs/<versionCode>.txt` nas 10 línguas (≤ 500 caracteres).
- [ ] VISUAL.md / IMPORTACAO.md / CLAUDE.md atualizados quando o assunto é deles.

### 11.5 Publicar uma versão (PRC-05)

1. Subir `versionCode`/`versionName`; `CHANGELOG.md` (inglês) e `docs/CHANGELOG.pt-BR.md`; changelogs do fastlane nas 10 línguas.
2. Testes passando; commit com a identidade pública.
3. APK de clone limpo (PRC-06) e entregar ao Hanry.
4. Com o "pode publicar": merge em `main`, push, Release `vX.Y` com o arquivo **exatamente** `Cryo-X.Y.apk`. As notas da
   Release saem em inglês primeiro e depois em português (RF-LNG-09). **Primeiro o push, depois a Release**: a etiqueta
   `vX.Y` precisa apontar para o commit do APK (no site, "Target" = o branch da versão). Uma Release criada antes do push
   prende a etiqueta no código antigo, e o F-Droid compilaria a versão errada (aconteceu com a 1.3 e a 1.4, corrigido em 06/11/2026).
5. F-Droid: a receita (`docs/fdroid/io.github.h3yk0.cryo.yml`) acha a tag sozinha. **Mudou a receita** (ex.: licença na 1.4)?
   Abrir merge request no fdroiddata com a mudança.
6. Imagens da loja (`fastlane/`) geradas de novo quando o visual muda (`StoreScreenshotsTest` e `StoreAssetsTest` com `-PstoreAssets=true`).
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

**Biblioteca de erros (lei).** Todo defeito corrigido entra também em **[docs/ERROS.md](ERROS.md)**, com um ID `ERR-`, a lei em
uma frase ("o que nunca mais pode acontecer") e o teste que guarda essa lei. Quem mexe numa área lê antes as linhas dela;
nenhum teste-guarda pode ser apagado, desligado ou afrouxado sem mudar primeiro o PRD e a tabela. `KnownIssuesTest` confere
sozinho (IDs em ordem, toda linha com lei e guarda, toda guarda existindo e ligada). Pedido do Hanry (10/11/2026): "qualquer
nova requisição deve passar por essas correções já feitas e seguir essa matriz".

### 11.9 Conformidade automática (PRC-09)

`app/src/test/java/io/github/h3yk0/cryo/PrdConformanceTest.kt` confere, a cada `testDebugUnitTest`, as regras que dá para
conferir sozinho:

| Confere | Regra |
|---|---|
| Todo `.kt` começa com `// SPDX-License-Identifier: AGPL-3.0-or-later`; `LICENSE` é a AGPL v3 | RNF-LIC-01 |
| Manifesto sem `INTERNET`; só `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `RECORD_AUDIO` e `POST_PROMOTED_NOTIFICATIONS` declaradas; `allowBackup="false"` | RNF-PRIV-01, RNF-PRIV-03 |
| Nenhum emoji nos textos nem no código | RF-ILU-01 |
| Voz: nada de rede no código de voz nem na ponte nativa; CMake com `GGML_NATIVE` e KleidiAI desligados, caminhos da máquina trocados (`-ffile-prefix-map`), sem informação de depuração | RF-VOZ-10, RNF-PRIV-01 |
| Nenhum extrato (`.ofx`, `.csv`, `.xlsx`, `.xls`, `.pdf`) nem chave (`.jks`, `.keystore`) versionado; `keystore/` no `.gitignore` | RNF-PRIV-04, RNF-SEG-01 |
| Nenhuma cor fixa (`Color(0x…)`) em telas e componentes | DS-COR-02 |
| Toda rota de `Routes` está no catálogo de telas (5.2) | PRC-02 |
| Toda chave de texto existe nas 10 línguas, com os mesmos marcadores; nenhum `Text("...")` com texto escrito no código nas telas | RF-LNG-03, RF-LNG-04 |
| As línguas dos `values-*` são as 10 do RF-LNG-01, iguais às da loja (fastlane) | RF-LNG-01, RF-LNG-07 |
| Todo caminho de arquivo citado no PRD existe; IDs do PRD não se repetem | PRC-02 |
| Existe o schema do Room da versão atual do banco | DAD-03 |
| `CHANGELOG.md` e `docs/CHANGELOG.pt-BR.md` têm a seção da versão atual; changelogs do fastlane com no máximo 500 caracteres | PRC-04, RF-LNG-09 |
| Nenhuma animação infinita (`rememberInfiniteTransition`, `infiniteRepeatable`) nas telas e componentes | DS-MOV-10 |
| Botão principal sempre com volume: nenhum `Button(` direto nas telas, só `CryoButton` | DS-PROF-05 |
| Nada em logs: nenhum `Log`, `println` ou `printStackTrace` no app | RF-SEG-07 |
| Motor sem Android e sem rede: `domain/engine` não importa `android.*` nem `java.net` | RF-MOT, RF-SEG-02 |
| O ícone e o selo não têm mais a estrela (G4) | DS-ICO-02 |
| Todas as rotas usam as transições do `Motion` (nenhum `NavHost` sem elas) | DS-MOV-02, DS-MOV-03 |
| `README.md` em inglês e os 9 `docs/readme/README.<língua>.md` existem, cada um com a linha das 10 línguas, e toda imagem ou arquivo que citam existe | RF-LNG-09 |
| As 10 pastas da loja têm as imagens das telas (`phoneScreenshots/`) | RF-LNG-10 |

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
| 1.3 | ✅ 06/11/2026 (GitHub) | Importar extrato, modo desenvolvedor, correções de bloqueio. |
| **1.4** | ✅ 06/11/2026 (GitHub; F-Droid pela etiqueta) | **AGPL** (RNF-LIC-01) · **PRD e conformidade** (PRC-01..10) · **Direção visual 2.0** em todas as telas (DS-DIR-01..05, RF-INI-11..17) · **Mais fácil**: 4 abas (RF-INI-14), atalhos, restaurar na apresentação (RF-BOA-06) · **10 línguas** (RF-LNG) e **moeda do país** (RF-MOE), frases nas 10 línguas (RF-REG-09), extratos de outros países (RF-IMP-21) · Bancos do Hanry (RF-IMP-20) · **Passeio animado** "Como usar o Cryo" (RF-TOUR) · **Movimento** em todo o app (DS-MOV) · **GitHub nas 10 línguas**, inglês como principal (RF-LNG-09, RF-LNG-10). |
| **1.5** | ✅ Pronta (12/11/2026), aguardando o teste e o "pode publicar" do Hanry. Junta as cinco etapas feitas depois da 1.4 | **Etapa 1**: **Ícone novo G4** (DS-ICO-02..05) · **Profundidade** em todo o app, com peso de app de iPhone (DS-PROF-01..09) · vibração ao toque · **Motor financeiro** robusto, todo no aparelho (RF-MOT-01..15): recorrências, previsão com faixa, próximos meses, fora do padrão, saúde financeira, ritmo das categorias, juros e plano das dívidas, rendimento real, metas, categoria aprendida, 500 testes de garantia · **Proteção dos dados** com hash e criptografia (RF-SEG). <br>**Etapa 2**: **Voz no aparelho** (RF-VOZ-01..12): Whisper local, microfone ao vivo com animação de eco, vocabulário da pessoa, números por extenso, vários lançamentos numa frase, correções faladas. <br>**Etapa 3**: **Avisos inteligentes** no estilo Duolingo, todos no aparelho (RF-AVI-01..11, com figurinha animada e modo compras ao vivo) · **sons próprios** de cristal (RF-SOM) · **momentos animados** (RF-MOM: meta, orçamento, sequência, fechamento do mês) · **ilustrações próprias** no lugar dos emojis (RF-ILU) · **banco versão 4** e **painel geral dos gastos**, remanejar só no mês (RF-ORC-04/06, DAD-12..14) · **alertas de orçamento** ao salvar e pelo ritmo (RF-ORC-04/05) · **botão de voz ao alcance do polegar** (RF-ALC, One UI) · **voz mais rápida** (RF-VOZ-13). Widgets (RF-WID-01): depois. <br>**Etapa 4**: **Auditoria do código inteiro** com as correções (RF-AUD-01..07) e a **biblioteca de erros como lei** (`docs/ERROS.md`, PRC-08) · **Previsor novo, feito do zero** (RF-PRV-01..09): conjunto de modelos clássicos pesados pelo passado da própria pessoa, faixa conferida no passado (calibração conforme), época do ano, previsão por categoria com a chance de passar do limite, investimentos "se continuar como foi" (rentabilidade no tempo, sobe e desce, maior queda, 1/3/5 anos), e as telas que explicam a conta. <br>**Etapa 5**: **Contas fixas e extrato** (RF-CFX-01..08: o Cryo reconhece o pagamento da conta fixa na linha importada, arruma o que já entrou em dobro e não conta duas vezes na previsão) · **Perfil** (RF-PRF-01..04: a bolinha com desenho, emoji ou foto abre um painel bonito no lugar da engrenagem) · **Guia "Aprender o Cryo"** (RF-GUI-01..07: dicas por regras, no ritmo de quem usa, no máximo uma por dia). · **Cartão** (RF-CAR-07 corrigido, RF-CAR-10 e 11, do issue #3: estorno à mão, escolher a fatura, ajustar o total) · **Sons mais abafados** (RF-SOM-01 revisto). |

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
