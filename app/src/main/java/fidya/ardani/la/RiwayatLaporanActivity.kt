package fidya.ardani.la

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var listViewRiwayatLaporan: ListView
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: LaporanAdapter
    private val db = FirebaseFirestore.getInstance()

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

        loadRiwayatLaporan()
    }

    private fun loadRiwayatLaporan() {
        db.collection("laporan_pelanggaran")
            .get()
            .addOnSuccessListener { result ->
                laporanList.clear()
                for (document in result) {
                    val namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui"
                    val kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                    val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"

                    val laporan = Laporan(namaSiswa, kategoriPelanggaran, tanggalPelanggaran)

                    // Ambil jumlah poin dari koleksi data_poin (kolom = jumlah)
                    db.collection("data_poin")
                        .whereEqualTo("nama", kategoriPelanggaran)
                        .get()
                        .addOnSuccessListener { poinDocs ->
                            if (!poinDocs.isEmpty) {
                                val jumlahPoin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                laporan.jumlahPoin = jumlahPoin
                            }
                            laporanList.add(laporan)
                            laporanAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener {
                            laporanList.add(laporan)
                            laporanAdapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengambil data laporan: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    data class Laporan(
        val namaSiswa: String,
        val kategoriPelanggaran: String,
        val tanggalPelanggaran: String,
        var jumlahPoin: Int = 0
    )
}
