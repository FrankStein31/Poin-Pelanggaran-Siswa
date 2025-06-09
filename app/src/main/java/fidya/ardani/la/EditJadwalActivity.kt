package fidya.ardani.la

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity

class EditJadwalActivity : AppCompatActivity() {

    private lateinit var jadwal: JadwalPiketModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_jadwal)

        jadwal = intent.getParcelableExtra("JADWAL_DATA") ?: run {
            finish()
            return
        }

        val spinnerHari = findViewById<Spinner>(R.id.spinnerHari)
        val etGuru = findViewById<EditText>(R.id.etGuru)
        val etTanggal = findViewById<EditText>(R.id.etTanggal)
        val btnUpdate = findViewById<Button>(R.id.btnUpdate)
        val btnBatal = findViewById<Button>(R.id.btnBatal)

        // Set adapter spinner hari
        val hariArray = resources.getStringArray(R.array.hari_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hariArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHari.adapter = adapter

        // Set data ke UI
        setSpinnerSelection(spinnerHari, jadwal.hari)
        etGuru.setText(jadwal.guruNama)
        etTanggal.setText(jadwal.tanggal)

        btnUpdate.setOnClickListener {
            // Ambil data terbaru dari UI
            val updatedHari = spinnerHari.selectedItem.toString()
            val updatedGuruNama = etGuru.text.toString()
            val updatedTanggal = etTanggal.text.toString()

            // TODO: Update data di database pakai id jadwal.id dan data di atas

            finish()
        }

        btnBatal.setOnClickListener {
            finish()
        }
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(i)
                break
            }
        }
    }
}
