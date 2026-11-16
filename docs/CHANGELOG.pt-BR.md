# Novidades

[English](../CHANGELOG.md) · **Português**

## 1.5 — voz no celular, motor financeiro, avisos inteligentes e contas fixas que se acertam com o extrato

Tudo o que foi feito depois da 1.4, numa versão só. Continua tudo rodando no seu celular, sem internet.

**Visual novo**
- **Ícone novo**: o C quadrado, agora dourado, com espessura e um fio de luz
- **Profundidade em todo o app**: cartões suaves sobre um fundo agrupado, listas agrupadas, botão principal com volume e uma vibração leve ao toque
- Ilustrações do próprio Cryo no lugar dos emojis de teclado; fonte grande (200%) conferida nas telas principais

**Anotar mais depressa**
- **Voz entendida no próprio celular** (Whisper, rodando no aparelho; o áudio nunca vira arquivo): uma tela de "eco" mostra as palavras enquanto você fala. Entende números por extenso, correções ("32, não, 35"), os seus apelidos e vários lançamentos numa frase só. Celulares que não conseguem rodá-lo usam o reconhecedor do próprio aparelho, sem internet
- **O botão de voz fica no meio da barra de baixo**, em todas as abas: tocar = falar, **segurar = escrever**

**Entender o seu dinheiro**
- **Motor financeiro**: descobre sozinho o que se repete (salário, aluguel, assinaturas, preço novo, atraso), mostra os próximos 3, 6 e 12 meses, aponta o que está fora do padrão e cobranças repetidas, dá a nota da saúde financeira (0 a 100, em seis partes), os juros embutidos nas dívidas com um plano para quitar, o rendimento real dos investimentos e quando cada meta chega lá
- **Previsão do mês com uma faixa que cumpre o que promete**: vários métodos simples, pesados por quanto cada um teria acertado nos seus próprios meses passados. "Entre X e Y, em 8 de cada 10 cenários" é conferido no seu passado antes de aparecer, e a previsão conta o que mais pesou
- **Orçamentos**: como cada categoria deve fechar o mês e a chance de passar do limite, um painel do mês e **remanejar o limite entre categorias só neste mês**
- **Investimentos, "se continuar como foi"**: onde eles podem estar em 1, 3 e 5 anos com os seus aportes de sempre

**Avisos que ajudam, sem chatice**
- **Avisos inteligentes, um de cada vez**: conta que vence amanhã (com **Já paguei** e **Lembrar amanhã** na própria notificação), orçamento perto do limite, fatura que fecha, dia do salário, resumos da semana e do mês. No máximo um aviso comum por dia, só em dois horários
- **Lembrete do dia** no horário que você escolher, só se nada foi anotado
- **Notificações animadas**, com uma figurinha desenhada para cada situação, e **modo compras ao vivo** (atualizações ao vivo do Android 16 e a Now Bar da One UI 8)
- **Momentos de comemoração**: meta alcançada, orçamento em 80% ou estourado (com a dica de onde cobrir), sequência de dias anotando, como o mês passado fechou
- **Sons do próprio Cryo**, suaves e abafados, com um fio do cristal de gelo por cima. Seguem o modo silencioso do celular e dá para desligar

**Contas fixas, cartões e o extrato do banco**
- **As contas fixas se acertam com o extrato**: ao importar, o Cryo reconhece a linha que paga uma conta fixa ("NETFLIX.COM 59,99" é a sua Netflix), liga a ela em vez de criar um segundo lançamento, e a conta fixa aparece como paga. A previsão não conta mais essa conta duas vezes
- **Arrumar o que já tinha sido importado**: Planejar › Contas fixas mostra as sugestões "pagamentos encontrados" e "pagas em dobro", um toque cada, sempre com desfazer. "Já paguei" procura o pagamento no extrato antes de criar outro, e desfazer nunca apaga um lançamento importado
- **Criar a conta fixa depois**: cadastre a conta fixa e o Cryo pergunta se os lançamentos que você já tem são ela. Ele também aprende o apelido que o banco usa ("DEB AUT CIA ENERG" = Luz), guardando só um código
- **Estorno no cartão de crédito, à mão** (pedido de quem usa): botão **Estorno** na tela do cartão, ou escolha o cartão numa receita. O estorno diminui a fatura, e editar um estorno o mantém no cartão
- **Total da fatura batendo com o banco** (pedido de quem usa): editar uma compra do cartão não a muda mais de fatura. Dá para **escolher a fatura** de uma compra, de um estorno ou de um pagamento, e **Ajustar total** lança a diferença para o que o banco mostra como um ajuste à vista, que dá para apagar

**Do seu jeito**
- **Seu perfil**: a bolinha do Início é um botão que abre o painel do perfil (no lugar da engrenagem). Use a sua inicial, um desenho do Cryo, um emoji ou **uma foto sua** (escolhida pelo seletor de fotos do Android, sem permissão nova; fica só neste celular)
- **"Aprender o Cryo"**: um guia local que percebe o que você ainda não usa e mostra uma dica por vez, no máximo uma por dia e três por semana, com **Me mostra** (abre a tela certa e destaca o ponto) e **Agora não**. Uma lista com as 18 lições mostra o que você já usa. São regras simples rodando no celular, sem IA; o guia só guarda contagens e datas

**Protegido**
- **Backup com senha** (opcional, AES-256): a senha é pedida ao restaurar e nunca fica guardada. Os lembretes não mostram nome nem valor na tela de bloqueio
- **Bloqueio mais firme**: toda abertura do zero pede o desbloqueio, e ligar o bloqueio confirma uma vez. No Android 8 a 12, com o bloqueio ligado, o app não aparece em capturas de tela nem nos apps recentes
- **Correções de uma revisão do código inteiro**: backup protegido nunca é gravado sem proteção, arquivo de backup estragado ou grande demais é recusado com uma mensagem clara, e a planilha exportada não roda fórmulas escritas em descrições
- Todo erro já corrigido agora tem um teste que falha se ele voltar (cerca de 940 testes automáticos)

- **Nem toda conta fixa é mensal** (pedido de quem usa): escolha o ritmo — todo mês, a cada 2, 3 ou 6 meses, ou uma vez por ano (IPVA, seguro, assinatura anual) —, o primeiro vencimento, em quantas parcelas paga cada ciclo, o preço à vista para ver quanto o parcelamento cobra de juros, e até quando a conta vale. Modelos na sua língua preenchem nome, ritmo e dia
- **Escolher qualquer fatura do cartão**: os chips mostram as vizinhas e "Outra…" abre a lista inteira do cartão, com o total de cada uma, para a compra antiga cair no mês em que o banco pôs
- **A previsão responde em uma frase** se a conta fica negativa, a partir de quando, e qual o menor saldo previsto
- **Apoiar o Cryo**: Ajustes › Sobre leva ao Buy Me a Coffee. O app continua sem permissão de internet — quem abre a página é o navegador
- A barra de desfazer não cobre mais o botão "+", e os botões flutuantes passaram a ter nome para o leitor de tela
- Banco de dados versão 5: as contas fixas ganharam ritmo, parcelas, preço à vista e prazo. As que já existem continuam mensais, do mesmo jeito

- **Saldo igual ao do banco, sozinho**: o extrato só traz o que se mexeu no período; o que já estava na conta é o saldo inicial dela. O leitor de PDF passa a ler o saldo do começo e o do fim, a importação acerta o saldo inicial da conta sozinha (sem criar lançamento) e a tela final mostra as etapas como o banco — saldo antes, entradas, saídas, saldo no fim — ao lado do saldo do Cryo no mesmo dia, com desfazer. Vale também para o OFX (o saldo de antes é deduzido do saldo do próprio arquivo), e reabrir um extrato que já está no Cryo acerta o saldo sem importar nada de novo. Se sobrar diferença, um toque em "Acertar o saldo" resolve.
- **Busca com período e por valor**: a busca do Extrato deixa escolher onde procurar — tudo, um mês ou um ano — e entende valor ("32" acha 32,00 e 32,50), além de nome, observação e categoria. Mostra quantos lançamentos achou e quanto entrou e saiu.
- **Conta fixa paga por fora**: ao marcar como paga, dá para escolher "Não mexer no saldo de nenhuma conta", para o que você pagou em dinheiro que o Cryo não acompanha. A conta fica paga e o mês continua contando, mas nenhum saldo muda.
- **Apagar uma importação inteira**: "Importações feitas" lista cada importação com o que ela trouxe e apaga tudo de uma vez, com desfazer. Apagar tira só o que veio daquele arquivo
- **Estornar uma movimentação**: um toque cria o lançamento contrário e o original fica no histórico
- **Leitura de extrato corrigida em três pontos** (achados com um extrato de verdade): o CNPJ ao lado do valor não entra mais no valor, o cabeçalho da página não joga mais o que vem depois para o dia 1, e um traço no meio da descrição não transforma mais uma saída em entrada. Com isso, um extrato de verdade passa a bater com o banco no centavo
- Ajustes › Avisos agora muda na hora, e não só depois de sair da tela

**Bom saber**
- Permissões novas: **microfone** (pedido só no primeiro toque no botão de voz) e **notificações promovidas** (as atualizações ao vivo do Android 16; não pergunta nada). Continua sem permissão de internet
- O app passa de uns 6 MB para uns 65 MB, porque o modelo de fala vai dentro dele
- Banco de dados versão 4 (remanejar orçamento no mês, dias sem gastos, ilustrações das metas); backups antigos continuam abrindo

## 1.4 — 10 línguas, moedas e visual novo

- **10 línguas**: português do Brasil, inglês, espanhol, francês, alemão, italiano, japonês, coreano, chinês (simplificado) e hindi. O Cryo segue a língua do celular (inglês, se ela não for uma das 10) e dá para trocar em Ajustes › Idioma e moeda, na hora
- **Moeda do app**: real, dólar, euro, libra, iene, won, yuan, rupia e outras (20 ao todo), com o jeito de escrever valores de cada país. A padrão vem do país do celular. Trocar a moeda não converte valores: muda só o símbolo (o Cryo não tem internet nem cotação)
- **Registro por frase e voz em todas as línguas**: "lunch 25 on debit", "almuerzo 12,50 con tarjeta de débito", "コーヒー500円", "점심 12000원 카드로", "午饭50元", "चाय 20 रुपये नकद". Entende valores como 1,234.56, 1万5千円 e 3만원, datas no formato de cada país e parcelas ("10 installments", "12回払い", "分12期")
- **Importar extratos de outros países**: colunas com nomes nas 10 línguas, datas com o mês antes do dia (EUA) descobertas pelo próprio arquivo, valores no formato de cada país, arquivos em Shift_JIS, EUC-KR/CP949 e GB18030, PDF em outros formatos e pagamento de fatura reconhecido também em outras línguas
- **Visual novo**, mais organizado, com as cores do Cryo: cabeçalho colorido no Início, seções limpas e atalhos. Agora são 4 abas: Início, Extrato, Planejar e Análises; a Carteira (contas, cartões, investimentos e dívidas) abre pelo Início
- **Restaurar backup na primeira abertura**: quem troca de celular já começa com os seus dados
- **Como usar o Cryo**: um passeio animado de 5 páginas mostra, na primeira abertura, como registrar por frase, a previsão do mês, cartões e parcelas, o planejamento e a privacidade, na língua e na moeda de quem usa. Dá para ver de novo em Ajustes › Sobre
- **Animações suaves**: as telas deslizam ao abrir e voltar, as abas trocam com um fade, o saldo conta até o novo valor, barras e gráficos crescem ao aparecer e o Extrato anima o que entra e sai. Com "Remover animações" ligado no Android, tudo aparece direto
- As categorias padrão são criadas na língua do app, com palavras-chave do país; as de quem já usa não mudam
- A loja (F-Droid) também está nas 10 línguas, com as imagens das telas em cada uma
- **Página do GitHub nas 10 línguas**, com o inglês como principal
- Os dados de exemplo do modo de teste ficam na escala de cada moeda (um salário em ienes ou wons parece de verdade)
- **Licença**: a partir desta versão, o Cryo é distribuído sob a GNU AGPL v3 ou posterior (até a 1.3, GNU GPL v3 ou posterior). Continua software livre, sem anúncios e sem internet

## 1.3 — importar extrato e modo desenvolvedor

- **Importar extrato do banco** (Ajustes › Seus dados, ou o botão ↥ na tela de uma conta ou de um cartão): OFX, CSV, planilha XLSX (e o ".xls" que muitos bancos geram) e PDF, lidos no próprio celular
- Antes de importar, você confere a lista: categoria sugerida em cada linha (pelas palavras-chave e pelo que você já lançou), e dá para mudar a descrição, a categoria ou deixar a linha de fora
- O que já está no Cryo fica desmarcado: movimentações importadas antes e as que parecem com algo que já está lá (mesmo valor, até 3 dias de diferença), inclusive o pagamento da fatura que já entrou pelo extrato do cartão
- Fatura do cartão: escolha em qual fatura as compras entram (ou pela data de cada compra); se o banco exporta as compras como valores positivos, o Cryo vira os sinais sozinho e avisa
- Pagamento de fatura no extrato da conta vira pagamento do cartão (o gasto não conta duas vezes); estornos entram como crédito na fatura
- CSV e planilha: o Cryo acha as colunas sozinho, mesmo com o nome do banco antes da tabela; quando não tem certeza, pergunta. Faturas com a parcela numa coluna própria ou com compras em dólar (como a do C6) também. PDF com senha também funciona (a senha não é guardada)
- Depois de importar: comparação com o saldo informado pelo banco e botão para desfazer tudo
- **Modo desenvolvedor** (7 toques na versão, em Ajustes › Sobre): modo de teste com um banco de dados separado e dados de exemplo, extratos de exemplo em todos os formatos, JSON de teste e relatório da leitura com versão anônima para pedir ajuda sem mostrar os seus dados. Nada do modo de teste chega aos dados de verdade
- Lembretes e backup automático continuam cuidando dos dados de verdade mesmo no modo de teste
- Correção: o bloqueio com digital ou senha agora também aparece quando o Android fecha o app em segundo plano e a tela volta sozinha
- Depois de desbloquear, o app volta exatamente para a tela em que você estava (antes voltava para o Início)
- Com o bloqueio ligado, nada do app aparece por trás da tela de bloqueio (nem uma janela que estava aberta), e a miniatura do Cryo em Recentes não mostra os seus dados (Android 13 ou mais novo)

## 1.2 — backup automático

- **Backup automático** (Ajustes › Backup automático): o Cryo salva seus dados sozinho, todo dia, toda semana ou todo mês
- Numa pasta do celular: substituir sempre o mesmo arquivo ou guardar várias cópias (5, 10, 30 ou todas); só as cópias automáticas mais antigas são apagadas, os outros arquivos da pasta nunca são mexidos
- Na nuvem: um arquivo no Google Drive, OneDrive, Nextcloud ou outra nuvem instalada no celular, atualizado a cada backup. Quem envia o arquivo é o app da nuvem: o Cryo continua sem permissão de internet
- Pasta e nuvem podem ser usadas juntas, e o botão "Fazer backup agora" salva na hora
- Se nada mudou desde o último backup, nenhuma cópia igual é gravada
- Proteções: o backup automático só substitui ou apaga arquivos que este celular gravou; ao escolher um lugar que já tem outro backup, o Cryo mostra os dois e pergunta (e dá para restaurar dali mesmo, útil ao trocar de celular); se outro celular alterar o arquivo, ou se muitas movimentações sumirem de repente, o backup pausa e avisa
- Notificação quando um backup não pode ser salvo
- Em Ajustes, "Fazer backup" agora se chama "Salvar uma cópia agora"

## 1.1 — dívidas e arquivo

- Nova área de **Dívidas**: empréstimos, financiamentos, acordos, cartão/cheque especial e dinheiro devido a pessoas
- Mostra quanto falta, parcelas pagas e atrasadas, quanto da renda vai para parcelas e quando você fica livre das dívidas
- Pagamento de parcela com juros ou multa, adiantamento, quitação com desconto e correção do saldo
- "Peguei emprestado" entra na conta sem contar como receita
- As parcelas aparecem em Planejar › Contas fixas, na previsão do mês, nos alertas do Início e nos lembretes
- O registro por frase entende "paguei a parcela da moto" e "peguei 300 emprestado com o João"
- Patrimônio passa a descontar as dívidas
- Novo arquivo de metas (Planejar › Metas › Metas arquivadas) e de contas, cartões e investimentos (Carteira › Arquivados), com opção de restaurar
- Backups da versão 1.0 continuam funcionando
- Compilação reproduzível: o APK do GitHub e o do F-Droid são idênticos e assinados pela mesma chave

## 1.0 — primeira versão pública

- Registro por frase e por voz ("almoço 32,50 no débito", "tv 1200 em 10x no cartão")
- Visão geral com saldo, patrimônio e resumo do mês
- Previsão do fim do mês e limite de gasto por dia
- Calendário de calor dos gastos e fluxo do dinheiro
- Cartões de crédito com faturas, limite e compras parceladas
- Contas fixas com lembrete de vencimento
- Orçamento por categoria com avisos aos 80% e 100%
- Metas e caixinhas, investimentos e transferências entre contas
- Bloqueio por digital, rosto ou senha; modo para esconder valores
- Backup em arquivo e exportação para planilha (CSV)
- Material You, tema claro e escuro
