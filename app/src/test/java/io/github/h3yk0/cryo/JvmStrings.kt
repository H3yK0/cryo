// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo

import io.github.h3yk0.cryo.domain.AppLocale
import io.github.h3yk0.cryo.domain.Res
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Para os testes de regras que rodam sem Android (sem Robolectric): os textos em português vêm direto do
 * `values/strings.xml`, como o app faria, e a língua e a moeda voltam ao padrão (português e real).
 */
object JvmStrings {
    private val strings = HashMap<String, String>()
    private val plurals = HashMap<String, Map<String, String>>()

    init {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/values/strings.xml"))
        val root = doc.documentElement
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            when (e.tagName) {
                "string" -> strings[e.getAttribute("name")] = unescape(e.textContent)
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    plurals[e.getAttribute("name")] = (0 until items.length).associate { k ->
                        val it = items.item(k) as Element
                        it.getAttribute("quantity") to unescape(it.textContent)
                    }
                }
            }
        }
    }

    private fun unescape(s: String) = s.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")

    private fun nameOf(cls: Class<*>, id: Int): String =
        cls.fields.firstOrNull { it.getInt(null) == id }?.name ?: error("id $id não está em ${cls.simpleName}")

    fun install() {
        AppLocale.reset()
        Res.text = { id, args -> String.format(AppLocale.locale, strings.getValue(nameOf(R.string::class.java, id)), *args) }
        Res.plural = { id, n, args ->
            val forms = plurals.getValue(nameOf(R.plurals::class.java, id))
            // Português: "one" para 0 e 1 (CLDR).
            val form = if (n in 0..1) forms["one"] ?: forms.getValue("other") else forms.getValue("other")
            String.format(AppLocale.locale, form, *args)
        }
    }
}
