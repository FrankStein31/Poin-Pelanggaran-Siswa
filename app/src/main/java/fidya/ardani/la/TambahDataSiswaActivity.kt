package fidya.ardani.la

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TambahDataSiswaActivity : AppCompatActivity() {

    private lateinit var edtNama: TextInputEditText
    private lateinit var edtNis: TextInputEditText
    private lateinit var spinnerJurusan: Spinner
    private lateinit var spinnerKelas: Spinner
    private lateinit var edtAlamat: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnSimpan: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val jurusanList = mutableListOf<String>()
    private val kelasList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_siswa)

        // Toolbar setup
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Data Siswa"
        toolbar.setNavigationOnClickListener { finish() }

        // Bind Views
        edtNama = findViewById(R.id.edtNamaLengkap)
        edtNis = findViewById(R.id.edtNis)
        spinnerJurusan = findViewById(R.id.spinnerJurusan)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        edtAlamat = findViewById(R.id.edtAlamat)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnSimpan = findViewById(R.id.btnSimpan)

        // Load data Spinner
        loadDataJurusan()
        loadDataKelas()

        btnSimpan.setOnClickListener {
            simpanDataSiswa()
        }
    }

    private fun loadDataJurusan() {
        firestore.collection("jurusan").get()
            .addOnSuccessListener { result ->
                jurusanList.clear()
                for (doc in result) {
                    doc.getString("nama")?.let { jurusanList.add(it) }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jurusanList)
                spinnerJurusan.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data jurusan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDataKelas() {
        firestore.collection("kelas").get()
            .addOnSuccessListener { result ->
                kelasList.clear()
                for (doc in result) {
                    doc.getString("kelas")?.let { kelasList.add(it) }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kelasList)
                spinnerKelas.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data kelas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanDataSiswa() {
        val nama = edtNama.text.toString().trim()
        val nis = edtNis.text.toString().trim()
        val jurusan = spinnerJurusan.selectedItem?.toString() ?: ""
        val kelas = spinnerKelas.selectedItem?.toString() ?: ""
        val alamat = edtAlamat.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (nama.isEmpty() || nis.isEmpty() || jurusan.isEmpty() || kelas.isEmpty()
            || alamat.isEmpty() || email.isEmpty() || password.isEmpty()
        ) {
            Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val siswa = hashMapOf(
                    "uid" to uid,
                    "nama" to nama,
                    "nis" to nis,
                    "jurusan" to jurusan,
                    "kelas" to kelas,
                    "alamat" to alamat,
                    "email" to email,
                    "password" to password,
                )

                firestore.collection("siswa").document(uid).set(siswa)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data siswa berhasil disimpan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menyimpan ke Firestore", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal membuat akun: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
