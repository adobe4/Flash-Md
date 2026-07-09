package com.vinakili.app.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinakili.app.AppState
import com.vinakili.app.OutFile
import com.vinakili.app.data.Repo
import com.vinakili.app.data.todayStr
import com.vinakili.app.i18n.LocalStr
import com.vinakili.app.ui.Card
import com.vinakili.app.ui.GhostButton
import com.vinakili.app.ui.LocalB
import com.vinakili.app.ui.PrimaryButton
import com.vinakili.app.ui.SectionTitle
import com.vinakili.app.ui.SegPicker

@Composable
fun SettingsScreen(app: AppState) {
    val b = LocalB.current
    val s = LocalStr.current
    val lang by Repo.lang.collectAsState()
    val theme by Repo.theme.collectAsState()

    OverlayScreen(s.settings, onBack = { app.pop() }) {
        // section: Personal / Business lives here now
        SectionTitle(s.sectionW)
        SegPicker(
            listOf(false to s.personal, true to s.business),
            app.business,
            { app.business = it },
            if (app.business) b.blue else b.amber,
            if (app.business) b.onBlue else b.onAmber,
        )

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.language)
        SegPicker(listOf("en" to "English", "sw" to "Kiswahili"), lang ?: "en",
            { Repo.setLang(it) }, b.amber, b.onAmber)

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.theme)
        SegPicker(listOf("dark" to s.darkTheme, "light" to s.lightTheme), theme,
            { Repo.setTheme(it) }, b.blue, b.onBlue)

        Spacer(Modifier.height(14.dp))
        SectionTitle(s.dataW)
        Card(Modifier.fillMaxWidth()) {
            Text(s.aboutOffline, color = b.textDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            PrimaryButton(s.exportFullBackup, {
                app.share(OutFile("vinakili-backup-${todayStr()}.json", "application/json",
                    text = Repo.buildBackupJson()))
                app.showToast(s.exportedOk)
            }, Modifier.fillMaxWidth(), accent = b.amber, onAccent = b.onAmber, icon = Icons.Rounded.CloudUpload)
            Spacer(Modifier.height(8.dp))
            GhostButton(s.importBackup, {
                app.importJson { raw ->
                    val n = try { Repo.restoreBackup(raw, "merge") } catch (_: Exception) { -1 }
                    app.showToast(if (n >= 0) "${s.backupRestored} — $n ${s.itemsLoaded}" else s.importFailed)
                }
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CloudDownload)
        }

        Spacer(Modifier.height(24.dp))
        Text("Vinakili · v1.0.0", color = b.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
