package fidya.ardani.la

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PDFGenerator(private val context: Context) {

    fun generateSuratPeringatan(
        namaSiswa: String,
        nis: String,
        kelas: String,
        totalPoin: Int,
        namaKepsek: String = "Drs. SUPRIYADI, M.Pd",
        nipKepsek: String = "19640410 198903 1 014"
    ): File {
        // Inflate template layout
        val view = LayoutInflater.from(context).inflate(R.layout.template_surat_peringatan, null)

        // Set data ke template
        view.findViewById<TextView>(R.id.tvNamaSiswa).text = namaSiswa
        view.findViewById<TextView>(R.id.tvNIS).text = nis
        view.findViewById<TextView>(R.id.tvKelas).text = kelas
        view.findViewById<TextView>(R.id.tvTotalPoin).text = totalPoin.toString()

        // Set tanggal
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        view.findViewById<TextView>(R.id.tvTanggal).text = "Nganjuk, ${dateFormat.format(Date())}"

        // Buat PDF Document
        val document = PdfDocument()
        
        // Ukuran A4 dalam pixels (300 dpi)
        val pageWidth = 2480
        val pageHeight = 3508

        // Buat halaman PDF
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)

        // Render view ke PDF
        view.measure(
            View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, pageWidth, pageHeight)
        view.draw(page.canvas)

        document.finishPage(page)

        // Simpan PDF
        val fileName = "SP_${namaSiswa.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        
        document.close()
        
        return file
    }
} 