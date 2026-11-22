package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SavedLocation
import com.example.ui.SearchUiState
import com.example.ui.theme.AuraCoralWarn
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchSheet(
    query: String,
    searchState: SearchUiState,
    savedLocations: List<SavedLocation>,
    activeLocation: SavedLocation,
    isLocating: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSelectSearchResult: (SavedLocation) -> Unit,
    onSelectSavedLocation: (SavedLocation) -> Unit,
    onRemoveSavedLocation: (SavedLocation) -> Unit,
    onRequestCurrentLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuraDeepNavy,
        contentColor = AuraTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Locations",
                style = MaterialTheme.typography.headlineMedium,
                color = AuraTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_cities_hint),
                        color = AuraTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AuraSkyAccent)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = AuraTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedContainerColor = AuraSurface,
                    unfocusedContainerColor = AuraSurface,
                    focusedBorderColor = AuraSkyAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestCurrentLocation,
                enabled = !isLocating,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraSkyAccent.copy(alpha = 0.16f),
                    contentColor = AuraSkyAccent
                ),
                border = BorderStroke(1.dp, AuraSkyAccent.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("use_current_location_button")
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AuraSkyAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLocating) "Acquiring GPS coordinates..." else stringResource(R.string.current_location),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                when {
                    query.trim().length >= 2 -> {
                        when (searchState) {
                            is SearchUiState.Searching -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AuraSkyAccent)
                                    }
                                }
                            }
                            is SearchUiState.Error -> {
                                item {
                                    Text(
                                        text = searchState.message,
                                        color = AuraCoralWarn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            }
                            is SearchUiState.Results -> {
                                if (searchState.items.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No matching cities found for \"${searchState.query}\".",
                                            color = AuraTextSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "SEARCH RESULTS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AuraSkyAccent
                                        )
                                    }
                                    items(searchState.items, key = { "${it.name}_${it.latitude}_${it.longitude}" }) { loc ->
                                        LocationRow(
                                            location = loc,
                                            isActive = loc.matchesCoordinates(activeLocation.latitude, activeLocation.longitude),
                                            canDelete = false,
                                            onSelect = { onSelectSearchResult(loc) },
                                            onDelete = {}
                                        )
                                    }
                                }
                            }
                            SearchUiState.Idle -> Unit
                        }
                    }
                    else -> {
                        item {
                            Text(
                                text = stringResource(R.string.saved_locations).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AuraSkyAccent
                            )
                        }
                        items(savedLocations, key = { "${it.name}_${it.latitude}_${it.longitude}" }) { loc ->
                            LocationRow(
                                location = loc,
                                isActive = loc.matchesCoordinates(activeLocation.latitude, activeLocation.longitude),
                                canDelete = savedLocations.size > 1,
                                onSelect = { onSelectSavedLocation(loc) },
                                onDelete = { onRemoveSavedLocation(loc) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: SavedLocation,
    isActive: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) AuraSkyAccent.copy(alpha = 0.14f) else AuraSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) AuraSkyAccent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Rounded.Bookmark else Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = if (isActive) AuraSkyAccent else AuraTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = location.displaySubtitle.ifBlank {
                    String.format(Locale.US, "%.2f°, %.2f°", location.latitude, location.longitude)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove ${location.name}",
                        tint = AuraTextSecondary
                    )
                }
            }
        }
    }
}
