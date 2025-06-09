package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import fidya.ardani.la.adapter.GuruAdapter

class DataGuruPiketActivity : AppCompatActivity() {

    private lateinit var listViewGuru: ListView
    private lateinit var fabTambahGuru: FloatingActionButton
    private lateinit var topAppBar: MaterialToolbar
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_guru) // Pastikan ini sesuai dengan nama layout XML-mu

        // Bind view
        listViewGuru = findViewById(R.id.listViewGuru)
        fabTambahGuru = findViewById(R.id.fabTambahGuru)
        topAppBar = findViewById(R.id.topAppBar)

        // Handle tombol back toolbar
        topAppBar.setNavigationOnClickListener {
            onBackPressed() // Bisa langsung finish() atau custom intent jika perlu
        }

        // Handle tombol tambah guru (FloatingActionButton)
        fabTambahGuru.setOnClickListener {
            startActivity(Intent(this, TambahGuruPiketActivity::class.java))
        }

        ambilDataGuru()
    }

    private fun ambilDataGuru() {
        firestore.collection("guru_piket").get()
            .addOnSuccessListener { result ->
                val listGuru = result.map {
                    Guru(
                        id = it.id,
                        nama = it.getString("nama") ?: "",
                        nip = it.getString("nip") ?: "",
                        email = it.getString("email") ?: "",
                        alamat = it.getString("alamat") ?: "",
                        jadwalPiket = it.getString("jadwalPiket") ?: "-"
                    )
                }

                listViewGuru.adapter = GuruAdapter(
                    this,
                    R.layout.list_item_guru,
                    listGuru,
                    onEdit = { guru -> editGuru(guru) },
                    onDelete = { guru -> hapusGuru(guru) }
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusGuru(guru: Guru) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah Anda yakin ingin menghapus guru ${guru.nama}?")
            .setCancelable(false)
            .setPositiveButton("Ya") { dialog, _ ->
                firestore.collection("guru_piket").document(guru.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Guru dihapus", Toast.LENGTH_SHORT).show()
                        ambilDataGuru()  // Reload data setelah dihapus
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menghapus guru", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
    }

    private fun editGuru(guru: Guru) {
        val intent = Intent(this, TambahGuruPiketActivity::class.java).apply {
            putExtra("id", guru.id)
            putExtra("nama", guru.nama)
            putExtra("nip", guru.nip)
            putExtra("email", guru.email)
            putExtra("alamat", guru.alamat)
            putExtra("jadwalPiket", guru.jadwalPiket)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        ambilDataGuru()
    }
}
