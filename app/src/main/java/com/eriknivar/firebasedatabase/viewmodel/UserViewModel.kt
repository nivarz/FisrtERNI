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

class UserViewModel : ViewModel() {

    private val _nombre = MutableLiveData("")
    val nombre: LiveData<String> = _nombre
    fun setNombre(v: String) { _nombre.postValue(v) }

    private val _tipo = MutableLiveData("")
    val tipo: LiveData<String> = _tipo
    fun setTipo(v: String) { _tipo.postValue(v) }


    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized

    private val _documentId = MutableLiveData("")
    val documentId: LiveData<String> = _documentId

    private val _fotoUrl = MutableLiveData<String?>()

    private val _clienteId = MutableLiveData("")
    val clienteId: LiveData<String> = _clienteId
    fun setClienteId(v: String) { _clienteId.postValue(v) }

     fun setUser(nombre: String, tipo: String, documentId: String) {
        _nombre.value = nombre
        _tipo.value = tipo
        _documentId.value = documentId
        _isInitialized.value = true
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

    fun logout() {
        Firebase.auth.signOut()
        _nombre.value = ""
        _tipo.value = ""
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


}




