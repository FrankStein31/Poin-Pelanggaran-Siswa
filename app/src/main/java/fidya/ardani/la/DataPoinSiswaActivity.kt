package fidya.ardani.la

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class DataPoinSiswaActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: DataPoinAdapter
    private val db = FirebaseFirestore.getInstance()
    private val dataPoinList = mutableListOf<DataPoin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_poin_siswa)

        // Inisialisasi toolbar dengan tombol kembali
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        listView = findViewById(R.id.listview_datapoin_siswa)
        adapter = DataPoinAdapter()
        listView.adapter = adapter

        loadDataPoin()
    }

    private fun loadDataPoin() {
        db.collection("data_poin")
            .get()
            .addOnSuccessListener { result ->
                dataPoinList.clear()
                for (document in result) {
                    val nama = document.getString("nama") ?: "Tanpa Nama"
                    val jumlah = document.getLong("jumlah")?.toInt() ?: 0
                    dataPoinList.add(DataPoin(nama, jumlah))
                }
                // Urutkan berdasarkan jumlah poin dari yang terbesar
                dataPoinList.sortByDescending { it.jumlah }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data poin", Toast.LENGTH_SHORT).show()
            }
    }

    inner class DataPoinAdapter : BaseAdapter() {
        override fun getCount(): Int = dataPoinList.size
        override fun getItem(position: Int): Any = dataPoinList[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@DataPoinSiswaActivity)
                .inflate(android.R.layout.simple_list_item_2, parent, false)

            val dataPoin = dataPoinList[position]
            
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            text1.text = dataPoin.nama
            text2.text = "${dataPoin.jumlah} poin"

            // Atur warna teks berdasarkan jumlah poin
            val textColor = when {
                dataPoin.jumlah >= 50 -> getColor(android.R.color.holo_red_dark)
                dataPoin.jumlah >= 25 -> getColor(android.R.color.holo_orange_dark)
                else -> getColor(android.R.color.darker_gray)
            }
            text2.setTextColor(textColor)

            return view
        }
    }

    data class DataPoin(val nama: String, val jumlah: Int)
}
