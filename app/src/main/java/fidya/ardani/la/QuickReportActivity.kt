package fidya.ardani.la

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.*

class QuickReportActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var edtTanggal: EditText
    private lateinit var edtJam: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var edtNamaSiswa: EditText
    private lateinit var edtKelas: EditText
    private lateinit var edtDeskripsi: EditText
    private lateinit var btnSubmit: Button

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_report)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        edtNamaSiswa = findViewById(R.id.edtNamaSiswa)
        edtKelas = findViewById(R.id.edtKelas)
        edtDeskripsi = findViewById(R.id.edtDeskripsi)
        btnSubmit = findViewById(R.id.btnSubmit)

        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Laporan Cepat"

        // Set up date picker dialog
        setCurrentDate()
        edtTanggal.setOnClickListener {
            showDatePicker()
        }

        // Set up time picker dialog
        setCurrentTime()
        edtJam.setOnClickListener {
            showTimePicker()
        }

        // Set up submit button
        btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitReport()
            }
        }

        // Populate spinner with categories (this would typically come from a database)
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("Pelanggaran Ringan", "Pelanggaran Sedang", "Pelanggaran Berat", "Lainnya")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = adapter
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        edtTanggal.setText(dateFormat.format(calendar.time))
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            setCurrentDate()
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setCurrentTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        edtJam.setText(timeFormat.format(calendar.time))
    }

    private fun showTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            setCurrentTime()
        }

        TimePickerDialog(
            this,
            timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validateForm(): Boolean {
        if (edtTanggal.text.toString().isEmpty()) {
            edtTanggal.error = "Tanggal tidak boleh kosong"
            return false
        }

        if (edtJam.text.toString().isEmpty()) {
            edtJam.error = "Jam tidak boleh kosong"
            return false
        }

        if (edtNamaSiswa.text.toString().isEmpty()) {
            edtNamaSiswa.error = "Nama siswa tidak boleh kosong"
            return false
        }

        if (edtKelas.text.toString().isEmpty()) {
            edtKelas.error = "Kelas tidak boleh kosong"
            return false
        }

        if (edtDeskripsi.text.toString().isEmpty()) {
            edtDeskripsi.error = "Deskripsi tidak boleh kosong"
            return false
        }

        return true
    }

    private fun submitReport() {
        // In a real app, this would save the report to a database
        // For now, just show a success message
        Toast.makeText(
            this,
            "Laporan berhasil disimpan",
            Toast.LENGTH_SHORT
        ).show()

        // Clear the form or finish the activity
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}