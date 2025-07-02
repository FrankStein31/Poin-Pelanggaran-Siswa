package fidya.ardani.la

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class TambahDataSiswaActivity : AppCompatActivity() {

    private lateinit var edtNama: TextInputEditText
    private lateinit var edtNis: TextInputEditText
    private lateinit var spinnerJurusan: Spinner
    private lateinit var spinnerKelas: Spinner
    private lateinit var edtAlamat: TextInputEditText
    private lateinit var edtNoHp: TextInputEditText
    private lateinit var edtNoHpOrtu: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var imgProfil: CircleImageView
    private lateinit var btnPilihFoto: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val jurusanList = mutableListOf<String>()
    private val kelasList = mutableListOf<String>()
    private var imageUri: Uri? = null

    // Launcher untuk memilih gambar dari galeri
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Tampilkan gambar yang dipilih menggunakan Glide
            Glide.with(this)
                .load(it)
                .centerCrop()
                .into(imgProfil)
        }
    }

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
        bindViews()

        // Load data Spinner
        loadDataJurusan()
        loadDataKelas()

        // Listener untuk tombol pilih foto
        btnPilihFoto.setOnClickListener {
            selectImageLauncher.launch("image/*") // Buka galeri untuk memilih gambar
        }

        // Listener untuk tombol simpan
        btnSimpan.setOnClickListener {
            simpanDataSiswa()
        }
    }

    private fun bindViews() {
        edtNama = findViewById(R.id.edtNamaLengkap)
        edtNis = findViewById(R.id.edtNis)
        spinnerJurusan = findViewById(R.id.spinnerJurusan)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        edtAlamat = findViewById(R.id.edtAlamat)
        edtNoHp = findViewById(R.id.edtNoHp)
        edtNoHpOrtu = findViewById(R.id.edtNoHpOrtu)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        imgProfil = findViewById(R.id.imgProfil)
        btnPilihFoto = findViewById(R.id.btnPilihFoto)
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
        val noHp = edtNoHp.text.toString().trim()
        val noHpOrtu = edtNoHpOrtu.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (nama.isEmpty() || nis.isEmpty() || jurusan.isEmpty() || kelas.isEmpty()
            || alamat.isEmpty() || alamat.isEmpty() || noHp.isEmpty() || noHpOrtu.isEmpty() || email.isEmpty() || password.isEmpty()
        ) {
            Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Upload foto profil jika ada
        if (imageUri != null) {
            val storageRef = storage.reference.child("foto_profil/${System.currentTimeMillis()}_${auth.uid}.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    // Jika upload berhasil, dapatkan URL downloadnya
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val fotoUrl = uri.toString()
                        // Buat akun dan simpan data siswa dengan URL foto
                        createUserAndSaveData(nama, nis, jurusan, kelas, alamat, noHp, noHpOrtu, email, password, fotoUrl)
                    }
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Langsung tanpa URL foto
            createUserAndSaveData(nama, nis, jurusan, kelas, alamat, noHp, noHpOrtu, email, password, "")
        }
    }

    private fun createUserAndSaveData(
        nama: String, nis: String, jurusan: String, kelas: String, alamat: String,
        noHp: String, noHpOrtu: String, email: String, password: String, fotoUrl: String
    ) {
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
                    "noHp" to noHp,
                    "noHpOrtu" to noHpOrtu,
                    "email" to email,
                    "password" to password,
                    "fotoProfilUrl" to fotoUrl
                )

                firestore.collection("siswa").document(uid).set(siswa)
                    .addOnSuccessListener {
                        setLoading(false)
                        Toast.makeText(this, "Data siswa berhasil disimpan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Gagal menyimpan ke Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Gagal membuat akun: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        // Fungsi helper untuk menampilkan/menyembunyikan loading state
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSimpan.isEnabled = !isLoading
    }
}
