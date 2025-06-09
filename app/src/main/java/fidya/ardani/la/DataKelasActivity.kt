package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DataKelasActivity : AppCompatActivity() {

    private lateinit var btnTambahKelas: FloatingActionButton
    private lateinit var listViewKelas: ListView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: ArrayAdapter<String>
    private val dataKelas = mutableListOf<String>()
    private val kelasIds = mutableListOf<String>() // Untuk menyimpan ID kelas dari Firestore

    private var kelasListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kelas)

        // Inisialisasi view
        btnTambahKelas = findViewById(R.id.btnTambahKelas)
        listViewKelas = findViewById(R.id.listViewKelas)
        toolbar = findViewById(R.id.toolbar)

        // Atur tombol kembali di toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        firestore = FirebaseFirestore.getInstance()

        // Inisialisasi adapter dan hubungkan ke ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataKelas)
        listViewKelas.adapter = adapter

        // Aksi tombol tambah kelas
        btnTambahKelas.setOnClickListener {
            startActivity(Intent(this, TambahKelasActivity::class.java))
        }

        // Long click untuk edit/hapus
        listViewKelas.setOnItemLongClickListener { _, _, position, _ ->
            val kelasId = kelasIds[position]
            val kelasNama = dataKelas[position]
            showEditDeleteDialog(kelasId, kelasNama)
            true
        }

        // Ambil data dari Firestore
        loadDataKelas()
    }

    private fun loadDataKelas() {
        kelasListener = firestore.collection("kelas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                dataKelas.clear()
                kelasIds.clear()
                snapshot?.forEach { document ->
                    val kelasId = document.id
                    val namaKelas = document.getString("kelas")
                    if (namaKelas != null) {
                        dataKelas.add(namaKelas)
                        kelasIds.add(kelasId)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showEditDeleteDialog(kelasId: String, kelasNama: String) {
        val options = arrayOf("Edit", "Hapus")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Aksi")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditDialog(kelasId, kelasNama)
                1 -> showDeleteDialog(kelasId)
            }
        }
        builder.show()
    }

    private fun showEditDialog(kelasId: String, kelasNama: String) {
        val editText = EditText(this)
        editText.setText(kelasNama)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Kelas")
        builder.setView(editText)
        builder.setPositiveButton("Simpan") { _, _ ->
            val updatedKelas = editText.text.toString().trim()
            if (updatedKelas.isNotEmpty()) {
                firestore.collection("kelas").document(kelasId)
                    .update("kelas", updatedKelas)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Kelas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal memperbarui kelas: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Nama kelas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showDeleteDialog(kelasId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hapus Kelas")
        builder.setMessage("Apakah Anda yakin ingin menghapus kelas ini?")
        builder.setPositiveButton("Hapus") { _, _ ->
            firestore.collection("kelas").document(kelasId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Kelas berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menghapus kelas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        kelasListener?.remove()
    }
}
