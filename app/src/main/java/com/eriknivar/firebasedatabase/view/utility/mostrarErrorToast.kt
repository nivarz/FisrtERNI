package com.eriknivar.firebasedatabase.view.utility

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.eriknivar.firebasedatabase.R

fun mostrarErrorToast(context: Context, mensaje: String) {
    val inflater = LayoutInflater.from(context)
    val rootView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
    val layout = inflater.inflate(R.layout.custom_toast_layout, rootView, false)

    val toastIcon = layout.findViewById<ImageView>(R.id.toast_icon)
    val toastText = layout.findViewById<TextView>(R.id.toast_text)

    toastIcon.setImageResource(R.drawable.ic_error)
    toastText.text = mensaje

    Toast(context).apply {
        duration = Toast.LENGTH_LONG
        view = layout
        setGravity(Gravity.CENTER, 0, 0) // Centrado en pantalla
        show()
    }
}


