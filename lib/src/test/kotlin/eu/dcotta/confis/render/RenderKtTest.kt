package eu.dcotta.confis.render

import eu.dcotta.confis.dsl.of
import eu.dcotta.confis.dsl.rangeTo
import eu.dcotta.confis.dsl.year
import eu.dcotta.confis.model.Agreement
import eu.dcotta.confis.model.Month.May
import eu.dcotta.confis.model.Purpose.Commercial
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

@Suppress("unused_variable")
class RenderKtTest : StringSpec({

    "agreement contains parties header and lists parties" {
        val a = Agreement {
            val alice by party(named = "Alice Liddell", description = "Alice Pleasance Hargreaves of Westminster")
            val bob by party(description = "Bob the Builder")
            val Charlie by party
        }

        val md = a.renderMarkdown()

        md shouldContain "## 1 - Parties"

        md shouldContain "1. Alice Pleasance Hargreaves of Westminster (**Alice Liddell**)"
        md shouldContain "2. Bob the Builder (**bob**)"
        md shouldContain "3. **Charlie**"
    }

    "agreement contains actions and lists them" {
        val md = Agreement {
            val alice by party
            val pay by action
            val notify by action(description = "Notify by email")

            alice may pay(alice)
            alice may notify(alice) asLongAs {
                with purpose Commercial
                after { alice did pay(alice) }
            }

            -"""
                Some useless text clause
            """
        }.renderMarkdown()

        println(md)

        md shouldContain """_"pay"_"""
        md shouldContain """_"notify"_: Notify by email"""
    }

    "render title and intro" {
        val md = Agreement {
            title = "My Agreement"
            introduction = "For specifying the terms and conditions of life"
        }.renderMarkdown()

        md shouldContain "# My Agreement"
        md shouldContain "terms and conditions of life"
    }

    "render simple permission clause" {
        val md = Agreement {
            val alice by party
            val pay by action

            alice may pay(alice)
        }

        md.clauses.first().renderMd(1) shouldContain "1. alice may pay alice"
    }

    "render simple permission clause with time circumstances" {
        val a = Agreement {
            val alice by party
            val pay by action

            alice may pay(alice) asLongAs {
                within { (1 of May)..(3 of May) year 2022 }
            }
        }

        val rendered = a.clauses.first().renderMd(1)
        rendered shouldContain "1. alice may pay alice under the following circumstances"
    }
})
