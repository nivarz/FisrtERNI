package com.eriknivar.firebasedatabase.view.utility.clientes

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.eriknivar.firebasedatabase.viewmodel.ClienteFormInput


class ClienteDuplicadoException(message: String) : Exception(message)
class ClienteDatosInvalidosException(message: String) : Exception(message)

data class ClienteCreateInput(
    val nombreComercial: String,
    val rncOCedula: String,
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val notas: String? = null,
    val creadoPorUid: String
)

class ClientesRepository {

    private val db = Firebase.firestore

    /**
     * Crea un cliente con:
     * - clienteId secuencial (contadores/clientes.ultimoNumero)
     * - índice único por RNC/Cédula (indices_clientes_rnc/{rncLimpio})
     * - auditoría (auditoria_clientes)
     *
     * @return clienteId creado (ej. "000123")
     */
    suspend fun crearCliente(input: ClienteCreateInput): String {
        // --- Validaciones de campo mínimas ---
        ClienteUtils.validarNombre(input.nombreComercial)
            ?.let { throw ClienteDatosInvalidosException(it) }
        ClienteUtils.validarRnc(input.rncOCedula)?.let { throw ClienteDatosInvalidosException(it) }
        ClienteUtils.validarEmailBasico(input.email)
            ?.let { throw ClienteDatosInvalidosException(it) }
        ClienteUtils.validarTelefonoRD(input.telefono)
            ?.let { throw ClienteDatosInvalidosException(it) }

        val nombreNorm = ClienteUtils.normalizarNombre(input.nombreComercial)
        val rncLimpio = ClienteUtils.limpiarRncOCedula(input.rncOCedula)

        val contRef = db.collection("contadores").document("clientes")
        val auditRef = db.collection("auditoria_clientes").document()
        val idxRef = db.collection("indices_clientes_rnc").document(rncLimpio)

        // Transacción para garantizar atomicidad (ID + índice único + doc cliente + auditoría)
        val clienteId = db.runTransaction { tx ->
            // 1) Leer e incrementar contador
            val contSnap = tx.get(contRef)
            val ultimo = (contSnap.getLong("ultimoNumero") ?: 0L)
            val siguiente = ultimo + 1L
            val nuevoId = ClienteUtils.zeroPad6(siguiente.toInt())

            // 2) Verificar índice único por RNC/Cédula
            val idxSnap = tx.get(idxRef)
            if (idxSnap.exists()) {
                throw ClienteDuplicadoException("Ya existe un cliente con ese RNC/Cédula.")
            }

            // 3) Preparar documento cliente
            val clienteRef = db.collection("clientes").document(nuevoId)
            val now = FieldValue.serverTimestamp()

            val dataCliente = hashMapOf(
                "clienteId" to nuevoId,
                "nombreComercial" to input.nombreComercial.trim(),
                "rncOCedula" to input.rncOCedula.trim(),
                "telefono" to input.telefono,
                "email" to input.email,
                "direccion" to input.direccion,
                "notas" to input.notas,
                "activo" to true,

                // Derivados / búsqueda
                "nombreNormalizado" to nombreNorm,
                "rncLimpio" to rncLimpio,

                // Auditoría
                "creadoPorUid" to input.creadoPorUid,
                "creadoEn" to now
            )

            // 4) Escribir: contador, cliente, índice y auditoría
            tx.set(contRef, mapOf("ultimoNumero" to siguiente), SetOptions.merge())
            tx.set(clienteRef, dataCliente)
            tx.set(idxRef, mapOf("clienteId" to nuevoId))

            val dataAudit = hashMapOf(
                "accion" to "crear",
                "clienteId" to nuevoId,
                "rncOCedula" to input.rncOCedula.trim(),
                "payloadAntes" to null,
                "payloadDespues" to dataCliente,
                "hechoPorUid" to input.creadoPorUid,
                "fecha" to now
            )
            tx.set(auditRef, dataAudit)

            nuevoId
        }.await()

        return clienteId
    }


    data class ClienteUpdateInput(
        val clienteId: String,
        val nombreComercial: String? = null,
        val rncOCedula: String? = null,
        val telefono: String? = null,
        val email: String? = null,
        val direccion: String? = null,
        val notas: String? = null,
        val actualizadoPorUid: String
    )

    suspend fun editarCliente(input: ClienteUpdateInput) {
        // --- Validaciones solo de lo que viene ---
        input.nombreComercial?.let {
            ClienteUtils.validarNombre(it)?.let { msg -> throw ClienteDatosInvalidosException(msg) }
        }
        input.rncOCedula?.let {
            ClienteUtils.validarRnc(it)?.let { msg -> throw ClienteDatosInvalidosException(msg) }
        }
        input.email?.let {
            ClienteUtils.validarEmailBasico(it)
                ?.let { msg -> throw ClienteDatosInvalidosException(msg) }
        }
        input.telefono?.let {
            ClienteUtils.validarTelefonoRD(it)
                ?.let { msg -> throw ClienteDatosInvalidosException(msg) }
        }

        val clienteRef = db.collection("clientes").document(input.clienteId)
        val auditRef = db.collection("auditoria_clientes").document()

        db.runTransaction { tx ->
            val snap = tx.get(clienteRef)
            if (!snap.exists()) throw ClienteDatosInvalidosException("Cliente no encontrado.")

            val before = snap.data ?: emptyMap<String, Any?>()

            // Mantener valores actuales como base
            var nombreComercial =
                (input.nombreComercial ?: before["nombreComercial"] as? String ?: "").trim()
            var rncOCedula = (input.rncOCedula ?: before["rncOCedula"] as? String ?: "").trim()
            val telefono = input.telefono ?: before["telefono"]
            val email = input.email ?: before["email"]
            val direccion = input.direccion ?: before["direccion"]
            val notas = input.notas ?: before["notas"]

            // Derivados
            val nombreNorm = ClienteUtils.normalizarNombre(nombreComercial)
            val rncLimpioNuevo = ClienteUtils.limpiarRncOCedula(rncOCedula)
            val rncLimpioPrevio = (before["rncLimpio"] as? String).orEmpty()

            // Si cambió el RNC (lógico), asegurar unicidad moviendo el índice
            if (rncLimpioNuevo != rncLimpioPrevio) {
                val newIdxRef = db.collection("indices_clientes_rnc").document(rncLimpioNuevo)
                val newIdxSnap = tx.get(newIdxRef)
                if (newIdxSnap.exists()) {
                    throw ClienteDuplicadoException("Ya existe un cliente con ese RNC/Cédula.")
                }
                // Crear índice nuevo
                tx.set(newIdxRef, mapOf("clienteId" to input.clienteId))
                // Borrar índice anterior si existía
                if (rncLimpioPrevio.isNotBlank()) {
                    val oldIdxRef = db.collection("indices_clientes_rnc").document(rncLimpioPrevio)
                    tx.delete(oldIdxRef)
                }
            }

            val now = FieldValue.serverTimestamp()

            // Campos a actualizar (no permitir cambiar clienteId/activo aquí)
            val updates = mutableMapOf<String, Any?>(
                "nombreComercial" to nombreComercial,
                "rncOCedula" to rncOCedula,
                "telefono" to telefono,
                "email" to email,
                "direccion" to direccion,
                "notas" to notas,
                "nombreNormalizado" to nombreNorm,
                "rncLimpio" to rncLimpioNuevo,
                "actualizadoPorUid" to input.actualizadoPorUid,
                "actualizadoEn" to now
            )

            tx.update(clienteRef, updates)

            // Auditoría (registramos cambios enviados)
            val audit = hashMapOf(
                "accion" to "editar",
                "clienteId" to input.clienteId,
                "rncOCedula" to rncOCedula,
                "payloadAntes" to before,
                "payloadDespues" to updates,
                "hechoPorUid" to input.actualizadoPorUid,
                "fecha" to now
            )
            tx.set(auditRef, audit)
            null
        }.await()
    }

    data class ClienteChangeEstadoInput(
        val clienteId: String,
        val activar: Boolean,        // true = activar, false = desactivar
        val motivo: String,          // obligatorio para auditoría
        val hechoPorUid: String
    )

    suspend fun cambiarEstadoCliente(input: ClienteChangeEstadoInput) {
        if (input.motivo.isBlank()) throw ClienteDatosInvalidosException("Debes indicar un motivo.")

        val clienteRef = db.collection("clientes").document(input.clienteId)
        val auditRef = db.collection("auditoria_clientes").document()

        db.runTransaction { tx ->
            val snap = tx.get(clienteRef)
            if (!snap.exists()) throw ClienteDatosInvalidosException("Cliente no encontrado.")

            val before = snap.data ?: emptyMap<String, Any?>()
            val estadoActual = before["activo"] as? Boolean ?: true
            if (estadoActual == input.activar) {
                val msg =
                    if (input.activar) "El cliente ya está activo." else "El cliente ya está inactivo."
                throw ClienteDatosInvalidosException(msg)
            }

            val now = FieldValue.serverTimestamp()
            val updates = mapOf(
                "activo" to input.activar,
                "motivoCambioEstado" to input.motivo,
                "actualizadoPorUid" to input.hechoPorUid,
                "actualizadoEn" to now
            )

            tx.update(clienteRef, updates)

            val audit = hashMapOf(
                "accion" to if (input.activar) "activar" else "desactivar",
                "clienteId" to input.clienteId,
                "payloadAntes" to before,
                "payloadDespues" to updates,
                "motivo" to input.motivo,
                "hechoPorUid" to input.hechoPorUid,
                "fecha" to now
            )
            tx.set(auditRef, audit)
            null
        }.await()
    }

    // --- DTOs de salida para la lista ---
    data class ClienteListItem(
        val clienteId: String,
        val nombreComercial: String,
        val rncOCedula: String,
        val activo: Boolean
    )

    data class PagedResult<T>(
        val items: List<T>,
        val lastSnapshot: DocumentSnapshot?
    )

    // --- Helper de mapeo ---
    private fun DocumentSnapshot.toClienteListItem(): ClienteListItem =
        ClienteListItem(
            clienteId = getString("clienteId") ?: id,
            nombreComercial = getString("nombreComercial") ?: "",
            rncOCedula = getString("rncOCedula") ?: "",
            activo = getBoolean("activo") ?: true
        )


    //Listar activos (ordenados por nombre) + paginación

    suspend fun listarClientesActivos(
        limit: Long = 20,
        startAfter: DocumentSnapshot? = null
    ): PagedResult<ClienteListItem> {
        var q = db.collection("clientes")
            .whereEqualTo("activo", true)
            .orderBy("nombreNormalizado")
            .limit(limit)

        if (startAfter != null) q = q.startAfter(startAfter)

        val snap = q.get().await()
        val items = snap.documents.map { it.toClienteListItem() }
        return PagedResult(items, snap.documents.lastOrNull())
    }

    //Búsqueda por prefijo de nombre (case/acentos-insensitive)
    suspend fun buscarClientesPorNombre(
        prefix: String,
        limit: Long = 20,
        startAfter: DocumentSnapshot? = null
    ): PagedResult<ClienteListItem> {
        val normalized = ClienteUtils.normalizarNombre(prefix)

        var q = db.collection("clientes")
            .whereEqualTo("activo", true)
            .orderBy("nombreNormalizado")
            .startAt(normalized)
            .endAt(normalized + "\uf8ff")
            .limit(limit)

        if (startAfter != null) q = q.startAfter(startAfter)

        val snap = q.get().await()
        val items = snap.documents.map { it.toClienteListItem() }
        return PagedResult(items, snap.documents.lastOrNull())
    }

    //Búsqueda por RNC/Cédula (exacta con limpieza)
    suspend fun buscarClientesPorRnc(
        rncOCedula: String,
        limit: Long = 20,
        startAfter: DocumentSnapshot? = null
    ): PagedResult<ClienteListItem> {
        val limpio = ClienteUtils.limpiarRncOCedula(rncOCedula)

        var q = db.collection("clientes")
            .whereEqualTo("activo", true)
            .whereEqualTo("rncLimpio", limpio)
            .orderBy("nombreNormalizado")
            .limit(limit)

        if (startAfter != null) q = q.startAfter(startAfter)

        val snap = q.get().await()
        val items = snap.documents.map { it.toClienteListItem() }
        return PagedResult(items, snap.documents.lastOrNull())
    }

    //Búsqueda “inteligente” (elige por ti nombre vs RNC)

    suspend fun buscarClientes(
        term: String,
        limit: Long = 20,
        startAfter: DocumentSnapshot? = null
    ): PagedResult<ClienteListItem> {
        val limpio = ClienteUtils.limpiarRncOCedula(term)
        val pareceRnc = limpio.length >= 6 && limpio.any { it.isDigit() }
        return if (pareceRnc) {
            buscarClientesPorRnc(term, limit, startAfter)
        } else {
            buscarClientesPorNombre(term, limit, startAfter)
        }
    }

    //obtener un cliente por ID (para editar)

    // Para precargar el formulario en modo EDIT
    suspend fun obtenerClienteFormInput(clienteId: String): ClienteFormInput? {
        val snap = db.collection("clientes").document(clienteId).get().await()
        if (!snap.exists()) return null

        return ClienteFormInput(
            clienteId = snap.getString("clienteId") ?: clienteId,
            nombreComercial = snap.getString("nombreComercial").orEmpty(),
            rncOCedula = snap.getString("rncOCedula").orEmpty(),
            telefono = snap.getString("telefono").orEmpty(),
            email = snap.getString("email").orEmpty(),
            direccion = snap.getString("direccion").orEmpty(),
            notas = snap.getString("notas").orEmpty()
        )
    }

    class ClienteNoEncontradoException(message: String) : Exception(message)

    suspend fun eliminarCliente(
        clienteId: String,
        motivo: String,
        hechoPorUid: String
    ) {
        val clienteRef = db.collection("clientes").document(clienteId)
        val auditRef = db.collection("auditoria_clientes").document()

        db.runTransaction { tx ->
            val snap = tx.get(clienteRef)
            if (!snap.exists()) throw ClienteNoEncontradoException("Cliente no encontrado.")

            val before = snap.data ?: emptyMap<String, Any?>()
            val rncLimpio = before["rncLimpio"] as? String

            // Borrar doc principal
            tx.delete(clienteRef)

            // Borrar índice por RNC si existe
            if (!rncLimpio.isNullOrBlank()) {
                val idxRef = db.collection("indices_clientes_rnc").document(rncLimpio)
                tx.delete(idxRef)
            }

            // Auditoría
            val now = FieldValue.serverTimestamp()
            val audit = hashMapOf(
                "accion" to "eliminar",
                "clienteId" to clienteId,
                "payloadAntes" to before,
                "payloadDespues" to null,
                "motivo" to motivo,
                "hechoPorUid" to hechoPorUid,
                "fecha" to now
            )
            tx.set(auditRef, audit)
            null
        }.await()
    }
}