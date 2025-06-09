package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class DataPoinActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: FloatingActionButton
    private lateinit var adapter: ArrayAdapter<String>

    private val db = FirebaseFirestore.getInstance()
    private val dataList = mutableListOf<String>()
    private val documentIds = mutableListOf<String>() // Menyimpan ID dokumen dari Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_poin)

        // Inisialisasi toolbar dan navigasi back
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Menutup activity dan kembali ke sebelumnya
        }

        listView = findViewById(R.id.listview_datapoin)
        btnTambah = findViewById(R.id.btn_tambah_poin)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        loadDataPoin()

        btnTambah.setOnClickListener {
            val intent = Intent(this, TambahPoinActivity::class.java)
            startActivity(intent)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDocumentId = documentIds[position]
            showEditDeleteDialog(selectedDocumentId)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDataPoin()
    }

    private fun loadDataPoin() {
        db.collection("data_poin")
            .get()
            .addOnSuccessListener { result ->
                dataList.clear()
                documentIds.clear()
                for (document in result) {
                    val nama = document.getString("nama") ?: "Tanpa Nama"
                    val jumlah = document.getLong("jumlah") ?: 0
                    val item = "$nama - $jumlah poin"
                    dataList.add(item)
                    documentIds.add(document.id)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDeleteDialog(documentId: String) {
        val options = arrayOf("Edit", "Hapus")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, TambahPoinActivity::class.java)
                        intent.putExtra("documentId", documentId)
                        startActivity(intent)
                    }
                    1 -> {
                        confirmDelete(documentId)
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(documentId: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deletePoin(documentId)
            }
            .setNegativeButton("Batal", null)
            .create()
        dialog.show()
    }

    private fun deletePoin(documentId: String) {
        db.collection("data_poin").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadDataPoin()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
            }
    }
}
