# Cryo — guia visual

Como o Cryo é e deve continuar parecendo: cores, letras, formas, componentes, telas e o jeito de escrever.

> Este guia é um anexo do **[PRD](PRD.md)** (seções 9 e 10, códigos DS-, TXT- e ACS-). Ele detalha as regras de lá; em caso de
> conflito, vale o PRD. A direção visual 2.0 (1.4: organização de app de banco, cores do Cryo) está no PRD, seção 9.1.
As imagens ficam em [`docs/visual/`](visual/) e são as telas reais do app (geradas pelos testes, com dados de exemplo).

<p>
  <img src="visual/inicio.png" width="24%" alt="Início, tema claro">
  <img src="visual/inicio-escuro.png" width="24%" alt="Início, tema escuro">
  <img src="visual/inicio-material-you.png" width="24%" alt="Início com as cores do papel de parede">
  <img src="visual/importar-conferencia.png" width="24%" alt="Conferência de extrato importado">
</p>

---

## 1. Personalidade

**"Seu dinheiro, claro como gelo."** O visual segue essa ideia:

- **Claro e calmo**: fundo limpo, muito respiro, cantos arredondados, uma cor de destaque. Nada piscando, nada agressivo.
- **Número em primeiro lugar**: o valor que importa é o maior elemento da tela (saldo no Início, valor no lançamento).
- **Uma ação principal por tela**: botão grande e fixo embaixo ("Importar 11 movimentações", "Salvar", "Escolher arquivo").
- **Explica em vez de esconder**: cada aviso diz o que aconteceu e o que fazer, em português simples.
- **Respeita a privacidade**: o olho no Início esconde todos os valores (`R$ ••••`); nada de dados sensíveis em notificações.
- **Material You de verdade**: no Android 12 ou mais novo, as cores acompanham o papel de parede (pode desligar em Ajustes › Aparência).

## 2. Cores

### 2.1 De onde vêm

1. **Cores do papel de parede** (Material You, padrão no Android 12+): `dynamicLightColorScheme` / `dynamicDarkColorScheme`.
2. **Paleta "gelo"** (`ui/theme/Theme.kt`): usada com o Material You desligado ou em Android 8 a 11.
3. **Cores de significado** (`CryoColors`): entrada, saída e atenção. **Não mudam** com o papel de parede, para o verde e o
   vermelho sempre quererem dizer a mesma coisa.

Nunca usar cor fixa (`Color(0xFF…)`) direto numa tela: sempre `MaterialTheme.colorScheme.*` ou `CryoTheme.colors.*`.
A exceção são as cores que a própria pessoa escolhe para categorias, contas e metas (paleta da seção 2.4).

### 2.2 Paleta "gelo"

| Papel | Claro | Escuro |
|---|---|---|
| primary / onPrimary | `#00677F` / `#FFFFFF` | `#5FD4FD` / `#003543` |
| primaryContainer / on | `#B8EAFF` / `#001F28` | `#004D61` / `#B8EAFF` |
| secondary / onSecondary | `#4C626B` / `#FFFFFF` | `#B3CAD4` / `#1E333C` |
| secondaryContainer / on | `#CFE6F1` / `#071E26` | `#354A53` / `#CFE6F1` |
| tertiary / onTertiary | `#5B5B7E` / `#FFFFFF` | `#C3C3EB` / `#2D2D4D` |
| tertiaryContainer / on | `#E1DFFF` / `#181837` | `#434465` / `#E1DFFF` |
| error / onError | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` |
| errorContainer / on | `#FFDAD6` / `#410002` | `#93000A` / `#FFDAD6` |
| background = surface / on | `#F6FAFD` / `#171C1F` | `#0F1417` / `#DFE3E6` |
| surfaceVariant / on | `#DBE4E8` / `#40484C` | `#40484C` / `#BFC8CC` |
| outline / outlineVariant | `#70787D` / `#BFC8CC` | `#8A9296` / `#40484C` |
| surfaceContainerLowest → Highest | `#FFFFFF` `#F0F4F7` `#EAEEF1` `#E4E9EC` `#DFE3E6` | `#0A0F11` `#171C1F` `#1B2023` `#262B2E` `#303538` |

### 2.3 Cores de significado (`CryoTheme.colors`)

| Significado | Claro (cor · container · texto no container) | Escuro |
|---|---|---|
| **Entrada** (`income`) | `#1B6D2F` · `#B6F2BB` · `#002109` | `#8BD893` · `#00531C` · `#B6F2BB` |
| **Saída** (`expense`) | `#B3261E` · `#FFDAD6` · `#410002` | `#FFB4AB` · `#93000A` · `#FFDAD6` |
| **Atenção** (`warning`) | `#8A5100` · `#FFDCBE` · `#2C1600` | `#FFB870` · `#693C00` · `#FFDCBE` |

Como usar:
- **Entrada** sempre verde com `+`. **Saída** em listas fica na cor normal do texto, com `−` (lista toda vermelha cansa e assusta);
  nos resumos ("Saiu R$ 1.502,55") e quando algo estoura (orçamento, fim do mês no vermelho), vermelho.
- **Avisos** (conta vencendo, linha que parece repetida): `warningContainer` + `onWarningContainer`.
- **Erros** (arquivo que não abriu, linhas que vão duplicar): `errorContainer` + `onErrorContainer`.
- **Informação neutra** (sinais invertidos na fatura, versão anônima do relatório): `secondaryContainer`.
- **Modo de teste** (modo desenvolvedor): `tertiaryContainer` + ícone de frasco (`Science`), em qualquer lugar onde apareça.
- Nunca depender só da cor: sempre há sinal (`+`/`−`), ícone ou texto junto.

### 2.4 Paleta para itens (`data/DefaultData.kt` › `Palette`)

20 cores para categorias, contas, cartões, metas e investimentos, todas legíveis no claro e no escuro:

`#E53935` `#D81B60` `#8E24AA` `#5E35B1` `#3949AB` `#1E88E5` `#039BE5` `#00ACC1` `#00897B` `#43A047`
`#7CB342` `#C0CA33` `#FDD835` `#FFB300` `#FB8C00` `#F4511E` `#6D4C41` `#757575` `#546E7A` `#26A69A`

Elas aparecem em **bolinhas com ícone** (`IconBadge`): fundo da cor com 16% de opacidade (28% no escuro) e o ícone na cor um
pouco mais escura (mais clara no escuro).

### 2.5 Marca

- Gradiente da marca: `#0A4F66` → `#29B6D6` (diagonal). Usado no ícone e na imagem de destaque da loja.
- `brand_dark` `#0B5C73`, `brand_light` `#22A6C9`. Fundo da janela antes do app desenhar: `#F6FAFD` / `#0F1417`.

## 3. Letras

Fonte do sistema (Roboto, padrão do Material 3) com ajustes em `Theme.kt`:

- **SemiBold** em `display*`, `headline*`, `title*` e `labelLarge`.
- **Números tabulares** (`fontFeatureSettings = "tnum"`) em quase todos os estilos: os dígitos têm a mesma largura e os valores
  ficam alinhados em colunas.

| Uso | Estilo |
|---|---|
| Valor sendo digitado (campo de valor) | `displaySmall` |
| Saldo do Início | `displaySmall` (com auto-ajuste para caber) |
| Título de tela de passo ("Traga as movimentações do seu banco") | `headlineSmall` |
| Título de seção ("Últimas movimentações") | `titleMedium` |
| Título de cartão, cabeçalho de grupo | `titleSmall` (cor `primary` nos cabeçalhos de Ajustes) |
| Linha de lista (descrição) | `bodyLarge` |
| Subtítulo de linha (data · categoria) | `bodySmall` em `onSurfaceVariant` |
| Rótulo de campo ("Para onde vão", "Categoria") | `labelLarge` em `onSurfaceVariant` (`FieldLabel`) |
| Etiquetas ("recomendado", status) | `labelSmall`/`labelMedium` |

Valores usam `MoneyText`: uma linha só, diminui sozinho até 10 sp para caber, respeita "esconder valores".

## 4. Formas e espaços

**Cantos** (`CryoShapes`): extraSmall 8 dp · small 12 · **medium 16** (banners, campos, caixas) · **large 24** (cartões) · extraLarge 32.

**Espaços** (múltiplos de 4): 4 · 8 · 12 · 16 · 20 · 24 · 32.
- Margem lateral: **16 dp** em listas, **20 dp** em formulários, folhas e telas de passo.
- Entre seções: 16–24 dp. Dentro de cartão: 16 dp.
- Botão principal: **56 dp** de altura, largura total. Botão secundário (contorno): 52 dp.
- Bolinha de ícone: 40 dp (44 na grade de categorias, 64 no topo das telas de passo, 72–88 em estados vazios e bloqueio).
- Ícones: 24 dp; 18 dp dentro de chips; 20 dp em banners.
- Toque: tudo que é clicável tem pelo menos 48 dp.

## 5. Como as telas são montadas

- **Barra de abas** (4, desde a 1.4): Início · Extrato · Planejar · Análises. A Carteira abre pelo Início.
  Ícones: `Home`, `ReceiptLong`, `EventNote`, `Insights` (estilo Rounded).
- **Início** (2.0): cabeçalho colorido (`HomeHeader`: saudação, data, olho, engrenagem, saldo e patrimônio), atalhos redondos,
  campo de registro rápido em pílula com microfone, avisos discretos e seções planas (mês, cartão, contas, dívidas, investimentos).
- **Passeio "Como usar o Cryo"** (RF-TOUR): 5 páginas com uma ilustração animada em cima (cartão `surfaceContainerLow`, cantos
  28 dp, montada com os próprios componentes), título `headlineSmall`, texto `bodyLarge` em `onSurfaceVariant`, pontos de página e
  botão de 56 dp embaixo; "Pular" no topo no primeiro uso. Aparece depois das boas-vindas e em Ajustes › Sobre.
- **Telas internas**: `CryoTopBar` com seta de voltar e título; conteúdo rolando; **ação principal fixa embaixo** (`Surface` com
  `tonalElevation = 3.dp` + botão de 56 dp, com `navigationBarsPadding()` e `imePadding()`).
- **Edição rápida**: `ModalBottomSheet` aberto por inteiro (`skipPartiallyExpanded = true`), título `titleLarge`, campos, botão
  principal e "Cancelar" contornado.
- **Passo a passo** (ex.: importação): bolinha 64 dp com ícone, título `headlineSmall`, texto explicativo, cartões, botão fixo.
- **Confirmação**: `ConfirmDialog`, título em forma de pergunta, texto com a consequência, botão com o verbo.
- **Tela inteira, de ponta a ponta** (edge-to-edge): barras do sistema transparentes; `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))`.
- **Modo de teste**: faixa `tertiaryContainer` acima da barra de abas ("Modo de teste: dados de exemplo · Sair") e avisos nas telas de importação.
- **Bloqueio**: tela própria no lugar do app (marca do Cryo, "Cryo bloqueado", botão "Desbloquear"). Aparece e some **sem
  animação** (DS-MOV-10).

### 5.1 Movimento (DS-MOV)

Suave e com propósito: o movimento mostra de onde a tela veio, para onde foi e o que mudou. Tudo vem de
`ui/components/Motion.kt`.

| O quê | Como | Tempo |
|---|---|---|
| Abrir uma tela interna | Desliza 1/10 da largura da direita, com fade (eixo X) | 300 ms |
| Voltar | A tela sai para a direita; a de baixo volta da esquerda | 300 ms |
| Trocar de aba | "Fade through": some rápido (90 ms) e a nova aparece crescendo de 94% a 100% | 300 ms |
| Barra de abas | Some e volta com a altura animada, junto com a troca de tela | 300 ms |
| Saldo e patrimônio do Início | Contam até o novo valor quando mudam (na primeira vez aparecem prontos) | 600 ms |
| Barras de uso, previsão, rosca, barras dos meses | Crescem / se desenham ao aparecer; vão até o novo valor quando mudam | 600–900 ms |
| Extrato | O que entra, sai ou muda de dia desliza e aparece com fade (`animateItem`) | padrão |
| Atalhos do Início | Encolhem para 92% enquanto o dedo está neles | 150 ms |
| Apresentação | Passos deslizam para a frente ou para trás; barra de progresso anda suave; campos do cartão abrem com a altura animada | 300 ms |
| Passeio | Cada página tem uma linha do tempo de 2,8 s que começa quando a página para na tela e termina no quadro final | 2,8 s |

Curvas: "emphasized" do Material 3 (`Motion.Emphasized`, `EmphasizedDecelerate` para entrar, `EmphasizedAccelerate` para sair).

Regras: **nada infinito** (conferido por `PrdConformanceTest`); só as APIs de animação do Compose, nunca `delay()` para
animar; com **"Remover animações"** do Android, tudo aparece direto no estado final (`animationsOn()`).

## 6. Componentes

Estão em `ui/components/` (Common.kt, Inputs.kt, Charts.kt). Antes de criar algo novo, procure aqui.

| Componente | Para quê | Detalhes |
|---|---|---|
| `CryoCard` | Agrupar conteúdo | `surfaceContainerLow`, cantos 24 dp, 16 dp por dentro; pode ser clicável |
| `IconBadge` | Ícone de categoria, conta, cartão | Círculo 40 dp, cor da paleta com transparência |
| `MoneyText` | Qualquer valor | Na moeda e no formato do app (R$ 1.234,56 · $1,234.56 · ¥1,235), sinal opcional, auto-ajuste, "esconder valores" |
| `TxRow` | Linha de movimentação | Bolinha + título + "data · categoria · conta" + valor |
| `SectionTitle` | Título de seção com ação ("Ver análises") | `titleMedium` |
| `Banner` | Aviso, erro, dica | Ícone 20 dp + texto, cantos 16 dp, cor pelo tipo (seção 2.3) |
| `Pill` | Status curto ("Paga", "Atrasada") | Cápsula `labelMedium` |
| `EmptyState` | Lista vazia | Círculo 72 dp `secondaryContainer`, título, explicação, ação |
| `UsageBar` | Uso de orçamento, progresso | 10 dp; cor normal, âmbar a partir de 80%, vermelho a partir de 100%; cresce ao aparecer |
| `CryoSection` | Seção plana (2.0) | Título `titleMedium` + "›", conteúdo, linha fina embaixo; margem 20 dp |
| `CryoListRow` / `FlatItem` / `HeroBlock` | Linhas e destaques planos (2.0) | Sem cartão em volta; linha fina entre itens |
| `Shortcuts` / `Shortcut` | Atalhos do Início | Círculo 64 dp `surfaceContainerHigh`, ícone `primary`, rótulo `labelMedium`; encolhe ao toque |
| `HomeHeader` | Cabeçalho do Início | `primary` no claro, `primaryContainer` no escuro; saldo `displaySmall` que conta até o novo valor |
| `InfoCard` | Aviso discreto | `surfaceContainerLow`, cantos 16, ícone colorido pelo tipo |
| `PageDots` | Pontos de página (passeio) | Pontos de 8 dp; o atual vira pílula de 24 dp na cor `primary`; lido como "Página 2 de 5" |
| `Motion` | Tempos, curvas e transições | Seção 5.1 |
| `CryoTopBar` | Barra de título | Voltar + título + ações |
| `ConfirmDialog` | Confirmar algo importante | Pergunta, consequência, verbo |
| `AmountField` | Digitar valor | Estilo caixa eletrônico: só números, centavos se ajustam sozinhos (1 → R$ 0,01) |
| `CategoryGrid` | Escolher categoria | Grade de 4 colunas, bolinha 44 dp, anel `primary` na escolhida |
| `SourceChips` / `AccountChips` | Escolher conta ou cartão | `FilterChip` com ✓ na escolhida |
| `DateChooser` | Escolher data | Chips "Hoje", "Ontem", "Outra data" (calendário) |
| `Segmented` | 2–3 opções | `SingleChoiceSegmentedButtonRow` |
| `SwitchRow` | Liga/desliga com explicação | Linha inteira tocável |
| `FieldLabel` | Rótulo de campo | `labelLarge`, 16 dp acima, 8 dp abaixo |
| `ChoiceRow` | Escolha única com explicação | Botão redondo + título + texto |
| `Stepper` | Número com − e + | Parcelas, dia do mês |
| `ColorChooser` / `IconChooser` / `EmojiChooser` | Personalizar itens | Círculos selecionáveis |
| Gráficos (`Charts.kt`) | Ver seção 8 | Desenhados à mão em Canvas, sem biblioteca |

## 7. Ícones e marca

- Todos os ícones são **Material Icons, estilo Rounded** (`Icons.Rounded.*`; setas `Icons.AutoMirrored.Rounded.*`).
- Ícones de categoria: chaves em `DefaultData.categoryIcons` e desenho em `categoryIcon()` (Common.kt):
  restaurant, cart, car, bus, home, bolt, water, wifi, phone, health, fitness, school, movie, game, shopping, subscriptions,
  spa, pets, volunteer, gift, flight, receipt, coffee, gas, baby, build, work, laptop, sell, trending, undo, redeem, savings,
  debt, money, star, more. Cada um tem uma descrição em português para leitores de tela.
- **Ícone do app** (ícone adaptativo, `res/drawable/ic_launcher_*.xml`): fundo com o gradiente da marca e uma onda clara bem
  suave; na frente, um hexágono branco aberto (como um "C" de cristal de gelo, traço de 8 com pontas redondas) com uma estrela
  de quatro pontas no meio. Tem versão monocromática (ícones temáticos do Android 13+).
- `ic_cryo_mark`: a marca sozinha (tela de bloqueio, imagem da loja). `ic_notification`: ícone das notificações.

<p>
  <img src="../fastlane/metadata/android/en-US/images/icon.png" width="96" alt="Ícone do Cryo">
  <img src="../fastlane/metadata/android/pt-BR/images/featureGraphic.png" width="420" alt="Imagem de destaque da loja">
</p>

## 8. Gráficos

Todos em `ui/components/Charts.kt`, desenhados em Canvas com as cores do tema:

- **Previsão do mês** (`ForecastChart`): linha do saldo; cheia até hoje, **tracejada** na projeção; o valor do fim do mês fica
  verde se fecha no azul e vermelho se fecha no vermelho.
- **Calendário de calor** (`HeatmapCalendar`): um quadradinho por dia; quanto mais gasto, mais forte a cor `primary`
  (5 níveis: 0, 22%, 42%, 70%, 100%); dias futuros apagados.
- **Fluxo do dinheiro** (`SankeyChart`): de onde o dinheiro veio e para onde foi.
- **Gastos por categoria** (`DonutChart`) com `LegendDot`; **últimos 6 meses** (`MonthBarsChart`).

## 9. Como escrever (voz e tom)

- Base em português do Brasil (traduzida nas outras 9 línguas no mesmo commit; em francês com "vous", em alemão com "du"), falando com **"você"**, frases curtas, palavras do dia a dia ("conta fixa", não "recorrência";
  "fatura", não "statement"). Sem termos técnicos na tela (nada de "JSON", "SAF", "sandbox", exceto no modo desenvolvedor).
- **Erro = o que aconteceu + o que fazer**: "Não consegui abrir esse arquivo. Tente baixar de novo ou escolher outro."
- **Aviso explica a consequência**: "Para o gasto não contar duas vezes (as compras já estão no cartão), registre como pagamento do cartão."
- **Botão = verbo** que diz o que acontece: "Importar 11 movimentações", "Escolher arquivo", "Desfazer importação", "Salvar".
- **Confirmação**: título pergunta ("Apagar tudo?"), texto diz o que acontece ("Não dá para desfazer."), botão repete o verbo ("Apagar tudo").
- **Plural certo**: "1 movimentação" / "2 movimentações", "1 toque" / "2 toques", "Entrou" / "Entraram".
- **Dinheiro**: `R$ 1.234,56`; com sinal: `+ R$ 10,00` e `− R$ 10,00` (sinal de menos de verdade, espaço que não quebra).
  Resumido em gráficos: `R$ 1,2 mil`. Escondido: `R$ ••••`. Tudo em `domain/Format.kt` (`Money`, `Dates`).
- **Datas**: "Segunda-feira, 5 de outubro" (cabeçalhos), "05/10" (listas do ano atual), "05/10/2025" (outros anos),
  "vence em 5 dias", "hoje", "amanhã".
- Mensagens rápidas (snackbar) curtas: "Backup salvo", "Planilha exportada", "Categoria arquivada".

## 10. Acessibilidade

- Todo botão só com ícone tem `contentDescription` ("Ajustes", "Voltar", "Registrar", "Mais opções").
- Títulos de seção marcados como cabeçalho (`semantics { heading() }`).
- Interruptores e caixas de marcar com `toggleable` na linha inteira e descrição do estado.
- Valor escondido é lido como "Valor escondido".
- Contraste: as cores de significado foram escolhidas com bom contraste no claro e no escuro; nunca só cor para dizer algo.
- Textos crescem com a fonte do sistema; valores usam auto-ajuste em vez de cortar.
- "Remover animações" do Android: tudo aparece direto no estado final, inclusive o passeio (ACS-08). As ilustrações do passeio
  são decorativas para o leitor de tela (o título e o texto dizem tudo).

## 11. Telas atuais

| | | |
|---|---|---|
| <img src="visual/inicio.png" width="220"><br>Início | <img src="visual/registro-rapido.png" width="220"><br>Registro rápido (confere antes de salvar) | <img src="visual/novo-lancamento.png" width="220"><br>Novo lançamento |
| <img src="visual/extrato.png" width="220"><br>Extrato | <img src="visual/planejar-orcamentos.png" width="220"><br>Planejar › Orçamentos | <img src="visual/planejar-contas-fixas.png" width="220"><br>Planejar › Contas fixas |
| <img src="visual/planejar-metas.png" width="220"><br>Planejar › Metas | <img src="visual/carteira.png" width="220"><br>Carteira | <img src="visual/cartao.png" width="220"><br>Cartão e faturas |
| <img src="visual/analises.png" width="220"><br>Análises | <img src="visual/dividas.png" width="220"><br>Dívidas | <img src="visual/divida-detalhe.png" width="220"><br>Detalhe da dívida |
| <img src="visual/ajustes.png" width="220"><br>Ajustes | <img src="visual/backup-automatico.png" width="220"><br>Backup automático | <img src="visual/backup-encontrado.png" width="220"><br>Backup encontrado |
| <img src="visual/boas-vindas.png" width="220"><br>Boas-vindas | <img src="visual/inicio-escuro.png" width="220"><br>Tema escuro | <img src="visual/inicio-material-you.png" width="220"><br>Material You |

**Como usar o Cryo (passeio, 1.4)**: cada página anima a ilustração e para no quadro final (aqui, o final).

| | | | | |
|---|---|---|---|---|
| <img src="visual/passeio-1.png" width="150"><br>Registrar | <img src="visual/passeio-2.png" width="150"><br>Previsão | <img src="visual/passeio-3.png" width="150"><br>Cartões | <img src="visual/passeio-4.png" width="150"><br>Planejar | <img src="visual/passeio-5.png" width="150"><br>Privacidade |

**Importar extrato (1.3)**

| | | |
|---|---|---|
| <img src="visual/importar-inicio.png" width="220"><br>Começo | <img src="visual/importar-conferencia.png" width="220"><br>Conferência (conta) | <img src="visual/importar-fatura.png" width="220"><br>Conferência (fatura) |
| <img src="visual/importar-linha.png" width="220"><br>Conferir uma linha | <img src="visual/importar-colunas.png" width="220"><br>Escolher colunas | <img src="visual/importar-senha.png" width="220"><br>PDF com senha |
| <img src="visual/importar-pronto.png" width="220"><br>Pronto (com saldo) | | |

**Modo desenvolvedor (1.3)**

| | | |
|---|---|---|
| <img src="visual/modo-desenvolvedor.png" width="220"><br>Modo desenvolvedor | <img src="visual/relatorio-anonimo.png" width="220"><br>Relatório anônimo | <img src="visual/modo-teste.png" width="220"><br>Modo de teste (faixa) |

## 12. Página da loja (F-Droid)

Em `fastlane/metadata/android/<língua>/` (as 10 línguas): título, descrição curta e completa, changelogs por `versionCode` e
6 telas em `images/phoneScreenshots/` na própria língua e moeda (1 Início, 2 Registro rápido, 3 Análises/previsão,
4 Orçamentos, 5 Cartão, 6 Tema escuro), geradas por `StoreScreenshotsTest` (360 × 780 dp, xhdpi). Em `en-US/` também
`icon.png` (512 px) e `featureGraphic.png` (1024 × 500, gradiente da marca + "Cryo" + slogan), gerados por `StoreAssetsTest`
(`pt-BR/` tem o próprio banner). As mesmas telas aparecem no README de cada língua (RF-LNG-09):
`./gradlew testDebugUnitTest --tests '*StoreScreenshotsTest' --tests '*StoreAssetsTest' -PstoreAssets=true`.

## 13. Conferir o visual sem celular

1. Toda tela nova ganha um teste de imagem (em `ScreensTest` ou no teste de fluxo dela) com `captureRoboImage`.
2. Rodar `./gradlew testDebugUnitTest` e abrir `app/screenshots/`.
3. Conferir em claro, escuro e Material You (o `ScreensTest` tem os três), na largura de 393 dp (celular comum).
4. Ao mudar algo grande, atualizar as imagens de `docs/visual/` (versão reduzida, 540 px de largura) e, se for uma tela da loja,
   as do fastlane.
