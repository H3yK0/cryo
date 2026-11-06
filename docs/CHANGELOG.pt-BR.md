# Novidades

[English](../CHANGELOG.md) · **Português**

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
