
package com.eriknivar.firebasedatabase.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {

    private val _nombre = MutableLiveData<String>()
    val nombre: LiveData<String> = _nombre

    private val _tipo = MutableLiveData<String>()
    val tipo: LiveData<String> = _tipo

    fun setUser(nombre: String, tipo: String) {
        _nombre.value = nombre
        _tipo.value = tipo
    }

    fun clearUser() {
        _nombre.value = ""
        _tipo.value = ""
    }
}

