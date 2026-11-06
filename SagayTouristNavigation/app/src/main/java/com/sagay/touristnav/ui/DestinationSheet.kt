package com.sagay.touristnav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Terminal

@Composable
fun DestinationSheet(
    destinations: List<Destination>,
    terminals: Map<String, Terminal>,
    onSelect: (Destination) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(destinations, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            destinations
        } else {
            destinations.filter {
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.42f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                singleLine = true,
                placeholder = { Text("Search a destination or category…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No destinations match your search.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(filtered, key = { it.id }) { destination ->
                        val terminal = terminals[destination.terminalId]
                        ListItem(
                            headlineContent = {
                                Text(destination.name, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = { Text(destination.category) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(parseHexColor(terminal?.colorHex))),
                                )
                            },
                            trailingContent = {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                            },
                            modifier = Modifier.clickable { onSelect(destination) },
                        )
                    }
                }
            }
        }
    }
}
