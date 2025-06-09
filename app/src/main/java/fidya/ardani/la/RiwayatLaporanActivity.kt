package fidya.ardani.la

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var listViewRiwayatLaporan: ListView
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: LaporanAdapter
    private lateinit var btnCetakSP: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var completedQueries = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
            
            loadRiwayatLaporanSiswa(userEmail)
        } else {
            // Layout untuk guru BK dengan tombol cetak SP
            setContentView(R.layout.activity_riwayat_laporan_siswa)
            listViewRiwayatLaporan = findViewById(R.id.listViewRiwayat)
            btnCetakSP = findViewById(R.id.btnCetakSP)
            btnCetakSP.setOnClickListener {
                cetakSuratPeringatan()
            }
            loadRiwayatLaporan()
        }

        laporanList = mutableListOf()
        laporanAdapter = LaporanAdapter(this, laporanList)
        listViewRiwayatLaporan.adapter = laporanAdapter
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

                                val laporan = Laporan(namaSiswa, kategoriPelanggaran, tanggalPelanggaran)
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

                    val laporan = Laporan(namaSiswa, kategoriPelanggaran, tanggalPelanggaran)
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
        var jumlahPoin: Int = 0
    )
}
