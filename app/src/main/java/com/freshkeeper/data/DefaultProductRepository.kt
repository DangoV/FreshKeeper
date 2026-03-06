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
            entities.map { entity ->
                Product(
                    id = entity.id,
                    name = entity.name,
                    expiryDate = LocalDate.parse(entity.expiryDateIso),
                    quantity = entity.quantity,
                    barcode = entity.barcode,
                )
            }
        }
    }

    override suspend fun saveProduct(product: Product) {
        productDao.upsert(
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
