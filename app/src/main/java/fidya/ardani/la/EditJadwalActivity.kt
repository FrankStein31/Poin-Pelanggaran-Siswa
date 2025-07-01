package fidya.ardani.la

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditJadwalActivity : AppCompatActivity() {

    private lateinit var jadwal: JadwalPiketModel
    private val db = FirebaseFirestore.getInstance()

    // Data untuk UI
    private val guruList = mutableListOf<Guru>()
    private lateinit var guruAdapter: ArrayAdapter<String>

    // Komponen UI
    private lateinit var spinnerHari: Spinner
    private lateinit var spinnerGuru: Spinner
    private lateinit var etTanggal: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnBatal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_jadwal)

        jadwal = intent.getParcelableExtra("JADWAL_DATA") ?: run {
            Toast.makeText(this, "Gagal memuat data jadwal", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        setupHariSpinner()
        ambilDataGuruDanSetUI()
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { finish() }

        spinnerHari = findViewById(R.id.spinnerHari)
        spinnerGuru = findViewById(R.id.spinnerGuru)
        etTanggal = findViewById(R.id.etTanggal)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnBatal = findViewById(R.id.btnBatal)
    }

    private fun setupListeners() {
        btnUpdate.setOnClickListener { updateJadwal() }
        btnBatal.setOnClickListener { finish() }
        // PERUBAHAN: Listener untuk EditText tanggal dihapus karena tidak bisa diedit lagi
        // etTanggal.setOnClickListener { showDatePicker() }
    }

    private fun setupHariSpinner() {
        val hariArray = resources.getStringArray(R.array.hari_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hariArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHari.adapter = adapter
    }

    private fun ambilDataGuruDanSetUI() {
        db.collection("guru_piket").get().addOnSuccessListener { result ->
            guruList.clear()
            val namaGuruList = mutableListOf<String>()
            for (doc in result) {
                val guru = Guru(doc.id, doc.getString("nama") ?: "")
                guruList.add(guru)
                namaGuruList.add(guru.nama)
            }
            guruAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaGuruList)
            guruAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGuru.adapter = guruAdapter

            setDataAwalKeUI()

        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memuat daftar guru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setDataAwalKeUI() {
        // Set hari
        setSpinnerSelectionByValue(spinnerHari, jadwal.hari)

        // Set guru
        val guruIndex = guruList.indexOfFirst { it.id == jadwal.guruId }
        if (guruIndex != -1) {
            spinnerGuru.setSelection(guruIndex)
        }

        // Set tanggal
        etTanggal.setText(jadwal.tanggal)

        // PERUBAHAN: Nonaktifkan komponen dari kode untuk memastikan tidak bisa di-klik
        spinnerHari.isEnabled = false
        etTanggal.isEnabled = false
    }

    // PERUBAHAN: Fungsi showDatePicker() dihapus karena tidak lagi digunakan.

    private fun updateJadwal() {
        val selectedGuruPosition = spinnerGuru.selectedItemPosition
        if (selectedGuruPosition < 0 || selectedGuruPosition >= guruList.size) {
            Toast.makeText(this, "Pilih guru terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedGuru = guruList[selectedGuruPosition]

        val updatedData = hashMapOf(
            "guru_id" to updatedGuru.id
        )

        db.collection("jadwal_piket").document(jadwal.id)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui jadwal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setSpinnerSelectionByValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(i)
                break
            }
        }
    }
}
