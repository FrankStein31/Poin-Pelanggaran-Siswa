package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class DataJurusanActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var fabTambahJurusan: FloatingActionButton
    private lateinit var firestore: FirebaseFirestore
    private lateinit var jurusanList: MutableList<Pair<String, String>> // Pair<ID, Nama>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_jurusan)

        // Bind view elements
        listView = findViewById(R.id.listViewJurusan)
        fabTambahJurusan = findViewById(R.id.fabTambahJurusan)
        topAppBar = findViewById(R.id.topAppBar)
        searchView = findViewById(R.id.searchView)
        firestore = FirebaseFirestore.getInstance()

        // Setup toolbar navigation (back) button
        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Initialize the list and adapter
        jurusanList = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        // FAB untuk tambah jurusan
        fabTambahJurusan.setOnClickListener {
            startActivity(Intent(this, TambahJurusanActivity::class.java))
        }

        // Klik item listview untuk aksi edit/hapus
        listView.setOnItemClickListener { _, _, position, _ ->
            val (id, namaLama) = jurusanList[position]
            tampilkanDialogAksi(id, namaLama)
        }

        // Setup SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterJurusanList(query?.trim() ?: "")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterJurusanList(newText?.trim() ?: "")
                return true
            }
        })

        ambilDataJurusan()
    }

    override fun onResume() {
        super.onResume()
        ambilDataJurusan()
    }

    override fun onBackPressed() {
        val intent = Intent(this, DashboardGuruBkActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun ambilDataJurusan() {
        firestore.collection("jurusan")
            .get()
            .addOnSuccessListener { result ->
                jurusanList.clear()
                val namaList = mutableListOf<String>()
                for (document in result) {
                    val id = document.id
                    val nama = document.getString("nama") ?: "Tidak diketahui"
                    jurusanList.add(id to nama)
                    namaList.add(nama)
                }
                adapter.clear()
                adapter.addAll(namaList)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterJurusanList(query: String) {
        val filteredList = jurusanList.filter { (_, nama) ->
            val namaUpper = nama.uppercase()
            val queryUpper = query.uppercase()

            if (query.isEmpty()) {
                true  // Tampilkan semua jika kosong
            } else if (query.length == 1 && query[0].isLetter()) {
                namaUpper.startsWith(queryUpper)
            } else {
                namaUpper.contains(queryUpper)
            }
        }.map { it.second }

        adapter.clear()
        adapter.addAll(filteredList)
        adapter.notifyDataSetChanged()
    }

    private fun tampilkanDialogAksi(id: String, namaLama: String) {
        val pilihan = arrayOf("Edit", "Hapus")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Aksi")
            .setItems(pilihan) { _, which ->
                when (which) {
                    0 -> tampilkanDialogEdit(id, namaLama)
                    1 -> hapusJurusan(id)
                }
            }
            .show()
    }

    private fun tampilkanDialogEdit(id: String, namaLama: String) {
        val input = android.widget.EditText(this)
        input.setText(namaLama)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Jurusan")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = input.text.toString().trim()
                if (namaBaru.isNotEmpty()) {
                    firestore.collection("jurusan").document(id)
                        .update("nama", namaBaru)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Berhasil diubah", Toast.LENGTH_SHORT).show()
                            ambilDataJurusan()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun hapusJurusan(id: String) {
        firestore.collection("jurusan").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Jurusan dihapus", Toast.LENGTH_SHORT).show()
                ambilDataJurusan()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
