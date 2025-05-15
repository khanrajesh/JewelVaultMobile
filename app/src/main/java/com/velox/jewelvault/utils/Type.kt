package com.velox.jewelvault.utils

sealed class ChargeType(val type: String) {
    data object Percentage : ChargeType("%")
    data object Piece : ChargeType("piece")
    data object PerGm : ChargeType("per/gm")

    companion object {
        fun list(): List<String> = listOf(Percentage.type, Piece.type, PerGm.type)
    }
}

sealed class EntryType(val type: String) {
    data object Piece : ChargeType("piece")
    data object Lot : ChargeType("Lot")

    companion object {
        fun list(): List<String> = listOf(Piece.type, Lot.type)
    }
}


sealed class Purity(val label: String, val multiplier: Double) {
    data object P1000 : Purity("1000", 1.000)
    data object P999  : Purity("999", 0.999)
    data object P916  : Purity("916", 0.916)
    data object P833  : Purity("833", 0.833)
    data object P750  : Purity("750", 0.750)
    data object P585  : Purity("585", 0.585)

    companion object {
        fun list(): List<String> = listOf(
            P1000.label, P585.label, P750.label, P833.label, P916.label, P999.label
        )

        fun fromLabel(label: String): Purity? = when (label) {
            P1000.label -> P1000
            P999.label -> P999
            P916.label -> P916
            P833.label -> P833
            P750.label -> P750
            P585.label -> P585
            else -> null
        }
    }
}