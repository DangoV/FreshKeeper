package com.freshkeeper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshkeeper.data.ProductRepository
import com.freshkeeper.domain.Product
import com.freshkeeper.reminders.ReminderScheduler
import com.freshkeeper.settings.NotificationSettings
import com.freshkeeper.settings.NotificationSettingsStore
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
    val id: Long,
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

data class NotificationSettingsUi(
    val enabled: Boolean = true,
    val remindThreeDays: Boolean = true,
    val remindOneDay: Boolean = true,
    val remindSameDay: Boolean = true,
    val reminderHour: String = "9",
)

@HiltViewModel
class FreshKeeperViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val reminderScheduler: ReminderScheduler,
    private val notificationSettingsStore: NotificationSettingsStore,
) : ViewModel() {

    private val formState = MutableStateFlow(AddProductFormState())
    private val _isScannerOpen = MutableStateFlow(false)
    private val notificationSettingsState = MutableStateFlow(loadNotificationSettings())
    private val productFilterQueryState = MutableStateFlow("")

    private val productsState: StateFlow<List<ProductUi>> = repository.observeProducts()
        .map { list ->
            list.map { product ->
                ProductUi(
                    id = product.id,
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

    private val groupedProductsState: StateFlow<GroupedProductsUi> = combine(
        productsState,
        productFilterQueryState,
    ) { products, filterQuery ->
        val filteredProducts = if (filterQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(filterQuery, ignoreCase = true) }
        }

        GroupedProductsUi(
            products = filteredProducts,
            expiredProducts = filteredProducts.filter { it.expiresInDays < 0 },
            expiringSoonProducts = filteredProducts.filter { it.expiresInDays in 0..3 },
            freshProducts = filteredProducts.filter { it.expiresInDays > 3 },
            productFilterQuery = filterQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupedProductsUi(),
    )

    val uiState: StateFlow<FreshKeeperUiState> = combine(
        formState,
        _isScannerOpen,
        notificationSettingsState,
        groupedProductsState,
    ) { form, isScannerOpen, notificationSettings, groupedProducts ->
        FreshKeeperUiState(
            products = groupedProducts.products,
            expiredProducts = groupedProducts.expiredProducts,
            expiringSoonProducts = groupedProducts.expiringSoonProducts,
            freshProducts = groupedProducts.freshProducts,
            form = form,
            isScannerOpen = isScannerOpen,
            notificationSettings = notificationSettings,
            productFilterQuery = groupedProducts.productFilterQuery,
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

    fun onNotificationsEnabledChanged(value: Boolean) {
        notificationSettingsState.update { it.copy(enabled = value) }
        saveNotificationSettings()
    }

    fun onRemindThreeDaysChanged(value: Boolean) {
        notificationSettingsState.update { it.copy(remindThreeDays = value) }
        saveNotificationSettings()
    }

    fun onRemindOneDayChanged(value: Boolean) {
        notificationSettingsState.update { it.copy(remindOneDay = value) }
        saveNotificationSettings()
    }

    fun onRemindSameDayChanged(value: Boolean) {
        notificationSettingsState.update { it.copy(remindSameDay = value) }
        saveNotificationSettings()
    }

    fun onReminderHourChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(2)
        notificationSettingsState.update { it.copy(reminderHour = filtered) }
    }

    fun saveReminderHour() {
        val hour = notificationSettingsState.value.reminderHour.toIntOrNull()
        if (hour == null || hour !in 0..23) {
            formState.update { it.copy(errorMessage = "Час уведомлений должен быть от 0 до 23") }
            return
        }

        formState.update { it.copy(errorMessage = null) }
        saveNotificationSettings()
    }


    fun onProductFilterQueryChanged(value: String) {
        productFilterQueryState.value = value
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

    private fun loadNotificationSettings(): NotificationSettingsUi {
        val settings = notificationSettingsStore.load()
        return NotificationSettingsUi(
            enabled = settings.enabled,
            remindThreeDays = settings.remindThreeDays,
            remindOneDay = settings.remindOneDay,
            remindSameDay = settings.remindSameDay,
            reminderHour = settings.reminderHour.toString(),
        )
    }

    private fun saveNotificationSettings() {
        val current = notificationSettingsState.value
        val parsedHour = current.reminderHour.toIntOrNull()?.coerceIn(0, 23) ?: 9

        notificationSettingsStore.save(
            NotificationSettings(
                enabled = current.enabled,
                remindThreeDays = current.remindThreeDays,
                remindOneDay = current.remindOneDay,
                remindSameDay = current.remindSameDay,
                reminderHour = parsedHour,
            ),
        )
    }
}


data class GroupedProductsUi(
    val products: List<ProductUi> = emptyList(),
    val expiredProducts: List<ProductUi> = emptyList(),
    val expiringSoonProducts: List<ProductUi> = emptyList(),
    val freshProducts: List<ProductUi> = emptyList(),
    val productFilterQuery: String = "",
)

data class FreshKeeperUiState(
    val products: List<ProductUi> = emptyList(),
    val expiredProducts: List<ProductUi> = emptyList(),
    val expiringSoonProducts: List<ProductUi> = emptyList(),
    val freshProducts: List<ProductUi> = emptyList(),
    val form: AddProductFormState = AddProductFormState(),
    val isScannerOpen: Boolean = false,
    val notificationSettings: NotificationSettingsUi = NotificationSettingsUi(),
    val productFilterQuery: String = "",
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
