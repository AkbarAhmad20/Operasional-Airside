package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvSpreadsheetExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileStampFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private val headers = listOf(
        "ID Log",
        "Registrasi Pesawat",
        "Nomor Penerbangan",
        "Parking Stand",
        "Aktivitas",
        "Tanggal Aktivitas",
        "Waktu Aktivitas",
        "Catatan Kegiatan Operasional",
        "Petugas Input",
        "NIP Petugas",
        "Status Verifikasi",
        "Diverifikasi Oleh",
        "Waktu Verifikasi",
        "Terakhir Diedit Oleh",
        "Waktu Input Sistem"
    )

    fun suggestedFileName(): String =
        "Garbarata_SHIA_${fileStampFormatter.format(Date())}.csv"

    fun buildCsvContent(logs: List<DockingLogEntity>): String {
        val sb = StringBuilder()
        // UTF-8 BOM ensures Google Sheets & Microsoft Excel preserve UTF-8 characters and split columns cleanly
        sb.append('\uFEFF')
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")
        logs.forEach { log ->
            val activityDate = Date(log.activityTimestampMillis)
            val row = listOf(
                log.id.toString(),
                log.aircraftRegistration,
                log.flightNumber.ifBlank { "-" },
                log.parkingStand,
                log.parsedActivityType.label,
                dateFormatter.format(activityDate),
                timeFormatter.format(activityDate),
                log.operationalNotes.ifBlank { "-" },
                log.operatorName,
                log.operatorEmployeeId,
                log.parsedVerificationStatus.label,
                log.verifiedByName ?: "-",
                log.verifiedAtMillis?.let { dateTimeFormatter.format(Date(it)) } ?: "-",
                log.lastEditedByName ?: "-",
                dateTimeFormatter.format(Date(log.createdAtMillis))
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun buildTsvForSheetsClipboard(logs: List<DockingLogEntity>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString("\t")).append("\n")
        logs.forEach { log ->
            val activityDate = Date(log.activityTimestampMillis)
            val row = listOf(
                log.id.toString(),
                log.aircraftRegistration,
                log.flightNumber.ifBlank { "-" },
                log.parkingStand,
                log.parsedActivityType.label,
                dateFormatter.format(activityDate),
                timeFormatter.format(activityDate),
                log.operationalNotes.replace("\n", " ").replace("\t", " ").ifBlank { "-" },
                log.operatorName,
                log.operatorEmployeeId,
                log.parsedVerificationStatus.label,
                log.verifiedByName ?: "-",
                log.verifiedAtMillis?.let { dateTimeFormatter.format(Date(it)) } ?: "-",
                log.lastEditedByName ?: "-",
                dateTimeFormatter.format(Date(log.createdAtMillis))
            )
            sb.append(row.joinToString("\t")).append("\n")
        }
        return sb.toString()
    }

    fun createShareableCsvFile(context: Context, logs: List<DockingLogEntity>): Pair<File, Uri> {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, suggestedFileName())
        file.writeText(buildCsvContent(logs), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    fun writeCsvToStream(outputStream: OutputStream, logs: List<DockingLogEntity>) {
        outputStream.use { stream ->
            stream.write(buildCsvContent(logs).toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun createShareToSheetsIntent(context: Context, uri: Uri, logCount: Int): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Log Doking/Undoking Garbarata SHIA ($logCount data)"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Laporan operasional doking & undoking garbarata Bandara Internasional Sultan Hasanuddin (InJourney Airports) — $logCount catatan siap dibuka di Google Sheets."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Buka atau Bagikan CSV ke Google Sheets")
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
