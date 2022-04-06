package eu.dcotta.confis.eval

import eu.dcotta.confis.eval.allowance.AllowanceQuestion
import eu.dcotta.confis.eval.allowance.ask
import eu.dcotta.confis.model.Action
import eu.dcotta.confis.model.Agreement
import eu.dcotta.confis.model.AllowanceResult.Allow
import eu.dcotta.confis.model.AllowanceResult.Depends
import eu.dcotta.confis.model.AllowanceResult.Forbid
import eu.dcotta.confis.model.AllowanceResult.Unspecified
import eu.dcotta.confis.model.Obj
import eu.dcotta.confis.model.Party
import eu.dcotta.confis.model.Purpose.Commercial
import eu.dcotta.confis.model.Purpose.Research
import eu.dcotta.confis.model.Sentence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class QueryableAgreementTest : StringSpec({

    val aliceEatsCake = Sentence(Party("alice"), Action("eat"), Obj("cake"))
    val bobEatsCake = Sentence(Party("bob"), Action("eat"), Obj("cake"))
    val aliceEatsCookie = Sentence(Party("alice"), Action("eat"), Obj("cookie"))

    "can answer simple sentence query" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cake by thing
            val cookie by thing

            alice may { eat(cake) }
            alice mayNot { eat(cookie) }
        }

        a.ask(AllowanceQuestion(aliceEatsCake)) shouldBe Allow
        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Forbid
        a.ask(AllowanceQuestion(bobEatsCake)) shouldBe Unspecified
    }

    "can answer on puroses simple" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cookie by thing

            alice may { eat(cookie) } asLongAs {
                with purpose (Research)
            }
        }
        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Allow
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Commercial)) shouldBe Unspecified
    }

    "can answer on purposes" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cake by thing
            val cookie by thing

            alice may { eat(cake) } unless {
                with purpose Commercial
            }

            alice may { eat(cookie) } asLongAs {
                with purpose (Research)
            }
        }

        a.ask(AllowanceQuestion(aliceEatsCake)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCake, purpose = Commercial)) shouldBe Forbid
        a.ask(AllowanceQuestion(aliceEatsCake, purpose = Research)) shouldBe Allow

        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Allow
    }

    "purposes in rules create precedence between them" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cake by thing
            val cookie by thing

            alice may { eat(cake) } unless {
                with purpose Commercial
            }

            // alice cannot eat cookies unless it is for research
            alice mayNot { eat(cookie) }
            alice may { eat(cookie) } asLongAs {
                with purpose Research
            }
        }

        a.ask(AllowanceQuestion(aliceEatsCake)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCake, purpose = Commercial)) shouldBe Forbid
        a.ask(AllowanceQuestion(aliceEatsCake, purpose = Research)) shouldBe Allow

        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Allow
    }

    "double negation well handled" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cookie by thing

            alice mayNot { eat(cookie) } unless {
                with purpose Research
            }
        }

        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Allow
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Commercial)) shouldBe Forbid
    }

    "negation overruled because it precedes some exception" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cookie by thing

            alice mayNot { eat(cookie) }
            alice mayNot { eat(cookie) } unless {
                with purpose Research
            }
        }

        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Allow
    }

    "can handle mayNot asLongAs" {
        val a = Agreement {

            val alice by party
            val eat by action
            val cookie by thing

            alice mayNot { eat(cookie) } asLongAs {
                with purpose Research
            }
        }

        a.ask(AllowanceQuestion(aliceEatsCookie)) shouldBe Depends
        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Research)) shouldBe Forbid

        a.ask(AllowanceQuestion(aliceEatsCookie, purpose = Commercial)) shouldBe Unspecified
    }
})
