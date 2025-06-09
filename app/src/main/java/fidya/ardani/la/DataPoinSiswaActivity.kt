package fidya.ardani.la

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class DataPoinSiswaActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private val db = FirebaseFirestore.getInstance()
    private val dataList = mutableListOf<String>()
    private val documentIds = mutableListOf<String>() // Untuk menyimpan ID dokumen Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_poin_siswa)

        // Inisialisasi toolbar dengan tombol kembali
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        listView = findViewById(R.id.listview_datapoin_siswa)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        // Optional: Tambahkan event klik jika ingin detail atau hapus data
        listView.setOnItemClickListener { _, _, position, _ ->
            val namaPoin = dataList[position]
            Toast.makeText(this, "Klik: $namaPoin", Toast.LENGTH_SHORT).show()
        }

        loadDataPoin()
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
}
