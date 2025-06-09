package fidya.ardani.la

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class KategoriPoinActivity : AppCompatActivity() {

    private lateinit var edtNamaKategori: EditText
    private lateinit var spinnerPoin: Spinner
    private lateinit var btnSimpanKategori: Button
    private lateinit var recyclerViewKategori: RecyclerView
    private lateinit var kategoriAdapter: KategoriPoinAdapter
    private val kategoriList = mutableListOf<KategoriPoin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori_poin)

        // Inisialisasi komponen UI
        edtNamaKategori = findViewById(R.id.edtNamaKategori)
        spinnerPoin = findViewById(R.id.spinnerPoin)
        btnSimpanKategori = findViewById(R.id.btnSimpanKategori)
        recyclerViewKategori = findViewById(R.id.recyclerViewKategori)

        // Inisialisasi RecyclerView
        kategoriAdapter = KategoriPoinAdapter(kategoriList)
        recyclerViewKategori.layoutManager = LinearLayoutManager(this)
        recyclerViewKategori.adapter = kategoriAdapter

        // Data poin yang bisa dipilih
        val poinList = listOf(10, 20, 30, 40, 50, 100)
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, poinList)
        spinnerPoin.adapter = adapterSpinner

        // Event klik tombol simpan
        btnSimpanKategori.setOnClickListener {
            tambahKategoriPoin()
        }
    }

    private fun tambahKategoriPoin() {
        val namaKategori = edtNamaKategori.text.toString().trim()
        val poin = spinnerPoin.selectedItem as Int

        if (namaKategori.isEmpty()) {
            Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Tambahkan kategori ke daftar
        val kategori = KategoriPoin(namaKategori, poin)
        kategoriList.add(kategori)
        kategoriAdapter.notifyDataSetChanged()

        // Reset input
        edtNamaKategori.text.clear()
        spinnerPoin.setSelection(0)

        Toast.makeText(this, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
    }
}
