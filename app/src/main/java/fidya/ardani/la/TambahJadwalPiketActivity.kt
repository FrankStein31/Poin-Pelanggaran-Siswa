package fidya.ardani.la

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TambahJadwalPiketActivity : AppCompatActivity() {

    private lateinit var hariEditText: EditText
    private lateinit var jamEditText: EditText
    private lateinit var tanggalEditText: EditText
    private lateinit var guruSpinner: Spinner
    private lateinit var simpanButton: Button
    private lateinit var toolbar: MaterialToolbar

    private val db = FirebaseFirestore.getInstance()
    private val guruList = mutableListOf<Guru>()
    private lateinit var guruAdapter: ArrayAdapter<String>
    private val calendar = Calendar.getInstance()

    private var jadwalId: String? = null  // untuk mode edit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jadwal_piket)

        // Inisialisasi view
        toolbar = findViewById(R.id.topAppBar)
        hariEditText = findViewById(R.id.hariEditText)
        jamEditText = findViewById(R.id.jamEditText)
        tanggalEditText = findViewById(R.id.tanggalEditText)
        guruSpinner = findViewById(R.id.guruSpinner)
        simpanButton = findViewById(R.id.tambahJadwalButton)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        hariEditText.isEnabled = false

        tanggalEditText.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
                    tanggalEditText.setText(format.format(calendar.time))
                    hariEditText.setText(hariFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        setupGuruSpinner {
            isiDataJikaEdit()
        }

        simpanButton.setOnClickListener {
            val hari = hariEditText.text.toString()
            val jam = jamEditText.text.toString()
            val tanggal = tanggalEditText.text.toString()
            val selectedPosition = guruSpinner.selectedItemPosition

            if (selectedPosition < 0 || selectedPosition >= guruList.size) {
                Toast.makeText(this, "Pilih guru terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val guruId = guruList[selectedPosition].id

            if (hari.isNotEmpty() && jam.isNotEmpty() && tanggal.isNotEmpty()) {
                if (jadwalId != null) {
                    updateJadwalPiket(jadwalId!!, tanggal, hari, jam, guruId)
                } else {
                    tambahJadwalPiket(tanggal, hari, jam, guruId)
                }
            } else {
                Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGuruSpinner(onReady: () -> Unit) {
        guruAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        guruAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        guruSpinner.adapter = guruAdapter

        db.collection("guru_piket")
            .get()
            .addOnSuccessListener { result ->
                guruList.clear()
                val namaGuruList = mutableListOf<String>()
                for (doc in result) {
                    val id = doc.id
                    val nama = doc.getString("nama") ?: ""
                    val alamat = doc.getString("alamat") ?: ""
                    val email = doc.getString("email") ?: ""
                    val jadwalPiket = doc.getString("jadwalPiket") ?: ""
                    val nip = doc.getString("nip") ?: ""

                    guruList.add(Guru(id, nama, alamat, email, jadwalPiket, nip))
                    namaGuruList.add(nama)
                }
                guruAdapter.clear()
                guruAdapter.addAll(namaGuruList)
                guruAdapter.notifyDataSetChanged()
                onReady()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isiDataJikaEdit() {
        val extras = intent.extras ?: return

        jadwalId = extras.getString("jadwal_id")
        val tanggal = extras.getString("tanggal") ?: ""
        val hari = extras.getString("hari") ?: ""
        val jam = extras.getString("jam") ?: ""
        val guruId = extras.getString("guru_id") ?: ""

        tanggalEditText.setText(tanggal)
        hariEditText.setText(hari)
        jamEditText.setText(jam)

        val index = guruList.indexOfFirst { it.id == guruId }
        if (index >= 0) {
            guruSpinner.setSelection(index)
        }
    }

    private fun tambahJadwalPiket(tanggal: String, hari: String, jam: String, guruId: String) {
        val data = hashMapOf(
            "tanggal" to tanggal,
            "hari" to hari,
            "jam" to jam,
            "guru_id" to guruId
        )

        db.collection("jadwal_piket")
            .add(data)
            .addOnSuccessListener {
                val jadwalString = "$hari, $tanggal ($jam)"
                db.collection("guru_piket").document(guruId)
                    .update("jadwalPiket", jadwalString)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Jadwal berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengupdate jadwal di guru", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal tambah jadwal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateJadwalPiket(id: String, tanggal: String, hari: String, jam: String, guruId: String) {
        val data = mapOf(
            "tanggal" to tanggal,
            "hari" to hari,
            "jam" to jam,
            "guru_id" to guruId
        )

        db.collection("jadwal_piket").document(id)
            .set(data)
            .addOnSuccessListener {
                val jadwalString = "$hari, $tanggal ($jam)"
                db.collection("guru_piket").document(guruId)
                    .update("jadwalPiket", jadwalString)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Jadwal berhasil diupdate", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengupdate jadwal di guru", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal update jadwal", Toast.LENGTH_SHORT).show()
            }
    }
}
