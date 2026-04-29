package com.example.voicebudget

data class ExpenseModel(
    val amount: Int,
    val category: String,
    val note: String,
    val datetime: String
)