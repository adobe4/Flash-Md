package com.bizana.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizana.app.AppState
import com.bizana.app.OutFile
import com.bizana.app.data.BizSettings
import com.bizana.app.data.Repo
import com.bizana.app.data.todayStr
import com.bizana.app.i18n.LocalStr
import com.bizana.app.ui.AmountField
import com.bizana.app.ui.Card
import com.bizana.app.ui.Field
import com.bizana.app.ui.GhostButton
import com.bizana.app.ui.LocalB
import com.bizana.app.ui.PrimaryButton
import com.bizana.app.ui.SectionTitle
import com.bizana.app.ui.SegPicker
import com.bizana.app.ui.Sheet
import com.bizana.app.ui.noRippleClickable
import com.bizana.app.ui.toAmount

@Composable
fun SettingsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang by Repo.lang.collectAsState()
    val theme by Repo.theme.collectAsState()
    val biz by Repo.bizSettings.collectAsState()

    var name by remember { mutableStateOf(biz.name) }
    var address by remember { mutableStateOf(biz.address) }
    var phone by remember { mutableStateOf(biz.phone) }
    var email by remember { mutableStateOf(biz.email) }
    var prefix by remember { mutableStateOf(biz.invoicePrefix) }
    var vat by remember { mutableStateOf(biz.defaultVat.takeIf { it > 0 }?.toString() ?: "") }
    var payNotes by remember { mutableStateOf(biz.paymentNotes) }
    var logo by remember { mutableStateOf(biz.logoBase64) }
    var importSheet by remember { mutableStateOf<String?>(null) }

    fun saveBiz() {
        Repo.saveBizSettings(BizSettings(name = name.trim(), logoBase64 = logo, address = address.trim(),
            phone = phone.trim(), email = email.trim(), invoicePrefix = prefix.trim().ifBlank { "INV" },
            defaultVat = vat.toAmount(), paymentNotes = payNotes.trim()))
        app.showToast(s.savedOk)
    }

    OverlayScreen(s.settings, onBack = { app.pop() }) {
        // language
        SectionTitle(s.language)
        SegPicker(listOf("en" to "English", "sw" to "Kiswahili"), lang ?: "en",
            { Repo.setLang(it) }, b.amber, b.onAmber)

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.theme)
        SegPicker(listOf("dark" to s.darkTheme, "light" to s.lightTheme), theme,
            { Repo.setTheme(it) }, b.blue, b.onBlue)

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.businessProfile)
        Card(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(b.surface2)
                    .noRippleClickable { app.pickImage { logo = it } }, contentAlignment = Alignment.Center) {
                    if (logo.isNotBlank()) {
                        LogoPreview(logo)
                    } else {
                        Icon(Icons.Rounded.Image, s.logo, tint = b.muted, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    GhostButton(if (logo.isBlank()) s.pickLogo else s.removeLogo, {
                        if (logo.isBlank()) app.pickImage { logo = it } else logo = ""
                    })
                }
            }
            Field(s.businessName, name, { name = it }, accent = b.blue)
            Field(s.address, address, { address = it }, singleLine = false, accent = b.blue)
            Field(s.phone, phone, { phone = it }, accent = b.blue)
            Field(s.email, email, { email = it }, accent = b.blue)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { Field(s.invoicePrefix, prefix, { prefix = it }, accent = b.blue) }
                Box(Modifier.weight(1f)) { AmountField(s.defaultVat, vat, { vat = it }, b.blue) }
            }
            Field(s.defaultPaymentNotes, payNotes, { payNotes = it }, singleLine = false, accent = b.blue)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(s.save, { saveBiz() }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.dataW)
        Card(Modifier.fillMaxWidth()) {
            Text(s.aboutOffline, color = b.textDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(s.exportFullBackup, {
                app.share(OutFile("bizana-backup-${todayStr()}.json", "application/json",
                    text = Repo.buildBackupJson()))
                app.showToast(s.exportedOk)
            }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber, icon = Icons.Rounded.CloudUpload)
            Spacer(Modifier.height(8.dp))
            GhostButton(s.importBackup, {
                app.importJson { raw -> importSheet = raw }
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CloudDownload)
        }

        Spacer(Modifier.height(20.dp))
        Text("Bizana · v1.0.0", color = b.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

    // import mode chooser
    Sheet(importSheet != null, s.importMode, onDismiss = { importSheet = null }) {
        Text(s.importModeDesc, color = b.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(s.mergeNewer, {
            val raw = importSheet ?: return@PrimaryButton
            val n = try { Repo.restoreBackup(raw, "merge") } catch (_: Exception) { -1 }
            app.showToast(if (n >= 0) "${s.backupRestored} — $n ${s.itemsLoaded}" else s.importFailed)
            importSheet = null
        }, Modifier.fillMaxWidth(), accent = b.blue, onAccent = b.onBlue)
        Spacer(Modifier.height(8.dp))
        GhostButton(s.replaceAll, {
            val raw = importSheet ?: return@GhostButton
            val n = try { Repo.restoreBackup(raw, "replace") } catch (_: Exception) { -1 }
            app.showToast(if (n >= 0) "${s.backupRestored} — $n ${s.itemsLoaded}" else s.importFailed)
            importSheet = null
        }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LogoPreview(base64: String) {
    val bmp = remember(base64) {
        try {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) { null }
    }
    if (bmp != null) {
        androidx.compose.foundation.Image(bitmap = bmp, contentDescription = "logo",
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}
