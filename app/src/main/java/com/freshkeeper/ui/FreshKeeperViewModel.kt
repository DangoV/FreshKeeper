package com.freshkeeper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshkeeper.data.ProductRepository
import com.freshkeeper.domain.Product
import com.freshkeeper.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductUi(
    val name: String,
    val quantity: String,
    val expiresInDays: Long,
)

data class AddProductFormState(
    val name: String = "",
    val quantity: String = "",
    val expiryDate: String = LocalDate.now().plusDays(3).toString(),
    val barcode: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class FreshKeeperViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val formState = MutableStateFlow(AddProductFormState())
    private val _isScannerOpen = MutableStateFlow(false)

    val uiState: StateFlow<FreshKeeperUiState> = combine(
        repository.observeProducts().map { list ->
            list.map { product ->
                ProductUi(
                    name = product.name,
                    quantity = product.quantity,
                    expiresInDays = ChronoUnit.DAYS.between(LocalDate.now(), product.expiryDate),
                )
            }
        },
        formState,
        _isScannerOpen,
    ) { products, form, isScannerOpen ->
        val expired = products.filter { it.expiresInDays < 0 }
        val expiringSoon = products.filter { it.expiresInDays in 0..3 }
        val fresh = products.filter { it.expiresInDays > 3 }

        FreshKeeperUiState(
            products = products,
            expiredProducts = expired,
            expiringSoonProducts = expiringSoon,
            freshProducts = fresh,
            form = form,
            isScannerOpen = isScannerOpen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FreshKeeperUiState(),
    )

    fun onNameChanged(value: String) {
        formState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onQuantityChanged(value: String) {
        formState.update { it.copy(quantity = value, errorMessage = null) }
    }

    fun onExpiryDateChanged(value: String) {
        formState.update { it.copy(expiryDate = value, errorMessage = null) }
    }

    fun onBarcodeChanged(value: String) {
        formState.update { it.copy(barcode = value, errorMessage = null) }
    }

    fun applyTemplate(template: ProductTemplate) {
        formState.update {
            it.copy(
                name = template.productName,
                quantity = template.defaultQuantity,
                expiryDate = LocalDate.now().plusDays(template.defaultExpiryDays).toString(),
                errorMessage = null,
            )
        }
    }


    fun onBarcodeScanClicked() {
        formState.update { it.copy(errorMessage = null) }
        _isScannerOpen.value = true
    }

    fun closeScanner() {
        _isScannerOpen.value = false
    }

    fun onBarcodeScanned(barcode: String) {
        formState.update { it.copy(barcode = barcode, errorMessage = null) }
        _isScannerOpen.value = false
    }

    fun addProduct() {
        val current = formState.value
        if (current.name.isBlank() || current.quantity.isBlank()) {
            formState.update { it.copy(errorMessage = "Заполните название и количество") }
            return
        }

        val parsedDate = runCatching { LocalDate.parse(current.expiryDate) }.getOrNull()
        if (parsedDate == null) {
            formState.update { it.copy(errorMessage = "Дата должна быть в формате YYYY-MM-DD") }
            return
        }

        viewModelScope.launch {
            val productId = repository.saveProduct(
                Product(
                    name = current.name.trim(),
                    quantity = current.quantity.trim(),
                    expiryDate = parsedDate,
                    barcode = current.barcode.takeIf { it.isNotBlank() }?.trim(),
                ),
            )

            reminderScheduler.scheduleProductReminders(
                productId = productId,
                productName = current.name.trim(),
                expiryDate = parsedDate,
            )

            formState.value = AddProductFormState(
                expiryDate = LocalDate.now().plusDays(3).toString(),
            )
        }
    }
}

data class FreshKeeperUiState(
    val products: List<ProductUi> = emptyList(),
    val expiredProducts: List<ProductUi> = emptyList(),
    val expiringSoonProducts: List<ProductUi> = emptyList(),
    val freshProducts: List<ProductUi> = emptyList(),
    val form: AddProductFormState = AddProductFormState(),
    val isScannerOpen: Boolean = false,
)

enum class ProductTemplate(
    val label: String,
    val productName: String,
    val defaultQuantity: String,
    val defaultExpiryDays: Long,
) {
    MILK(label = "Молоко", productName = "Молоко", defaultQuantity = "1 л", defaultExpiryDays = 5),
    EGGS(label = "Яйца", productName = "Яйца", defaultQuantity = "10 шт", defaultExpiryDays = 14),
    CHEESE(label = "Сыр", productName = "Сыр", defaultQuantity = "200 г", defaultExpiryDays = 10),
}
