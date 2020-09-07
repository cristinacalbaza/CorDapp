package com.template.contracts

import net.corda.core.identity.AbstractParty
import com.template.states.TokenState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [TokenState], which in turn encapsulates an [TokenState].
 *
 * For a new [TokenState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero or more input states. (No input for Issue command, one or more inputs for Move and Redeem)
 * - Zero or more output states: the new [TokenState]s. (No state is created in case of Redeem, one or more states are created in case of Move or Issue)
 * - An Issue() command with the public key of the holder
 * - An Move() command with the public key of the holder
 * - An Redeem() command with the public key of the holder
 *
 * All contracts must sub-class the [Contract] interface.
 */
class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.TokenContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        val inputs = tx.inputsOfType<TokenState>()
        val outputs = tx.outputsOfType<TokenState>()

        when (command.value) {
            is Commands.Issue -> requireThat {
                // Constraints regarding Issue Command
                "No inputs should be consumed when issuing Tokens." using (inputs.isEmpty())
                "One or more output states should be created." using (outputs.isNotEmpty())
                "Quantities must be greater than 0." using (outputs.all { it.quantity > 0 })

                // Constraints regarding signers (Just the issuer should sign)
                "The issuer of the Tokens should sign." using (command.signers.containsAll(outputs.map { it.issuer.owningKey }.distinct()))

            }
            is Commands.Move -> requireThat {
                // Constraints regarding Move Command
                "One or more inputs should be consumed when moving Tokens." using (inputs.isNotEmpty())
                "One or more output states should be created." using (outputs.isNotEmpty())
                "Quantities must be greater than 0." using (inputs.all { it.quantity > 0 } && outputs.all { it.quantity > 0 })

                val inputIssuerQuantity = inputs.groupingBy { it.issuer }.fold(0) { accumulator, (quantity) -> accumulator + quantity }
                val outputIssuerQuantity = outputs.groupingBy { it.issuer }.fold(0) { accumulator, (quantity) -> accumulator + quantity }

                "The list of the issuers should be preserved." using (inputIssuerQuantity.keys == outputIssuerQuantity.keys)

                "The sum of the Token quantities for each issuer should be preserved." using (inputIssuerQuantity.all { it.value == outputIssuerQuantity[it.key] })

                // Constraints regarding signers (Just the holder should sign)
                "The holder of the Tokens should sign." using (command.signers.containsAll(inputs.map { it.holder.owningKey }.distinct()))
            }
            is Commands.Redeem -> requireThat {
                // Constraints regarding Redeem Command
                "One or more inputs should be consumed when redeeming Tokens." using (inputs.isNotEmpty())
                "No output state should be created." using (outputs.isEmpty())
                "Quantities of the inputs should be greater than 0." using (inputs.all { it.quantity > 0 })

                // Constraints regarding signers
                "The issuers of the Tokens should sign." using (command.signers.containsAll(inputs.map { it.issuer.owningKey }.distinct()))
                "The holders of the Tokens should sign." using (command.signers.containsAll(inputs.map { it.holder.owningKey }.distinct()))
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}
