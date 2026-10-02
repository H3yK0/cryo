// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.data

import io.github.h3yk0.cryo.data.db.Category
import io.github.h3yk0.cryo.data.db.CategoryKind

/** Paleta usada para categorias, contas, cartões, metas e investimentos (boa leitura no claro e no escuro). */
object Palette {
    val colors: List<Int> = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1,
        0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E,
        0xFF6D4C41, 0xFF757575, 0xFF546E7A, 0xFF26A69A,
    ).map { it.toInt() }

    fun pick(i: Int): Int = colors[((i % colors.size) + colors.size) % colors.size]
}

object DefaultData {
    private fun c(name: String, kind: CategoryKind, icon: String, color: Long, keywords: String, order: Int) =
        Category(name = name, kind = kind, icon = icon, color = color.toInt(), keywords = keywords, sortOrder = order)

    const val DEBT_ICON = "debt"

    /** Categoria usada nos pagamentos de dívidas (também criada na atualização para a versão 1.1). */
    fun debtCategory() = c(
        "Dívidas e empréstimos", CategoryKind.EXPENSE, DEBT_ICON, 0xFFB23A48,
        "divida,dividas,emprestimo,emprestimos,financiamento,consignado,crediario,carne,prestacao,acordo,agiota",
        16,
    )

    fun categories(): List<Category> {
        val e = CategoryKind.EXPENSE
        val i = CategoryKind.INCOME
        return listOf(
            c("Alimentação", e, "restaurant", 0xFFF4511E, "almoco,almocei,jantar,janta,lanche,lanchei,restaurante,ifood,i food,pizza,hamburguer,burger,cafe,padaria,comida,marmita,acai,sorvete,lanchonete,salgado,pastel,churrasco,sushi,delivery,refeicao,cafeteria,doce,bolo,pao,coxinha,espetinho,self service", 0),
            c("Mercado", e, "cart", 0xFF43A047, "mercado,supermercado,feira,hortifruti,atacadao,assai,carrefour,acougue,sacolao,compras do mes,mercadinho,quitanda,atacarejo", 1),
            c("Transporte", e, "car", 0xFF1E88E5, "uber,99,taxi,onibus,metro,trem,gasolina,combustivel,etanol,alcool,diesel,posto,estacionamento,pedagio,oficina,mecanico,ipva,licenciamento,passagem,bilhete,brt,mototaxi,lava jato,lavagem,pneu,seguro do carro,troca de oleo", 2),
            c("Moradia", e, "home", 0xFF6D4C41, "aluguel,condominio,iptu,reforma,moveis,movel,manutencao,diarista,faxina,pedreiro,encanador,eletricista", 3),
            c("Contas da casa", e, "bolt", 0xFFFFB300, "luz,energia,agua,internet,gas,botijao,telefone,conta de luz,conta de agua,wifi,claro,vivo,tim,saneamento,enel,equatorial,saneago,sabesp,cemig,copel,celesc", 4),
            c("Saúde", e, "health", 0xFFE53935, "farmacia,remedio,remedios,medicamento,medico,consulta,exame,dentista,plano de saude,hospital,psicologo,terapia,drogaria,academia,vacina,oculos,fisioterapia,suplemento", 5),
            c("Educação", e, "school", 0xFF3949AB, "curso,faculdade,escola,livro,livros,material escolar,mensalidade,apostila,udemy,alura,matricula,ingles,aula", 6),
            c("Lazer", e, "movie", 0xFF8E24AA, "cinema,show,jogo,game,steam,playstation,xbox,passeio,festa,ingresso,balada,parque,teatro,bar,cerveja,boliche,clube,role", 7),
            c("Compras", e, "shopping", 0xFFD81B60, "roupa,roupas,tenis,sapato,shopping,loja,amazon,mercado livre,shopee,shein,aliexpress,eletronico,celular,fone,tv,televisao,notebook,computador,camiseta,calca,relogio,mochila,bolsa", 8),
            c("Assinaturas", e, "subscriptions", 0xFF5E35B1, "netflix,spotify,youtube,youtube premium,prime,prime video,disney,hbo,max,globoplay,icloud,google one,assinatura,chatgpt,claude,deezer,crunchyroll,paramount,apple music,game pass,microsoft 365,office", 9),
            c("Cuidados pessoais", e, "spa", 0xFFEC407A, "cabelo,barbeiro,barbearia,salao,cosmetico,perfume,manicure,unha,creme,shampoo,maquiagem,depilacao,corte", 10),
            c("Pets", e, "pets", 0xFF7CB342, "racao,veterinario,pet,petshop,pet shop,banho e tosa,areia,cachorro,gato", 11),
            c("Dízimo e doações", e, "volunteer", 0xFF00897B, "dizimo,oferta,doacao,doei,igreja,caridade,vaquinha,contribuicao", 12),
            c("Presentes", e, "gift", 0xFFFB8C00, "presente,presentes,aniversario,lembrancinha,amigo secreto", 13),
            c("Viagem", e, "flight", 0xFF039BE5, "viagem,hotel,passagem aerea,hospedagem,airbnb,pousada,aviao,rodoviaria,excursao", 14),
            c("Impostos e taxas", e, "receipt", 0xFF546E7A, "imposto,taxa,tarifa,juros,multa,iof,anuidade,cartorio,das,darf,inss,irpf", 15),
            debtCategory(),
            c("Outros gastos", e, "more", 0xFF757575, "", 17),

            c("Salário", i, "work", 0xFF2E7D32, "salario,holerite,contracheque,adiantamento,13o,decimo terceiro,ferias,plr,pagamento do mes", 20),
            c("Extras e freelas", i, "laptop", 0xFF00ACC1, "freela,freelance,bico,extra,servico,job,trabalho extra,comissao,bonus,gorjeta,hora extra", 21),
            c("Vendas", i, "sell", 0xFFC0CA33, "vendi,venda,vendas", 22),
            c("Rendimentos", i, "trending", 0xFF3949AB, "rendimento,rendimentos,juros recebidos,dividendos,cashback", 23),
            c("Reembolsos", i, "undo", 0xFF8E24AA, "reembolso,estorno,devolucao,ressarcimento", 24),
            c("Presentes recebidos", i, "redeem", 0xFFF4511E, "presente,mesada,ganhei de presente", 25),
            c("Outras entradas", i, "more", 0xFF757575, "", 26),
        )
    }

    /** Ícones disponíveis para categorias (chave -> descrição para leitores de tela). */
    val categoryIcons: List<Pair<String, String>> = listOf(
        "restaurant" to "Comida", "cart" to "Mercado", "car" to "Carro", "bus" to "Ônibus", "home" to "Casa",
        "bolt" to "Energia", "water" to "Água", "wifi" to "Internet", "phone" to "Celular", "health" to "Saúde",
        "fitness" to "Academia", "school" to "Estudos", "movie" to "Lazer", "game" to "Jogos", "shopping" to "Compras",
        "subscriptions" to "Assinaturas", "spa" to "Beleza", "pets" to "Pets", "volunteer" to "Doações",
        "gift" to "Presente", "flight" to "Viagem", "receipt" to "Taxas", "coffee" to "Café", "gas" to "Combustível",
        "baby" to "Bebê", "build" to "Reforma", "work" to "Trabalho", "laptop" to "Freela", "sell" to "Vendas",
        "trending" to "Rendimentos", "undo" to "Reembolso", "redeem" to "Presentes", "savings" to "Poupança",
        DEBT_ICON to "Dívidas",
        "money" to "Dinheiro", "star" to "Favorito", "more" to "Outros",
    )

    val goalEmojis = listOf("🎯", "🏠", "✈️", "🚗", "🎓", "💍", "🛡️", "📱", "💻", "🎮", "🏖️", "👶", "🐶", "🎸", "🏍️", "💰", "🎁", "⛪", "🏋️", "📚")
}
