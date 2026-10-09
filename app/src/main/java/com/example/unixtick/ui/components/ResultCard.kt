package com.example.unixtick.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unixtick.data.converter.UnixTickConverter
import com.example.unixtick.data.model.ConversionMode
import com.example.unixtick.data.model.UnixTickResult
import com.example.unixtick.data.model.UserSettings
import com.example.unixtick.ui.theme.AmberWarning
import com.example.unixtick.ui.theme.MonospaceResultStyle
import com.example.unixtick.ui.theme.MonospaceValueStyle
import com.example.unixtick.ui.theme.TerminalGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultCard(
    result: UnixTickResult,
    mode: ConversionMode,
    settings: UserSettings,
    haptic: HapticFeedback,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("UnixTick", text)
        clipboard?.setPrimaryClip(clip)
        if (settings.hapticFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onShowSnackbar(label)
    }

    fun shareSummary() {
        val summary = buildString {
            appendLine("UnixTick")
            appendLine("Unix Seconds: ${result.unixSeconds}")
            appendLine("Unix Milliseconds: ${result.unixMilliseconds}")
            appendLine("UTC: ${UnixTickConverter.formatUtc(result.instant, settings.dateFormat, settings.timeFormat)}")
            appendLine("Local: ${UnixTickConverter.formatLocal(result.instant, result.localDateTime.zone, settings.dateFormat, settings.timeFormat)}")
            appendLine("ISO 8601: ${result.isoUtc}")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summary)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Timestamp")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // DST Warning if present
        if (result.dstWarningMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = result.dstWarningMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Primary Visual Result Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val primaryLabel = if (mode == ConversionMode.TIMESTAMP_TO_DATE) "CONVERTED TIME" else "UNIX TIMESTAMP"
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val primaryValue = if (mode == ConversionMode.TIMESTAMP_TO_DATE) {
                    UnixTickConverter.formatUtc(result.instant, settings.dateFormat, settings.timeFormat)
                } else {
                    result.unixSeconds.toString()
                }

                Text(
                    text = primaryValue,
                    style = MonospaceResultStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = if (mode == ConversionMode.TIMESTAMP_TO_DATE) "Converted UTC time" else "Unix seconds result"
                    }
                )

                if (settings.showHumanDate && mode == ConversionMode.TIMESTAMP_TO_DATE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${result.dayOfWeek}, ${result.humanFormattedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (mode == ConversionMode.DATE_TO_TIMESTAMP && settings.showMilliseconds) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MILLISECONDS: ${result.unixMilliseconds}",
                        style = MonospaceValueStyle.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.semantics {
                            contentDescription = "Unix milliseconds result"
                        }
                    )
                }
            }
        }

        // Converted Date Details Card (UTC & Local)
        if (settings.showUtc || settings.showLocalTime) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (settings.showUtc) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UTC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Offset: +00:00",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = UnixTickConverter.formatUtc(result.instant, settings.dateFormat, settings.timeFormat),
                                style = MonospaceValueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics {
                                    contentDescription = "Converted UTC time"
                                }
                            )
                        }
                    }

                    if (settings.showLocalTime) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEVICE LOCAL TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${result.localDateTime.zone.id} (${result.localDateTime.offset.id})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = UnixTickConverter.formatLocal(result.instant, result.localDateTime.zone, settings.dateFormat, settings.timeFormat),
                                style = MonospaceValueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics {
                                    contentDescription = "Converted local time"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Timestamp Units Card (Seconds & Milliseconds)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UNIX SECONDS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = result.unixSeconds.toString(),
                            style = MonospaceValueStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard(result.unixSeconds.toString(), "Unix seconds copied") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.semantics { contentDescription = "Copy Unix seconds" }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Copy Sec", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (settings.showMilliseconds) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "UNIX MILLISECONDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = result.unixMilliseconds.toString(),
                                style = MonospaceValueStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = { copyToClipboard(result.unixMilliseconds.toString(), "Unix milliseconds copied") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.semantics { contentDescription = "Copy Unix milliseconds" }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Copy Ms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // ISO 8601 Representation Card
        if (settings.showIso8601) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ISO 8601 (UTC)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        OutlinedButton(
                            onClick = { copyToClipboard(result.isoUtc, "ISO 8601 copied") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.semantics { contentDescription = "Copy ISO 8601" }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Copy ISO", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = result.isoUtc,
                        style = MonospaceValueStyle.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ISO 8601 (Local)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = result.isoLocal,
                        style = MonospaceValueStyle.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Action Buttons Row: Copy Actions & Share
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val utcStr = UnixTickConverter.formatUtc(result.instant, settings.dateFormat, settings.timeFormat)
                    copyToClipboard(utcStr, "UTC date/time copied")
                },
                modifier = Modifier.semantics { contentDescription = "Copy UTC time" }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Copy UTC")
            }

            FilledTonalButton(
                onClick = {
                    val localStr = UnixTickConverter.formatLocal(result.instant, result.localDateTime.zone, settings.dateFormat, settings.timeFormat)
                    copyToClipboard(localStr, "Local date/time copied")
                },
                modifier = Modifier.semantics { contentDescription = "Copy local time" }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Copy Local")
            }

            FilledTonalButton(
                onClick = {
                    val allText = buildString {
                        appendLine("Unix Seconds: ${result.unixSeconds}")
                        appendLine("Unix Milliseconds: ${result.unixMilliseconds}")
                        appendLine("UTC: ${UnixTickConverter.formatUtc(result.instant, settings.dateFormat, settings.timeFormat)}")
                        appendLine("Local: ${UnixTickConverter.formatLocal(result.instant, result.localDateTime.zone, settings.dateFormat, settings.timeFormat)}")
                        appendLine("ISO 8601: ${result.isoUtc}")
                    }
                    copyToClipboard(allText, "All timestamp details copied")
                }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Copy All")
            }

            FilledTonalButton(
                onClick = { shareSummary() }
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Share")
            }
        }
    }
}
