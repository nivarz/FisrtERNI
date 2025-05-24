package com.eriknivar.firebasedatabase.view.inventoryentry

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.viewmodel.UserViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun ReconteoCard(
    item: Map<String, Any>,
    onEliminarCard: () -> Unit,
    actualizarActividad: () -> Unit
) {
    val sku = item["sku"] as? String ?: ""
    val descripcion = item["descripcion"] as? String ?: "-"
    val cantidadEsperada = when (val raw = item["cantidadEsperada"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val cantidadFisicaOriginal = (item["cantidadFisica"] as? Number)?.toDouble() ?: 0.0
    val ubicacion = item["ubicacion"] as? String ?: "-"
    val estado = item["estado"] as? String ?: "-"
    val localidad = item["localidad"]?.toString()?.ifBlank { "SIN_LOCALIDAD" } ?: "SIN_LOCALIDAD"
    val nombreAsignado = item["nombreAsignado"] as? String ?: "-"
    val lote = item["lote"] as? String ?: "-"

    // 🟡 Log de depuración
    Log.d("RECONTEO_CARD", "SKU: $sku | Estado: $estado | Lote: $lote | Esperado: $cantidadEsperada | Físico original: $cantidadFisicaOriginal")

    // 👇 Este remember lo puedes hacer más seguro con cantidadFisicaOriginal como key
    var cantidadFisicaText by remember(cantidadFisicaOriginal) {
        mutableStateOf(cantidadFisicaOriginal.toString() )
    }
    var cantidadFisica by remember(cantidadFisicaOriginal) {
        mutableStateOf(cantidadFisicaOriginal.toDouble() )
    }

    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Descripción: $descripcion", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("SKU: $sku", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Lote: $lote", fontWeight = FontWeight.Normal, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Físico:", modifier = Modifier.width(70.dp), fontSize = 13.sp

                )
                TextField(
                    value = cantidadFisicaText,
                    onValueChange = {
                        actualizarActividad()
                        cantidadFisicaText = it
                        cantidadFisica = it.toDoubleOrNull() ?: 0.0
                    },
                    modifier = Modifier
                        .weight(.5f)
                        .height(60.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Lote:", modifier = Modifier.width(70.dp), fontSize = 13.sp)
                TextField(
                    value = lote,
                    onValueChange = {},
                    readOnly = false,
                    modifier = Modifier.weight(.5f).height(60.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ubicación:", modifier = Modifier.width(70.dp), fontSize = 13.sp)
                TextField(
                    value = ubicacion,
                    onValueChange = {},
                    readOnly = false,
                    modifier = Modifier.weight(.5f).height(60.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Esperado: ${cantidadEsperada.toDouble()} | Localidad: $localidad", fontSize = 13.sp)
            Text("Asignado a: $nombreAsignado", fontSize = 13.sp)
            Text("Estado: ${estado.uppercase()}", color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = "Esperado: ${cantidadEsperada.toDouble()} | Localidad: ${localidad.ifBlank { "-" }}",
                fontSize = 13.sp
            )


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    actualizarActividad()
                    isSaving = true

                    FirebaseFirestore.getInstance()
                        .collection("reconteo_pendiente")
                        .whereEqualTo("sku", sku)
                        .whereEqualTo("ubicacion", ubicacion)
                        .whereEqualTo("lote", lote)
                        .whereEqualTo("cantidadEsperada", cantidadEsperada)
                        .whereEqualTo("estado", "pendiente")
                        .get()
                        .addOnSuccessListener { docs ->
                            for (doc in docs) {
                                doc.reference.update(
                                    mapOf(
                                        "cantidadFisica" to cantidadFisica,
                                        "estado" to "completado"
                                    )
                                )
                            }
                            Toast.makeText(context, "Guardado y marcado como completado", Toast.LENGTH_SHORT).show()
                            onEliminarCard() // ✅ Elimina de la UI
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            isSaving = false
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Guardar", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar cambios", fontSize = 13.sp)
            }
        }
    }
}
