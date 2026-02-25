package dev.jmcerezo.centinela.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import java.io.File

/**
 * Motor de generación de informes legales en formato PDF.
 */
object PdfReportGenerator {

    /**
     * Genera un informe detallado de la evidencia y abre el selector de compartir.
     */
    fun generarYCompartir(contexto: Context, grabacion: GrabacionDato) {
        val storageDir = contexto.getExternalFilesDir(null)
        val pdfFile = File(storageDir, "Informe_${grabacion.nombre.replace(" ", "_")}.pdf")

        try {
            val writer = PdfWriter(pdfFile)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // --- TÍTULO PRINCIPAL ---
            document.add(Paragraph("INFORME DE EVIDENCIA DIGITAL")
                .setBold()
                .setFontSize(20f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY))

            document.add(Paragraph("Sistema Centinela - Captura Forense")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setItalic())

            document.add(Paragraph("\n")) // Espaciador

            // --- SECCIÓN: DETALLES DE LA GRABACIÓN ---
            document.add(Paragraph().add(Text("DETALLES DE LA EVIDENCIA").setBold().setUnderline()))
            document.add(Paragraph("Nombre del archivo: ${grabacion.nombre}.m4a"))
            document.add(Paragraph("Fecha y hora de captura: ${grabacion.fecha}"))
            
            document.add(Paragraph("\n"))

            // --- SECCIÓN: INTEGRIDAD (HASH) ---
            document.add(Paragraph().add(Text("CERTIFICADO DE INTEGRIDAD").setBold().setUnderline()))
            document.add(Paragraph("El siguiente código identifica de forma única este archivo. Cualquier modificación del audio invalidará esta firma digital."))
            document.add(Paragraph("HASH SHA-256:").setBold().setFontSize(9f))
            document.add(Paragraph(grabacion.hash.uppercase())
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLUE)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY))

            document.add(Paragraph("\n"))

            // --- SECCIÓN: GEOLOCALIZACIÓN ---
            document.add(Paragraph().add(Text("UBICACIÓN Y CONTEXTO").setBold().setUnderline()))
            document.add(Paragraph("Lugar de la captura:"))
            document.add(Paragraph(grabacion.ubicacion)
                .setItalic()
                .setFontSize(11f))

            document.add(Paragraph("\n\n\n"))

            // --- PIE DE PÁGINA ---
            document.add(Paragraph("Este documento ha sido generado automáticamente por la aplicación Centinela.")
                .setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY))

            document.close()

            // Compartir el archivo generado
            compartirPdf(contexto, pdfFile)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun compartirPdf(contexto: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(Intent.createChooser(intent, "Compartir Informe Legal..."))
    }
}
