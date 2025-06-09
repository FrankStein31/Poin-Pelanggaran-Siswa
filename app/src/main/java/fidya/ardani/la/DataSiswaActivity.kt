package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class DataSiswaActivity : AppCompatActivity() {

    private lateinit var listViewSiswa: ListView
    private lateinit var searchView: SearchView
    private lateinit var fabTambahSiswa: FloatingActionButton
    private lateinit var adapter: SiswaAdapter
    private val semuaSiswa = mutableListOf<Siswa>()
    private val filteredSiswa = mutableListOf<Siswa>()

    private val db = FirebaseFirestore.getInstance()
    private var currentKelasFilter: String? = null  // Untuk menyimpan filter kelas aktif

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_siswa)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnBack: ImageView = findViewById(R.id.btnBack)
        searchView = findViewById(R.id.searchView)
        listViewSiswa = findViewById(R.id.listViewSiswa)
        fabTambahSiswa = findViewById(R.id.fabTambahSiswa)

        adapter = SiswaAdapter(this, filteredSiswa)
        listViewSiswa.adapter = adapter

        btnBack.setOnClickListener { onBackPressed() }

        fabTambahSiswa.setOnClickListener {
            startActivity(Intent(this, TambahDataSiswaActivity::class.java))
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterData(newText)
                return true
            }
        })

        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.filter_kelas_x -> {
                    currentKelasFilter = "X"
                    filterData(searchView.query.toString())
                    true
                }
                R.id.filter_kelas_xi -> {
                    currentKelasFilter = "XI"
                    filterData(searchView.query.toString())
                    true
                }
                R.id.filter_kelas_xii -> {
                    currentKelasFilter = "XII"
                    filterData(searchView.query.toString())
                    true
                }
                else -> false
            }
        }

        ambilDataSiswa()
    }

    private fun ambilDataSiswa() {
        db.collection("siswa")
            .get()
            .addOnSuccessListener { result ->
                semuaSiswa.clear()
                for (document in result) {
                    val siswa = Siswa(
                        id = document.id,
                        nama = document.getString("nama") ?: "",
                        nis = document.getString("nis") ?: "",
                        jurusan = document.getString("jurusan") ?: "",
                        kelas = document.getString("kelas") ?: "",
                        alamat = document.getString("alamat") ?: "",
                        password = document.getString("password") ?: "",
                    )
                    semuaSiswa.add(siswa)
                }
                filterData(searchView.query.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data siswa", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterData(query: String?) {
        filteredSiswa.clear()

        val hasilFilter = semuaSiswa.filter { siswa ->
            val cocokKelas = currentKelasFilter == null || siswa.kelas.contains(currentKelasFilter!!, ignoreCase = true)

            if (query.isNullOrEmpty()) {
                cocokKelas
            } else if (query.length == 1 && query[0].isLetter()) {
                siswa.nama.startsWith(query, ignoreCase = true) && cocokKelas
            } else {
                siswa.nama.contains(query, ignoreCase = true) && cocokKelas
            }
        }

        filteredSiswa.addAll(hasilFilter)
        adapter.notifyDataSetChanged()
    }
}
