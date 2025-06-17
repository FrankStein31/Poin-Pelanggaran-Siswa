package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

class LaporanCepatActivity : AppCompatActivity() {

    private lateinit var fabTambahLaporan: FloatingActionButton
    private lateinit var listViewLaporan: ListView
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: LaporanAdapter
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

        // Custom adapter untuk ListView dengan layout item_laporan
        laporanAdapter = LaporanAdapter(this, laporanList)
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
                    val nis = document.getString("nis") ?: "Tidak Diketahui"
                    val jurusan = document.getString("jurusan") ?: "Tidak Diketahui"
                    val kelas = document.getString("kelas") ?: "Tidak Diketahui"
                    val poinPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                    val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                    val guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui"
                    val fotoBukti = document.getString("foto_bukti") // URL gambar jika ada

                    val laporan = Laporan(
                        namaSiswa = namaSiswa,
                        nis = nis,
                        jurusan = jurusan,
                        kelas = kelas,
                        poinPelanggaran = poinPelanggaran,
                        tanggalPelanggaran = tanggalPelanggaran,
                        guruPiket = guruPiket,
                        fotoBukti = fotoBukti
                    )
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

    // Custom Adapter untuk ListView
    class LaporanAdapter(
        private val context: AppCompatActivity,
        private val laporanList: List<Laporan>
    ) : BaseAdapter() {

        override fun getCount(): Int = laporanList.size

        override fun getItem(position: Int): Any = laporanList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_laporan, parent, false)

            val laporan = laporanList[position]

            // Bind data ke views
            val tvNamaSiswa = view.findViewById<TextView>(R.id.tv_nama_siswa)
            val tvNis = view.findViewById<TextView>(R.id.tv_nis)
            val tvJurusanKelas = view.findViewById<TextView>(R.id.tv_jurusan_kelas)
            val tvPoinPelanggaran = view.findViewById<TextView>(R.id.tv_poin_pelanggaran)
            val tvTanggalPelanggaran = view.findViewById<TextView>(R.id.tv_tanggal_pelanggaran)
            val tvGuruPiket = view.findViewById<TextView>(R.id.tv_guru_piket)
            val imgBuktiPelanggaran = view.findViewById<ImageView>(R.id.img_bukti_pelanggaran)

            // Set data
            tvNamaSiswa.text = laporan.namaSiswa
            tvNis.text = "NIS: ${laporan.nis}"
            tvJurusanKelas.text = "Jurusan: ${laporan.jurusan} | Kelas: ${laporan.kelas}"
            tvPoinPelanggaran.text = "Poin Pelanggaran: ${laporan.poinPelanggaran}"
            tvTanggalPelanggaran.text = "Tanggal: ${laporan.tanggalPelanggaran}"
            tvGuruPiket.text = "Guru Piket: ${laporan.guruPiket}"

            // Debug: Log untuk memeriksa apakah ImageView ditemukan
            if (imgBuktiPelanggaran == null) {
                android.util.Log.e("LaporanAdapter", "ImageView tidak ditemukan!")
            } else {
                android.util.Log.d("LaporanAdapter", "ImageView ditemukan, setting gambar...")
            }

            // Handle foto bukti
            imgBuktiPelanggaran?.let { imageView ->
                imageView.visibility = View.VISIBLE

                if (!laporan.fotoBukti.isNullOrEmpty()) {
                    // Jika ada URL foto, gunakan Glide atau Picasso
                    Glide.with(context)
                        .load(laporan.fotoBukti)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(imageView)
                } else {
                    // Placeholder untuk tidak ada foto
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    imageView.setBackgroundColor(context.getColor(android.R.color.background_light))
                    imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    imageView.alpha = 0.6f
                    imageView.setPadding(12, 12, 12, 12)
                }
            }

            return view
        }
    }

    // Updated Data class untuk Laporan
    data class Laporan(
        val namaSiswa: String,
        val nis: String,
        val jurusan: String,
        val kelas: String,
        val poinPelanggaran: String,
        val tanggalPelanggaran: String,
        val guruPiket: String,
        val fotoBukti: String? = null
    )
}