package eu.dcotta.confis.model

import eu.dcotta.confis.dsl.AgreementBuilder
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.Serializable

@Serializable
data class Agreement(
    val clauses: List<Clause>,
    val parties: List<Party>,
    val title: String? = null,
    val introduction: String? = null,
) {
    val actions by lazy { clauses.flatMap { it.extractActions() }.toPersistentSet() }
    val objs by lazy { clauses.flatMap { it.extractObjs() }.toPersistentSet() }
}

fun Agreement(builder: AgreementBuilder.() -> Unit): Agreement = AgreementBuilder(builder)

sealed interface NoCircumstance

@Serializable
sealed interface Clause {
    @JvmInline
    @Serializable
    value class Text(val string: String) : Clause, NoCircumstance

    @Serializable
    data class PermissionWithCircumstances(
        val permission: Permission,
        val circumstanceAllowance: Allowance,
        val circumstances: CircumstanceMap,
    ) : Clause {
        val sentence by permission::sentence
    }

    @Serializable
    data class RequirementWithCircumstances(
        val sentence: Sentence,
        val circumstances: CircumstanceMap,
    ) : Clause

    @Serializable
    data class Requirement(val sentence: Sentence) : Clause, NoCircumstance {
        val subject by sentence::subject
        val obj by sentence::obj
        val action by sentence::action

        override fun toString() = "Requirement($sentence)"
    }

    @Serializable
    data class Permission(val allowance: Allowance, val sentence: Sentence) : Clause, NoCircumstance {
        val subject by sentence::subject
        val obj by sentence::obj
        val action by sentence::action

        override fun toString() = "Rule($allowance $sentence)"
    }
}
