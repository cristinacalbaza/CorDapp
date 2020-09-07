package com.template.states

import com.template.contracts.TokenContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * The state object recording Tokens.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param quantity the number of the Tokens.
 * @param issuer the party issuing the Tokens.
 * @param holder the party owning the Tokens.
 */
@BelongsToContract(TokenContract::class)
data class TokenState(val quantity: Int,
                      val issuer: Party,
                      val holder: Party) : ContractState {

    override val participants: List<AbstractParty> get() = listOf(holder)
}
