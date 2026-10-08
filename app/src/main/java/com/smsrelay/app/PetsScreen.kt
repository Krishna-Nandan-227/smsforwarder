package com.smsrelay.app

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun PetsScreen(store: WorkspaceStore, modifier: Modifier) {
    var species by remember(store) { mutableStateOf(store.petSpecies) }
    var colour by remember(store) { mutableStateOf(store.petColour) }
    var name by remember(store) { mutableStateOf(store.petName) }
    var hex by remember(store) { mutableStateOf("#%06X".format(colour and 0xFFFFFF)) }
    var error by remember { mutableStateOf<String?>(null) }
    fun setColour(value: Int) {
        colour = value; store.petColour = value
        hex = "#%06X".format(value and 0xFFFFFF); error = null
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your companion", style = MaterialTheme.typography.headlineSmall)
        Text("Choose a pet and make it yours.", color = MaterialTheme.colorScheme.secondary)
        WorkspaceCard {
            AndroidView(factory = { PetFace(it) }, update = {
                it.species = species; it.coat = colour; it.petName = name; it.invalidate()
            }, modifier = Modifier.size(140.dp).align(Alignment.CenterHorizontally))
            Text(name.ifBlank { species }, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            PetControls()
            Text("Drag your floating pet to move it. Tap it to see today's count and ask for a spoken brief.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("Choose your pet", style = MaterialTheme.typography.titleMedium)
        listOf(listOf("Cat", "Dog"), listOf("Elephant", "Rabbit")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { choice ->
                    OutlinedButton(onClick = { species = choice; store.petSpecies = choice },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (species == choice) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
                        Text(if (species == choice) "✓ $choice" else choice)
                    }
                }
            }
        }
        OutlinedTextField(name, { name = it.take(30); store.petName = name },
            label = { Text("Pet name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Coat colour", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0xFFF7C18C.toInt(), 0xFFCCD0CF.toInt(), 0xFFAED6C8.toInt(),
                0xFFB8C8E5.toInt(), 0xFFE5BCCB.toInt(), 0xFFC5B7DA.toInt()).forEach { swatch ->
                Surface(onClick = { setColour(swatch) }, color = Color(swatch), shape = CircleShape,
                    border = BorderStroke(if (colour == swatch) 3.dp else 1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { if (colour == swatch) Text("✓", color = Color(0xFF253745)) }
                }
            }
        }
        OutlinedTextField(hex, { hex = it.take(7); error = null }, label = { Text("Custom colour (#RRGGBB)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
        Button(onClick = {
            if (Regex("^#[0-9a-fA-F]{6}$").matches(hex)) setColour(AndroidColor.parseColor(hex))
            else error = "Enter six hexadecimal digits, for example #F7C18C."
        }) { Text("Apply colour") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Your selection is saved on this device. Appearance changes also update the floating pet.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
