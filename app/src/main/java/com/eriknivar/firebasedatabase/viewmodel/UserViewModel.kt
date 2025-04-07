
package com.eriknivar.firebasedatabase.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class UserViewModel : ViewModel() {

    private val _nombre = MutableLiveData("")
    val nombre: LiveData<String> = _nombre

    private val _tipo = MutableLiveData("")
    val tipo: LiveData<String> = _tipo

    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized

    // ✅ Variables temporales para restaurar campos después del logout
    var tempSku = ""
    var tempLote = ""
    var tempCantidad = ""
    var tempUbicacion = ""
    var tempFecha = ""

    fun setUser(nombre: String, tipo: String) {
        _nombre.value = nombre
        _tipo.value = tipo
        _isInitialized.value = true
    }

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

        Log.d("TEMPORAL", "Guardado -> SKU: $sku, Lote: $lote, Cant: $cantidad, Ub: $ubicacion, Fecha: $fecha")

    }

    fun limpiarValoresTemporales() {
        tempSku = ""
        tempLote = ""
        tempCantidad = ""
        tempUbicacion = ""
        tempFecha = ""
    }

}





