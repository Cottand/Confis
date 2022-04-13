package eu.dcotta.confis.eval.inference

import eu.dcotta.confis.model.Allowance.Allow
import eu.dcotta.confis.model.Allowance.Forbid
import eu.dcotta.confis.model.CircumstanceMap
import eu.dcotta.confis.model.Clause
import eu.dcotta.confis.model.Clause.Rule
import eu.dcotta.confis.model.Clause.SentenceWithCircumstances
import eu.dcotta.confis.model.Clause.Text
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.plus

sealed interface CircumstanceResult {
    object Forbidden : CircumstanceResult
    data class UnderCircumstances(
        val circumstances: Set<CircumstanceMap>,
        val forbidden: Set<CircumstanceMap> = emptySet(),
    ) : CircumstanceResult

    data class Contradictory(val contradictions: Set<List<Clause>>) : CircumstanceResult
}

typealias CircumstancesToClauses = PersistentMap<CircumstanceMap, Clause>

interface CircumstanceContext {
    val q: CircumstanceQuestion
    var circumstances: CircumstancesToClauses
    var contradictions: PersistentSet<List<Clause>>
    var forbidden: CircumstancesToClauses
}

/**
 * Unlike allowance rules, a [CircumstanceRule] should
 * - **match when there are circumstances to add** to be able to perform the action
 * - **then add the new required circumstances** to the result
 */
data class CircumstanceRule(val case: CircumstanceContext.() -> Boolean, val then: CircumstanceContext.() -> Unit)

fun Clause.asCircumstanceRules(): List<CircumstanceRule> =
    when (this) {
        is Rule -> asCircumstanceRules(this)
        is SentenceWithCircumstances -> asCircumstanceRules(this)
        is Text -> emptyList()
    }

fun asCircumstanceRules(r: SentenceWithCircumstances): List<CircumstanceRule> = when (r.rule.allowance) {
    Allow -> when (r.circumstanceAllowance) {
        // may .. asLongAs
        Allow -> listOf(
            CircumstanceRule(
                case = { r.rule.sentence generalises q.s && r.circumstances !in circumstances },
                then = { circumstances += (r.circumstances to r) },
            ),
        )
        // may .. unless
        /**
         * TODO many things need to happen here
         * - probably need to enforce checking both allow and forbid sets every single time
         *  - if I do that then we can start discussing unspecified again :D
         * - decide if we reaaaly need all 4 variants.. it looks cool but we could accomplish the same with more
         * complex circumstances no?
         * -
         */
        Forbid -> listOf(
            // forbid when circumstances hold
            // contradiction detection
            CircumstanceRule(
                // if this forbid overlaps with any of the existing allowances
                case = {
                    r.sentence generalises q.s &&
                        circumstances.keys.any { it overlapsWith r.circumstances } &&
                        r !in contradictions.flatten()
                },
                then = {
                    val dissidents = circumstances.values
                    contradictions += setOf(dissidents + r)
                },
            ),
            // add to forbid set
            CircumstanceRule(
                case = { r.sentence generalises q.s && r.circumstances !in forbidden.keys },
                then = { forbidden += (r.circumstances to r) },
            ),
            // // allow any other time
            // CircumstanceRule(
            //    case = { r.sentence generalises q.s && CircumstanceMap.empty !in circumstances },
            //    then = { circumstances += (CircumstanceMap.empty to r) },
            // ),
        )
    }
    Forbid -> when (r.circumstanceAllowance) {
        Allow -> TODO()
        Forbid -> TODO()
    }
}

private fun asCircumstanceRules(r: Rule) = when (r.allowance) {
    Allow -> listOf(
        // TODO contradiction detection?
        CircumstanceRule(
            case = { r.sentence generalises q.s && CircumstanceMap.empty !in circumstances },
            then = { circumstances += (CircumstanceMap.empty to r) },
        ),
    )
    Forbid -> listOf(
        // contradiction detection
        CircumstanceRule(
            case = { r.sentence generalises q.s && circumstances.isNotEmpty() && r !in contradictions.flatten() },
            then = {
                // find the other clauses that disagree with this one
                val dissidents = circumstances.values
                contradictions += setOf(dissidents + r)
            },
        ),
    )
}