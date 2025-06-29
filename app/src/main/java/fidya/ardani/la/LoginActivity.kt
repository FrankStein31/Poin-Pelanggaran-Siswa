package fidya.ardani.la

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login berhasil di Firebase Authentication
                        cekRoleUser(email)
                    } else {
                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun cekRoleUser(email: String) {
        // Cek koleksi siswa dulu
        db.collection("siswa").whereEqualTo("email", email).get()
            .addOnSuccessListener { siswaDocs ->
                if (!siswaDocs.isEmpty) {
                    // Email ditemukan di koleksi siswa
                    Toast.makeText(this, "Login sebagai Siswa", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardSiswaActivity::class.java))
                    finish()
                } else {
                    // Tidak ditemukan di siswa, cek guru_bk
                    db.collection("guru_bk").whereEqualTo("email", email).get()
                        .addOnSuccessListener { guruBkDocs ->
                            if (!guruBkDocs.isEmpty) {
                                Toast.makeText(this, "Login sebagai Guru BK", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DashboardGuruBkActivity::class.java))
                                finish()
                            } else {
                                // Tidak ditemukan di guru_bk, cek guru_piket
                                db.collection("guru_piket").whereEqualTo("email", email).get()
                                .addOnSuccessListener { guruPiketDocs ->
                                    if (!guruPiketDocs.isEmpty) {
                                        val guruDoc = guruPiketDocs.documents[0]
                                        val namaGuru = guruDoc.getString("nama") ?: "Guru Piket"
                                        val guruId = guruDoc.id

                                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        val currentTime = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date()) // pakai titik bukan titik dua

                                        db.collection("jadwal_piket")
                                            .whereEqualTo("guru_id", guruId)
                                            .whereEqualTo("tanggal", currentDate)
                                            .get()
                                            .addOnSuccessListener { jadwalDocs ->
                                                val aktifSekarang = jadwalDocs.any { doc ->
                                                    val jamStr = doc.getString("jam") ?: return@any false
                                                    val jamParts = jamStr.split(" - ")
                                                    if (jamParts.size != 2) return@any false
                                                    val jamMulai = jamParts[0]
                                                    val jamSelesai = jamParts[1]
                                                    currentTime >= jamMulai && currentTime <= jamSelesai
                                                }

                                                if (aktifSekarang) {
                                                    Toast.makeText(this, "Login sebagai Guru Piket", Toast.LENGTH_SHORT).show()
                                                    val intent = Intent(this, DashboardGuruPiketActivity::class.java)
                                                    intent.putExtra("nama_guru", namaGuru)
                                                    startActivity(intent)
                                                    finish()
                                                } else {
                                                    Toast.makeText(this, "Anda tidak memiliki jadwal piket saat ini!", Toast.LENGTH_SHORT).show()
                                                    auth.signOut()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Gagal cek jadwal piket: ${e.message}", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }

                                    } else {
                                        Toast.makeText(this, "Role user tidak ditemukan di database!", Toast.LENGTH_SHORT).show()
                                        auth.signOut()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal cek role: ${e.message}", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                }

                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal cek role: ${e.message}", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal cek role: ${e.message}", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }
}
