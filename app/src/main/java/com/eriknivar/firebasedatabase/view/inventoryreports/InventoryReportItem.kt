package com.eriknivar.firebasedatabase.view.inventoryreports

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eriknivar.firebasedatabase.view.storagetype.DataFields
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.eriknivar.firebasedatabase.view.utility.validarUbicacionEnMaestro
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue


@Composable
fun InventoryReportItem(
    item: DataFields,
    onDelete: (String) -> Unit,
    onEdit: (DataFields) -> Unit,
    puedeModificarRegistro: (String, String) -> Boolean,
    tipoUsuarioActual: String,
    clienteIdActual: String
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAuditDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val fechaFormateada = item.fechaRegistro?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"
    val calendar = Calendar.getInstance()
    val context = LocalContext.current
    var fechaVencimiento by remember { mutableStateOf(item.expirationDate) }

    val esInvitadoActual = tipoUsuarioActual.lowercase() == "invitado"
    var esAuditado by remember { mutableStateOf(item.auditado) }
    val backgroundColor = if (expanded) Color(0xFFE3F2FD) else Color.White

    var isSaving by remember { mutableStateOf(false) }

    var showUbiInvalida by remember { mutableStateOf(false) }
    var ubiInvalidaTexto by remember { mutableStateOf(AnnotatedString("")) }


    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            fechaVencimiento =
                String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fechaFormateada,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Blue
                )
                if (esAuditado) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDITADO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Ubicación:", item.location)
                InfoRow("Sku:", item.sku)
                InfoRow("Lote:", item.lote)
                InfoRow("Fecha Vencimiento:", item.expirationDate)
                InfoRow("Cantidad:", "${item.quantity}")
                InfoRow("Unidad de medida:", item.unidadMedida)
                InfoRow("Usuario:", item.usuario)
                InfoRow("Localidad:", item.localidad)

                // 🟦 Mostrar enlace a foto si existe
                if (item.fotoUrl.isNotBlank()) {
                    var showImageDialog by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Foto: ",
                            fontSize = 13.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(150.dp)
                        )
                        Text(
                            text = "VER",
                            color = Color.Black,
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    Log.d(
                                        "FotoDebug", "🟢 VER presionado en Reporte: ${item.fotoUrl}"
                                    )
                                    showImageDialog = true
                                })
                    }

                    if (showImageDialog) {
                        AlertDialog(
                            onDismissRequest = { showImageDialog = false },
                            confirmButton = {
                                TextButton(onClick = { showImageDialog = false }) {
                                    Text(
                                        "Cerrar",
                                        color = Color(0xFF003366),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "📷 Imagen Asociada", fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Log.d(
                                    "FotoDebug",
                                    "📷 Mostrando imagen en Reporte desde URL: ${item.fotoUrl}"
                                )
                                AsyncImage(
                                    model = item.fotoUrl.trim(),
                                    contentDescription = "Imagen asociada",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                )
                            })
                    }
                }

                Log.d(
                    "PERMISO",
                    "Evaluando permiso para registro. Usuario del registro: ${item.usuario}, Tipo creador: ${item.tipoUsuarioCreador}"
                )

                val puedeEditarEliminar =
                    !esAuditado && !esInvitadoActual && puedeModificarRegistro(
                        item.usuario,
                        item.tipoUsuarioCreador
                    )

                if (puedeEditarEliminar) {
                    Log.d("PERMISO", "✅ PUEDE MODIFICAR este registro")

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { showEditDialog = true },
                        ) {
                            Icon(
                                Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Eliminar",
                                tint = Color.Red
                            )
                        }
                    }

                    // 👇 Botón de AUDITORÍA solo si todavía no está auditado
                    TextButton(
                        onClick = { showAuditDialog = true }) {
                        Text(
                            text = "Cerrar conteo (Auditar)",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                } else {
                    Log.d("PERMISO", "❌ NO PUEDE MODIFICAR este registro")

                    if (esAuditado) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Conteo auditado (solo lectura)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item.documentId)
                    showDeleteDialog = false
                }) {
                    Text("Sí", color = Color(0xFF003366), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            })
    }

    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text("Cerrar conteo (Auditar)") },
            text = {
                Text(
                    "¿Marcar este conteo como auditado?\n" + "Luego no podrá ser modificado excepto por el Superuser."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    val auditorNombre =
                        FirebaseAuth.getInstance().currentUser?.displayName ?: tipoUsuarioActual

                    FirebaseFirestore.getInstance().collection("clientes").document(clienteIdActual)
                        .collection("inventario").document(item.documentId).update(
                            mapOf(
                                "auditado" to true,
                                "auditadoPorUid" to uid,
                                "auditadoPorNombre" to (auditorNombre ?: ""),
                                "auditadoEn" to FieldValue.serverTimestamp()
                            )
                        ).addOnSuccessListener {
                            esAuditado = true
                            showAuditDialog = false
                            Toast.makeText(
                                context, "Conteo auditado correctamente", Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener { e ->
                            showAuditDialog = false
                            Toast.makeText(
                                context, "Error al auditar: ${e.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                }) {
                    Text(
                        "Sí, auditar", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuditDialog = false }) {
                    Text(
                        "Cancelar", color = Color.Red, fontWeight = FontWeight.Bold
                    )
                }
            })
    }

    if (showEditDialog) {
        var lote by remember { mutableStateOf(item.lote) }
        var cantidad by remember { mutableStateOf(item.quantity.toString()) }
        var ubicacion by remember { mutableStateOf(item.location) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Registro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ubicacion.uppercase(),
                        singleLine = true,
                        onValueChange = { ubicacion = it },
                        label = { Text("Editar Ubicación") })
                    OutlinedTextField(
                        value = lote.uppercase(),
                        singleLine = true,
                        onValueChange = { lote = it },
                        label = { Text("Editar Lote") })

                    OutlinedTextField(
                        value = fechaVencimiento,
                        onValueChange = { fechaVencimiento = it },
                        label = {
                            Text(
                                "Editar Fecha de Vencimiento", fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Seleccionar Fecha"
                                )
                            }
                        },
                        readOnly = true
                    )

                    OutlinedTextField(
                        value = cantidad,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),// ACTIVA EL TECLADO NUMERICO
                        onValueChange = { cantidad = it },
                        label = { Text("Editar Cantidad") })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving, onClick = {
                        if (isSaving) return@TextButton
                        isSaving = true

                        val cid = clienteIdActual.trim().uppercase()
                        val loc = item.localidad.trim()
                            .uppercase()          // usa el campo correcto de tu DataFields
                        val ubi = ubicacion.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
                        val loteEdit = lote.trim().uppercase()
                        val fechaEdit = fechaVencimiento.trim()
                        val cantEdit = cantidad.replace(",", ".").toDoubleOrNull() ?: item.quantity

                        if (cid.isBlank() || loc.isBlank() || ubi.isBlank()) {
                            isSaving = false
                            return@TextButton
                        }

                        validarUbicacionEnMaestro(
                            clienteId = cid,
                            localidadCodigo = loc,
                            codigoUbi = ubi,
                            onResult = { existe ->
                                if (existe) {
                                    val actualizado = item.copy(
                                        location = ubi,
                                        lote = loteEdit,
                                        expirationDate = fechaEdit,
                                        quantity = cantEdit
                                    )
                                    onEdit(actualizado)
                                    isSaving = false
                                    showEditDialog = false
                                } else {
                                    ubiInvalidaTexto = buildAnnotatedString {
                                        append("“")
                                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                        append(ubi)
                                        pop()
                                        append("” no existe en el maestro para ")
                                        // Si también quieres resaltar la localidad, deja este bloque:
                                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                        append(loc)
                                        pop()
                                        append(".")
                                    }
                                    showUbiInvalida = true
                                    isSaving = false
                                }
                            },
                            onError = {
                                ubiInvalidaTexto =
                                    AnnotatedString("No se pudo validar la ubicación.")
                                showUbiInvalida = true
                                isSaving = false
                            })

                    }) {
                    Text(
                        if (isSaving) "Validando…" else "Guardar",
                        color = Color(0xFF003366),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            })
        if (showUbiInvalida) {
            AlertDialog(
                onDismissRequest = { showUbiInvalida = false },
                confirmButton = {
                    TextButton(onClick = { showUbiInvalida = false }) {
                        Text("Entendido", fontWeight = FontWeight.Bold, color = Color(0xFF003366))
                    }
                },
                title = { Text("Ubicación inválida") },
                text = { Text(ubiInvalidaTexto) } // <- AnnotatedString con “ubi” (y “loc”) en negrita
            )
        }
    }
}