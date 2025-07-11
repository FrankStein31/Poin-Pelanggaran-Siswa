package fidya.ardani.la

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class EditDataSiswaActivity : AppCompatActivity() {

    // Views
    private lateinit var edtNama: TextInputEditText
    private lateinit var edtNis: TextInputEditText
    private lateinit var edtAlamat: TextInputEditText
    private lateinit var edtNoHp: TextInputEditText
    private lateinit var edtNoHpOrtu: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var spinnerKelas: Spinner
    private lateinit var spinnerJurusan: Spinner
    private lateinit var btnUpdate: MaterialButton
    private lateinit var imgProfil: CircleImageView
    private lateinit var btnUbahFoto: MaterialButton

    // Firebase
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Data
    private var siswaId: String? = null
    private var currentSiswa: Siswa? = null
    private var newImageUri: Uri? = null // Menyimpan URI gambar baru jika ada
    private val kelasList = mutableListOf<String>()
    private val jurusanList = mutableListOf<String>()

    // Activity Result Launcher untuk memilih gambar
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            newImageUri = it
            Glide.with(this).load(it).into(imgProfil)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_siswa)

        bindViews()
        setupToolbar()

        // Refactor: Ambil ID dari Intent, lalu load semua data dari Firestore
        siswaId = intent.getStringExtra("SISWA_ID")
        if (siswaId == null) {
            Toast.makeText(this, "ID Siswa tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSiswaData(siswaId!!)
        loadJurusanAndKelas()

        btnUbahFoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnUpdate.setOnClickListener {
            updateDataSiswa()
        }
    }

    private fun bindViews() {
        edtNama = findViewById(R.id.edtNamaSiswa)
        edtNis = findViewById(R.id.edtNis)
        edtAlamat = findViewById(R.id.edtAlamat)
        edtNoHp = findViewById(R.id.edtNoHp)
        edtNoHpOrtu = findViewById(R.id.edtNoHpOrtu)
        edtEmail = findViewById(R.id.edtEmail)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        spinnerJurusan = findViewById(R.id.spinnerJurusan)
        btnUpdate = findViewById(R.id.btnUpdate)
        imgProfil = findViewById(R.id.imgProfil)
        btnUbahFoto = findViewById(R.id.btnUbahFoto)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadSiswaData(id: String) {
        firestore.collection("siswa").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentSiswa = document.toObject<Siswa>()
                    currentSiswa?.let { populateUi(it) }
                } else {
                    Toast.makeText(this, "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUi(siswa: Siswa) {
        edtNama.setText(siswa.nama)
        edtNis.setText(siswa.nis)
        edtAlamat.setText(siswa.alamat)
        edtNoHp.setText(siswa.noHp)
        edtNoHpOrtu.setText(siswa.noHpOrtu)
        edtEmail.setText(siswa.email)

        if (siswa.fotoProfilUrl.isNotEmpty()) {
            Glide.with(this)
                .load(siswa.fotoProfilUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(imgProfil)
        }
        // Logika untuk set spinner akan dipanggil setelah data spinner ter-load
    }

    private fun loadJurusanAndKelas() {
        // Load Jurusan
        firestore.collection("jurusan").get().addOnSuccessListener { result ->
            jurusanList.clear()
            result.documents.forEach { doc -> doc.getString("nama")?.let { jurusanList.add(it) } }
            val jurusanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jurusanList)
            spinnerJurusan.adapter = jurusanAdapter
            // Set selection setelah data ter-load
            currentSiswa?.jurusan?.let {
                val pos = jurusanList.indexOf(it)
                if (pos >= 0) spinnerJurusan.setSelection(pos)
            }
        }
        // Load Kelas
        firestore.collection("kelas").get().addOnSuccessListener { result ->
            kelasList.clear()
            result.documents.forEach { doc -> doc.getString("kelas")?.let { kelasList.add(it) } }
            val kelasAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kelasList)
            spinnerKelas.adapter = kelasAdapter
            // Set selection setelah data ter-load
            currentSiswa?.kelas?.let {
                val pos = kelasList.indexOf(it)
                if (pos >= 0) spinnerKelas.setSelection(pos)
            }
        }
    }

    private fun updateDataSiswa() {
        val nama = edtNama.text.toString().trim()
        val nis = edtNis.text.toString().trim()
        val alamat = edtAlamat.text.toString().trim()
        val noHp = edtNoHp.text.toString().trim()
        val noHpOrtu = edtNoHpOrtu.text.toString().trim()
        val kelas = spinnerKelas.selectedItem.toString()
        val jurusan = spinnerJurusan.selectedItem.toString()

        // Validasi nomor HP
        if (noHp.length < 11 || noHp.length > 13) {
            edtNoHp.error = "Nomor HP harus 11-13 digit"
            edtNoHp.setBackgroundResource(android.R.color.holo_red_light)
            return
        } else {
            edtNoHp.setBackgroundResource(android.R.color.transparent)
        }

        if (noHpOrtu.length < 11 || noHpOrtu.length > 13) {
            edtNoHpOrtu.error = "Nomor HP Orang Tua harus 11-13 digit"
            edtNoHpOrtu.setBackgroundResource(android.R.color.holo_red_light)
            return
        } else {
            edtNoHpOrtu.setBackgroundResource(android.R.color.transparent)
        }

        if (nama.isEmpty() || nis.isEmpty() || alamat.isEmpty() || noHp.isEmpty() || noHpOrtu.isEmpty()) {
            Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Buat map untuk menampung data yang akan diupdate
        val updates = hashMapOf<String, Any>(
            "nama" to nama,
            "nis" to nis,
            "alamat" to alamat,
            "noHp" to noHp,
            "noHpOrtu" to noHpOrtu,
            "kelas" to kelas,
            "jurusan" to jurusan
        )

        // Cek jika ada gambar baru yang dipilih
        if (newImageUri != null) {
            val storageRef = storage.reference.child("foto_profil/${System.currentTimeMillis()}_${siswaId}.jpg")
            storageRef.putFile(newImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updates["fotoProfilUrl"] = uri.toString() // Tambahkan URL baru ke map
                        updateFirestore(updates) // Lakukan update ke firestore
                    }
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Jika tidak ada gambar baru, langsung update data lainnya
            updateFirestore(updates)
        }
    }

    private fun updateFirestore(updates: Map<String, Any>) {
        siswaId?.let { id ->
            // Gunakan .update() bukan .set() agar tidak menghapus field yang tidak ada di map
            firestore.collection("siswa").document(id).update(updates)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal memperbarui data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnUpdate.isEnabled = !isLoading
        // Bisa ditambahkan ProgressBar jika diinginkan
    }
}
