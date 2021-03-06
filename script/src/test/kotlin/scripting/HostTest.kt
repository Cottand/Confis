package eu.dcotta.confis.scripting

import eu.dcotta.confis.dsl.AgreementBuilder
import eu.dcotta.confis.model.Clause
import eu.dcotta.confis.model.circumstance.Date
import eu.dcotta.confis.model.circumstance.Month.April
import eu.dcotta.confis.model.circumstance.Month.May
import eu.dcotta.confis.model.circumstance.TimeRange
import eu.dcotta.confis.render.renderMarkdown
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beOfType
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics

class HostTest : StringSpec({
    "compile well-formed script" {

        val res = evalFile(File("src/test/resources/scripts/simple.confis.kts"))

        if (res !is ResultWithDiagnostics.Success) failAndPrint(res)

        val instance = res.value.returnValue.scriptInstance

        val i = (instance as AgreementBuilder)

        val assembled = AgreementBuilder.assemble(i)
        println(assembled)

        assembled.clauses shouldNotBe emptyList<Clause>()
    }

    // TODO FIXME
    "fails on nesting for top level clause building".config(enabled = false) {
        val res = evalFile(File("src/test/resources/scripts/nestedBad.confis.kts"))

        res should beOfType<ResultWithDiagnostics.Failure>()
    }

    "fails on unknown subject" {
        val res = evalFile(File("src/test/resources/scripts/unknownSubject.confis.kts"))

        res should beOfType<ResultWithDiagnostics.Failure>()
    }

    "fails on unknown object" {
        val res = evalFile(File("src/test/resources/scripts/unknownObject.confis.kts"))

        res should beOfType<ResultWithDiagnostics.Failure>()
    }

    "can define dates wihtout additional imports" {
        val res = evalFile(File("src/test/resources/scripts/dates.confis.kts"))

        if (res !is ResultWithDiagnostics.Success) failAndPrint(res)

        val instance = res.value.returnValue.scriptInstance as AgreementBuilder

        val assembled = AgreementBuilder.assemble(instance)

        (assembled.clauses.first() as Clause.PermissionWithCircumstances)
            .circumstances[TimeRange]
            .shouldBe(Date(1, May, 2022)..Date(13, April, 2023))
    }

    "must clause syntax" {
        val res = evalFile(File("src/test/resources/scripts/must.confis.kts"))

        if (res !is ResultWithDiagnostics.Success) failAndPrint(res)
    }

    "render" {
        val res = evalFile(File("src/test/resources/scripts/simple.confis.kts"))
        if (res !is ResultWithDiagnostics.Success) failAndPrint(res)

        val i = res.value.returnValue.scriptInstance as AgreementBuilder
        println(AgreementBuilder.assemble(i).renderMarkdown())
    }
})

private fun failAndPrint(res: ResultWithDiagnostics<EvaluationResult>): Nothing =
    fail("test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}")
