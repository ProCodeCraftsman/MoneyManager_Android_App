package com.moneymanager.domain.transaction

import com.moneymanager.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmiTransactionFactoryTest {

    private fun baseTx(amount: Double = 12000.0, note: String = "Laptop", date: Long = 1_700_000_000_000L) =
        TransactionEntity(
            accountId = 1L,
            type = "expense",
            amount = amount,
            date = date,
            note = note,
        )

    @Test
    fun `buildEmi with no interest splits amount evenly across tenure`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 12000.0),
            tenureMonths = 12,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        assertEquals(1000.0, emi.monthlyAmount, 0.001)
        assertEquals(12, emi.tenureMonths)
        assertEquals("ACTIVE", emi.status)
    }

    @Test
    fun `buildEmi with isNoCost true ignores interest rate`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 6000.0),
            tenureMonths = 6,
            annualInterestRate = 18.0,
            isNoCost = true,
            processingFee = 0.0,
        )

        assertEquals(1000.0, emi.monthlyAmount, 0.001)
    }

    @Test
    fun `buildEmi with interest produces a monthly amount greater than the even split`() {
        val principal = 12000.0
        val tenure = 12
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = principal),
            tenureMonths = tenure,
            annualInterestRate = 18.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        assertTrue(emi.monthlyAmount > principal / tenure)
    }

    @Test
    fun `buildEmi coerces non-positive tenure to at least one month`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 500.0),
            tenureMonths = 0,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        assertEquals(1, emi.tenureMonths)
        assertEquals(500.0, emi.monthlyAmount, 0.001)
    }

    @Test
    fun `buildFeeTransaction returns null when fee is zero or negative`() {
        assertNull(EmiTransactionFactory.buildFeeTransaction(baseTx(), processingFee = 0.0, emiId = 1L))
        assertNull(EmiTransactionFactory.buildFeeTransaction(baseTx(), processingFee = -5.0, emiId = 1L))
    }

    @Test
    fun `buildFeeTransaction carries the fee amount and links to the emi`() {
        val fee = EmiTransactionFactory.buildFeeTransaction(baseTx(note = "Phone"), processingFee = 199.0, emiId = 42L)

        requireNotNull(fee)
        assertEquals(199.0, fee.amount, 0.001)
        assertEquals(42L, fee.emiId)
        assertFalse(fee.isRecurring)
        assertTrue(fee.note.contains("Phone"))
    }

    @Test
    fun `buildInstallments sums exactly to the emi total despite rounding`() {
        val principal = 1000.0
        val tenure = 3 // 333.33 x2 + remainder -> must still sum to 1000.0 exactly
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = principal),
            tenureMonths = tenure,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        val installments = EmiTransactionFactory.buildInstallments(baseTx(amount = principal), emi, emiId = 7L)

        assertEquals(tenure, installments.size)
        val total = installments.sumOf { it.amount }
        assertEquals(principal, total, 0.001)
    }

    @Test
    fun `buildInstallments only marks the first installment as posted to balance`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 900.0),
            tenureMonths = 3,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        val installments = EmiTransactionFactory.buildInstallments(baseTx(amount = 900.0), emi, emiId = 7L)

        assertTrue(installments[0].postedToBalance)
        assertFalse(installments[1].postedToBalance)
        assertFalse(installments[2].postedToBalance)
    }

    @Test
    fun `buildInstallments assigns sequential installment numbers and emi id`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 400.0),
            tenureMonths = 4,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        val installments = EmiTransactionFactory.buildInstallments(baseTx(amount = 400.0), emi, emiId = 55L)

        installments.forEachIndexed { index, tx ->
            assertEquals(index + 1, tx.emiInstallmentNumber)
            assertEquals(55L, tx.emiId)
        }
    }

    @Test
    fun `buildInstallments advances each installment date by one month`() {
        val emi = EmiTransactionFactory.buildEmi(
            baseTx = baseTx(amount = 300.0, date = 1_700_000_000_000L),
            tenureMonths = 3,
            annualInterestRate = 0.0,
            isNoCost = false,
            processingFee = 0.0,
        )

        val installments = EmiTransactionFactory.buildInstallments(
            baseTx(amount = 300.0, date = 1_700_000_000_000L), emi, emiId = 1L
        )

        assertTrue(installments[1].date > installments[0].date)
        assertTrue(installments[2].date > installments[1].date)
    }
}
