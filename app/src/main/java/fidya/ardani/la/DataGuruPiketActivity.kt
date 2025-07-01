package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import fidya.ardani.la.adapter.GuruAdapter

class DataGuruPiketActivity : AppCompatActivity(), GuruAdapter.AdapterListener {

    private lateinit var listViewGuru: ListView
    private lateinit var fabTambahGuru: FloatingActionButton
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var adapter: GuruAdapter
    private val listGuru = mutableListOf<Guru>()

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_guru)

        listViewGuru = findViewById(R.id.listViewGuru)
        fabTambahGuru = findViewById(R.id.fabTambahGuru)
        topAppBar = findViewById(R.id.topAppBar)

        adapter = GuruAdapter(this, listGuru, this)
        listViewGuru.adapter = adapter

        topAppBar.setNavigationOnClickListener { onBackPressed() }

        fabTambahGuru.setOnClickListener {
            startActivity(Intent(this, TambahGuruPiketActivity::class.java))
        }

        // Tambahkan listener klik untuk menampilkan detail
        listViewGuru.setOnItemClickListener { parent, view, position, id ->
            val selectedGuru = listGuru[position]
            showDetailDialog(selectedGuru)
        }
    }

    override fun onResume() {
        super.onResume()
        ambilDataGuru()
    }

    private fun ambilDataGuru() {
        firestore.collection("guru_piket").get()
            .addOnSuccessListener { result ->
                listGuru.clear()
                for (document in result) {
                    val guru = document.toObject<Guru>()
                    guru.id = document.id // Penting: set ID dari dokumen
                    listGuru.add(guru)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data guru", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onEdit(guru: Guru) {
        val intent = Intent(this, TambahGuruPiketActivity::class.java).apply {
            putExtra("GURU_ID", guru.id)
        }
        startActivity(intent)
    }

    override fun onDelete(guru: Guru) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Guru")
            .setMessage("Apakah Anda yakin ingin menghapus ${guru.nama}?")
            .setPositiveButton("Ya") { _, _ ->
                firestore.collection("guru_piket").document(guru.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Guru berhasil dihapus", Toast.LENGTH_SHORT).show()
                        ambilDataGuru()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    // FUNGSI BARU UNTUK MENAMPILKAN POPUP DETAIL
    private fun showDetailDialog(guru: Guru) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_detail_guru, null)
        builder.setView(view)

        // Bind views dari layout dialog
        val imgProfil = view.findViewById<ImageView>(R.id.detailImgGuru)
        val nama = view.findViewById<TextView>(R.id.detailNama)
        val nip = view.findViewById<TextView>(R.id.detailNip)
        val noHp = view.findViewById<TextView>(R.id.detailNoHp)
        val alamat = view.findViewById<TextView>(R.id.detailAlamat)
        val jadwalPiket = view.findViewById<TextView>(R.id.detailJadwalPiket)
        val email = view.findViewById<TextView>(R.id.detailEmail)

        // Set data ke views
        nama.text = "Nama: ${guru.nama}"
        nip.text = "NIP: ${guru.nip}"
        noHp.text = "No. HP: ${guru.noHp.ifEmpty { "-" }}"
        alamat.text = "Alamat: ${guru.alamat}"
        jadwalPiket.text = "Jadwal Piket: ${guru.jadwalPiket}"
        email.text = "Email: ${guru.email}"

        // Load gambar
        if (guru.fotoProfilUrl.isNotEmpty()) {
            Glide.with(this).load(guru.fotoProfilUrl).placeholder(R.drawable.ic_person).into(imgProfil)
        }

        builder.setPositiveButton("Tutup") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
