package com.vinakili.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class Screen {
    object Root : Screen()
    object Settings : Screen()
    data class ClientDetail(val clientId: String) : Screen()
    data class InvoiceEditor(val invoiceId: String?) : Screen()
    data class InvoiceDetail(val invoiceId: String) : Screen()
}

/** A file the app wants to hand to Android to save/share. */
data class OutFile(val name: String, val mime: String, val text: String? = null, val bytes: ByteArray? = null)

class AppState {
    var business by mutableStateOf(false)          // false = Personal, true = Business
    var personalTab by mutableStateOf(0)           // 0..4
    var businessTab by mutableStateOf(0)           // 0..5

    val stack = mutableStateListOf<Screen>()       // overlay screens above Root

    var toast by mutableStateOf<String?>(null)

    // Android bridges — set by MainActivity
    var doShare: ((OutFile) -> Unit)? = null
    var doImportJson: ((onPicked: (String) -> Unit) -> Unit)? = null
    var doImportCsv: ((onPicked: (String) -> Unit) -> Unit)? = null
    var doPickImage: ((onPicked: (String) -> Unit) -> Unit)? = null

    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) }
    val current: Screen get() = stack.lastOrNull() ?: Screen.Root

    fun showToast(msg: String) { toast = msg }

    fun share(f: OutFile) { doShare?.invoke(f) }
    fun importJson(onPicked: (String) -> Unit) { doImportJson?.invoke(onPicked) }
    fun importCsv(onPicked: (String) -> Unit) { doImportCsv?.invoke(onPicked) }
    fun pickImage(onPicked: (String) -> Unit) { doPickImage?.invoke(onPicked) }
}
