package com.eriknivar.firebasedatabase.view.utility

import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

/**
 * Normaliza códigos de ubicación: mayúsculas y sin caracteres que no sean A-Z o 0-9.
 */
fun normalizeUbi(input: String): String =
    input.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")

/**
 * Valida si la ubicación existe en el maestro (ruta NUEVA y fallback LEGACY).
 *   NUEVA:  /clientes/{cid}/localidades/{loc}/ubicaciones/{ubi}
 *   LEGACY: /clientes/{cid}/ubicaciones  (query por campo 'codigo_ubi' == {ubi})
 *
 * @param clienteId        ID de cliente (000002, etc.)
 * @param localidadCodigo  Código de localidad (ALM_REP, etc.)
 * @param codigoUbi        Código de ubicación (RP01A, etc.)
 * @param onResult         true si existe en maestro (nueva o legacy), false si no
 * @param onError          error de red/permiso (opcional)
 */
fun validarUbicacionEnMaestro(
    clienteId: String,
    localidadCodigo: String,
    codigoUbi: String,
    onResult: (Boolean) -> Unit,
    onError: (Exception) -> Unit = {}
) {
    val cid  = clienteId.trim().uppercase()
    val loc  = localidadCodigo.trim().uppercase()
    val code = normalizeUbi(codigoUbi)

    val db = Firebase.firestore

    // 1) Maestro NUEVO
    db.collection("clientes").document(cid)
        .collection("localidades").document(loc)
        .collection("ubicaciones").document(code)
        .get()
        .addOnSuccessListener { snap ->
            if (snap.exists()) {
                onResult(true)
            } else {
                // 2) Fallback LEGACY: query por 'codigo_ubi'
                db.collection("clientes").document(cid)
                    .collection("ubicaciones")
                    .whereEqualTo("codigo_ubi", code)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { q -> onResult(!q.isEmpty) }
                    .addOnFailureListener { onError(it) }
            }
        }
        .addOnFailureListener { onError(it) }
}
