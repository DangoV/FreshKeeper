package com.freshkeeper.data

import com.freshkeeper.domain.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    suspend fun saveProduct(product: Product)
}
