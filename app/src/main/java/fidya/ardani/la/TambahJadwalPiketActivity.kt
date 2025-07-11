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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import android.os.Parcel
import android.os.Parcelable

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

    // Validator custom: hanya tanggal >= hari ini dan tidak termasuk disabledDates
    class CustomDateValidator(
        private val todayMillis: Long,
        private val disabledDates: Set<Long>
    ) : CalendarConstraints.DateValidator {
        override fun isValid(date: Long): Boolean {
            val cal = Calendar.getInstance()
            cal.timeInMillis = date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return date >= todayMillis && !disabledDates.contains(cal.timeInMillis)
        }
        override fun writeToParcel(dest: Parcel, flags: Int) {}
        override fun describeContents(): Int = 0
        companion object CREATOR : Parcelable.Creator<CustomDateValidator> {
            override fun createFromParcel(parcel: Parcel): CustomDateValidator {
                // Tidak digunakan, karena validator ini tidak dipassing antar komponen
                return CustomDateValidator(0L, emptySet())
            }
            override fun newArray(size: Int): Array<CustomDateValidator?> = arrayOfNulls(size)
        }
    }

    private fun showWeekPicker() {
        val dateFormatDB = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMillis = today.timeInMillis

        db.collection("jadwal_piket")
            .get()
            .addOnSuccessListener { result ->
                val disabledDates = mutableSetOf<Long>()
                for (document in result) {
                    val tanggal = document.getString("tanggal")
                    if (tanggal != null) {
                        try {
                            val date = dateFormatDB.parse(tanggal)
                            if (date != null) {
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                disabledDates.add(cal.timeInMillis)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val validator = CustomDateValidator(todayMillis, disabledDates)
                val constraints = CalendarConstraints.Builder()
                    .setValidator(validator)
                    .build()

                val picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Pilih Tanggal Mulai Minggu")
                    .setSelection(todayMillis)
                    .setCalendarConstraints(constraints)
                    .build()

                picker.addOnPositiveButtonClickListener { selection ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = selection
                    // Set jam ke 00:00:00 lokal
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis = cal.timeInMillis
                    updateUIUntukMingguTerpilih()
                }

                picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data jadwal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUIUntukMingguTerpilih() {
        // Reset tampilan dan data sebelum ambil data baru
        jamEditText.setText("")
        jamEditText.isEnabled = true
        jadwalTersimpanMap.clear()
        spinnerPerHari.clear()
        tanggalPerHari.clear()
        containerJadwalHarian.removeAllViews()

        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val dateFormatDB = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatDisplay = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))

        val tanggalMulaiDB = dateFormatDB.format(calendar.time)
        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        val tanggalSelesaiDB = dateFormatDB.format(endCalendar.time)

        mingguEditText.setText("${dateFormatDisplay.format(calendar.time)} - ${dateFormatDisplay.format(endCalendar.time)}")

        ambilJadwalTersimpan(tanggalMulaiDB, tanggalSelesaiDB)
    }

    private fun ambilJadwalTersimpan(tanggalMulai: String, tanggalSelesai: String) {
        db.collection("jadwal_piket")
            .whereGreaterThanOrEqualTo("tanggal", tanggalMulai)
            .whereLessThanOrEqualTo("tanggal", tanggalSelesai)
            .get()
            .addOnSuccessListener { result ->
                jadwalTersimpanMap.clear()
                for (document in result) {
                    val tanggal = document.getString("tanggal") ?: continue
                    val guruId = document.getString("guru_id") ?: continue
                    jadwalTersimpanMap[tanggal] = JadwalPiketTersimpan(document.id, guruId)
                }
                generateWeeklyScheduleViews()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil jadwal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isJadwalExists(tanggal: String): Boolean {
        return jadwalTersimpanMap.containsKey(tanggal)
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

            // Cek apakah tanggal ini sudah ada jadwalnya
            if (isJadwalExists(tanggalSaatIni)) {
                val jadwalHariIni = jadwalTersimpanMap[tanggalSaatIni]
                if (jadwalHariIni != null) {
                    val guruIndex = guruList.indexOfFirst { it.id == jadwalHariIni.guruId }
                    if (guruIndex != -1) {
                        spinner.setSelection(guruIndex + 1)
                    }
                }
                // Nonaktifkan spinner jika sudah ada jadwal
                spinner.isEnabled = false
                tvNamaHari.setTextColor(resources.getColor(android.R.color.darker_gray))

                if (!isJamPiketFilled) {
                    db.collection("jadwal_piket").document(jadwalHariIni!!.docId).get()
                        .addOnSuccessListener {
                            jamEditText.setText(it.getString("jam"))
                            jamEditText.isEnabled = false
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
