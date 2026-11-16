# Cryo — biblioteca de erros já corrigidos (lei do projeto)

Cada linha é um erro que **já aconteceu** no Cryo e foi corrigido. A coluna "Lei" diz o que nunca mais pode acontecer; a
coluna "Guarda" é o teste que falha se o erro voltar. Esta tabela faz parte do processo (PRD, PRC-08) e vale para qualquer
pessoa ou modelo que mexer no código.

## Como usar (obrigatório)

1. **Antes de mexer** numa área (bloqueio, backup, dinheiro, importação, banco, avisos, previsor), leia as linhas dela aqui.
2. **Nenhum teste-guarda** pode ser apagado, desligado (`@Ignore`), afrouxado ou renomeado sem atualizar esta tabela. Se a regra
   mudou de verdade, muda-se primeiro o PRD, depois esta tabela, depois o código.
3. **Todo defeito novo** corrigido entra aqui no mesmo commit: um `ERR-` novo (o próximo número), a lei em uma frase e um teste
   que **falha no código anterior**. Sem teste-guarda, a correção não está pronta (PRC-04).
4. **Toda mudança** só está pronta com `./gradlew testDebugUnitTest` inteiro passando. `KnownIssuesTest` confere esta tabela:
   IDs sem repetição, toda linha com guarda, todo teste-guarda existindo no código.
5. Mudança no previsor (`domain/predict/`) só entra se **errar menos no laboratório** (`ForecastLabTest`), nunca por palpite.

Severidade (PRC-08): S1 = perde dados, valor errado, dado à vista com o app bloqueado, permissão indevida; S2 = função
principal falha; S3 = texto, visual, acessibilidade.

## Segurança e privacidade

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-001 | S1 | 1.3 | O bloqueio não aparecia quando o Android matava o app em segundo plano e restaurava a tela. | O desbloqueio vale só para o processo atual e cai depois de 60 s em segundo plano. | `DevModeFlowTest.bloqueioVoltaQuandoOAndroidFechaOAppEmSegundoPlano` |
| ERR-002 | S1 | 1.3 | Diálogos, menus e folhas ficavam por cima do bloqueio; Voltar e teclado chegavam ao app escondido. | Bloqueado, o conteúdo **não é composto** (`SaveableStateHolder` em `MainActivity`). Nunca "uma camada por cima". | `DevModeFlowTest.bloqueioTiraOAppDaTelaEVoltaParaOndeEstava` |
| ERR-003 | S1 | 1.5 | Qualquer erro do sensor de digital liberava o app, mesmo com trava de tela no aparelho. | Erro do sensor só libera em aparelho **sem** trava de tela (`LockPolicy.unlockOnError`). Na dúvida, continua bloqueado. | `Audit18DataTest.erroDoSensorSoAbreEmAparelhoSemTrava` |
| ERR-004 | S1 | 1.5 | Com o backup com senha ligado, se o cofre do Android falhasse o backup saía **sem proteção**. | Proteção ligada falha fechada: sem chave não grava (`BackupProtection.Unavailable`). Nunca cair para "aberto". | `Audit18DataTest.protecaoLigadaSemCofreNaoGravaBackupAberto` |
| ERR-005 | S2 | 1.5 | Backup estragado, gigante ou adulterado derrubava o app ou entrava com valores e datas absurdos. | Todo arquivo de fora tem limite de tamanho (`Backup.MAX_BYTES`), é lido fora da linha principal e conferido campo a campo; erro vira mensagem. | `Audit18DataTest.backupEstragadoERecusadoComMensagemClara` |
| ERR-006 | S2 | 1.5 | A planilha exportada (CSV) executava fórmulas escritas em descrições (`=`, `+`, `-`, `@`). | Texto da pessoa que vai para planilha passa por `esc()`, que neutraliza fórmulas. | `Audit18DataTest.planilhaNaoRodaFormulasDeDescricoes` |
| ERR-007 | S1 | 1.3 | O relatório "anônimo" da importação vazava cabeçalhos, nome do arquivo e notas. | Nada que veio do arquivo da pessoa aparece no relatório anônimo. | `StatementImportTest.relatorioAnonimoNaoMostraNadaDaPessoa`, `StatementImportTest.notasDoRelatorioAnonimoNaoMostramOQueVeioDoArquivo` |
| ERR-008 | S1 | todas | Risco permanente: biblioteca ou recurso novo pedir internet ou outra permissão. | Sem `INTERNET`. Só as permissões listadas no PRD (RNF-PRIV). | `PrdConformanceTest.semInternetEPermissoesSoAsCombinadas`, `PrdConformanceTest.motorSemAndroidESemRede` |
| ERR-009 | S1 | 1.5 | Dados da pessoa podiam ir parar em logs do sistema. | Nada de `Log`, `println` ou `printStackTrace` (RF-SEG-07). | `PrdConformanceTest.nadaEmLogs` |
| ERR-010 | S1 | todas | Risco permanente: extrato de verdade ou a chave de assinatura irem para o git. | Testes só com arquivos inventados; `keystore/` fora do git. | `PrdConformanceTest.semExtratosNemChaveNoGit` |
| ERR-011 | S2 | 1.5 | Dois avisos diferentes abriam a mesma tela (o Android tratava os `PendingIntent` como iguais). | Cada `PendingIntent` de aviso tem uma ação própria (`…alert.open.<id>`). | `AlertActionsTest.cadaAvisoAbreASuaTela` |

## Dados de teste nunca chegam aos dados de verdade

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-012 | S1 | 1.3 | Extratos de exemplo, JSON de teste e backups do modo de teste podiam entrar nos dados de verdade. | Exemplos e JSON de teste só no modo de teste; cópia do modo de teste sai marcada e não restaura por cima dos dados de verdade. | `ImportFlowTest.jsonDeTesteEExemplosSoNoModoDeTeste`, `StatementImportTest.copiaDoModoDeTesteFicaMarcada`, `DevModeFlowTest.modoDeTesteNaoMexeNosDadosDeVerdade` |
| ERR-013 | S2 | 1.3 | Uma fatura de verdade (Uber, Spotify, iFood) era tomada por arquivo de exemplo e recusada. | Exemplo se reconhece por descrição **e** valor, nunca só pela descrição. | `StatementImportTest.faturaDeVerdadeComOsMesmosLugaresDosExemplosNaoEExemplo` |
| ERR-014 | S2 | 1.3 | Um aviso de conta (dados de verdade) tocado com o modo de teste ligado abria os dados de teste. | Avisos e backup automático usam sempre `CryoApp.real`; o aviso sai do modo de teste antes de abrir. | `DevModeFlowTest.avisoDeContaComOModoDeTesteLigadoAbreOsDadosDeVerdade` |

## Dinheiro e banco de dados

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-015 | S1 | 1.5 | Um valor com muitos algarismos "dava a volta" e virava outro número (frase, extrato, JSON). | Todo texto ou número de fora vira centavos por `Money.centsOrNull` (teto de 13 algarismos). Nunca `toLong()` direto. | `Audit18Test.valorEnormeNaoDaAVolta` |
| ERR-016 | S2 | 1.5 | Frases estranhas ou hostis no registro rápido. | A frase nunca derruba o app nem inventa valor: ou entende, ou devolve nada. | `Audit18Test.fraseHostilNaoDerrubaNemInventaValor` |
| ERR-017 | S2 | 1.5 | Dividir uma compra em 0 parcelas dividia por zero. | `CardMath.split` com 1 parcela ou menos devolve o total. Toda divisão confere o divisor. | `Audit18Test.parcelasNuncaDividemPorZero` |
| ERR-018 | S1 | 1.5 | Tocar "Já paguei" duas vezes (app e notificação) gravava dois pagamentos. | "Olhar e depois gravar" no banco é sempre dentro de `db.withTransaction`. | `Audit18DataTest.pagueiDuasVezesGravaUmPagamentoSo`, `AlertActionsTest.jaPagueiMarcaAContaUmaVezSo` |
| ERR-019 | S2 | 1.5 | Apagar uma categoria deixava remanejamentos de orçamento apontando para o nada. | Apagar leva junto, na mesma transação, tudo o que aponta para o que foi apagado. | `Audit18DataTest.apagarCategoriaLevaOsRemanejamentos` |
| ERR-020 | S1 | todas | Risco permanente: uma versão nova do banco perder dados de quem já usa. | Mudou o banco = versão nova do Room, migração e teste de migração; backups antigos continuam abrindo. | `MigrationTest.atualizaDa16Para17SemPerderNada`, `MigrationTest.atualizaDa11Para13SemPerderNada`, `PrdConformanceTest.schemaDoBancoAtualExportado` |
| ERR-021 | S1 | 1.2 | O backup automático podia apagar ou substituir o arquivo de outro celular, ou gravar por cima de um backup bom quando os dados sumiam. | Só mexe no que este celular gravou; se muita coisa sumiu, pausa e avisa; falha ao gravar não deixa arquivo vazio. | `AutoBackupTest.substituirNaoApagaArquivoDeOutroCelular`, `AutoBackupTest.seguraQuandoSomeMuitaCoisa`, `AutoBackupTest.falhaAoGravarNaoDeixaArquivoVazioERegravaDepois` |

## Importação de extratos

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-022 | S1 | 1.3 | O pagamento da fatura entrava duas vezes (pela fatura e pelo extrato da conta). | Pagamento de fatura é reconhecido e casado entre os dois extratos. | `StatementImportTest.pagamentoImportadoPelaFaturaNaoEntraDeNovoPeloExtratoDaConta` |
| ERR-023 | S1 | 1.3 | A chave de importação era global (escondia movimentações de outra conta) e o mesmo período em outro formato entrava em dobro. | A chave leva conta ou cartão **e** formato; o mesmo período em outro formato passa pela busca de repetidas. | `StatementImportTest.mesmoPeriodoEmOutroFormatoPareceRepetidoMasOCafezinhoDeTodoDiaNao`, `StatementImportTest.pdfEDepoisPlanilhaDoMesmoPeriodoPareceRepetido` |
| ERR-024 | S2 | 1.3 | A parcela seguinte (mesma data, descrição e valor) era escondida como "já importada". | No cartão, "já importada" vale só dentro da mesma fatura. | `StatementImportTest.mesmaLinhaEmOutraFaturaEOutraParcela` |
| ERR-025 | S2 | 1.3 | Datas sem ano ("05/12") ganhavam o ano errado na virada do ano. | Data sem ano usa o ano da data completa mais recente do arquivo, voltando um ano se cair mais de 31 dias depois. | `StatementImportTest.datasSemAnoNaViradaDoAno` |
| ERR-026 | S1 | 1.3 | PDF sem sinais: entradas e saídas trocadas; o saldo negativo decidia os sinais; o "-" do saldo colava no valor. | Em PDF sem sinais, quem decide são as palavras da linha, nunca o saldo. | `StatementImportTest.pdfDeContaSemSinaisNaoConfundeFaturaNemCompraNoCredito`, `StatementImportTest.pdfSemSinaisComSaldoNegativoContinuaPelasPalavras`, `StatementImportTest.pdfComSaldoNegativoNaColunaAoLado` |
| ERR-027 | S2 | 1.3 | Fatura com valor em dólar e nome do titular era lida errada; OFX trazia linhas de saldo e de valor zero como movimentações. | Colunas extras e linhas que não são movimentação ficam de fora. | `StatementImportTest.faturaComValorEmDolarNomeDoTitularEParcelaEmColunas`, `StatementImportTest.ofxPulaLinhasDeSaldoEValorZero` |
| ERR-028 | S2 | 1.3 | "Marcar todas" marcava pagamento de fatura sem cartão escolhido; o desfazer não tratava erro. | O que não dá para importar certo avisa em vez de entrar; desfazer tira tudo o que entrou. | `ImportFlowTest.marcarTodasAvisaPagamentoDeFaturaSemCartao`, `ImportFlowTest.desfazerTiraTudoQueEntrou` |

## Telas, textos e compilação

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-029 | S2 | 1.5 | O painel do mês fechava sozinho: itens de tabelas diferentes com a mesma chave na lista (categoria 1 e remanejamento 1). | Chave de `LazyColumn` leva prefixo por tipo (`"b$id"`, `"a$id"`). | `Screens17Test.painelDoMes` |
| ERR-030 | S3 | 1.5 | Com a fonte do sistema em 200%, rótulos curtos quebravam no meio da palavra e escondiam o botão de voz. | Abas e atalhos usam `CappedFontScale`; o resto cresce sem limite. | `Screens17Test.fonteGrande` |
| ERR-031 | S3 | 1.4 | Texto escrito direto no código ou faltando em alguma das 10 línguas. | Todo texto em `strings.xml`, nas 10 línguas, no mesmo commit, com os mesmos marcadores. | `PrdConformanceTest.asDezLinguasTemTodosOsTextos`, `PrdConformanceTest.nenhumTextoEscritoNoCodigo` |
| ERR-032 | S2 | 1.1 | O APK do GitHub saía diferente do que o F-Droid compilava. | Não mexer em `vcsInfo`, `dependenciesInfo` nem nas tarefas de ArtProfile; a voz não traz rede. | `PrdConformanceTest.vozSemRedeECompilacaoReproduzivel` |
| ERR-033 | S3 | 1.4 | Animação infinita travava os testes e gastava bateria. | Animação sempre finita e pelas APIs do Compose (DS-MOV-10). | `PrdConformanceTest.semAnimacaoInfinita` |

## Previsão

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-034 | S2 | 1.5 | A faixa "de 80%" da previsão do mês continha o valor real em só 68% dos casos. | Faixa prometida é faixa conferida contra o passado (RF-PRV-05). | `ForecastTest.erraMenosQueOMotorDa15EAFaixaCumpreOQuePromete` |
| ERR-035 | S2 | 1.5 | Ideias "óbvias" pioravam a previsão (somar o viés dos erros, modelos nervosos, pulso do mês, época com um ano só). | Mudança no previsor só entra medida: precisa errar menos que a régua (o motor da 1.5) no laboratório. | `ForecastTest.erraMenosQueOMotorDa15EAFaixaCumpreOQuePromete`, `ForecastTest.categoriasFechamOMesComMenosErroQueORitmoDa15`, `ForecastTest.epocaQueSeRepetiuEmDoisAnosEntraNaPrevisao` |
| ERR-036 | S2 | 1.5 | Rentabilidade de investimento distorcida por um aporte grande; projeção com poucos dados. | Rentabilidade é a do investimento (ponderada no tempo); com menos de 6 períodos, não projeta. | `ForecastTest.aporteGrandeNaoMudaARentabilidadeEPoucoHistoricoNaoProjeta` |
| ERR-037 | S2 | 1.5 | A previsão por categoria dava alarme falso (61% de chance de passar) num mercado gasto todo no começo do mês: uma fatia fixa de peso ia para os modelos que erravam feio. O laboratório não viu porque os mundos inventados compravam em dias sorteados. | O peso de cada modelo vem do erro encolhido para o erro médio, nunca de uma fatia fixa. Toda mudança no previsor passa no caso do mercado do começo do mês e no mundo `MONTHLY_SHOP` do laboratório. | `ForecastTest.mercadoDoComecoDoMesNaoDaAlarmeFalso`, `EngineTest.ritmoDaCategoriaSemAlarmeNoComeco` |
| ERR-038 | S3 | 1.5 | Com um investimento só, a Carteira mostrava uma projeção de 5 anos diferente da tela do investimento (os cenários usavam sementes diferentes). | Mesmos dados, mesmo número em todas as telas: a semente dos cenários vem dos próprios dados (os investimentos da conta), nunca de onde a conta é chamada. | `ForecastTest.investimentoComSobeEDesce` |

## Achados em uso (da 1.5 em diante, na ordem em que apareceram)

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-039 | S1 | 1.5 | Com o bloqueio ligado, fechar o app (tirar dos Recentes) e abrir de novo em menos de 1 minuto não pedia a digital: o "desbloqueado" morava no processo, que continuava vivo. E ligar a chave não confirmava nada. Existia desde a 1.3; achado pelo Hanry no teste da 1.5. | Toda abertura do zero (tela criada sem estado salvo) pede o desbloqueio; só giro de tela e recriações do próprio app ficam de fora. Ligar o bloqueio exige passar por ele uma vez. | `DevModeFlowTest.abrirDoZeroPedeDesbloqueioMesmoComOProcessoVivo`, `DevModeFlowTest.girarATelaNaoPedeDesbloqueioDeNovo` |
| ERR-040 | S1 | 1.5 | A linha do extrato que pagava uma conta fixa entrava sem ligação: a conta fixa seguia pendente (e virava "atrasada"), a previsão do mês contava a despesa duas vezes e o "Já paguei" criava uma segunda movimentação. Existia desde a 1.3; achado pelo Hanry ("valores estranhos depois de importar"). | Pagamento que está no extrato com o nome e o valor da conta fixa conta como pago (ligado na importação ou reconhecido pelo `Ledger`); a previsão nunca soma a mesma conta fixa duas vezes. | `BillMatchTest.previsaoNaoContaEmDobroOQueJaEstaNoExtrato`, `BillFlowTest.importarOExtratoDeixaAContaFixaPagaComUmaMovimentacaoSo` |
| ERR-041 | S2 | 1.5 | Conta fixa paga à mão numa conta e a cobrança chegando depois pelo extrato de outra conta ou do cartão: entrava como movimentação nova (em dobro). | A linha que repete um pagamento de conta fixa já feito à mão aparece como "parece repetida" e desmarcada, em qualquer conta ou cartão. | `BillFlowTest.pagaAMaoNaContaEExtratoDoCartaoNaoEntraEmDobro` |
| ERR-042 | S2 | 1.5 | Desfazer o pagamento de uma conta fixa apagava a movimentação, mesmo quando ela tinha vindo do extrato (o lançamento sumia do saldo); e duas movimentações podiam ficar ligadas à mesma conta fixa no mesmo mês. | Movimentação importada nunca é apagada ao desfazer: só perde a ligação. Uma conta fixa tem no máximo um pagamento por mês (ligar recusa o segundo; "Juntar" fica com a do extrato). | `BillFlowTest.ligarDesligarEJuntarNoBanco` |
| ERR-043 | S1 | 1.5 | Estorno no cartão: não dava para lançar à mão (o formulário exigia uma conta, contra o RF-CAR-07) e, ao editar um estorno importado, ele abria sem origem, pedia uma conta e saía do cartão: a fatura subia e a conta ganhava uma receita que não existiu. Achado por quem usa (issue #3 do GitHub). | Receita num cartão é estorno e fica no cartão, na fatura dela, ao criar e ao editar; nenhum salvar troca o cartão por uma conta sem a pessoa escolher. | `CardFixTest.editarEstornoImportadoFicaNoCartao`, `CardFixTest.estornoNoCartaoAMao` |
| ERR-044 | S1 | 1.5 | Editar uma compra do cartão (só o nome ou a categoria) recalculava a fatura pela data: a compra importada na fatura certa do banco pulava para outra, e o total da fatura deixava de bater "mesmo depois de corrigir tudo". Existia desde a 1.0; achado por quem usa (issue #3 do GitHub). | Salvar um lançamento do cartão mantém a fatura em que ele está; ela só volta a seguir a data quando a pessoa muda a data ou o cartão, ou escolhe outra fatura (`TxDraft.invoiceFor`). | `CardFixTest.editarCompraNaoMudaAFatura` |
| ERR-045 | S2 | 1.5 | "Já paguei" tocado na notificação gravava sem olhar o extrato: com o pagamento já importado, o toque não fazia nada (a conta fixa ficava só "reconhecida", sem a confirmação que a pessoa deu) e, antes da 1.5, criava uma segunda movimentação. Achado na auditoria da 1.5. | Todo caminho que marca uma conta fixa como paga (tela, notificação, chave "já paguei" do editor) passa por `FinanceRepository.markBillPaidLinking`: primeiro procura o pagamento que já existe e liga a ele; só grava um novo quando não há nenhum. | `AlertActionsTest.jaPagueiComOPagamentoNoExtratoSoLiga`, `BillFlowTest.jaPagueiAoCriarLigaAoQueExisteEMaisNadaDepois` |
| ERR-046 | S3 | 1.5 | A barra de mensagem ("Lançamento salvo · Desfazer") cobria o botão "Lançar" do Extrato: para lançar de novo era preciso esperar a barra sumir, ou arriscar tocar no "Desfazer". Existia desde a 1.4; achado por quem usa (issue #3, fredericofqd). | A barra de mensagem nunca cobre um botão flutuante: o botão sobe a altura medida da barra enquanto ela está à vista (`LocalFabLift`, `Modifier.fabLift()`). | `Screens19Test.aBarraDeDesfazerNaoCobreOBotaoDeLancar` |
| ERR-047 | S3 | 1.5 | Os botões flutuantes ("Lançar", "Nova dívida", "Nova categoria") não tinham nome para o leitor de tela: o Material esconde o texto de dentro do botão estendido e o botão era anunciado só como "Botão". Existia desde a 1.0; achado na auditoria do ERR-046. | Todo botão flutuante leva o seu nome em `contentDescription`, além do texto que aparece. | `Screens19Test.aBarraDeDesfazerNaoCobreOBotaoDeLancar` |
| ERR-048 | S2 | 1.5 | Em Ajustes › Avisos e sons, trocar o tom (ou qualquer chave) só aparecia depois de sair e voltar da tela. Ela até lia o contador de mudanças, mas **nunca usava o valor**, e o Compose só redesenha o que ele vê ser lido. Achado pelo Hanry no teste da 1.5. | Tela que mostra ajustes guardados fora do banco lê o estado por um valor de verdade (aqui, `rememberAlertUi`), nunca por uma variável declarada e não usada. | `Screens19Test.trocarOTomDoAvisoApareceNaHora` |
| ERR-049 | S1 | 1.5 | No extrato em PDF, o fim de um CNPJ na mesma linha colava no valor: "…/0001-49 512,30" virava **R$ 49.512,30** (o leitor aceita milhar separado por espaço, que é o costume de outros países). Um lançamento assim destrói o saldo. Existia desde a 1.3; achado no extrato de verdade do Hanry. | Antes de procurar o valor, CNPJ, CPF e documentos com `/` são mascarados (`maskDocuments`), e um valor nunca começa logo depois de um dígito seguido de `-` ou `/`. | `StatementImportTest.pdfComSecoesPorDiaCnpjEQuebraDePagina` |
| ERR-050 | S1 | 1.5 | O cabeçalho que se repete em toda página do extrato ("01 de novembro de 2026 a 13 de novembro de 2026") era lido como a data do dia: tudo o que vinha depois de uma quebra de página ia para o dia 1. | Data no começo da linha seguida de "a/to/au/bis…" e outra data é período, não dia: não muda o dia corrente. | `StatementImportTest.pdfComSecoesPorDiaCnpjEQuebraDePagina` |
| ERR-051 | S1 | 1.5 | Num extrato que agrupa o dia em "Total de entradas" e "Total de saídas", um traço do próprio texto ("LTDA - 87,50") era lido como sinal e invertia a movimentação: uma saída virava entrada e o saldo ficava errado. | Quando o extrato agrupa por seção, **a seção decide** o sinal de quem não tem marca; um traço solto antes do valor ("- 87,50") é separador do texto, não sinal. O limite desta regra é o ERR-052. | `StatementImportTest.pdfComSecoesPorDiaCnpjEQuebraDePagina` |
| ERR-052 | S1 | 1.5 | O conserto do ERR-051 fez a seção mandar **sempre**. Só que o resumo no topo de muitos extratos ("Créditos 3.500,00", "Débitos 250,00") parece título de seção e ficava valendo até o fim do arquivo: um salário marcado pelo banco com "C" ou "+" virava **saída**. Achado na revisão antes de publicar (15/11/2026); não chegou a sair em nenhuma versão. | Dentro de uma seção, a **marca forte do banco vence**: letra "C"/"D", sinal colado no valor ("-150,00", "+3.500,00") e traço no fim ("100,00-"). A seção só decide quem não tem marca ou tem só um traço solto. Todo conserto no leitor é conferido também com extratos de outros moldes. | `StatementImportTest.pdfMarcaForteDoBancoVenceASecao` |
| ERR-053 | S2 | 1.5 | O botão "Estornar" copiava a categoria do original para o lançamento contrário: a receita do estorno ficava com categoria de **gasto** (e o contrário). "Mercado" aparecia entre as receitas em Análises e a categoria aprendida passava a sugerir categoria de gasto para dinheiro que entra. O botão também não tinha teste nenhum. Achado na revisão antes de publicar; não chegou a sair. | O estorno (`refundOf`) nunca leva o que é só do original: categoria, chave de importação, conta fixa, dívida e parcelas. Categoria e tipo do lançamento são sempre do mesmo lado. | `CardFixTest.estornarCriaOContrarioSemLevarACategoria` |
| ERR-054 | S1 | 1.5 | Depois de importar o extrato do mês, o saldo do Cryo ficava diferente do banco exatamente pelo valor que **já estava na conta antes do período** (achado pelo Hanry: a diferença era, no centavo, o saldo inicial do extrato). O PDF traz "Saldo inicial" e "Saldo final do período", mas o leitor ignorava os dois; só o OFX era lido, e mesmo assim o app apenas avisava, comparando com o saldo de "agora" e não com o do dia do extrato. Existia desde a 1.3. | O extrato que diz quanto havia antes faz o Cryo partir do mesmo ponto **sozinho** (acerto no saldo inicial da conta, nunca um lançamento), e a tela final mostra o saldo do banco ao lado do do Cryo no mesmo dia, com o que foi acertado e o desfazer. Conferir um extrato é conferir **três** coisas: entradas, saídas e o saldo de partida. | `ImportFlowTest.pdfComSaldoInicialAcertaOSaldoSozinho` |

## Regras sem guarda automática (conferir à mão)

Estas não dão para testar no computador; quem mexer na área confere e escreve no relatório da entrega:

- **Tela protegida no Android 8–12** (1.5): com o bloqueio ligado, `MainActivity.hideFromRecents` liga `FLAG_SECURE` (API < 33).
  Não remover: é o que esconde a miniatura em Recentes nesses aparelhos.
- **Release só depois do push** (1.4): criar a Release antes do push prendeu as etiquetas `v1.3` e `v1.4` no código antigo.
- **Nunca dar push, criar tag ou Release sem o "pode publicar" do Hanry**; commits sempre com a identidade pública dele.
- **Whisper**: o resultado final usa a janela inteira (a curta ouviu "32,40" no lugar de "32,50"); o som de "pode falar" não
  entra na gravação; a biblioteca só é carregada depois de conferir o processador.
- **Cofre do Android de verdade** (Keystore): o Robolectric não tem; o caminho real só roda no celular.
