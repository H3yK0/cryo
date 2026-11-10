# Novidades

[English](../CHANGELOG.md) · **Português**

## 1.8 — previsão nova, conferida no seu próprio passado

- **Previsão nova, feita do zero** (continua toda no celular, sem modelo de IA): sete métodos simples olham o seu passado, cada um de um jeito, e o Cryo dá mais peso a quem teria acertado mais nos seus próprios meses anteriores. Nos testes com históricos inventados, a previsão do mês erra ~7% menos que a da 1.7, e até 57% menos para quem faz uma compra grande do mês
- **A faixa cumpre o que promete**: "entre X e Y, em 8 de cada 10 cenários" agora é conferida nos seus meses passados antes de aparecer (nos testes, acertou 80% das vezes; antes, 68%), e o cartão conta como foi: "conferida no seu passado: acertou em 10 de 12 meses"
- **A previsão se explica**: o que mais pesou na conta e se esta época do ano costuma sair mais cara para você (só quando isso se repetiu nos dois anos anteriores)
- **Orçamentos**: cada categoria mostra como deve fechar o mês e a chance de passar do limite; os avisos "no ritmo atual" usam a previsão nova
- **Investimentos, "se continuar como foi"**: quanto o investimento em si rendeu, o sobe e desce, a maior queda, onde ele pode estar em 1, 3 e 5 anos com os seus aportes de sempre e a chance de terminar acima do que você colocou. É o passado projetado, nunca promessa
- **Correções de segurança e confiabilidade**, de uma revisão do código inteiro: backup com senha nunca é gravado sem proteção; arquivo de backup estragado ou grande demais é recusado com uma mensagem clara; a planilha exportada não roda mais fórmulas escritas nas descrições; erro do sensor de digital só libera o app em celular sem trava de tela; no Android 8 a 12, com o bloqueio ligado, não dá para tirar print do Cryo; valores enormes não "dão a volta" e viram outro número; tocar "Já paguei" duas vezes grava um pagamento só; apagar uma categoria leva junto os remanejamentos do mês; cada notificação abre a sua própria tela; com o bloqueio ligado, fechar o Cryo e abrir de novo sempre pede o desbloqueio (antes, não pedia no primeiro minuto), e ligar o bloqueio agora confirma na hora com a digital, o rosto ou a senha do celular
- Nenhuma permissão nova e nenhuma mudança no banco de dados

## 1.7 — avisos inteligentes, sons e o botão de voz ao alcance do polegar

- **O botão de voz agora fica no meio da barra de baixo**, em todas as abas, onde o polegar alcança (como na One UI da Samsung): tocar = falar, **segurar = escrever**. O campo de frase do Início fica só para escrever
- **Avisos inteligentes, um de cada vez**: conta que vence amanhã (com **Já paguei** e **Lembrar amanhã** na própria notificação), orçamento perto ou acima do limite, categoria no ritmo de passar, fatura que fecha amanhã, dia do salário ("Registrar salário" com o valor de sempre), salário atrasado, assinatura que ficou mais cara, cobrança repetida, gasto fora do comum, saldo que vai ficar negativo, meta quase lá, última parcela, saúde financeira que subiu, a semana no domingo e como o mês fechou
- **Nada de chatice**: no máximo um aviso comum por dia, só em dois horários (de manhã e no horário do lembrete), nada para quem acabou de chegar além das contas; tipos que você dispensa sempre passam a vir menos. Tudo calculado no celular
- **Lembrete do dia no horário que você escolher** (o Cryo pergunta uma vez): só se nada foi anotado no dia, com os botões **Dia sem gastos** e **Anotar**
- **Notificações animadas**: cada aviso vem como uma mensagem do Cryo com uma figurinha animada desenhada para a situação (o Android só anima neste estilo de conversa). Nenhum emoji de teclado: todas as imagens são do próprio Cryo
- **Modo compras ao vivo**: "Vou às compras, até R$ 300" mostra uma notificação ao vivo com quanto já foi e quanto falta (atualizações ao vivo do Android 16 e a Now Bar da One UI 8)
- **Momentos de comemoração**: meta completa (tela cheia, com cristais de gelo), orçamento em 80% ou estourado (a barra passa da linha e o cartão treme, com a dica de onde cobrir), sequência de dias anotando (3, 7, 14, 30...) e como o mês passado fechou
- **Painel dos gastos do mês** com todas as categorias com orçamento e as sem orçamento; **remanejar o orçamento entre categorias só neste mês**, com desfazer
- **Sons próprios do Cryo** (de "cristal de gelo", feitos do zero): salvar, entrou dinheiro, atenção, conquista, começo e fim da voz. Sempre seguem o modo silencioso do celular e dá para desligar
- **Ajustes › Avisos e sons**: tom Amigo ou Discreto, o horário do lembrete, cada tipo de aviso com **Ver exemplo** e os sons
- **Voz**: o som de "pode falar" não entra mais na gravação junto com o que você diz; o modo desenvolvedor mostra o tempo e tem um modo mais rápido experimental para testar
- Fonte grande (200%) conferida nas telas principais
- Banco de dados versão 4 (remanejamentos do mês, dias sem gastos, ilustrações das metas); backups antigos continuam abrindo e as metas ganham a ilustração do emoji que tinham
- Permissão nova: **notificações promovidas** (as atualizações ao vivo do Android 16; não pergunta nada)

## 1.6 — voz entendida no celular

- **Registro por voz dentro do Cryo, com o Whisper** (um modelo aberto de reconhecimento de fala, dentro do app): o que você fala é entendido no próprio celular, sem internet. O áudio só passa pela memória e é apagado na hora; o Cryo continua sem permissão de internet
- **As palavras aparecem enquanto você fala**, com uma animação de eco: o microfone cresce com a sua voz, cada sílaba solta um anel e o que o Cryo entendeu (valor, categoria, cartão, data) salta em cartõezinhos. Termina sozinho quando você para de falar
- **Conhece os seus nomes**: contas, cartões, categorias e metas ajudam a ouvir certo ("Roxo", "Pet Shop")
- **Entende o jeito falado** (português primeiro): "dezoito e noventa" ou "18 e 90" = 18,90, "trinta e dois e cinquenta", "cento e vinte", "dois mil e quinhentos", "conto", apelidos como "roxinho" para o Cartão Roxo
- **Vários lançamentos de uma vez**: "almoço 30 e uber 15" vira dois lançamentos para conferir; o cartão ou a data ditos uma vez valem para todos
- **Correção falada**: "32, não, 35" fica 35
- Escolha o reconhecedor em Ajustes › Registro rápido › Reconhecimento de voz: Whisper (padrão) ou o do celular (só no aparelho). Celulares que não rodam o Whisper usam o do celular
- O app ficou maior (cerca de 65 MB) porque o modelo de voz vem dentro dele
- Permissão nova: **microfone**, pedida só no primeiro toque no microfone, com explicação

## 1.5 — motor financeiro, backups protegidos e visual novo

- **Motor financeiro no celular** (tudo sem internet): a previsão do fim do mês agora mostra uma faixa e a chance de fechar no vermelho, com 2.000 cenários simulados a partir do seu histórico, e erra ~38% menos que antes no teste com meses passados
- **Recorrências achadas sozinhas**: salário, aluguel, assinaturas; o Cryo avisa quando uma fica mais cara ou quando o salário atrasa, e vira conta fixa com um toque
- **Próximos meses**: o saldo dia a dia por 3, 6 ou 12 meses, com faturas, parcelas, contas fixas e dívidas, o dia mais apertado e a chance de ficar negativo
- **Nota de saúde financeira** (0 a 100) em 6 partes explicadas, com o que fazer para melhorar; gastos **fora do padrão** e **cobranças repetidas**; ritmo das categorias ("no ritmo atual, Alimentação passa do limite por volta do dia 24") e sugestões de limite mais espertas
- **Dívidas**: os juros embutidos de cada uma, um plano para quitar (maior juro primeiro ou menor dívida primeiro) com quanto você economiza, e o valor justo para antecipar parcelas
- **Investimentos**: rendimento real ao ano, contando cada aporte na sua data, com comparação opcional com a inflação; **metas** mostram quando você chega lá
- **Categoria aprendida**: o Cryo aprende como você classifica e ajuda na importação de extratos e no registro por frase
- **Backups protegidos com senha** (opcional, AES-256, chave no cofre seguro do Android); os lembretes de contas não mostram nome nem valor com o celular bloqueado
- 500 testes de garantia a mais (estabilidade, escalabilidade, desempenho, economia de recursos e segurança)
- **Ícone novo**: o C quadrado agora é dourado, com espessura e um fio de luz, sobre o mesmo azul do Cryo. A estrelinha saiu. Escolhido comparando 11 desenhos com pesquisa sobre atenção visual
- **Profundidade em todo o app**: o conteúdo fica em cartões com sombra suave sobre um fundo "agrupado", como nos melhores apps de iPhone; o cabeçalho do Início usa o degradê do ícone; atalhos e o botão principal têm volume e encolhem com mola ao toque
- **Listas agrupadas**: cada dia do Extrato, a fatura de um cartão e o histórico de uma conta formam um cartão só; os Ajustes ficam agrupados como no iPhone
- **Vibração ao toque**: ao confirmar, ligar e desligar, trocar de aba e de opção. Segue o ajuste "vibração ao toque" do celular e não precisa de permissão nova
- O ícone do Cryo aparece dentro do app (boas-vindas, bloqueio e Sobre)

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
