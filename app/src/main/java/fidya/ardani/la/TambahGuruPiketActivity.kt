package fidya.ardani.la

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TambahGuruPiketActivity : AppCompatActivity() {

    private lateinit var edtNama: EditText
    private lateinit var edtNip: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtAlamat: EditText
    private lateinit var edtJadwalPiket: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSimpan: Button
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private var guruId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_guru_piket)

        auth = FirebaseAuth.getInstance()

        // Inisialisasi view
        edtNama = findViewById(R.id.edtNama)
        edtNip = findViewById(R.id.edtNip)
        edtEmail = findViewById(R.id.edtEmail)
        edtAlamat = findViewById(R.id.edtAlamat)
        edtJadwalPiket = findViewById(R.id.edtJadwalPiket)
        edtPassword = findViewById(R.id.edtPassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        topAppBar = findViewById(R.id.topAppBar)

        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }

        guruId = intent.getStringExtra("id")
        if (guruId != null) {
            edtNama.setText(intent.getStringExtra("nama"))
            edtNip.setText(intent.getStringExtra("nip"))
            edtEmail.setText(intent.getStringExtra("email"))
            edtAlamat.setText(intent.getStringExtra("alamat"))
            edtJadwalPiket.setText(intent.getStringExtra("jadwalPiket"))
            edtPassword.setText("")
            btnSimpan.text = "Update"
        }

        btnSimpan.setOnClickListener {
            if (guruId == null) {
                simpanGuru()
            } else {
                updateGuru()
            }
        }
    }

    private fun simpanGuru() {
        val nama = edtNama.text.toString().trim()
        val nip = edtNip.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val alamat = edtAlamat.text.toString().trim()
        val jadwalPiket = edtJadwalPiket.text.toString().trim().ifEmpty { "-" }
        val password = edtPassword.text.toString()

        if (email.isEmpty() || password.isEmpty() || nama.isEmpty()) {
            Toast.makeText(this, "Nama, email, dan password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    val guruData = hashMapOf(
                        "nama" to nama,
                        "nip" to nip,
                        "email" to email,
                        "alamat" to alamat,
                        "jadwalPiket" to jadwalPiket,
                        "password" to password
                    )

                    firestore.collection("guru_piket").add(guruData)
                        .addOnSuccessListener { documentRef ->
                            val jadwalData = hashMapOf(
                                "hari" to jadwalPiket,
                                "nama" to nama,
                                "guruId" to documentRef.id
                            )

                            firestore.collection("jadwal_piket").add(jadwalData)
                                .addOnSuccessListener {
                                    firestore.collection("guru_piket").document(documentRef.id)
                                        .update("jadwalPiket", jadwalPiket)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Guru berhasil ditambahkan dan akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal menyimpan data guru", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Gagal membuat akun: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateGuru() {
        val nama = edtNama.text.toString().trim()
        val nip = edtNip.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val alamat = edtAlamat.text.toString().trim()
        val jadwalPiket = edtJadwalPiket.text.toString().trim().ifEmpty { "-" }
        val password = edtPassword.text.toString()

        if (password.isEmpty()) {
            Toast.makeText(this, "Password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "nama" to nama,
            "nip" to nip,
            "email" to email,
            "alamat" to alamat,
            "jadwalPiket" to jadwalPiket
        )

        firestore.collection("guru_piket").document(guruId!!)
            .set(data)
            .addOnSuccessListener {
                firestore.collection("jadwal_piket")
                    .whereEqualTo("guruId", guruId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (doc in querySnapshot) {
                                firestore.collection("jadwal_piket").document(doc.id)
                                    .update("hari", jadwalPiket, "nama", nama)
                            }
                        } else {
                            val newJadwal = hashMapOf(
                                "hari" to jadwalPiket,
                                "nama" to nama,
                                "guruId" to guruId!!
                            )
                            firestore.collection("jadwal_piket").add(newJadwal)
                        }
                        Toast.makeText(this, "Guru diperbarui", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui guru", Toast.LENGTH_SHORT).show()
            }
    }
}
