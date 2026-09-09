package com.moneymanager.domain.transaction

import com.moneymanager.data.entity.EmiEntity
import com.moneymanager.data.entity.TransactionEntity
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.round

/**
 * Builds the [EmiEntity] and its installment/fee [TransactionEntity] rows for a "Pay via EMI"
 * expense purchase.
 *
 * Shared by [com.moneymanager.app.ui.transactions.TransactionsViewModel] and
 * [com.moneymanager.app.ui.addtransaction.AddTransactionViewModel] (previously duplicated
 * amortization math in both) so the two entry points can't drift out of sync.
 */
object EmiTransactionFactory {

    /** Computes the amortized EMI record. Does not touch the database. */
    fun buildEmi(
        baseTx: TransactionEntity,
        tenureMonths: Int,
        annualInterestRate: Double,
        isNoCost: Boolean,
        processingFee: Double,
    ): EmiEntity {
        val totalAmount = baseTx.amount
        val tenure = tenureMonths.coerceAtLeast(1)

        val monthlyAmount = if (annualInterestRate > 0 && !isNoCost) {
            val r = annualInterestRate / (12.0 * 100.0)
            val pmt = totalAmount * r * (1.0 + r).pow(tenure.toDouble()) / ((1.0 + r).pow(tenure.toDouble()) - 1.0)
            val gst = (pmt * r) * 0.18 // 18% GST on the interest portion
            round((pmt + gst) * 100.0) / 100.0
        } else {
            round((totalAmount / tenure) * 100.0) / 100.0
        }

        return EmiEntity(
            title = baseTx.note.ifBlank { "EMI Purchase" },
            accountId = baseTx.accountId,
            categoryId = baseTx.categoryId,
            totalAmount = totalAmount,
            tenureMonths = tenure,
            monthlyAmount = monthlyAmount,
            annualInterestRate = annualInterestRate,
            isNoCost = isNoCost,
            processingFee = processingFee,
            startDate = baseTx.date,
            status = "ACTIVE",
        )
    }

    /** Builds the one-off processing-fee transaction, or null if there's no fee. */
    fun buildFeeTransaction(baseTx: TransactionEntity, processingFee: Double, emiId: Long): TransactionEntity? {
        if (processingFee <= 0) return null
        return baseTx.copy(
            amount = processingFee,
            note = if (baseTx.note.isBlank()) "EMI Processing Fee" else "EMI Processing Fee - ${baseTx.note}",
            isRecurring = false,
            emiId = emiId,
        )
    }

    /**
     * Builds every monthly installment transaction for [emi]. Only the first installment is
     * marked `postedToBalance = true` (applied immediately); the rest are posted later by
     * [com.moneymanager.data.worker.EmiPostingWorker] as their due dates arrive.
     */
    fun buildInstallments(baseTx: TransactionEntity, emi: EmiEntity, emiId: Long): List<TransactionEntity> {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTx.date }
        val installments = mutableListOf<TransactionEntity>()
        var runningTotal = 0.0

        for (i in 1..emi.tenureMonths) {
            val installmentDate = calendar.timeInMillis
            val currentAmount = if (i == emi.tenureMonths) {
                round((emi.totalAmount - runningTotal) * 100.0) / 100.0
            } else {
                emi.monthlyAmount
            }
            runningTotal += currentAmount

            installments += baseTx.copy(
                amount = currentAmount,
                date = installmentDate,
                note = if (baseTx.note.isBlank()) "EMI ($i/${emi.tenureMonths})" else "${baseTx.note} (EMI $i/${emi.tenureMonths})",
                emiId = emiId,
                emiInstallmentNumber = i,
                postedToBalance = i == 1,
            )

            calendar.add(Calendar.MONTH, 1)
        }

        return installments
    }
}
