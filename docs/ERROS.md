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
| ERR-003 | S1 | 1.8 | Qualquer erro do sensor de digital liberava o app, mesmo com trava de tela no aparelho. | Erro do sensor só libera em aparelho **sem** trava de tela (`LockPolicy.unlockOnError`). Na dúvida, continua bloqueado. | `Audit18DataTest.erroDoSensorSoAbreEmAparelhoSemTrava` |
| ERR-004 | S1 | 1.8 | Com o backup com senha ligado, se o cofre do Android falhasse o backup saía **sem proteção**. | Proteção ligada falha fechada: sem chave não grava (`BackupProtection.Unavailable`). Nunca cair para "aberto". | `Audit18DataTest.protecaoLigadaSemCofreNaoGravaBackupAberto` |
| ERR-005 | S2 | 1.8 | Backup estragado, gigante ou adulterado derrubava o app ou entrava com valores e datas absurdos. | Todo arquivo de fora tem limite de tamanho (`Backup.MAX_BYTES`), é lido fora da linha principal e conferido campo a campo; erro vira mensagem. | `Audit18DataTest.backupEstragadoERecusadoComMensagemClara` |
| ERR-006 | S2 | 1.8 | A planilha exportada (CSV) executava fórmulas escritas em descrições (`=`, `+`, `-`, `@`). | Texto da pessoa que vai para planilha passa por `esc()`, que neutraliza fórmulas. | `Audit18DataTest.planilhaNaoRodaFormulasDeDescricoes` |
| ERR-007 | S1 | 1.3 | O relatório "anônimo" da importação vazava cabeçalhos, nome do arquivo e notas. | Nada que veio do arquivo da pessoa aparece no relatório anônimo. | `StatementImportTest.relatorioAnonimoNaoMostraNadaDaPessoa`, `StatementImportTest.notasDoRelatorioAnonimoNaoMostramOQueVeioDoArquivo` |
| ERR-008 | S1 | todas | Risco permanente: biblioteca ou recurso novo pedir internet ou outra permissão. | Sem `INTERNET`. Só as permissões listadas no PRD (RNF-PRIV). | `PrdConformanceTest.semInternetEPermissoesSoAsCombinadas`, `PrdConformanceTest.motorSemAndroidESemRede` |
| ERR-009 | S1 | 1.5 | Dados da pessoa podiam ir parar em logs do sistema. | Nada de `Log`, `println` ou `printStackTrace` (RF-SEG-07). | `PrdConformanceTest.nadaEmLogs` |
| ERR-010 | S1 | todas | Risco permanente: extrato de verdade ou a chave de assinatura irem para o git. | Testes só com arquivos inventados; `keystore/` fora do git. | `PrdConformanceTest.semExtratosNemChaveNoGit` |
| ERR-011 | S2 | 1.8 | Dois avisos diferentes abriam a mesma tela (o Android tratava os `PendingIntent` como iguais). | Cada `PendingIntent` de aviso tem uma ação própria (`…alert.open.<id>`). | `AlertActionsTest.cadaAvisoAbreASuaTela` |

## Dados de teste nunca chegam aos dados de verdade

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-012 | S1 | 1.3 | Extratos de exemplo, JSON de teste e backups do modo de teste podiam entrar nos dados de verdade. | Exemplos e JSON de teste só no modo de teste; cópia do modo de teste sai marcada e não restaura por cima dos dados de verdade. | `ImportFlowTest.jsonDeTesteEExemplosSoNoModoDeTeste`, `StatementImportTest.copiaDoModoDeTesteFicaMarcada`, `DevModeFlowTest.modoDeTesteNaoMexeNosDadosDeVerdade` |
| ERR-013 | S2 | 1.3 | Uma fatura de verdade (Uber, Spotify, iFood) era tomada por arquivo de exemplo e recusada. | Exemplo se reconhece por descrição **e** valor, nunca só pela descrição. | `StatementImportTest.faturaDeVerdadeComOsMesmosLugaresDosExemplosNaoEExemplo` |
| ERR-014 | S2 | 1.3 | Um aviso de conta (dados de verdade) tocado com o modo de teste ligado abria os dados de teste. | Avisos e backup automático usam sempre `CryoApp.real`; o aviso sai do modo de teste antes de abrir. | `DevModeFlowTest.avisoDeContaComOModoDeTesteLigadoAbreOsDadosDeVerdade` |

## Dinheiro e banco de dados

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-015 | S1 | 1.8 | Um valor com muitos algarismos "dava a volta" e virava outro número (frase, extrato, JSON). | Todo texto ou número de fora vira centavos por `Money.centsOrNull` (teto de 13 algarismos). Nunca `toLong()` direto. | `Audit18Test.valorEnormeNaoDaAVolta` |
| ERR-016 | S2 | 1.8 | Frases estranhas ou hostis no registro rápido. | A frase nunca derruba o app nem inventa valor: ou entende, ou devolve nada. | `Audit18Test.fraseHostilNaoDerrubaNemInventaValor` |
| ERR-017 | S2 | 1.8 | Dividir uma compra em 0 parcelas dividia por zero. | `CardMath.split` com 1 parcela ou menos devolve o total. Toda divisão confere o divisor. | `Audit18Test.parcelasNuncaDividemPorZero` |
| ERR-018 | S1 | 1.8 | Tocar "Já paguei" duas vezes (app e notificação) gravava dois pagamentos. | "Olhar e depois gravar" no banco é sempre dentro de `db.withTransaction`. | `Audit18DataTest.pagueiDuasVezesGravaUmPagamentoSo`, `AlertActionsTest.jaPagueiMarcaAContaUmaVezSo` |
| ERR-019 | S2 | 1.8 | Apagar uma categoria deixava remanejamentos de orçamento apontando para o nada. | Apagar leva junto, na mesma transação, tudo o que aponta para o que foi apagado. | `Audit18DataTest.apagarCategoriaLevaOsRemanejamentos` |
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
| ERR-029 | S2 | 1.7 | O painel do mês fechava sozinho: itens de tabelas diferentes com a mesma chave na lista (categoria 1 e remanejamento 1). | Chave de `LazyColumn` leva prefixo por tipo (`"b$id"`, `"a$id"`). | `Screens17Test.painelDoMes` |
| ERR-030 | S3 | 1.7 | Com a fonte do sistema em 200%, rótulos curtos quebravam no meio da palavra e escondiam o botão de voz. | Abas e atalhos usam `CappedFontScale`; o resto cresce sem limite. | `Screens17Test.fonteGrande` |
| ERR-031 | S3 | 1.4 | Texto escrito direto no código ou faltando em alguma das 10 línguas. | Todo texto em `strings.xml`, nas 10 línguas, no mesmo commit, com os mesmos marcadores. | `PrdConformanceTest.asDezLinguasTemTodosOsTextos`, `PrdConformanceTest.nenhumTextoEscritoNoCodigo` |
| ERR-032 | S2 | 1.1 | O APK do GitHub saía diferente do que o F-Droid compilava. | Não mexer em `vcsInfo`, `dependenciesInfo` nem nas tarefas de ArtProfile; a voz não traz rede. | `PrdConformanceTest.vozSemRedeECompilacaoReproduzivel` |
| ERR-033 | S3 | 1.4 | Animação infinita travava os testes e gastava bateria. | Animação sempre finita e pelas APIs do Compose (DS-MOV-10). | `PrdConformanceTest.semAnimacaoInfinita` |

## Previsão

| ID | Sev. | Versão | O que acontecia | Lei | Guarda |
|---|---|---|---|---|---|
| ERR-034 | S2 | 1.8 | A faixa "de 80%" da previsão do mês continha o valor real em só 68% dos casos. | Faixa prometida é faixa conferida contra o passado (RF-PRV-05). | `ForecastTest.erraMenosQueOMotorDa15EAFaixaCumpreOQuePromete` |
| ERR-035 | S2 | 1.8 | Ideias "óbvias" pioravam a previsão (somar o viés dos erros, modelos nervosos, pulso do mês, época com um ano só). | Mudança no previsor só entra medida: precisa errar menos que a régua (o motor da 1.5) no laboratório. | `ForecastTest.erraMenosQueOMotorDa15EAFaixaCumpreOQuePromete`, `ForecastTest.categoriasFechamOMesComMenosErroQueORitmoDa15`, `ForecastTest.epocaQueSeRepetiuEmDoisAnosEntraNaPrevisao` |
| ERR-036 | S2 | 1.8 | Rentabilidade de investimento distorcida por um aporte grande; projeção com poucos dados. | Rentabilidade é a do investimento (ponderada no tempo); com menos de 6 períodos, não projeta. | `ForecastTest.aporteGrandeNaoMudaARentabilidadeEPoucoHistoricoNaoProjeta` |

## Regras sem guarda automática (conferir à mão)

Estas não dão para testar no computador; quem mexer na área confere e escreve no relatório da entrega:

- **Tela protegida no Android 8–12** (1.8): com o bloqueio ligado, `MainActivity.hideFromRecents` liga `FLAG_SECURE` (API < 33).
  Não remover: é o que esconde a miniatura em Recentes nesses aparelhos.
- **Release só depois do push** (1.4): criar a Release antes do push prendeu as etiquetas `v1.3` e `v1.4` no código antigo.
- **Nunca dar push, criar tag ou Release sem o "pode publicar" do Hanry**; commits sempre com a identidade pública dele.
- **Whisper**: o resultado final usa a janela inteira (a curta ouviu "32,40" no lugar de "32,50"); o som de "pode falar" não
  entra na gravação; a biblioteca só é carregada depois de conferir o processador.
- **Cofre do Android de verdade** (Keystore): o Robolectric não tem; o caminho real só roda no celular.
