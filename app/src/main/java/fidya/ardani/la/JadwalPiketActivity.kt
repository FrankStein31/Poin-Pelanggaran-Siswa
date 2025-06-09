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

class JadwalPiketActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tambahButton: FloatingActionButton
    private lateinit var adapter: JadwalPiketAdapter
    private val db = FirebaseFirestore.getInstance()

    private val jadwalList = mutableListOf<JadwalPiketModel>()
    private val guruList = mutableListOf<Guru>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_piket)

        listView = findViewById(R.id.jadwalListView)
        tambahButton = findViewById(R.id.tambahJadwalButton)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = JadwalPiketAdapter(this, jadwalList, guruList)
        listView.adapter = adapter

        ambilDataGuru()

        tambahButton.setOnClickListener {
            val intent = Intent(this, TambahJadwalPiketActivity::class.java)
            startActivity(intent)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedJadwal = jadwalList[position]
            tampilkanOpsiDialog(selectedJadwal, position)
        }
    }

    private fun ambilDataGuru() {
        db.collection("guru_piket")
            .get()
            .addOnSuccessListener { result ->
                guruList.clear()
                for (document in result) {
                    val nama = document.getString("nama") ?: ""
                    val alamat = document.getString("alamat") ?: ""
                    val email = document.getString("email") ?: ""
                    val jadwalPiket = document.getString("jadwalPiket") ?: ""
                    val nip = document.getString("nip") ?: ""

                    guruList.add(
                        Guru(
                            id = document.id,
                            nama = nama,
                            alamat = alamat,
                            email = email,
                            jadwalPiket = jadwalPiket,
                            nip = nip
                        )
                    )
                }
                ambilJadwalPiket()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ambilJadwalPiket() {
        db.collection("jadwal_piket")
            .get()
            .addOnSuccessListener { result ->
                jadwalList.clear()
                for (doc in result) {
                    val tanggal = doc.getString("tanggal") ?: ""
                    val hari = doc.getString("hari") ?: ""
                    val jam = doc.getString("jam") ?: ""
                    val guruId = doc.getString("guru_id") ?: ""
                    val guruNama = guruList.find { it.id == guruId }?.nama ?: "Tidak diketahui"

                    jadwalList.add(JadwalPiketModel(doc.id, tanggal, hari, jam, guruId, guruNama))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat jadwal piket", Toast.LENGTH_SHORT).show()
            }
    }

    private fun tampilkanOpsiDialog(jadwal: JadwalPiketModel, position: Int) {
        val options = arrayOf("Edit", "Hapus")
        AlertDialog.Builder(this)
            .setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, TambahJadwalPiketActivity::class.java)
                        intent.putExtra("jadwal_id", jadwal.id)
                        startActivity(intent)
                    }
                    1 -> {
                        tampilkanKonfirmasiHapusDialog(jadwal, position)
                    }
                }
            }
            .show()
    }

    private fun tampilkanKonfirmasiHapusDialog(jadwal: JadwalPiketModel, position: Int) {
        AlertDialog.Builder(this)
            .setMessage("Apakah Anda yakin ingin menghapus jadwal ini?")
            .setPositiveButton("Ya") { _, _ ->
                hapusJadwalPiket(jadwal, position)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun hapusJadwalPiket(jadwal: JadwalPiketModel, position: Int) {
        db.collection("jadwal_piket")
            .document(jadwal.id)
            .delete()
            .addOnSuccessListener {
                jadwalList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus jadwal", Toast.LENGTH_SHORT).show()
            }
    }
}
