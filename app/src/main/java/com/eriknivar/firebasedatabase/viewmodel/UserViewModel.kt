package com.eriknivar.firebasedatabase.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue

enum class LogoutReason {
    MANUAL,
    INACTIVIDAD,
    ERROR,
    FORZADO
}

class UserViewModel : ViewModel() {

    private val _nombre = MutableLiveData("")
    val nombre: LiveData<String> = _nombre
    fun setNombre(v: String) {
        _nombre.postValue(v)
    }

    private val _tipo = MutableLiveData("")
    val tipo: LiveData<String> = _tipo
    fun setTipo(v: String) {
        _tipo.postValue(v)
    }

    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized

    private val _documentId = MutableLiveData("")
    val documentId: LiveData<String> = _documentId

    private val _fotoUrl = MutableLiveData<String?>()

    private val _clienteId = MutableLiveData("")
    val clienteId: LiveData<String> = _clienteId
    fun setClienteId(v: String) {
        _clienteId.postValue(v)
    }

    private fun limpiarEstadoLocal() {
        _nombre.value = ""
        _tipo.value = ""
        _documentId.value = ""
        _clienteId.value = ""
        _clienteNombre.value = ""
        _sessionId.value = ""
        _isInitialized.value = false
        isManualLogout.value = false
        limpiarValoresTemporales()
    }

    fun cerrarSesion(
        motivo: LogoutReason = LogoutReason.MANUAL,
        onComplete: (() -> Unit)? = null
    ) {
        val auth = Firebase.auth
        val currentUid = auth.currentUser?.uid

        // docId puede ser el que guardas tú o el uid de Auth
        val docId = documentId.value?.takeIf { it.isNotBlank() } ?: currentUid

        // marcar si fue manual para mensajes en UI
        isManualLogout.value = (motivo == LogoutReason.MANUAL)

        if (docId == null) {
            // No hay usuario logueado, solo limpiamos local
            limpiarEstadoLocal()
            auth.signOut()
            onComplete?.invoke()
            return
        }

        val userDoc = Firebase.firestore
            .collection("usuarios")
            .document(docId)

        val updates = hashMapOf<String, Any?>(
            "sessionId" to FieldValue.delete(),            // 👈 se borra SIEMPRE
            "sesionActiva" to false,
            "ultimoLogout" to FieldValue.serverTimestamp(),
            "motivoUltimoLogout" to motivo.name
        )

        userDoc.update(updates)
            .addOnCompleteListener {
                limpiarEstadoLocal()
                auth.signOut()
                onComplete?.invoke()
            }
    }


    fun setUser(nombre: String, tipo: String, documentId: String) {
        _nombre.value = nombre
        _tipo.value = tipo
        _documentId.value = documentId
        _isInitialized.value = true
    }

    // en UserViewModel
    private val _clienteNombre = MutableLiveData("")
    val clienteNombre: LiveData<String> = _clienteNombre

    fun setClienteNombre(n: String) {
        _clienteNombre.value = n
    }

    fun cargarFotoUrl(documentId: String) {
        Firebase.firestore.collection("usuarios")
            .document(documentId)
            .get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("fotoUrl")
                _fotoUrl.value = url
                Log.d("FOTO_DEBUG", "fotoUrl cargada: $url")
            }
            .addOnFailureListener {
                Log.e("FOTO_DEBUG", "Error al cargar fotoUrl", it)
            }
    }

    private val _sessionId = mutableStateOf("")
    val sessionId: State<String> = _sessionId

    fun setSessionId(newSessionId: String) {
        _sessionId.value = newSessionId
    }

    private val _recargarUsuarios = mutableStateOf(false)

    fun activarRecargaUsuarios() {
        _recargarUsuarios.value = !_recargarUsuarios.value // ⚡ Dispara la recarga
    }

    var isManualLogout = mutableStateOf(false)

    // ✅ Variables temporales para restaurar campos después del logout
    var tempSku = ""
    var tempLote = ""
    var tempCantidad = ""
    var tempUbicacion = ""
    var tempFecha = ""

    fun clearUser() {
        _nombre.value = ""
        _tipo.value = ""
        _isInitialized.value = true
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        cerrarSesion(LogoutReason.MANUAL, onComplete)
    }


    // ✅ Esta función guarda los datos actuales antes del logout
    fun guardarValoresTemporalmente(
        sku: String,
        lote: String,
        cantidad: String,
        ubicacion: String,
        fecha: String
    ) {
        tempSku = sku
        tempLote = lote
        tempCantidad = cantidad
        tempUbicacion = ubicacion
        tempFecha = fecha

        Log.d(
            "TEMPORAL",
            "Guardado -> SKU: $sku, Lote: $lote, Cant: $cantidad, Ub: $ubicacion, Fecha: $fecha"
        )

    }

    fun limpiarValoresTemporales() {
        tempSku = ""
        tempLote = ""
        tempCantidad = ""
        tempUbicacion = ""
        tempFecha = ""
    }

    fun puedeModificarRegistro(registroUsuario: String, registroTipo: String): Boolean {
        val actualTipo = tipo.value?.lowercase()?.trim() ?: ""
        val actualNombre = nombre.value?.trim() ?: ""

        return when (actualTipo) {
            "superuser" -> true // Puede modificar todo
            "admin" -> registroTipo.lowercase() != "superuser" // No puede modificar registros de superuser
            "invitado" -> registroUsuario == actualNombre // Solo puede modificar lo suyo
            else -> false
        }
    }

    // UserViewModel
    var onUserInteracted: (() -> Unit)? = null

    // === Session (Absolute Timeout) - GLOBAL en el VM ===
    private val _sessionStartMs = MutableLiveData<Long>(System.currentTimeMillis())
    val sessionStartMs: LiveData<Long> = _sessionStartMs

    /** Llama esto al iniciar sesión / cuando el usuario queda listo en pantalla */
    fun resetSessionStart() {
        _sessionStartMs.value = System.currentTimeMillis()
    }
}