package com.ravenguard.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ravenguard.app.data.database.ContactEntity

@Composable
fun ContactList(
    contacts: List<ContactEntity>,
    onDelete: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contacts, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact,
                    onDelete = onDelete,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}
