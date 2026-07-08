package com.bizana.app

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.bizana.app.data.Repo
import com.bizana.app.platform.Files

class MainActivity : ComponentActivity() {

    private var pendingText: ((String) -> Unit)? = null
    private var pendingImage: ((String) -> Unit)? = null

    private val openText = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val cb = pendingText; pendingText = null
        if (uri != null && cb != null) {
            try { cb(Files.readText(this, uri)) } catch (_: Exception) { }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val cb = pendingImage; pendingImage = null
        if (uri != null && cb != null) {
            try {
                val bytes = Files.readBytes(this, uri)
                cb("data:image/*;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP))
            } catch (_: Exception) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(applicationContext)

        val app = AppState()
        app.doShare = { out ->
            try {
                val uri = if (out.bytes != null)
                    Files.writeShareableBytes(this, out.name, out.bytes)
                else
                    Files.writeShareable(this, out.name, out.text ?: "")
                Files.share(this, uri, out.mime, out.name)
            } catch (_: Exception) { }
        }
        app.doImportJson = { onPicked ->
            pendingText = onPicked
            try { openText.launch(arrayOf("application/json", "text/*", "*/*")) } catch (_: Exception) { }
        }
        app.doImportCsv = { onPicked ->
            pendingText = onPicked
            try { openText.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*")) } catch (_: Exception) { }
        }
        app.doPickImage = { onPicked ->
            pendingImage = onPicked
            try { pickImage.launch("image/*") } catch (_: Exception) { }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (app.stack.isNotEmpty()) app.pop() else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            val state = remember { app }
            BizanaApp(state)
        }
    }
}
