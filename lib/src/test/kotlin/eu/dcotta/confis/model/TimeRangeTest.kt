package eu.dcotta.confis.model

import eu.dcotta.confis.dsl.days
import eu.dcotta.confis.dsl.months
import eu.dcotta.confis.dsl.of
import eu.dcotta.confis.dsl.plus
import eu.dcotta.confis.dsl.year
import eu.dcotta.confis.dsl.years
import eu.dcotta.confis.model.circumstance.Month.January
import eu.dcotta.confis.model.circumstance.Month.June
import eu.dcotta.confis.model.circumstance.Month.May
import eu.dcotta.confis.model.circumstance.Month.September
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TimeRangeTest : StringSpec({

    val may = 1 of May year 2020
    val june = 1 of June year 2020
    val sept = 1 of September year 2020

    "larger time range generalises smaller one" {
        (may..sept) generalises (may..june) shouldBe true

        (CircumstanceMap.of(may..sept) generalises CircumstanceMap.of(may..june)) shouldBe true
    }

    "smaller time range does not generalise bigger one" {
        (may..june) generalises (may..sept) shouldBe false
    }

    "equal time ranges generalise each other" {
        (may..sept) generalises (may..sept) shouldBe true
    }

    "a single-point time range is generalised by a large one" {
        (may..sept) generalises (may..may) shouldBe true

        (CircumstanceMap.of(may..sept) generalises CircumstanceMap.of(may..may)) shouldBe true
    }

    "no circumstances is more general than a time range" {

        (CircumstanceMap.empty generalises CircumstanceMap.of(may..may)) shouldBe true
    }

    "can sum days in DSL" {
        may.plus(31.days) shouldBe june
    }

    "can sum months in DSL" {
        may.plus(1.months) shouldBe june
    }

    "can sum years in DSL" {
        ((1 of January year 2020) + 1.years) shouldBe (1 of January year 2021)
    }
})
