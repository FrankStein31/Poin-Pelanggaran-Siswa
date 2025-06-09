package fidya.ardani.la

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FieldPath 
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PiketHariIniActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var txtTanggal: TextView
    private lateinit var toolbar: Toolbar

    private val db = FirebaseFirestore.getInstance()
    private val piketList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_piket_hari_ini)

        // Setup Toolbar dan tombol back
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Bind view
        listView = findViewById(R.id.listViewPiketHariIni)
        txtTanggal = findViewById(R.id.txtCurrentDate)

        // Setup adapter untuk ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, piketList)
        listView.adapter = adapter

        // Tampilkan tanggal hari ini dengan format Indonesia
        val hariIni = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
        txtTanggal.text = "Hari ini: $hariIni"

        // Format tanggal untuk query Firestore (yyyy-MM-dd)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = sdf.format(Date())

        loadJadwalPiket(todayDate)
    }

    private fun loadJadwalPiket(tanggal: String) {
        db.collection("jadwal_piket")
            .whereEqualTo("tanggal", tanggal)
            .get()
            .addOnSuccessListener { documents ->
                piketList.clear()

                if (documents.isEmpty) {
                    piketList.add("Tidak ada jadwal piket hari ini.")
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Karena ada query async di dalam loop, kita perlu mengumpulkan semua data guru dulu
                val guruIdList = mutableListOf<String>()
                val jadwalMap = mutableMapOf<String, String>() // guruId ke jam

                for (doc in documents) {
                    val jam = doc.getString("jam") ?: "-"
                    val guruId = doc.getString("guru_id") ?: ""
                    guruIdList.add(guruId)
                    jadwalMap[guruId] = jam
                }

                // Ambil data guru sekaligus dari koleksi "guru_piket"
                // Kita bisa ambil semua guru dengan satu query menggunakan whereIn
                if (guruIdList.isNotEmpty()) {
                    db.collection("guru_piket")
                        .whereIn(FieldPath.documentId(), guruIdList)
                        .get()
                        .addOnSuccessListener { guruDocs ->
                            for (guruDoc in guruDocs) {
                                val guruId = guruDoc.id
                                val guruNama = guruDoc.getString("nama") ?: "Tidak diketahui"
                                val jam = jadwalMap[guruId] ?: "-"
                                piketList.add("Guru: $guruNama\nJam: $jam")
                            }
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener {
                            // Kalau gagal ambil data guru, tampilkan jadwal dengan info guru tidak diketahui
                            for (guruId in guruIdList) {
                                val jam = jadwalMap[guruId] ?: "-"
                                piketList.add("Guru: Tidak diketahui\nJam: $jam")
                            }
                            adapter.notifyDataSetChanged()
                        }
                } else {
                    adapter.notifyDataSetChanged()
                }

            }
            .addOnFailureListener {
                piketList.clear()
                piketList.add("Gagal memuat data piket.")
                adapter.notifyDataSetChanged()
            }
    }
}
