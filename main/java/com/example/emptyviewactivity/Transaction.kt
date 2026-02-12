package com.example.emptyviewactivity
import java.io.Serializable
data class Transaction(
    val id: Long,
    val description: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String = "others",
) : Serializable
