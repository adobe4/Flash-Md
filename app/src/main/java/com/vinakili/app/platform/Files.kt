package com.vinakili.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Files {
    /** Write [content] to a shareable file in cache and return its content:// uri. */
    fun writeShareable(context: Context, fileName: String, content: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun writeShareableBytes(context: Context, fileName: String, bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, uri: Uri, mime: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun readText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: ""
    }

    fun readBytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }
}
