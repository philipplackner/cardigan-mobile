package com.plcoding.cardiganapp.tooling.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** The project's custom detekt rule set `cardigan`. */
class CardiganRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("cardigan")

    override fun instance(): RuleSet = RuleSet(ruleSetId, listOf(::NoMockingLibraries))
}
