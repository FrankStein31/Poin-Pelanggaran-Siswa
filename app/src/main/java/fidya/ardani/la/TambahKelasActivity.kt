package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class TambahKelasActivity : AppCompatActivity() {

    private lateinit var etKelas: EditText
    private lateinit var btnSimpan: Button
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_kelas)

        // Inisialisasi View
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        etKelas = findViewById(R.id.etKelas)
        btnSimpan = findViewById(R.id.btnSimpan)
        firestore = FirebaseFirestore.getInstance()

        // Tombol kembali di toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Tombol simpan kelas
        btnSimpan.setOnClickListener {
            val namaKelas = etKelas.text.toString().trim()

            if (namaKelas.isEmpty()) {
                etKelas.error = "Nama kelas tidak boleh kosong"
                return@setOnClickListener
            }

            val data = hashMapOf("kelas" to namaKelas)

            firestore.collection("kelas")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Kelas berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    // Kembali ke DataKelasActivity
                    val intent = Intent(this, DataKelasActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menambahkan kelas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
