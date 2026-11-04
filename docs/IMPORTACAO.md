# Importar extratos

> Este guia é um anexo do **[PRD](PRD.md)** (requisitos RF-IMP, seção 6.13). Em caso de conflito, vale o PRD.

O Cryo lê o extrato que você baixa do banco e mostra uma lista para conferir antes de qualquer coisa entrar no app. O arquivo é lido no próprio celular: o Cryo continua sem permissão de internet.

## Como usar

1. Baixe o extrato no app ou no site do banco. Normalmente fica em **Extrato › Exportar** (ou **Compartilhar**). Para o cartão de crédito, abra a fatura e procure **Exportar**.
2. No Cryo, vá em **Ajustes › Seus dados › Importar extrato do banco**. Também dá para começar pela tela de uma conta ou de um cartão (botão ↥ no alto): aí ela já vem escolhida como destino.
3. Escolha o arquivo, confira a lista e toque em **Importar**.

Se algo der errado, a tela final tem **Desfazer importação**, que tira tudo o que aquele arquivo colocou.

### Formatos

| Formato | Como o Cryo lê |
|---|---|
| **OFX** (recomendado) | É o formato feito para isso: datas e valores exatos, e cada movimentação tem um identificador do banco, então o Cryo sabe o que já foi importado. Lê o OFX antigo (SGML) e o novo (XML), em UTF-8 ou Windows-1252. |
| **CSV** | Descobre o separador (`;` `,` tabulação `\|`), a codificação e as colunas pelo cabeçalho, mesmo que ele venha depois de algumas linhas com o nome do banco e o período. Entende valor com sinal, colunas separadas de entrada e saída, coluna "C/D" e coluna "Parcela" (vira "- Parcela 2/3" na descrição). Ignora o valor em dólar, a cotação e o nome de quem tem o cartão (faturas com compras internacionais). Se não tiver certeza, pergunta quais são as colunas. |
| **Planilha (XLSX)** | Igual ao CSV, usando as datas e os números de verdade da planilha. Muitos bancos chamam de "Excel (.xls)" um arquivo que por dentro é uma página com uma tabela: esse também é lido. O `.xls` antigo de verdade (binário) não é: abra e salve como `.xlsx`. |
| **PDF** | Lido pelo texto do arquivo, procurando data, descrição e valor em cada linha. Quando o banco marca só as saídas (com "-" ou "D"), o que vem sem sinal é entrada; quando marca só as entradas ("+" ou "C"), o que vem sem sinal é saída; sem sinal nenhum, decidem as palavras ("compra", "pix recebido"...). Saldos e totais não entram nessa conta. Cada banco desenha o PDF de um jeito, então confira com atenção. PDF com senha: o Cryo pergunta a senha e não guarda. PDF que é foto ou digitalização não tem texto e não dá para ler. Precisa do Android 15, ou do 12 ao 14 com a atualização do leitor de PDF do sistema. |

Linhas de saldo ("Saldo anterior", "Saldo do dia") e de totais ficam de fora sozinhas. Datas sem ano ("05/12") ganham o ano da data completa mais recente do arquivo: numa fatura que vence em janeiro, "05/12" é dezembro do ano anterior.

### A conferência

Cada linha vem marcada ou desmarcada, com a categoria sugerida (pelas suas palavras-chave e pelo que você já lançou antes). Toque numa linha para mudar a descrição, a categoria ou se ela entra. Ficam **desmarcadas** de início:

- **Já importada**: veio numa importação anterior para a mesma conta ou cartão, no mesmo formato (mesmo identificador do banco, ou mesma data, valor e descrição). No cartão, só conta como já importada se estiver na mesma fatura: a parcela de novembro de uma compra antiga pode vir com a mesma data, descrição e valor da parcela de outubro, e é outra parcela.
- **Parece repetida**: já existe no Cryo algo com o mesmo valor, na mesma conta ou cartão, até 3 dias de diferença: algo que você lançou à mão, o pagamento da fatura que já entrou pelo extrato do cartão, ou o mesmo período importado antes em outro formato (OFX e depois CSV, PDF e depois planilha). Movimentações iguais de dias diferentes que vieram do mesmo extrato, como o café de todo dia, não contam como repetidas.
- **Pagamento de fatura**: no extrato da conta, o pagamento do cartão. Se ele entrasse como gasto, as compras contariam duas vezes (no cartão e na conta). Toque na linha e escolha o cartão: ele vira um pagamento de fatura, como em **Cartão › Pagar fatura**. Se ele for marcado sem o cartão (por exemplo, com **Marcar todas**), um aviso explica que vai entrar como gasto comum.
- **Pagamento da fatura** no extrato do cartão: é o mesmo pagamento, visto do lado do cartão. Diga de qual conta saiu, ou deixe de fora se ele já entrou pelo extrato da conta.

Estornos, reembolsos e créditos na fatura entram marcados e diminuem o valor da fatura.

### Fatura do cartão

- **Em qual fatura**: o Cryo sugere a fatura da compra mais recente, e todas as compras do arquivo entram nela (o certo para uma fatura fechada, inclusive parcelas de compras antigas). Para um arquivo com compras de vários meses, escolha **Pela data de cada compra**.
- **Sinais**: muitos bancos exportam a fatura com as compras positivas. Quando a maioria dos valores vem positiva, o Cryo vira os sinais sozinho e avisa. Se estiver errado, use o menu ⋮ › **Inverter entradas e saídas**.

### Saldo

Quando o arquivo informa o saldo (o OFX costuma informar), a tela final mostra o saldo do extrato e o do Cryo. Se forem diferentes, pode ter ficado algo desmarcado, ter faltado alguma movimentação antes do período do arquivo, ou o saldo inicial da conta precisa de ajuste (Carteira › conta › editar).

## Modo desenvolvedor

Para quem quer testar o Cryo ou ajudar a melhorar a leitura de extratos.

- **Ligar**: toque 7 vezes na versão do app, em **Ajustes › Sobre**. Aparece a seção **Desenvolvedor**.
- **Desligar**: Ajustes › Modo desenvolvedor › **Desligar o modo desenvolvedor**.

No modo de teste (abaixo), a importação também aceita o **JSON de teste**.

### Modo de teste

**Usar dados de teste** troca o que as telas mostram por outro banco de dados (`cryo-teste.db`), com ajustes próprios e 4 meses de dados de exemplo. Os seus dados ficam guardados, sem mudança nenhuma, e voltam quando você sair (pela faixa **Modo de teste · Sair**, acima da barra de abas, ou pela tela do modo desenvolvedor).

- Os lembretes e o backup automático continuam cuidando dos **seus dados de verdade** o tempo todo.
- O bloqueio com digital ou senha vale também no modo de teste.
- **Recriar os dados de exemplo** e **Começar do zero** (com a apresentação inicial) só mexem no modo de teste.
- Dá para restaurar um backup dentro do modo de teste (Ajustes › Restaurar backup): ele vai para o banco de teste, e os dados de verdade não mudam.
- **Salvar uma cópia agora** no modo de teste grava `cryo-teste-backup-<data>.json`, marcado como dados de teste: fora do modo de teste, o Cryo se recusa a restaurar esse arquivo por cima dos seus dados.
- O backup automático e a transferência para outro celular nunca levam os dados de teste.

### Arquivos de exemplo

No modo de teste, **Testar com um arquivo de exemplo** abre a importação já com um extrato inventado: OFX de conta, CSV de conta (no jeito do Nubank), CSV de fatura, planilha XLSX com cabeçalho do banco e linhas de saldo, JSON de teste e PDF. **Salvar os exemplos numa pasta** grava todos eles, para testar pelo seletor de arquivos. Os exemplos só podem ser importados no modo de teste: com os dados de verdade, o Cryo reconhece e recusa.

### Relatório da leitura

Mostra como o último arquivo foi lido: formato, separador, codificação, colunas, cada movimentação encontrada (com a linha do arquivo de onde ela veio) e cada linha que ficou de fora, com o motivo.

- **Anônimo**: troca as letras por `x` e os números por `9`, mas mantém o formato de cada linha (separadores, sinais, tamanho). Palavras comuns de extrato, como "saldo", "pix" e "pagamento", continuam, porque explicam o que a linha é sem dizer nada sobre você. O nome do arquivo vira `arquivo.<extensão>`.
- **Completo**: tem as suas movimentações de verdade. Use só para você.

## Ajudar a melhorar a leitura

Se o extrato do seu banco não foi lido direito:

1. **Nunca mande o arquivo de verdade** (nem em issue, nem por e-mail).
2. Ligue o modo desenvolvedor, importe o arquivo e abra **Relatório da última leitura** na versão **Anônimo**.
3. Abra uma issue em <https://github.com/H3yK0/cryo/issues> com o banco, o formato (OFX, CSV, XLSX ou PDF), o que saiu errado e o relatório anônimo.

Com o formato das linhas, dá para criar um arquivo de teste inventado com a mesma estrutura e corrigir a leitura sem nunca ver os seus dados.

## Formato JSON de teste

Só no modo de teste do modo desenvolvedor. Serve para montar casos de teste à mão.

```json
{
  "cryoImport": 1,
  "card": false,
  "account": "Banco de teste",
  "transactions": [
    { "date": "2026-10-01", "description": "Padaria", "amount": -12.50, "id": "t1" },
    { "date": "2026-10-02", "description": "Salário", "amount": 3500, "id": "t2" }
  ]
}
```

| Campo | Obrigatório | Significado |
|---|---|---|
| `cryoImport` | não | Versão do formato (hoje, `1`). Arquivos de versão mais nova são recusados. |
| `card` | não | `true` se for fatura de cartão (o Cryo sugere um cartão como destino). |
| `account` | não | Nome da conta, só para mostrar. |
| `transactions` | sim | A lista. Também vale mandar só a lista (`[ {...}, {...} ]`). |
| `date` | sim | Data no formato `AAAA-MM-DD` (também aceita `DD/MM/AAAA`). |
| `description` | não | Texto da movimentação. |
| `amount` | sim | Valor em reais: negativo = saiu, positivo = entrou. Número (`-12.5`) ou texto (`"-12,50"`). |
| `id` | não | Identificador único: com ele, a mesma movimentação nunca é importada duas vezes. |

Linhas com problema (sem data, sem valor, valor zero) ficam de fora e aparecem no relatório. Um backup do Cryo (`cryo-backup-....json`) é reconhecido e o app indica **Ajustes › Restaurar backup**.

## Para quem programa

| Arquivo | O que faz |
|---|---|
| `importer/StatementReader.kt` | Descobre o formato pelo conteúdo (não pelo nome) e chama o leitor certo |
| `importer/Ofx.kt`, `Tables.kt` (CSV e colunas), `Xlsx.kt`, `HtmlTable.kt`, `PdfStatement.kt`, `JsonStatement.kt` | Um leitor por formato; todos devolvem um `Statement` (valores em centavos, negativo = saída) |
| `importer/PdfText.kt` | Texto do PDF com o leitor do próprio Android (sem bibliotecas de fora) |
| `importer/Values.kt` | Datas, valores e textos no jeito brasileiro |
| `importer/ImportPlan.kt` | A conferência: categorias, repetidas, pagamentos de fatura, e a conversão em movimentações |
| `importer/ImportReport.kt` | O relatório e a versão anônima |
| `importer/SampleFiles.kt` | Os extratos de exemplo |
| `ui/screens/ImportModel.kt`, `ImportScreen.kt` | O passo a passo na tela |
| `ui/screens/DevScreens.kt`, `data/Dev.kt`, `data/SampleData.kt` | Modo desenvolvedor e modo de teste |

Cada movimentação importada guarda uma chave (`importKey`) com a conta ou o cartão de destino, o formato do arquivo e a linha: `a3|ofx.id:<identificador do banco>` quando o arquivo tem um, ou `c7|csv.h:<hash de data, valor e descrição>`, com `#1`, `#2`... quando a mesma linha se repete no arquivo. É ela que reconhece o que já foi importado. Uma importação anterior com a mesma conta ou cartão e o mesmo formato e tipo de chave só é "a mesma linha" se a chave for igual (o café de todo dia não vira repetido); de outro formato, ela passa pela busca de repetidas (mesmo valor, até 3 dias).

Testes: `StatementImportTest` (leitores e conferência), `ImportFlowTest` (a tela, de ponta a ponta) e `DevModeFlowTest` (modo desenvolvedor, modo de teste e bloqueio).

Para ensinar o Cryo a ler um banco novo, escreva um teste com um arquivo **inventado** que tenha a mesma estrutura do relatório anônimo e ajuste o leitor (por exemplo, os nomes de colunas em `Columns`, ou os padrões de `PdfStatement`).
