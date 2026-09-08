package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO(Member 4): top SearchBar, LazyColumn list of stretches (required component #6),
// tap a card to view details, FloatingActionButton to add a custom stretch.
@Composable
fun StretchListScreen(onStretchClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Stretch Library", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("TODO: SearchBar + LazyColumn list + FloatingActionButton")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onStretchClick("sample_stretch_1") }) {
            Text("View sample stretch")
        }
    }
}
