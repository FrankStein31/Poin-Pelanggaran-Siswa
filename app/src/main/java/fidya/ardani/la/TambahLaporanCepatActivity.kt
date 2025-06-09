package fidya.ardani.la

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TambahLaporanCepatActivity : AppCompatActivity() {

    private lateinit var etNis: EditText
    private lateinit var etNamaSiswa: EditText
    private lateinit var spinnerPoin: Spinner
    private lateinit var tvJurusan: TextView
    private lateinit var tvKelas: TextView
    private lateinit var etTanggal: EditText
    private lateinit var btnSimpan: Button
    private lateinit var tvJumlahPoin: TextView
    private lateinit var tvGuruPiket: TextView
    private lateinit var topAppBar: MaterialToolbar

    private val db = FirebaseFirestore.getInstance()
    private val poinList = mutableListOf<String>()
    private val poinMap = mutableMapOf<String, Int>()
    private val siswaRef = db.collection("siswa")

    private var namaGuruPiketHariIni: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_laporan_cepat)

        // Inisialisasi views
        topAppBar = findViewById(R.id.topAppBar)
        etNis = findViewById(R.id.et_nis)
        etNamaSiswa = findViewById(R.id.et_nama_siswa)
        spinnerPoin = findViewById(R.id.spinner_poin)
        tvJurusan = findViewById(R.id.tv_jurusan)
        tvKelas = findViewById(R.id.tv_kelas)
        etTanggal = findViewById(R.id.et_tanggal)
        btnSimpan = findViewById(R.id.btn_simpan)
        tvJumlahPoin = findViewById(R.id.tv_jumlah_poin)
        tvGuruPiket = findViewById(R.id.tv_guru_piket)

        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadPoinPelgaraan()

        // Ambil nama guru dari Intent, simpan ke variabel, tampilkan dulu sementara
        val namaGuruDariIntent = intent.getStringExtra("nama_guru") ?: ""
        if (namaGuruDariIntent.isNotEmpty()) {
            namaGuruPiketHariIni = namaGuruDariIntent
            tvGuruPiket.text = "Guru Piket (Intent): $namaGuruPiketHariIni"
        } else {
            tvGuruPiket.text = "Memuat data guru piket..."
        }

        loadGuruPiketHariIni()

        // Listener input NIS: baru fetch jika nis >= 3 digit (ubah sesuai kebutuhan)
        etNis.addTextChangedListener { text ->
            val nis = text.toString().trim()
            if (nis.length >= 3) {
                fetchSiswaDataByNis(nis)
            } else {
                clearSiswaInfo()
            }
        }

        // Listener nama siswa (opsional)
        etNamaSiswa.addTextChangedListener { text ->
            val nama = text.toString().trim()
            if (nama.isNotEmpty()) {
                fetchSiswaDataByNama(nama)
            } else if (etNis.text.toString().isEmpty()) {
                clearSiswaInfo()
            }
        }

        btnSimpan.setOnClickListener {
            saveLaporan()
        }

        etTanggal.setOnClickListener {
            showDatePickerDialog()
        }

        spinnerPoin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedPoin = spinnerPoin.selectedItem.toString()
                tvJumlahPoin.text = "Jumlah Poin: ${poinMap[selectedPoin] ?: 0}"
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadPoinPelgaraan() {
        db.collection("data_poin").get()
            .addOnSuccessListener { result ->
                poinList.clear()
                poinMap.clear()
                for (doc in result) {
                    val poin = doc.getString("nama") ?: "Tidak Diketahui"
                    val jumlahPoin = doc.getLong("jumlah")?.toInt() ?: 0
                    poinList.add(poin)
                    poinMap[poin] = jumlahPoin
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, poinList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPoin.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data poin pelanggaran", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGuruPiketHariIni() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val todayString = String.format("%04d-%02d-%02d", year, month, day)

        db.collection("jadwal_guru_piket")
            .whereEqualTo("tanggal", todayString)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val guruPiketNames = result.documents.mapNotNull { it.getString("nama_guru") }
                    if (guruPiketNames.isNotEmpty()) {
                        namaGuruPiketHariIni = guruPiketNames.joinToString(", ")
                        tvGuruPiket.text = namaGuruPiketHariIni
                    } else {
                        if (namaGuruPiketHariIni.isNotEmpty()) {
                            tvGuruPiket.text = namaGuruPiketHariIni
                        } else {
                            tvGuruPiket.text = "Data guru piket kosong"
                        }
                    }
                } else {
                    if (namaGuruPiketHariIni.isNotEmpty()) {
                        tvGuruPiket.text = namaGuruPiketHariIni
                    } else {
                        tvGuruPiket.text = "-"
                    }
                }
            }
            .addOnFailureListener {
                if (namaGuruPiketHariIni.isNotEmpty()) {
                    tvGuruPiket.text = namaGuruPiketHariIni
                } else {
                    tvGuruPiket.text = "Gagal memuat jadwal guru piket"
                }
            }
    }

    private fun fetchSiswaDataByNis(nis: String) {
        siswaRef.whereEqualTo("nis", nis).get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    clearSiswaInfo()
                    Toast.makeText(this, "Data siswa tidak ditemukan berdasarkan NIS", Toast.LENGTH_SHORT).show()
                } else {
                    val doc = result.documents[0]
                    etNamaSiswa.setText(doc.getString("nama") ?: "")
                    tvJurusan.text = doc.getString("jurusan") ?: "Tidak Diketahui"
                    tvKelas.text = doc.getString("kelas") ?: "Tidak Diketahui"
                }
            }
            .addOnFailureListener {
                clearSiswaInfo()
                Toast.makeText(this, "Gagal mengambil data siswa", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchSiswaDataByNama(nama: String) {
        siswaRef.whereEqualTo("nama", nama).get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    clearSiswaInfo()
                    Toast.makeText(this, "Data siswa tidak ditemukan berdasarkan nama", Toast.LENGTH_SHORT).show()
                } else {
                    val doc = result.documents[0]
                    etNis.setText(doc.getString("nis") ?: "")
                    tvJurusan.text = doc.getString("jurusan") ?: "Tidak Diketahui"
                    tvKelas.text = doc.getString("kelas") ?: "Tidak Diketahui"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data siswa", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearSiswaInfo() {
        etNamaSiswa.setText("")
        tvJurusan.text = "Jurusan"
        tvKelas.text = "Kelas"
    }

    private fun saveLaporan() {
        val nis = etNis.text.toString()
        val namaSiswa = etNamaSiswa.text.toString()
        val poinPelgaraan = spinnerPoin.selectedItem.toString()
        val tanggalPelanggaran = etTanggal.text.toString()
        val guruPiket = namaGuruPiketHariIni

        if (nis.isEmpty() || namaSiswa.isEmpty() || tanggalPelanggaran.isEmpty()) {
            Toast.makeText(this, "Semua data harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val laporan = hashMapOf(
            "nis" to nis,
            "nama_siswa" to namaSiswa,
            "poin_pelanggaran" to poinPelgaraan,
            "jurusan" to tvJurusan.text.toString(),
            "kelas" to tvKelas.text.toString(),
            "tanggal_pelanggaran" to tanggalPelanggaran,
            "guru_piket" to guruPiket
        )

        db.collection("laporan_pelanggaran")
            .add(laporan)
            .addOnSuccessListener {
                Toast.makeText(this, "Laporan berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                etTanggal.setText("$dayOfMonth-${month + 1}-$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
}
