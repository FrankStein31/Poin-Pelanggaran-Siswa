package fidya.ardani.la

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

//data class JadwalPiketTersimpan(val docId: String, val guruId: String)

class TambahJadwalPiketActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var mingguEditText: EditText
    private lateinit var jamEditText: EditText
    private lateinit var containerJadwalHarian: LinearLayout
    private lateinit var simpanButton: Button
    private lateinit var toolbar: MaterialToolbar

    // --- Data & Helpers ---
    private val db = FirebaseFirestore.getInstance()
    private val guruList = mutableListOf<Guru>()
    private lateinit var guruAdapter: ArrayAdapter<String>
    private val calendar = Calendar.getInstance()
    private val spinnerPerHari = mutableMapOf<String, Spinner>()
    private val tanggalPerHari = mutableMapOf<String, String>()
    private val jadwalTersimpanMap = mutableMapOf<String, JadwalPiketTersimpan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jadwal_piket)

        initViews()
        setupGuruSpinner()

        mingguEditText.setOnClickListener { showWeekPicker() }
        simpanButton.setOnClickListener { simpanJadwalHarian() }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.topAppBar)
        mingguEditText = findViewById(R.id.mingguEditText)
        jamEditText = findViewById(R.id.jamEditText)
        containerJadwalHarian = findViewById(R.id.container_jadwal_harian)
        simpanButton = findViewById(R.id.simpanButton)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupGuruSpinner() {
        db.collection("guru_piket").get().addOnSuccessListener { result ->
            guruList.clear()
            val namaGuruList = mutableListOf("Tidak Ada Piket") // Opsi default
            for (doc in result) {
                guruList.add(Guru(doc.id, doc.getString("nama") ?: ""))
                namaGuruList.add(doc.getString("nama") ?: "")
            }
            guruAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaGuruList)
            guruAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun showWeekPicker() {
        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            updateUIUntukMingguTerpilih()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun updateUIUntukMingguTerpilih() {
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // PERBAIKAN KUNCI: Gunakan dua format tanggal yang berbeda.
        val dateFormatDB = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatDisplay = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))

        // Tanggal untuk query ke database (Format: yyyy-MM-dd)
        val tanggalMulaiDB = dateFormatDB.format(calendar.time)

        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        val tanggalSelesaiDB = dateFormatDB.format(endCalendar.time)

        // Set teks di UI menggunakan format yang mudah dibaca
        mingguEditText.setText("${dateFormatDisplay.format(calendar.time)} - ${dateFormatDisplay.format(endCalendar.time)}")

        // Panggil fungsi ambil data dengan format tanggal yang benar untuk query
        ambilJadwalTersimpan(tanggalMulaiDB, tanggalSelesaiDB)
    }

    private fun ambilJadwalTersimpan(tanggalMulai: String, tanggalSelesai: String) {
        db.collection("jadwal_piket")
            .whereGreaterThanOrEqualTo("tanggal", tanggalMulai)
            .whereLessThanOrEqualTo("tanggal", tanggalSelesai)
            .get()
            .addOnSuccessListener { result ->
                jadwalTersimpanMap.clear()
                for (doc in result) {
                    val tanggal = doc.getString("tanggal") ?: continue
                    val guruId = doc.getString("guru_id") ?: ""
                    jadwalTersimpanMap[tanggal] = JadwalPiketTersimpan(doc.id, guruId)
                }
                generateWeeklyScheduleViews()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data jadwal yang ada.", Toast.LENGTH_SHORT).show()
                generateWeeklyScheduleViews()
            }
    }

    private fun generateWeeklyScheduleViews() {
        containerJadwalHarian.removeAllViews()
        spinnerPerHari.clear()
        tanggalPerHari.clear()

        if (!::guruAdapter.isInitialized) return

        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val inflater = LayoutInflater.from(this)
        val dateFormatDB = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatLabel = SimpleDateFormat(" (dd/MM)", Locale("id", "ID"))

        val loopCalendar = calendar.clone() as Calendar

        // Atur agar jam hanya diisi sekali
        var isJamPiketFilled = false

        for (day in days) {
            val rowView = inflater.inflate(R.layout.item_jadwal_harian, containerJadwalHarian, false)
            val tvNamaHari = rowView.findViewById<TextView>(R.id.tv_nama_hari)
            val spinner = rowView.findViewById<Spinner>(R.id.spinner_guru_harian)

            val tanggalSaatIni = dateFormatDB.format(loopCalendar.time)
            tvNamaHari.text = "$day${dateFormatLabel.format(loopCalendar.time)}"
            spinner.adapter = guruAdapter

            val jadwalHariIni = jadwalTersimpanMap[tanggalSaatIni]
            if (jadwalHariIni != null) {
                val guruIndex = guruList.indexOfFirst { it.id == jadwalHariIni.guruId }
                if (guruIndex != -1) {
                    spinner.setSelection(guruIndex + 1)
                }

                if (!isJamPiketFilled) {
                    db.collection("jadwal_piket").document(jadwalHariIni.docId).get().addOnSuccessListener {
                        jamEditText.setText(it.getString("jam"))
                    }
                    isJamPiketFilled = true
                }
            }

            spinnerPerHari[day] = spinner
            tanggalPerHari[day] = tanggalSaatIni
            containerJadwalHarian.addView(rowView)
            loopCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun simpanJadwalHarian() {
        val jam = jamEditText.text.toString().trim()
        if (jam.isEmpty()) {
            Toast.makeText(this, "Harap isi jam piket", Toast.LENGTH_SHORT).show()
            return
        }
        if (spinnerPerHari.isEmpty()) {
            Toast.makeText(this, "Silakan pilih minggu terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()

        spinnerPerHari.forEach { (hari, spinner) ->
            val selectedPosition = spinner.selectedItemPosition
            val tanggal = tanggalPerHari[hari]!!
            val jadwalLama = jadwalTersimpanMap[tanggal]

            if (selectedPosition > 0) {
                val guru = guruList[selectedPosition - 1]
                val data = hashMapOf("guru_id" to guru.id, "hari" to hari, "jam" to jam, "tanggal" to tanggal)

                if (jadwalLama != null) {
                    val jadwalRef = db.collection("jadwal_piket").document(jadwalLama.docId)
                    batch.update(jadwalRef, data as Map<String, Any>)
                } else {
                    val jadwalRef = db.collection("jadwal_piket").document()
                    batch.set(jadwalRef, data)
                }
            } else {
                if (jadwalLama != null) {
                    val jadwalRef = db.collection("jadwal_piket").document(jadwalLama.docId)
                    batch.delete(jadwalRef)
                }
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Jadwal mingguan berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan jadwal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Guru(val id: String, val nama: String)
}
