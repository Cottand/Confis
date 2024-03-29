package eu.dcotta.confis.dsl

import eu.dcotta.confis.Sentence
import eu.dcotta.confis.asOrFail
import eu.dcotta.confis.model.Action
import eu.dcotta.confis.model.Agreement
import eu.dcotta.confis.model.Allowance
import eu.dcotta.confis.model.Allowance.Allow
import eu.dcotta.confis.model.Allowance.Forbid
import eu.dcotta.confis.model.CircumstanceMap
import eu.dcotta.confis.model.Clause
import eu.dcotta.confis.model.Clause.PermissionWithCircumstances
import eu.dcotta.confis.model.Obj
import eu.dcotta.confis.model.Party
import eu.dcotta.confis.model.circumstance.Consent
import eu.dcotta.confis.model.circumstance.Month.June
import eu.dcotta.confis.model.circumstance.Purpose.Commercial
import eu.dcotta.confis.model.circumstance.Purpose.Research
import eu.dcotta.confis.model.circumstance.PurposePolicy
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

inline fun <reified M> Any.narrowedTo() = if (this is M) this else fail("$this should be of type ${M::class}")

@Suppress("UNUSED_VARIABLE")
class LicenseTest : StringSpec({

    "can define freetext clause" {

        val l = AgreementBuilder {
            -"This is a freetext clause"
        }

        l.clauses.first() shouldBe Clause.Text("This is a freetext clause")
    }

    "can declare actions" {
        AgreementBuilder {

            val copy by action
        }
    }

    "can declare title and description" {
        val a = AgreementBuilder {
            title = "The Big License"
            introduction = "Relating to use of the cookie by alice"
        }

        a.title shouldBe "The Big License"
        a.introduction shouldBe "Relating to use of the cookie by alice"
    }

    "can declare parties" {
        val l = AgreementBuilder {

            val alice by party
            val bob by party(named = "You!")
        }

        l.parties.shouldContainAll(Party("alice"), Party("You!"))
    }

    "can allow a declared action to a party" {
        val l = AgreementBuilder {
            val alice by party("alice")

            val bob by party("bob")

            val hug by action

            alice may { hug(bob) }
        }

        val sentence = l.clauses.first().narrowedTo<Clause.Permission>()

        sentence should {
            it.action.name shouldBe "hug"
            it.subject shouldBe Party("alice")
            it.obj shouldBe Party("bob")
            it.allowance shouldBe Allowance.Allow
        }
    }

    "can forbid a declared action to a party" {
        val l = AgreementBuilder {
            val alice by party("alice")

            val bob by party("bob")

            val hug by action

            alice mayNot { hug(bob) }
        }

        val sentence = l.clauses.first().narrowedTo<Clause.Permission>()

        sentence should {
            it.action.name shouldBe "hug"
            it.subject shouldBe Party("alice")
            it.obj shouldBe Party("bob")
            it.allowance shouldBe Allowance.Forbid
        }
    }

    "add purposes to a sentence" {
        val l = AgreementBuilder {
            val alice by party("alice")

            val bob by party("bob")

            val hug by action

            alice may { hug(bob) } asLongAs {
                with purpose (Research)
            }

            alice mayNot { hug(bob) } asLongAs {
                with purpose Commercial
            }
        }

        val clauses = l.clauses.filterIsInstance<Clause.PermissionWithCircumstances>()

        val (research, commercial) = clauses

        research should {
            it.circumstances shouldBe CircumstanceMap.of(PurposePolicy(Research))
            it.permission.allowance shouldBe Allow
            it.circumstanceAllowance shouldBe Allow
        }

        commercial should {
            it.circumstances shouldBe CircumstanceMap.of(PurposePolicy(Commercial))
            it.permission.allowance shouldBe Forbid
            it.circumstanceAllowance shouldBe Allow
        }
    }

    "add descriptions to parties" {
        val a = AgreementBuilder {
            val alice by party(description = "Alice in Wonderland")
        }

        a.parties.firstOrNull() shouldBe Party("alice", "Alice in Wonderland")
    }

    "add descriptions to actions" {
        val a = AgreementBuilder {
            val pay by action(description = "as in pay 10 EUR")
            val alice by party

            alice may pay(alice)
        }

        a.clauses.firstOrNull()?.asOrFail<Clause.Permission>()?.action shouldBe Action("pay", "as in pay 10 EUR")
    }

    "add descriptions to things" {
        val a = AgreementBuilder {
            val pay by action
            val alice by party
            val money by thing(named = "cash", description = "10€")

            alice may pay(money)
        }
        a.clauses.firstOrNull()?.asOrFail<Clause.Permission>()?.obj shouldBe Obj(named = "cash", description = "10€")
    }

    "can extract actions" {
        val a = AgreementBuilder {
            val pay by action
            val text by action
            val receive by action

            val unused by action

            val alice by party
            val money by thing(named = "cash", description = "10€")

            alice may pay(money)
            alice may pay(money) asLongAs {
                after { alice did receive(money) }
            }
            alice must pay(money) underCircumstances {
                after { alice did text(alice) }
            }
        }

        a.actions shouldHaveSize 3

        a.actions shouldContainAll listOf("pay", "text", "receive").map(::Action)
    }

    "add consent clauses" {
        val a = AgreementBuilder {
            val pay by action
            val alice by party
            val money by thing

            alice may pay(money) asLongAs {
                with consentFrom alice
            }
        }

        a.clauses.first().asOrFail<PermissionWithCircumstances>().circumstances shouldBe
            CircumstanceMap.of(
                Consent(
                    sentence = Sentence { "alice"("pay", Obj("money")) },
                    from = Party("alice")
                )
            )
    }

    "can jsonify simple agreement".config(enabled = false) {
        val a = AgreementBuilder {
            val alice by party
            val use by action
            val data by thing

            alice may use(data) asLongAs {
                within { (1 of June year 2022)..(30 of June year 2022) }
            }
        }
        val json = Json {
            allowStructuredMapKeys = true
            // serializersModule = SerializersModule {
            //    polymorphic(Circumstance.Key::class, TimeRange.Key::class, TimeRange.Key.serializer())
            // }
        }
        println(json.encodeToString(Agreement.serializer(), a))
    }
})
