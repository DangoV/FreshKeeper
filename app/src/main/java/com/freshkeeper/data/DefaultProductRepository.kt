package com.freshkeeper.data

import com.freshkeeper.data.local.ProductDao
import com.freshkeeper.data.local.ProductEntity
import com.freshkeeper.domain.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class DefaultProductRepository @Inject constructor(
    private val productDao: ProductDao,
) : ProductRepository {

    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeProducts().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getProductById(id: Long): Product? {
        return productDao.getById(id)?.toDomain()
    }

    override suspend fun saveProduct(product: Product): Long {
        return productDao.upsert(
            ProductEntity(
                id = product.id,
                name = product.name,
                expiryDateIso = product.expiryDate.toString(),
                quantity = product.quantity,
                barcode = product.barcode,
            ),
        )
    }
}

private fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        name = name,
        expiryDate = LocalDate.parse(expiryDateIso),
        quantity = quantity,
        barcode = barcode,
    )
}
