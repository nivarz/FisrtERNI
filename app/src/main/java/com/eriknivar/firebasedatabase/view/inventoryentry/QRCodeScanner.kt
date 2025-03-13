package com.eriknivar.firebasedatabase.view.inventoryentry

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.zxing.integration.android.IntentIntegrator

class QRCodeScanner(private val qrScanLauncher: ActivityResultLauncher<Intent>) {

    fun startQRCodeScanner(activity: Activity) {
        val integrator = IntentIntegrator(activity)

        // ✅ Configuración para escanear tanto códigos QR como códigos de barras
        integrator.setDesiredBarcodeFormats(
            listOf(
                IntentIntegrator.QR_CODE, // QR Code
                IntentIntegrator.CODE_128, // Código de barras tipo CODE_128
                IntentIntegrator.CODE_39, // Código de barras tipo CODE_39
                IntentIntegrator.EAN_13,  // Código de barras tipo EAN-13 (usado en productos)
                IntentIntegrator.UPC_A    // Código de barras tipo UPC-A
            )
        )

        integrator.setPrompt("Escanea un código QR o un código de barras")
        integrator.setCameraId(0) // Cámara trasera
        integrator.setBeepEnabled(true) // Sonido al escanear
        integrator.setBarcodeImageEnabled(true)

        val scanIntent = integrator.createScanIntent()
        qrScanLauncher.launch(scanIntent)
    }
}

