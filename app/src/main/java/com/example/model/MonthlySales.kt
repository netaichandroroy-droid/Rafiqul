package com.example.model

data class MonthlySalesRecord(
    val id: String,
    val yearMonth: String,
    val code: String,
    val name: String,
    val tpSales: Double,
    val tpPlusVat: Double,
    val createdAtMs: Long = System.currentTimeMillis()
)
