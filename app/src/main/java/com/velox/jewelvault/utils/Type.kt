package com.velox.jewelvault.utils

sealed class ChargeType(val type: String) {
    data object Percentage : ChargeType("%")
    data object Piece : ChargeType("piece")
    data object PerGm : ChargeType("/gm")

    companion object {
        fun list(): List<String> = listOf(Percentage.type, Piece.type, PerGm.type)
    }
}

sealed class EntryType(val type: String) {
    data object Piece : EntryType("piece")
    data object Lot : EntryType("Lot")

    companion object {
        fun list(): List<String> = listOf(Piece.type, Lot.type)
    }
}


sealed class Purity(val label: String, val multiplier: Double) {
    // Gold Purity Standards (Karat and Millesimal Fineness)
    data object P1000 : Purity("1000", 1.000)  // 24K Gold, Fine Gold
    data object P999  : Purity("999", 0.999)   // 24K Gold, Investment Grade
    data object P9995 : Purity("999.5", 0.9995) // 24K Gold, Ultra Fine
    data object P958  : Purity("958", 0.958)   // 23K Gold, Britannia Gold
    data object P916  : Purity("916", 0.916)   // 22K Gold
    data object P900  : Purity("900", 0.900)   // 21.6K Gold, Coin Gold
    data object P833  : Purity("833", 0.833)   // 20K Gold
    data object P750  : Purity("750", 0.750)   // 18K Gold
    data object P625  : Purity("625", 0.625)   // 15K Gold
    data object P585  : Purity("585", 0.585)   // 14K Gold
    data object P500  : Purity("500", 0.500)   // 12K Gold
    data object P417  : Purity("417", 0.417)   // 10K Gold
    data object P375  : Purity("375", 0.375)   // 9K Gold
    data object P333  : Purity("333", 0.333)   // 8K Gold

    // Silver Purity Standards
    data object P1000S : Purity("1000", 1.000)  // Fine Silver, Investment Grade
    data object P999S : Purity("999", 0.999)  // Fine Silver, Investment Grade
    data object P958S : Purity("958", 0.958)  // Britannia Silver
    data object P925S : Purity("925", 0.925)  // Sterling Silver
    data object P900S : Purity("900", 0.900)  // Coin Silver
    data object P800S : Purity("800", 0.800)  // 800 Silver
    data object P750S : Purity("750", 0.750)  // 750 Silver
    data object P500S : Purity("500", 0.500)  // 500 Silver

    // Platinum Purity Standards
    data object P999P : Purity("999", 0.999)  // Fine Platinum, Investment Grade
    data object P9995P : Purity("999.5", 0.9995) // Ultra Fine Platinum
    data object P950P : Purity("950", 0.950)  // 950 Platinum
    data object P900P : Purity("900", 0.900)  // 900 Platinum
    data object P850P : Purity("850", 0.850)  // 850 Platinum
    data object P800P : Purity("800", 0.800)  // 800 Platinum

    // Palladium Purity Standards
    data object P999Pd : Purity("999", 0.999)  // Fine Palladium, Investment Grade
    data object P950Pd : Purity("950", 0.950)  // 950 Palladium
    data object P500Pd : Purity("500", 0.500)  // 500 Palladium

    companion object {
        fun list(): List<String> = listOf(
            // Gold purities (existing + new)
            P1000.label, P999.label, P9995.label, P958.label, P916.label, P900.label, 
            P833.label, P750.label, P625.label, P585.label, P500.label, P417.label, 
            P375.label, P333.label,
            // Silver purities
            P1000S.label,P999S.label, P958S.label, P925S.label, P900S.label, P800S.label, P750S.label, P500S.label,
            // Platinum purities
            P999P.label, P9995P.label, P950P.label, P900P.label, P850P.label, P800P.label,
            // Palladium purities
            P999Pd.label, P950Pd.label, P500Pd.label
        )

        fun catList(category: String): List<String> {
            val cat = category.lowercase()
            return when {
                cat.contains("gold") -> listOf(
                    P1000.label, P999.label, P9995.label, P958.label, P916.label, P900.label,
                    P833.label, P750.label, P625.label, P585.label, P500.label, P417.label,
                    P375.label, P333.label
                )
                cat.contains("silver") -> listOf(
                    P1000S.label,P999S.label, P958S.label, P925S.label, P900S.label, P800S.label, P750S.label, P500S.label
                )
                cat.contains("platinum") -> listOf(
                    P999P.label, P9995P.label, P950P.label, P900P.label, P850P.label, P800P.label
                )
                cat.contains("palladium") -> listOf(
                    P999Pd.label, P950Pd.label, P500Pd.label
                )
                else -> list()
            }
        }

        fun fromLabel(label: String): Purity? = when (label) {
            // Gold purities
            P1000.label -> P1000
            P999.label -> P999
            P9995.label -> P9995
            P958.label -> P958
            P916.label -> P916
            P900.label -> P900
            P833.label -> P833
            P750.label -> P750
            P625.label -> P625
            P585.label -> P585
            P500.label -> P500
            P417.label -> P417
            P375.label -> P375
            P333.label -> P333
            // Silver purities
            P1000S.label -> P1000S
            P999S.label -> P999S
            P958S.label -> P958S
            P925S.label -> P925S
            P900S.label -> P900S
            P800S.label -> P800S
            P750S.label -> P750S
            P500S.label -> P500S
            // Platinum purities
            P999P.label -> P999P
            P9995P.label -> P9995P
            P950P.label -> P950P
            P900P.label -> P900P
            P850P.label -> P850P
            P800P.label -> P800P
            // Palladium purities
            P999Pd.label -> P999Pd
            P950Pd.label -> P950Pd
            P500Pd.label -> P500Pd
            else -> null
        }
    }
}

enum class ExportFormat {
    CSV, XLS, XLSX
}

enum class SortOrder { ASCENDING, DESCENDING }
