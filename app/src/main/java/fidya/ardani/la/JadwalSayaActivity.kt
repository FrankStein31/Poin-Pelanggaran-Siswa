package fidya.ardani.la

import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class JadwalSayaActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: JadwalSayaAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val displayList = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_saya)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        listView = findViewById(R.id.listViewJadwalSaya)
        adapter = JadwalSayaAdapter(this, displayList)
        listView.adapter = adapter

        // PERBAIKAN: Ubah alur pengambilan data
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            // Panggil fungsi baru untuk memulai alur yang benar
            getGuruDocIdAndLoadJadwal(currentUser.email!!)
        } else {
            Toast.makeText(this, "Gagal mengidentifikasi email pengguna. Silakan login ulang.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // PERBAIKAN: Fungsi baru untuk mencari ID Dokumen guru berdasarkan email
    private fun getGuruDocIdAndLoadJadwal(email: String) {
        db.collection("guru_piket")
            .whereEqualTo("email", email)
            .limit(1) // Hanya perlu satu dokumen guru
            .get()
            .addOnSuccessListener { guruResult ->
                if (guruResult.isEmpty) {
                    Toast.makeText(this, "Data guru tidak ditemukan untuk email ini.", Toast.LENGTH_SHORT).show()
                    displayList.clear()
                    displayList.add("Data guru tidak ditemukan.")
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Ambil ID Dokumen dari guru yang ditemukan
                val guruDocId = guruResult.documents[0].id

                // Panggil fungsi load jadwal dengan ID Dokumen yang benar
                loadJadwalSaya(guruDocId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mencari data guru: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi ini sekarang menerima ID Dokumen, bukan Auth UID
    private fun loadJadwalSaya(guruDocumentId: String) {
        db.collection("jadwal_piket")
            .whereEqualTo("guru_id", guruDocumentId)
            .orderBy("tanggal", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    displayList.clear()
                    displayList.add("Anda tidak memiliki jadwal piket.")
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }
                prosesDanKelompokkanJadwal(result)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat jadwal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prosesDanKelompokkanJadwal(result: QuerySnapshot) {
        displayList.clear()

        val jadwalList = result.map { doc ->
            Jadwal(
                hari = doc.getString("hari") ?: "",
                tanggal = doc.getString("tanggal") ?: "",
                jam = doc.getString("jam") ?: ""
            )
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var currentWeekIdentifier: String? = null

        for (jadwal in jadwalList) {
            val tanggalDate = dateFormat.parse(jadwal.tanggal) ?: continue
            val cal = Calendar.getInstance().apply { time = tanggalDate }
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekIdentifier = dateFormat.format(cal.time)

            if (weekIdentifier != currentWeekIdentifier) {
                currentWeekIdentifier = weekIdentifier
                val endCal = cal.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, 6)
                val displayFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
                val headerText = "Minggu, ${displayFormat.format(cal.time)} - ${displayFormat.format(endCal.time)}"
                displayList.add(headerText)
            }
            displayList.add(jadwal)
        }
        adapter.notifyDataSetChanged()
    }

    // --- Data class dan Adapter (tidak ada perubahan) ---

    data class Jadwal(val hari: String, val tanggal: String, val jam: String)

    class JadwalSayaAdapter(private val context: Context, private val data: List<Any>) : BaseAdapter() {
        private val TYPE_ITEM = 0
        private val TYPE_HEADER = 1
        private val TYPE_EMPTY = 2

        private class ItemViewHolder(view: View) {
            val tvHari: TextView = view.findViewById(R.id.tvHari)
            val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
            val tvJam: TextView = view.findViewById(R.id.tvJam)
        }

        private class HeaderViewHolder(view: View) {
            val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        }

        override fun getCount(): Int = data.size
        override fun getItem(position: Int): Any = data[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getViewTypeCount(): Int = 3

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is Jadwal -> TYPE_ITEM
                is String -> if ((getItem(position) as String).startsWith("Anda tidak") || (getItem(position) as String).startsWith("Data guru")) TYPE_EMPTY else TYPE_HEADER
                else -> TYPE_EMPTY
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewType = getItemViewType(position)
            var view = convertView

            when (viewType) {
                TYPE_ITEM -> {
                    val holder: ItemViewHolder
                    if (view == null) {
                        view = LayoutInflater.from(context).inflate(R.layout.item_jadwal_saya, parent, false)
                        holder = ItemViewHolder(view)
                        view.tag = holder
                    } else {
                        holder = view.tag as ItemViewHolder
                    }
                    val item = getItem(position) as Jadwal
                    holder.tvHari.text = item.hari
                    holder.tvTanggal.text = formatTanggalTampil(item.tanggal)
                    holder.tvJam.text = item.jam
                }
                TYPE_HEADER -> {
                    val holder: HeaderViewHolder
                    if (view == null) {
                        view = LayoutInflater.from(context).inflate(R.layout.item_jadwal_header, parent, false)
                        holder = HeaderViewHolder(view)
                        view.tag = holder
                    } else {
                        holder = view.tag as HeaderViewHolder
                    }
                    holder.headerTextView.text = getItem(position) as String
                }
                TYPE_EMPTY -> {
                    if (view == null) {
                        view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
                    }
                    val emptyTextView = view!!.findViewById<TextView>(android.R.id.text1)
                    emptyTextView.text = getItem(position) as String
                }
            }
            return view!!
        }

        private fun formatTanggalTampil(tanggalDb: String): String {
            return try {
                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val date = dbFormat.parse(tanggalDb)
                displayFormat.format(date!!)
            } catch (e: Exception) {
                tanggalDb
            }
        }
    }
}
