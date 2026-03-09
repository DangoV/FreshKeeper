package com.freshkeeper.domain

import java.time.LocalDate

data class Product(
    val id: Long = 0,
    val name: String,
    val expiryDate: LocalDate,
    val quantity: String,
    val barcode: String? = null,
)
