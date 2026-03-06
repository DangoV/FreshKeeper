package com.freshkeeper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshKeeperRoot(
    viewModel: FreshKeeperViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isScannerOpen) {
        BarcodeScannerRoute(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FreshKeeper") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AddProductForm(
                    state = state.form,
                    onNameChanged = viewModel::onNameChanged,
                    onQuantityChanged = viewModel::onQuantityChanged,
                    onExpiryDateChanged = viewModel::onExpiryDateChanged,
                    onBarcodeChanged = viewModel::onBarcodeChanged,
                    onTemplateClick = viewModel::applyTemplate,
                    onSaveClick = viewModel::addProduct,
                    onScanBarcodeClick = viewModel::onBarcodeScanClicked,
                )
            }

            item {
                NotificationSettingsCard(
                    settings = state.notificationSettings,
                    onNotificationsEnabledChanged = viewModel::onNotificationsEnabledChanged,
                    onRemindThreeDaysChanged = viewModel::onRemindThreeDaysChanged,
                    onRemindOneDayChanged = viewModel::onRemindOneDayChanged,
                    onRemindSameDayChanged = viewModel::onRemindSameDayChanged,
                    onReminderHourChanged = viewModel::onReminderHourChanged,
                    onSaveHour = viewModel::saveReminderHour,
                )
            }

            item {
                StatusOverview(state)
            }


            item {
                ProductFilterCard(
                    query = state.productFilterQuery,
                    onQueryChanged = viewModel::onProductFilterQueryChanged,
                    shownCount = state.products.size,
                )
            }

            ProductSection(
                title = "Просроченные",
                products = state.expiredProducts,
                emptyState = "Нет просроченных продуктов",
            )

            ProductSection(
                title = "Скоро испортятся (0-3 дня)",
                products = state.expiringSoonProducts,
                emptyState = "Нет продуктов с близким сроком",
            )

            ProductSection(
                title = "Свежие",
                products = state.freshProducts,
                emptyState = "Добавьте продукты",
            )
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    settings: NotificationSettingsUi,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onRemindThreeDaysChanged: (Boolean) -> Unit,
    onRemindOneDayChanged: (Boolean) -> Unit,
    onRemindSameDayChanged: (Boolean) -> Unit,
    onReminderHourChanged: (String) -> Unit,
    onSaveHour: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Настройки уведомлений", style = MaterialTheme.typography.titleMedium)

            SettingsSwitchRow(
                title = "Уведомления включены",
                checked = settings.enabled,
                onCheckedChange = onNotificationsEnabledChanged,
            )
            SettingsSwitchRow(
                title = "Напомнить за 3 дня",
                checked = settings.remindThreeDays,
                onCheckedChange = onRemindThreeDaysChanged,
            )
            SettingsSwitchRow(
                title = "Напомнить за 1 день",
                checked = settings.remindOneDay,
                onCheckedChange = onRemindOneDayChanged,
            )
            SettingsSwitchRow(
                title = "Напомнить в день срока",
                checked = settings.remindSameDay,
                onCheckedChange = onRemindSameDayChanged,
            )

            OutlinedTextField(
                value = settings.reminderHour,
                onValueChange = onReminderHourChanged,
                label = { Text("Час уведомлений (0-23)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSaveHour,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить настройки")
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
private fun ProductFilterCard(
    query: String,
    onQueryChanged: (String) -> Unit,
    shownCount: Int,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Фильтр продуктов", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                label = { Text("Поиск по названию") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Найдено: $shownCount")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ProductSection(
    title: String,
    products: List<ProductUi>,
    emptyState: String,
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    if (products.isEmpty()) {
        item {
            Text(
                text = emptyState,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        items(
            items = products,
            key = { product -> product.id },
            contentType = { "product" },
        ) { product ->
            ProductCard(product)
        }
    }
}

@Composable
private fun StatusOverview(state: FreshKeeperUiState) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Обзор холодильника", style = MaterialTheme.typography.titleMedium)
            Text("Всего продуктов: ${state.products.size}")
            Text("Просрочено: ${state.expiredProducts.size}")
            Text("Скоро испортится: ${state.expiringSoonProducts.size}")
            Text("Свежих: ${state.freshProducts.size}")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddProductForm(
    state: AddProductFormState,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onExpiryDateChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onTemplateClick: (ProductTemplate) -> Unit,
    onSaveClick: () -> Unit,
    onScanBarcodeClick: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Добавить продукт", style = MaterialTheme.typography.titleMedium)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductTemplate.entries.forEach { template ->
                    AssistChip(
                        onClick = { onTemplateClick(template) },
                        label = { Text(template.label) },
                    )
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.quantity,
                onValueChange = onQuantityChanged,
                label = { Text("Количество") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.expiryDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Срок годности") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showDatePicker = true },
                )
            }

            OutlinedTextField(
                value = state.barcode,
                onValueChange = onBarcodeChanged,
                label = { Text("Штрихкод (опционально)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = onScanBarcodeClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сканировать штрихкод")
            }

            state.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
        }
    }

    if (showDatePicker) {
        val initialDateMillis = runCatching {
            LocalDate.parse(state.expiryDate)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedDate != null) {
                        val parsed = Instant.ofEpochMilli(selectedDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onExpiryDateChanged(parsed.toString())
                    }
                    showDatePicker = false
                }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

}

@Composable
private fun ProductCard(product: ProductUi) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Количество: ${product.quantity}")
            Text(text = "Истекает через ${product.expiresInDays} дн.")
        }
    }
}
