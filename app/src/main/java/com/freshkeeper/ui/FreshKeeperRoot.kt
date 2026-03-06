package com.freshkeeper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                StatusOverview(state)
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
        items(products) { product ->
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

@OptIn(ExperimentalLayoutApi::class)
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

            OutlinedTextField(
                value = state.expiryDate,
                onValueChange = onExpiryDateChanged,
                label = { Text("Срок (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

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
                Text("Сканировать штрихкод (следующий шаг)")
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
