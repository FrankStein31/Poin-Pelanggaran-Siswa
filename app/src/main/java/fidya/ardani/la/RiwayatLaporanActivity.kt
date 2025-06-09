package fidya.ardani.la

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var listViewRiwayatLaporan: ListView
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: LaporanAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var completedQueries = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_laporan)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        listViewRiwayatLaporan = findViewById(R.id.list_view_riwayat_laporan)
        laporanList = mutableListOf()

        laporanAdapter = LaporanAdapter(this, laporanList)
        listViewRiwayatLaporan.adapter = laporanAdapter

        // Cek email dari intent (untuk siswa) atau langsung load semua data (untuk guru)
        val userEmail = intent.getStringExtra("USER_EMAIL")
        if (userEmail != null) {
            loadRiwayatLaporanSiswa(userEmail)
        } else {
            loadRiwayatLaporan()
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
