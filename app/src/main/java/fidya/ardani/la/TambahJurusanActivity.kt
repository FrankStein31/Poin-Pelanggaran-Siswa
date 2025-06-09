package fidya.ardani.la

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class TambahJurusanActivity : AppCompatActivity() {

    private lateinit var editTextJurusan: EditText
    private lateinit var buttonSimpan: Button
    private lateinit var toolbar: MaterialToolbar
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jurusan)

        editTextJurusan = findViewById(R.id.editTextJurusan)
        buttonSimpan = findViewById(R.id.buttonSimpan)
        toolbar = findViewById(R.id.topAppBar)
        firestore = FirebaseFirestore.getInstance()

        // Set toolbar sebagai action bar
        setSupportActionBar(toolbar)

        // Handle klik icon back di toolbar
        toolbar.setNavigationOnClickListener {
            finish() // kembali ke activity sebelumnya
        }

        buttonSimpan.setOnClickListener {
            val namaJurusan = editTextJurusan.text.toString().trim()
            if (namaJurusan.isEmpty()) {
                editTextJurusan.error = "Nama jurusan tidak boleh kosong"
                return@setOnClickListener
            }

            val jurusan = hashMapOf(
                "nama" to namaJurusan
            )

            firestore.collection("jurusan")
                .add(jurusan)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data jurusan berhasil disimpan", Toast.LENGTH_SHORT).show()
                    editTextJurusan.text.clear()

                    // Bisa langsung kembali jika ingin, contoh:
                    // finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onBackPressed() {
        // Kembali ke layar sebelumnya
        finish()
    }
}
