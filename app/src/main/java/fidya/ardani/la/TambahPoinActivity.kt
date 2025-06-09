package fidya.ardani.la

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class TambahPoinActivity : AppCompatActivity() {

    private lateinit var edtNama: EditText
    private lateinit var edtJumlah: EditText
    private lateinit var btnSimpan: Button
    private lateinit var toolbar: MaterialToolbar

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String? = null  // untuk edit mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_poin)

        // Inisialisasi view
        toolbar = findViewById(R.id.toolbar)
        edtNama = findViewById(R.id.edt_nama)
        edtJumlah = findViewById(R.id.edt_jumlah)
        btnSimpan = findViewById(R.id.btn_simpan)

        // Setup toolbar sebagai actionbar dan aktifkan tombol back
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Navigasi tombol back toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Ambil documentId jika ada (edit mode)
        documentId = intent.getStringExtra("documentId")

        if (documentId != null) {
            loadDataForEdit(documentId!!)
        }

        btnSimpan.setOnClickListener {
            val nama = edtNama.text.toString().trim()
            val jumlah = edtJumlah.text.toString().toIntOrNull() ?: 0

            if (nama.isEmpty()) {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf("nama" to nama, "jumlah" to jumlah)

            if (documentId == null) {
                // Tambah data baru
                db.collection("data_poin")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menambahkan data", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Update data
                db.collection("data_poin").document(documentId!!)
                    .set(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengupdate data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadDataForEdit(docId: String) {
        db.collection("data_poin").document(docId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    edtNama.setText(document.getString("nama") ?: "")
                    edtJumlah.setText(document.getLong("jumlah")?.toString() ?: "0")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }
}
