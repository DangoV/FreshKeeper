package com.freshkeeper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshkeeper.data.ProductRepository
import com.freshkeeper.domain.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ProductUi(
    val name: String,
    val quantity: String,
    val expiresInDays: Long,
)

@HiltViewModel
class FreshKeeperViewModel @Inject constructor(
    private val repository: ProductRepository,
) : ViewModel() {

    val products: StateFlow<List<ProductUi>> = repository.observeProducts()
        .map { list ->
            list.map { product ->
                ProductUi(
                    name = product.name,
                    quantity = product.quantity,
                    expiresInDays = ChronoUnit.DAYS.between(LocalDate.now(), product.expiryDate),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addDemoProduct() {
        viewModelScope.launch {
            repository.saveProduct(
                Product(
                    name = "Milk",
                    expiryDate = LocalDate.now().plusDays(3),
                    quantity = "1 bottle",
                ),
            )
        }
    }
}
