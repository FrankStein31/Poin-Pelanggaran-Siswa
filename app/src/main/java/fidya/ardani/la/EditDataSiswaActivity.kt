package fidya.ardani.la

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class EditDataSiswaActivity : AppCompatActivity() {

    private lateinit var edtNamaSiswa: TextInputEditText
    private lateinit var edtNis: TextInputEditText
    private lateinit var edtAlamat: TextInputEditText
    private lateinit var spinnerKelas: Spinner
    private lateinit var spinnerJurusan: Spinner
    private lateinit var btnUpdate: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private var siswaId: String? = null

    private val kelasList = mutableListOf<String>()
    private val jurusanList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_siswa)

        // Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Menutup activity saat ikon back ditekan
        }

        // Ambil data dari intent
        siswaId = intent.getStringExtra("SISWA_ID")
        val nama = intent.getStringExtra("NAMA") ?: ""
        val nis = intent.getStringExtra("NIS") ?: ""
        val kelasLama = intent.getStringExtra("KELAS") ?: ""
        val alamat = intent.getStringExtra("ALAMAT") ?: ""
        val jurusanLama = intent.getStringExtra("JURUSAN") ?: ""

        // Inisialisasi View
        edtNamaSiswa = findViewById(R.id.edtNamaSiswa)
        edtNis = findViewById(R.id.edtNis)
        edtAlamat = findViewById(R.id.edtAlamat)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        spinnerJurusan = findViewById(R.id.spinnerJurusan)
        btnUpdate = findViewById(R.id.btnUpdate)

        edtNamaSiswa.setText(nama)
        edtNis.setText(nis)
        edtAlamat.setText(alamat)

        // Load spinner data
        loadKelas(kelasLama)
        loadJurusan(jurusanLama)

        // Tombol update
        btnUpdate.setOnClickListener {
            val updatedNama = edtNamaSiswa.text.toString()
            val updatedNis = edtNis.text.toString()
            val updatedKelas = spinnerKelas.selectedItem.toString()
            val updatedJurusan = spinnerJurusan.selectedItem.toString()
            val updatedAlamat = edtAlamat.text.toString()

            if (updatedNama.isNotEmpty() && updatedNis.isNotEmpty() && updatedAlamat.isNotEmpty()) {
                val siswaData = hashMapOf(
                    "nama" to updatedNama,
                    "nis" to updatedNis,
                    "kelas" to updatedKelas,
                    "jurusan" to updatedJurusan,
                    "alamat" to updatedAlamat
                )

                siswaId?.let {
                    firestore.collection("siswa").document(it)
                        .set(siswaData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Data Siswa berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal memperbarui data siswa", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadKelas(kelasLama: String) {
        firestore.collection("kelas").get()
            .addOnSuccessListener { result ->
                kelasList.clear()
                kelasList.add(kelasLama)
                for (doc in result) {
                    val namaKelas = doc.getString("kelas")
                    if (namaKelas != null && namaKelas != kelasLama) {
                        kelasList.add(namaKelas)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kelasList)
                spinnerKelas.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data kelas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadJurusan(jurusanLama: String) {
        firestore.collection("jurusan").get()
            .addOnSuccessListener { result ->
                jurusanList.clear()
                jurusanList.add(jurusanLama)
                for (doc in result) {
                    val namaJurusan = doc.getString("nama")
                    if (namaJurusan != null && namaJurusan != jurusanLama) {
                        jurusanList.add(namaJurusan)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jurusanList)
                spinnerJurusan.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data jurusan", Toast.LENGTH_SHORT).show()
            }
    }
}
