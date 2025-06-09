package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class LaporanCepatActivity : AppCompatActivity() {

    private lateinit var fabTambahLaporan: FloatingActionButton
    private lateinit var listViewLaporan: ListView
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: ArrayAdapter<Laporan>

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_cepat)

        val namaGuru = intent.getStringExtra("nama_guru") ?: ""

        // Setup toolbar dengan tombol back
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }

        fabTambahLaporan = findViewById(R.id.btn_tambah_laporan)
        listViewLaporan = findViewById(R.id.list_view_laporan)

        laporanList = mutableListOf()

        // Adapter untuk ListView
        laporanAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, laporanList)
        listViewLaporan.adapter = laporanAdapter

        // Load data laporan dari Firestore
        loadLaporan()

        // Aksi tombol FloatingActionButton "Tambah Laporan"
        fabTambahLaporan.setOnClickListener {
          val intent = Intent(this, TambahLaporanCepatActivity::class.java)
            intent.putExtra("nama_guru", namaGuru)
            startActivity(intent)
        }

        // Aksi ketika item di ListView diklik
        listViewLaporan.setOnItemClickListener { _, _, position, _ ->
            val laporan = laporanList[position]
            Toast.makeText(this, "Laporan dipilih: ${laporan.namaSiswa}", Toast.LENGTH_SHORT).show()
        }
    }

    // Mengambil data laporan dari Firestore
    private fun loadLaporan() {
        db.collection("laporan_pelanggaran")
            .get()
            .addOnSuccessListener { result ->
                laporanList.clear()
                for (document in result) {
                    val namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui"
                    val poinPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                    val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                    val laporan = Laporan(namaSiswa, poinPelanggaran, tanggalPelanggaran)
                    laporanList.add(laporan)
                }
                laporanAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengambil data laporan: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_LAPORAN_REQUEST_CODE && resultCode == RESULT_OK) {
            loadLaporan() // Reload data laporan setelah berhasil menambah laporan
        }
    }

    companion object {
        const val ADD_LAPORAN_REQUEST_CODE = 1
    }

    // Data class untuk Laporan
    data class Laporan(val namaSiswa: String, val poinPelanggaran: String, val tanggalPelanggaran: String) {
        override fun toString(): String {
            return "$namaSiswa - $poinPelanggaran - $tanggalPelanggaran"
        }
    }
}
