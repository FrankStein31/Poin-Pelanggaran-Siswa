package fidya.ardani.la

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var listViewRiwayatLaporan: ListView
    private lateinit var rvRingkasanPoin: RecyclerView
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var ringkasanList: MutableList<RingkasanPoin>
    private lateinit var laporanAdapter: BaseAdapter
    private lateinit var ringkasanAdapter: RingkasanPoinAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var completedQueries = 0
    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi list
        laporanList = mutableListOf()
        ringkasanList = mutableListOf()

        // Cek email dari intent (untuk siswa) atau langsung load semua data (untuk guru)
        val userEmail = intent.getStringExtra("USER_EMAIL")

        // Pilih layout berdasarkan role
        if (userEmail != null) {
            // Layout untuk siswa
            setContentView(R.layout.activity_riwayat_laporan)
            listViewRiwayatLaporan = findViewById(R.id.list_view_riwayat_laporan)

            // Setup toolbar jika ada
            findViewById<MaterialToolbar>(R.id.topAppBar)?.let { toolbar ->
                toolbar.setNavigationOnClickListener { finish() }
            }

            // Setup adapter khusus untuk siswa
            laporanAdapter = RiwayatSiswaAdapter(this, laporanList)
            listViewRiwayatLaporan.adapter = laporanAdapter

            loadRiwayatLaporanSiswa(userEmail)
        } else {
            // Layout untuk guru BK dengan tombol cetak SP dan export Excel
            setContentView(R.layout.activity_riwayat_laporan_siswa)
            listViewRiwayatLaporan = findViewById(R.id.listViewRiwayat)
            rvRingkasanPoin = findViewById(R.id.rv_ringkasan_poin)

            // Setup adapters
            laporanAdapter = LaporanAdapter(this, laporanList)
            listViewRiwayatLaporan.adapter = laporanAdapter

            setupRecyclerView()
            loadRingkasanPoinSiswa()

            loadRiwayatLaporan()
        }
    }


    // PERBAIKAN: Fungsi baru untuk export data yang ditampilkan di ListView
    private fun exportDisplayedDataToExcel() {
        if (laporanList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk di-export", Toast.LENGTH_SHORT).show()
            return
        }

        val allLaporanExcel = mutableListOf<LaporanExcel>()
        var processedCount = 0
        val totalCount = laporanList.size

        // Proses setiap laporan yang ditampilkan di ListView
        for (laporan in laporanList) {
            // Cari NIS berdasarkan nama siswa
            db.collection("siswa")
                .whereEqualTo("nama", laporan.namaSiswa)
                .get()
                .addOnSuccessListener { siswaDoc ->
                    val nis = if (!siswaDoc.isEmpty) siswaDoc.documents[0].getString("nis") ?: "" else ""

                    // Ambil data poin dan keterangan
                    db.collection("data_poin")
                        .whereEqualTo("nama", laporan.kategoriPelanggaran)
                        .get()
                        .addOnSuccessListener { poinDocs ->
                            var keterangan = ""
                            if (!poinDocs.isEmpty) {
                                keterangan = poinDocs.documents[0].getString("keterangan") ?: ""
                            }

                            // Cek apakah siswa sudah pernah diberi surat peringatan
                            checkSuratPeringatan(nis) { sudahDiberiSP ->
                                val laporanExcel = LaporanExcel(
                                    namaSiswa = laporan.namaSiswa,
                                    nis = nis,
                                    guruPelapor = laporan.guruPiket,
                                    kategoriPelanggaran = laporan.kategoriPelanggaran,
                                    keterangan = keterangan,
                                    jumlahPoin = laporan.jumlahPoin,
                                    tanggalPelanggaran = laporan.tanggalPelanggaran,
                                    sudahDiberiSP = sudahDiberiSP,
                                    fotoBukti = laporan.fotoBukti ?: ""
                                )

                                allLaporanExcel.add(laporanExcel)
                                processedCount++

                                if (processedCount == totalCount) {
                                    // Kelompokkan data per minggu
                                    val weeklyData = groupByWeek(allLaporanExcel)
                                    createExcelFile(weeklyData)
                                }
                            }
                        }
                        .addOnFailureListener {
                            // Jika gagal ambil data poin, tetap lanjutkan dengan data default
                            checkSuratPeringatan(nis) { sudahDiberiSP ->
                                val laporanExcel = LaporanExcel(
                                    namaSiswa = laporan.namaSiswa,
                                    nis = nis,
                                    guruPelapor = laporan.guruPiket,
                                    kategoriPelanggaran = laporan.kategoriPelanggaran,
                                    keterangan = "",
                                    jumlahPoin = laporan.jumlahPoin,
                                    tanggalPelanggaran = laporan.tanggalPelanggaran,
                                    sudahDiberiSP = sudahDiberiSP,
                                    fotoBukti = laporan.fotoBukti ?: ""
                                )

                                allLaporanExcel.add(laporanExcel)
                                processedCount++

                                if (processedCount == totalCount) {
                                    val weeklyData = groupByWeek(allLaporanExcel)
                                    createExcelFile(weeklyData)
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    // Jika gagal ambil NIS, tetap lanjutkan dengan NIS kosong
                    checkSuratPeringatan("") { sudahDiberiSP ->
                        val laporanExcel = LaporanExcel(
                            namaSiswa = laporan.namaSiswa,
                            nis = "",
                            guruPelapor = laporan.guruPiket,
                            kategoriPelanggaran = laporan.kategoriPelanggaran,
                            keterangan = "",
                            jumlahPoin = laporan.jumlahPoin,
                            tanggalPelanggaran = laporan.tanggalPelanggaran,
                            sudahDiberiSP = sudahDiberiSP,
                            fotoBukti = laporan.fotoBukti ?: ""
                        )

                        allLaporanExcel.add(laporanExcel)
                        processedCount++

                        if (processedCount == totalCount) {
                            val weeklyData = groupByWeek(allLaporanExcel)
                            createExcelFile(weeklyData)
                        }
                    }
                }
        }
    }

    // Fungsi exportToExcel() yang lama tetap ada untuk referensi (bisa dihapus jika tidak diperlukan)
    private fun exportToExcel() {
        // Ambil semua data pelanggaran dari database
        db.collection("laporan_pelanggaran")
            .get()
            .addOnSuccessListener { laporanDocs ->
                val allLaporan = mutableListOf<LaporanExcel>()
                var processedCount = 0
                val totalCount = laporanDocs.size()

                if (laporanDocs.isEmpty) {
                    Toast.makeText(this, "Tidak ada data pelanggaran untuk di-export", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Proses setiap laporan
                for (laporanDoc in laporanDocs) {
                    val namaSiswa = laporanDoc.getString("nama_siswa") ?: ""
                    val nis = laporanDoc.getString("nis") ?: ""
                    val kategoriPelanggaran = laporanDoc.getString("poin_pelanggaran") ?: ""
                    val tanggalPelanggaran = laporanDoc.getString("tanggal_pelanggaran") ?: ""
                    val guruPiket = laporanDoc.getString("guru_piket") ?: ""
                    val fotoBukti = laporanDoc.getString("foto_bukti") ?: ""

                    // Ambil data poin dan keterangan
                    db.collection("data_poin")
                        .whereEqualTo("nama", kategoriPelanggaran)
                        .get()
                        .addOnSuccessListener { poinDocs ->
                            var jumlahPoin = 0
                            var keterangan = ""

                            if (!poinDocs.isEmpty) {
                                val poinDoc = poinDocs.documents[0]
                                jumlahPoin = poinDoc.getLong("jumlah")?.toInt() ?: 0
                                keterangan = poinDoc.getString("keterangan") ?: ""
                            }

                            // Cek apakah siswa sudah pernah diberi surat peringatan
                            checkSuratPeringatan(nis) { sudahDiberiSP ->
                                val laporanExcel = LaporanExcel(
                                    namaSiswa = namaSiswa,
                                    nis = nis,
                                    guruPelapor = guruPiket,
                                    kategoriPelanggaran = kategoriPelanggaran,
                                    keterangan = keterangan,
                                    jumlahPoin = jumlahPoin,
                                    tanggalPelanggaran = tanggalPelanggaran,
                                    sudahDiberiSP = sudahDiberiSP,
                                    fotoBukti = fotoBukti
                                )

                                allLaporan.add(laporanExcel)
                                processedCount++

                                if (processedCount == totalCount) {
                                    // Kelompokkan data per minggu
                                    val weeklyData = groupByWeek(allLaporan)
                                    createExcelFile(weeklyData)
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkSuratPeringatan(nis: String, callback: (Boolean) -> Unit) {
        // Cek di collection surat_peringatan atau berdasarkan total poin
        db.collection("laporan_pelanggaran")
            .whereEqualTo("nis", nis)
            .get()
            .addOnSuccessListener { docs ->
                var totalPoin = 0
                var processedDocs = 0

                if (docs.isEmpty) {
                    callback(false)
                    return@addOnSuccessListener
                }

                for (doc in docs) {
                    val kategori = doc.getString("poin_pelanggaran") ?: ""

                    db.collection("data_poin")
                        .whereEqualTo("nama", kategori)
                        .get()
                        .addOnSuccessListener { poinDocs ->
                            if (!poinDocs.isEmpty) {
                                val poin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                totalPoin += poin
                            }

                            processedDocs++
                            if (processedDocs == docs.size()) {
                                // Jika total poin >= 50, berarti sudah diberi SP
                                callback(totalPoin >= 50)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun groupByWeek(laporanList: List<LaporanExcel>): Map<String, List<LaporanExcel>> {
        val weeklyMap = mutableMapOf<String, MutableList<LaporanExcel>>()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        for (laporan in laporanList) {
            try {
                val date = sdf.parse(laporan.tanggalPelanggaran)
                if (date != null) {
                    calendar.time = date

                    // Set ke hari Senin minggu tersebut
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    val startWeek = sdf.format(calendar.time)

                    // Set ke hari Minggu minggu tersebut
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    val endWeek = sdf.format(calendar.time)

                    val weekKey = "Minggu $startWeek - $endWeek"

                    if (!weeklyMap.containsKey(weekKey)) {
                        weeklyMap[weekKey] = mutableListOf()
                    }
                    weeklyMap[weekKey]?.add(laporan)
                }
            } catch (e: Exception) {
                // Jika parsing tanggal gagal, masukkan ke kategori "Tanggal Tidak Valid"
                val weekKey = "Tanggal Tidak Valid"
                if (!weeklyMap.containsKey(weekKey)) {
                    weeklyMap[weekKey] = mutableListOf()
                }
                weeklyMap[weekKey]?.add(laporan)
            }
        }

        return weeklyMap
    }

    private fun createExcelFile(weeklyData: Map<String, List<LaporanExcel>>) {
        try {
            val workbook = XSSFWorkbook()

            // Buat sheet untuk setiap minggu
            for ((week, laporanList) in weeklyData) {
                val sheet = workbook.createSheet(week.replace("/", "-"))

                // Header row
                val headerRow = sheet.createRow(0)
                val headers = arrayOf(
                    "No", "Nama Siswa", "NIS", "Guru Pelapor", "Kategori Pelanggaran",
                    "Keterangan", "Jumlah Poin", "Tanggal Pelanggaran", "Status Surat Peringatan", "Foto Bukti"
                )

                val headerStyle = workbook.createCellStyle()
                val headerFont = workbook.createFont()
                headerFont.bold = true
                headerStyle.setFont(headerFont)
                headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.index)
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND)

                for (i in headers.indices) {
                    val cell = headerRow.createCell(i)
                    cell.setCellValue(headers[i])
                    cell.cellStyle = headerStyle
                    sheet.setColumnWidth(i, 4000)
                }

                // Data rows
                var rowNum = 1
                for (laporan in laporanList) {
                    val row = sheet.createRow(rowNum)

                    row.createCell(0).setCellValue(rowNum.toDouble())
                    row.createCell(1).setCellValue(laporan.namaSiswa)
                    row.createCell(2).setCellValue(laporan.nis)
                    row.createCell(3).setCellValue(laporan.guruPelapor)
                    row.createCell(4).setCellValue(laporan.kategoriPelanggaran)
                    row.createCell(5).setCellValue(laporan.keterangan)
                    row.createCell(6).setCellValue(laporan.jumlahPoin.toDouble())
                    row.createCell(7).setCellValue(laporan.tanggalPelanggaran)
                    row.createCell(8).setCellValue(if (laporan.sudahDiberiSP) "Sudah" else "Belum")
                    row.createCell(9).setCellValue(if (laporan.fotoBukti.isNotEmpty()) "Ada" else "Tidak Ada")

                    rowNum++
                }

                // Auto-size columns
                for (i in headers.indices) {
                    sheet.autoSizeColumn(i)
                }
            }

            // Simpan file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Rekap_Pelanggaran_Mingguan_$timestamp.xlsx"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            Toast.makeText(this, "File Excel berhasil disimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()

            // Buka file yang baru dibuat
            openExcelFile(file)

        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file Excel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExcelFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Jika tidak ada aplikasi yang bisa membuka file Excel
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent, "Buka file Excel dengan"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        rvRingkasanPoin.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        ringkasanAdapter = RingkasanPoinAdapter(ringkasanList)
        rvRingkasanPoin.adapter = ringkasanAdapter
    }

    private fun loadRingkasanPoinSiswa() {
        db.collection("siswa").get()
            .addOnSuccessListener { siswaDocuments ->
                for (siswaDoc in siswaDocuments) {
                    val namaSiswa = siswaDoc.getString("nama") ?: continue
                    val nis = siswaDoc.getString("nis") ?: continue

                    // Hitung total poin untuk setiap siswa
                    db.collection("laporan_pelanggaran")
                        .whereEqualTo("nis", nis)
                        .get()
                        .addOnSuccessListener { laporanDocs ->
                            var totalPoin = 0
                            var processedReports = 0
                            val totalReports = laporanDocs.size()

                            for (laporanDoc in laporanDocs) {
                                val poinPelanggaran = laporanDoc.getString("poin_pelanggaran") ?: continue

                                db.collection("data_poin")
                                    .whereEqualTo("nama", poinPelanggaran)
                                    .get()
                                    .addOnSuccessListener { poinDocs ->
                                        if (!poinDocs.isEmpty) {
                                            val poin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                            totalPoin += poin
                                        }

                                        processedReports++
                                        if (processedReports == totalReports) {
                                            ringkasanList.add(RingkasanPoin(namaSiswa, totalPoin))
                                            ringkasanList.sortByDescending { it.totalPoin }
                                            ringkasanAdapter.notifyDataSetChanged()
                                        }
                                    }
                            }
                        }
                }
            }
    }

    private fun cetakSuratPeringatan() {
        // Ambil semua siswa dengan total poin >= 50
        db.collection("siswa")
            .get()
            .addOnSuccessListener { siswaDocuments ->
                var foundSiswaWithHighPoints = false
                var processedStudents = 0
                val totalStudents = siswaDocuments.size()

                for (siswaDoc in siswaDocuments) {
                    val nis = siswaDoc.getString("nis") ?: continue
                    val namaSiswa = siswaDoc.getString("nama") ?: continue
                    val kelas = siswaDoc.getString("kelas") ?: continue

                    // Hitung total poin untuk setiap siswa
                    db.collection("laporan_pelanggaran")
                        .whereEqualTo("nis", nis)
                        .get()
                        .addOnSuccessListener { laporanDocuments ->
                            var totalPoin = 0
                            var processedReports = 0
                            val totalReports = laporanDocuments.size()

                            if (laporanDocuments.isEmpty) {
                                processedStudents++
                                if (processedStudents == totalStudents && !foundSiswaWithHighPoints) {
                                    Toast.makeText(this, "Tidak ada siswa yang memiliki total poin >= 50", Toast.LENGTH_SHORT).show()
                                }
                                return@addOnSuccessListener
                            }

                            for (laporanDoc in laporanDocuments) {
                                val poinPelanggaran = laporanDoc.getString("poin_pelanggaran") ?: continue

                                // Ambil nilai poin
                                db.collection("data_poin")
                                    .whereEqualTo("nama", poinPelanggaran)
                                    .get()
                                    .addOnSuccessListener { poinDocs ->
                                        if (!poinDocs.isEmpty) {
                                            val poin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                            totalPoin += poin
                                        }

                                        processedReports++
                                        if (processedReports == totalReports) {
                                            // Jika total poin >= 50, generate SP
                                            if (totalPoin >= 50) {
                                                foundSiswaWithHighPoints = true
                                                generateAndShareSP(namaSiswa, nis, kelas, totalPoin)
                                            }

                                            processedStudents++
                                            if (processedStudents == totalStudents && !foundSiswaWithHighPoints) {
                                                Toast.makeText(this, "Tidak ada siswa yang memiliki total poin >= 50", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateAndShareSP(namaSiswa: String, nis: String, kelas: String, totalPoin: Int) {
        try {
            val pdfGenerator = PDFGenerator(this)
            val file = pdfGenerator.generateSuratPeringatan(namaSiswa, nis, kelas, totalPoin)

            // Share PDF menggunakan FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRiwayatLaporanSiswa(email: String) {
        // Cari NIS siswa berdasarkan email
        db.collection("siswa")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val siswa = documents.documents[0]
                    val nis = siswa.getString("nis") ?: return@addOnSuccessListener

                    // Ambil riwayat pelanggaran berdasarkan NIS
                    db.collection("laporan_pelanggaran")
                        .whereEqualTo("nis", nis)
                        .get()
                        .addOnSuccessListener { result ->
                            val tempList = mutableListOf<Laporan>()
                            completedQueries = 0

                            if (result.isEmpty) {
                                laporanList.clear()
                                laporanAdapter.notifyDataSetChanged()
                                return@addOnSuccessListener
                            }

                            for (document in result) {
                                val namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui"
                                val kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                                val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                                val guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui"
                                val fotoBukti = document.getString("foto_bukti")

                                val laporan = Laporan(
                                    namaSiswa = namaSiswa,
                                    kategoriPelanggaran = kategoriPelanggaran,
                                    tanggalPelanggaran = tanggalPelanggaran,
                                    guruPiket = guruPiket,
                                    fotoBukti = fotoBukti
                                )
                                tempList.add(laporan)

                                // Ambil jumlah poin
                                db.collection("data_poin")
                                    .whereEqualTo("nama", kategoriPelanggaran)
                                    .get()
                                    .addOnSuccessListener { poinDocs ->
                                        if (!poinDocs.isEmpty) {
                                            val jumlahPoin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                            laporan.jumlahPoin = jumlahPoin
                                        }

                                        completedQueries++
                                        // Setelah semua query selesai
                                        if (completedQueries == result.size()) {
                                            tempList.sortByDescending { it.tanggalPelanggaran }
                                            laporanList.clear()
                                            laporanList.addAll(tempList)
                                            laporanAdapter.notifyDataSetChanged()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        completedQueries++
                                        Toast.makeText(this, "Gagal mengambil data poin: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal mengambil riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRiwayatLaporan() {
        // Load semua riwayat pelanggaran untuk guru
        db.collection("laporan_pelanggaran")
            .get()
            .addOnSuccessListener { result ->
                val tempList = mutableListOf<Laporan>()
                completedQueries = 0

                if (result.isEmpty) {
                    laporanList.clear()
                    laporanAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui"
                    val kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                    val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                    val guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui"
                    val fotoBukti = document.getString("foto_bukti")

                    val laporan = Laporan(
                        namaSiswa = namaSiswa,
                        kategoriPelanggaran = kategoriPelanggaran,
                        tanggalPelanggaran = tanggalPelanggaran,
                        guruPiket = guruPiket,
                        fotoBukti = fotoBukti
                    )
                    tempList.add(laporan)

                    // Ambil jumlah poin
                    db.collection("data_poin")
                        .whereEqualTo("nama", kategoriPelanggaran)
                        .get()
                        .addOnSuccessListener { poinDocs ->
                            if (!poinDocs.isEmpty) {
                                val jumlahPoin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                laporan.jumlahPoin = jumlahPoin
                            }

                            completedQueries++
                            // Setelah semua query selesai
                            if (completedQueries == result.size()) {
                                tempList.sortByDescending { it.tanggalPelanggaran }
                                laporanList.clear()
                                laporanList.addAll(tempList)
                                laporanAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener { e ->
                            completedQueries++
                            Toast.makeText(this, "Gagal mengambil data poin: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengambil data laporan: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Laporan(
        val namaSiswa: String,
        val kategoriPelanggaran: String,
        val tanggalPelanggaran: String,
        val guruPiket: String = "",
        val fotoBukti: String? = null,
        var jumlahPoin: Int = 0
    )

    data class RingkasanPoin(
        val namaSiswa: String,
        val totalPoin: Int
    )

    data class LaporanExcel(
        val namaSiswa: String,
        val nis: String,
        val guruPelapor: String,
        val kategoriPelanggaran: String,
        val keterangan: String,
        val jumlahPoin: Int,
        val tanggalPelanggaran: String,
        val sudahDiberiSP: Boolean,
        val fotoBukti: String
    )
}