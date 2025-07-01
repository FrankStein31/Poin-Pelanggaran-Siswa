package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class JadwalPiketActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tambahButton: FloatingActionButton
    private lateinit var adapter: JadwalPiketAdapter
    private val db = FirebaseFirestore.getInstance()
    private val EDIT_JADWAL_REQUEST_CODE = 1

    // Daftar ini sekarang akan menampung data yang sudah dikelompokkan (header dan item)
    private val displayList = mutableListOf<Any>()
    private val guruList = mutableListOf<Guru>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_piket)

        listView = findViewById(R.id.jadwalListView)
        tambahButton = findViewById(R.id.tambahJadwalButton)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { finish() }

        // Inisialisasi adapter dengan displayList
        adapter = JadwalPiketAdapter(this, displayList)
        listView.adapter = adapter

        // Ambil data guru dulu, baru data jadwal
        ambilDataGuru()

        tambahButton.setOnClickListener {
            val intent = Intent(this, TambahJadwalPiketActivity::class.java)
            startActivity(intent)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            // Pastikan yang diklik adalah item, bukan header
            val selectedItem = displayList[position]
            if (selectedItem is JadwalPiketModel) {
                tampilkanOpsiDialog(selectedItem)
            }
        }
    }

    // Panggil ambilDataGuru() saat kembali ke activity ini untuk refresh
    override fun onResume() {
        super.onResume()
        if (guruList.isNotEmpty()) {
            ambilJadwalPiket()
        }
    }

    private fun ambilDataGuru() {
        db.collection("guru_piket").get()
            .addOnSuccessListener { result ->
                guruList.clear()
                for (document in result) {
                    guruList.add(Guru(id = document.id, nama = document.getString("nama") ?: ""))
                }
                // Setelah data guru siap, ambil data jadwal
                ambilJadwalPiket()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ambilJadwalPiket() {
        // Ambil data dan urutkan berdasarkan tanggal
        db.collection("jadwal_piket")
            .orderBy("tanggal", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val jadwalList = mutableListOf<JadwalPiketModel>()
                for (doc in result) {
                    val guruId = doc.getString("guru_id") ?: ""
                    jadwalList.add(
                        JadwalPiketModel(
                            id = doc.id,
                            tanggal = doc.getString("tanggal") ?: "",
                            hari = doc.getString("hari") ?: "",
                            jam = doc.getString("jam") ?: "",
                            guruId = guruId,
                            guruNama = guruList.find { it.id == guruId }?.nama ?: "Tidak diketahui"
                        )
                    )
                }
                // Panggil fungsi untuk mengelompokkan data
                kelompokkanJadwalPerMinggu(jadwalList)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat jadwal piket", Toast.LENGTH_SHORT).show()
            }
    }

    private fun kelompokkanJadwalPerMinggu(jadwalList: List<JadwalPiketModel>) {
        displayList.clear()
        if (jadwalList.isEmpty()) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var currentWeekIdentifier: String? = null

        for (jadwal in jadwalList) {
            val tanggal = dateFormat.parse(jadwal.tanggal) ?: continue
            val cal = Calendar.getInstance().apply { time = tanggal }
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekIdentifier = dateFormat.format(cal.time)

            if (weekIdentifier != currentWeekIdentifier) {
                currentWeekIdentifier = weekIdentifier
                // Buat header untuk minggu baru
                val startCal = cal.clone() as Calendar
                val endCal = cal.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, 6)

                val displayFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
                val headerText = "${displayFormat.format(startCal.time)} - ${displayFormat.format(endCal.time)}"
                displayList.add(headerText)
            }
            displayList.add(jadwal)
        }
    }

    private fun tampilkanOpsiDialog(jadwal: JadwalPiketModel) {
        AlertDialog.Builder(this)
            .setTitle("Pilih Aksi untuk ${jadwal.guruNama} (${jadwal.hari})")
            .setItems(arrayOf("Edit", "Hapus")) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, EditJadwalActivity::class.java)
                        intent.putExtra("JADWAL_DATA", jadwal)
                        startActivityForResult(intent, EDIT_JADWAL_REQUEST_CODE)
                    }
                    1 -> tampilkanKonfirmasiHapusDialog(jadwal)
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_JADWAL_REQUEST_CODE && resultCode == RESULT_OK) {
            // Jika edit berhasil, muat ulang data dari Firestore untuk menampilkan perubahan
            Toast.makeText(this, "Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
            ambilJadwalPiket()
        }
    }

    private fun tampilkanKonfirmasiHapusDialog(jadwal: JadwalPiketModel) {
        AlertDialog.Builder(this)
            .setMessage("Apakah Anda yakin ingin menghapus jadwal ini?")
            .setPositiveButton("Ya") { _, _ -> hapusJadwalPiket(jadwal) }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun hapusJadwalPiket(jadwal: JadwalPiketModel) {
        db.collection("jadwal_piket").document(jadwal.id).delete()
            .addOnSuccessListener {
                // Hapus dari daftar dan refresh
                displayList.remove(jadwal)
                // Cek jika header menjadi yatim piatu (tidak ada item di bawahnya)
                val index = displayList.indexOfFirst { it is JadwalPiketModel && it.id == jadwal.id } -1
                if(index >= 0 && index < displayList.size -1) {
                    val itemBefore = displayList.getOrNull(index)
                    val itemAfter = displayList.getOrNull(index + 1)
                    if (itemBefore is String && (itemAfter == null || itemAfter is String)) {
                        displayList.removeAt(index)
                    }
                }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus jadwal", Toast.LENGTH_SHORT).show()
            }
    }
}
