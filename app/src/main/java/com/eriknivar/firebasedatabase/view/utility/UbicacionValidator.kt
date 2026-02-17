package com.eriknivar.firebasedatabase.view.utility

import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore


fun ubiVariants(input: String): List<String> {
    val raw = input.trim().uppercase()
    val noDash = raw.replace("-", "")
    return listOf(raw, noDash).distinct()
}

/**
 * Normaliza códigos de ubicación: mayúsculas y sin caracteres que no sean A-Z o 0-9.
 */
fun normalizeUbi(input: String): String =
    input.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")

/**
 * Válida si la ubicación existe en el maestro (ruta NUEVA y fallback LEGACY).
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
    onResult: (Boolean, String) -> Unit, // existe + codigo “ganador”
    onError: (Exception) -> Unit = {}
) {
    val cid = clienteId.trim().uppercase()
    val loc = localidadCodigo.trim().uppercase()

    val raw  = codigoUbi.trim().uppercase()
    val norm = normalizeUbi(raw)
    val variants = listOf(raw, norm).distinct()

    val db = Firebase.firestore

    fun tryNew(i: Int) {
        if (i >= variants.size) {
            val list = variants.take(10)
            db.collection("clientes").document(cid)
                .collection("ubicaciones")
                .whereIn("codigo_ubi", list)
                .limit(1)
                .get()
                .addOnSuccessListener { q ->
                    if (!q.isEmpty) {
                        val encontrado = q.documents.first().getString("codigo_ubi") ?: list.first()
                        onResult(true, encontrado)
                    } else {
                        onResult(false, raw)
                    }
                }
                .addOnFailureListener { onError(it) }
            return
        }

        val v = variants[i]
        db.collection("clientes").document(cid)
            .collection("localidades").document(loc)
            .collection("ubicaciones").document(v)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) onResult(true, v)
                else tryNew(i + 1)
            }
            .addOnFailureListener { onError(it) }
    }

    tryNew(0)
}
