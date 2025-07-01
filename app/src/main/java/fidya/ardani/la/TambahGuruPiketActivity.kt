package fidya.ardani.la

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class TambahGuruPiketActivity : AppCompatActivity() {

    private lateinit var edtNama: TextInputEditText
    private lateinit var edtNip: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtAlamat: TextInputEditText
    private lateinit var edtJadwalPiket: TextInputEditText
    private lateinit var edtNoHp: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var btnSimpan: Button
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var imgProfil: CircleImageView
    private lateinit var btnPilihFoto: Button

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var guruId: String? = null
    private var currentGuru: Guru? = null
    private var newImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            newImageUri = it
            Glide.with(this).load(it).into(imgProfil)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_guru_piket)

        auth = FirebaseAuth.getInstance()
        bindViews()

        topAppBar.setNavigationOnClickListener { onBackPressed() }
        btnPilihFoto.setOnClickListener { selectImageLauncher.launch("image/*") }

        guruId = intent.getStringExtra("GURU_ID")
        if (guruId != null) {
            topAppBar.title = "Edit Guru"
            btnSimpan.text = "Update"
            layoutPassword.visibility = View.GONE // Sembunyikan password saat edit
            loadGuruData()
        } else {
            topAppBar.title = "Tambah Guru"
            btnSimpan.text = "Simpan"
        }

        btnSimpan.setOnClickListener {
            if (guruId == null) simpanGuruBaru() else updateGuru()
        }
    }

    private fun bindViews() {
        edtNama = findViewById(R.id.edtNama)
        edtNip = findViewById(R.id.edtNip)
        edtEmail = findViewById(R.id.edtEmail)
        edtAlamat = findViewById(R.id.edtAlamat)
        edtJadwalPiket = findViewById(R.id.edtJadwalPiket)
        edtNoHp = findViewById(R.id.edtNoHp)
        edtPassword = findViewById(R.id.edtPassword)
        layoutPassword = findViewById(R.id.layoutPassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        topAppBar = findViewById(R.id.topAppBar)
        imgProfil = findViewById(R.id.imgProfilGuru)
        btnPilihFoto = findViewById(R.id.btnPilihFoto)
    }

    private fun loadGuruData() {
        guruId?.let {
            firestore.collection("guru_piket").document(it).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentGuru = doc.toObject<Guru>()
                        populateForm(currentGuru)
                    }
                }
        }
    }

    private fun populateForm(guru: Guru?) {
        guru?.let {
            edtNama.setText(it.nama)
            edtNip.setText(it.nip)
            edtEmail.setText(it.email)
            edtAlamat.setText(it.alamat)
            edtJadwalPiket.setText(it.jadwalPiket)
            edtNoHp.setText(it.noHp)
            edtEmail.isEnabled = false // Email tidak bisa diubah

            if (it.fotoProfilUrl.isNotEmpty()) {
                Glide.with(this).load(it.fotoProfilUrl).into(imgProfil)
            }
        }
    }

    private fun simpanGuruBaru() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        // Validasi input...
        if (email.isEmpty() || password.isEmpty() || edtNama.text.toString().isEmpty()) {
            Toast.makeText(this, "Nama, email, dan password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Buat akun Auth
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                // 2. Jika ada gambar, upload dulu
                if (newImageUri != null) {
                    uploadFotoDanSimpanData(newImageUri!!, authTask.result.user?.uid ?: "")
                } else {
                    // 3. Jika tidak ada gambar, langsung simpan data
                    simpanDataGuruKeFirestore("", authTask.result.user?.uid ?: "")
                }
            } else {
                Toast.makeText(this, "Gagal membuat akun: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadFotoDanSimpanData(uri: Uri, authUid: String) {
        val storageRef = storage.reference.child("foto_guru/${System.currentTimeMillis()}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    simpanDataGuruKeFirestore(downloadUrl.toString(), authUid)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal upload foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanDataGuruKeFirestore(fotoUrl: String, authUid: String) {
        val guruData = hashMapOf(
            "nama" to edtNama.text.toString().trim(),
            "nip" to edtNip.text.toString().trim(),
            "email" to edtEmail.text.toString().trim(),
            "alamat" to edtAlamat.text.toString().trim(),
            "noHp" to edtNoHp.text.toString().trim(),
            "jadwalPiket" to edtJadwalPiket.text.toString().trim(),
            "fotoProfilUrl" to fotoUrl
        )

        // Gunakan UID dari Auth sebagai ID dokumen agar sinkron
        firestore.collection("guru_piket").document(authUid).set(guruData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data guru berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateGuru() {
        if (newImageUri != null) {
            val storageRef = storage.reference.child("foto_guru/${System.currentTimeMillis()}.jpg")
            storageRef.putFile(newImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updateDataGuruDiFirestore(downloadUrl.toString())
                    }
                }
        } else {
            updateDataGuruDiFirestore(currentGuru?.fotoProfilUrl ?: "")
        }
    }

    private fun updateDataGuruDiFirestore(fotoUrl: String) {
        val guruUpdate = mapOf(
            "nama" to edtNama.text.toString().trim(),
            "nip" to edtNip.text.toString().trim(),
            "alamat" to edtAlamat.text.toString().trim(),
            "noHp" to edtNoHp.text.toString().trim(),
            "jadwalPiket" to edtJadwalPiket.text.toString().trim(),
            "fotoProfilUrl" to fotoUrl
        )

        guruId?.let {
            firestore.collection("guru_piket").document(it).update(guruUpdate)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data guru berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
