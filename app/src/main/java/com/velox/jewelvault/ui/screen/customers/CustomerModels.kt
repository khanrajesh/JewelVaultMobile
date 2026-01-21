package com.velox.jewelvault.ui.screen.customers

// Data classes for khata book plans
data class KhataBookPlan(
    val name: String,
    val payMonths: Int,
    val benefitMonths: Int,
    val description: String,
    val benefitPercentage: Double = if (payMonths + benefitMonths > 0) benefitMonths * 100.0 / (payMonths + benefitMonths) else 0.0,
    val planId: String = "",
    val isCustom: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val effectiveMonths: Int = payMonths + benefitMonths
}

data class CalculatorResults(
    val totalPayAmount: Double,
    val totalBenefitAmount: Double,
    val effectiveMonthlyAmount: Double,
    val totalSavings: Double,
    val savingsPercentage: Double
)

fun getPredefinedPlans(): List<KhataBookPlan> {
    return listOf(
        KhataBookPlan(
            name = "Standard Plan",
            payMonths = 11,
            benefitMonths = 1,
            description = "Pay for 11 months, get 1 month reward",
            benefitPercentage = 8.33,
            planId = "predefined:Standard Plan",
            isCustom = false
        ),
        KhataBookPlan(
            name = "Premium Plan",
            payMonths = 22,
            benefitMonths = 2,
            description = "Pay for 22 months, get 2 months reward",
            benefitPercentage = 8.33,
            planId = "predefined:Premium Plan",
            isCustom = false
        ),
        KhataBookPlan(
            name = "Extended Plan",
            payMonths = 33,
            benefitMonths = 3,
            description = "Pay for 33 months, get 3 months reward",
            benefitPercentage = 8.33,
            planId = "predefined:Extended Plan",
            isCustom = false
        ),
        KhataBookPlan(
            name = "Long Term Plan",
            payMonths = 44,
            benefitMonths = 4,
            description = "Pay for 44 months, get 4 months reward",
            benefitPercentage = 8.33,
            planId = "predefined:Long Term Plan",
            isCustom = false
        )
    )
}

fun calculateKhataBook(monthlyAmount: Double, plan: KhataBookPlan): CalculatorResults {
    val totalPayAmount = monthlyAmount * plan.payMonths
    val totalBenefitAmount = monthlyAmount * plan.benefitMonths
    val effectiveMonthlyAmount = totalPayAmount / plan.effectiveMonths
    val totalSavings = totalBenefitAmount
    val savingsPercentage = (totalSavings / (totalPayAmount + totalBenefitAmount)) * 100
    
    return CalculatorResults(
        totalPayAmount = totalPayAmount,
        totalBenefitAmount = totalBenefitAmount,
        effectiveMonthlyAmount = effectiveMonthlyAmount,
        totalSavings = totalSavings,
        savingsPercentage = savingsPercentage
    )
} 
