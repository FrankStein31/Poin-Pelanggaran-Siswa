package fidya.ardani.la

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import kotlin.math.ceil

class DataSiswaActivity : AppCompatActivity(), SiswaAdapter.SiswaAdapterListener {

    // Views
    private lateinit var listViewSiswa: ListView
    private lateinit var searchView: SearchView
    private lateinit var fabTambahSiswa: FloatingActionButton
    private lateinit var adapter: SiswaAdapter
    private lateinit var toolbar: MaterialToolbar

    // PENAMBAHAN: Instance Firebase Auth untuk membuat user baru
    private val auth = FirebaseAuth.getInstance()

    // Views untuk Paginasi
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var tvPageInfo: TextView
    private lateinit var tvRowCount: TextView

    // Firebase & Data Lists
    private val db = FirebaseFirestore.getInstance()
    private val semuaSiswa = mutableListOf<Siswa>() // Menyimpan semua data dari Firestore
    private val listHasilFilter = mutableListOf<Siswa>() // Menyimpan hasil filter sebelum di-paginate
    private val listTampil = mutableListOf<Siswa>() // Data yang benar-benar ditampilkan di adapter

    // Variabel State Paginasi
    private var currentPage = 1
    private val ITEMS_PER_PAGE = 10 // Ubah sesuai kebutuhan jumlah item per halaman

    // Konstanta untuk file picker
    private val FILE_PICKER_REQUEST_CODE = 123

    private var currentKelasFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_siswa)

        bindViews()

        // Adapter sekarang menggunakan 'listTampil'
        adapter = SiswaAdapter(this, listTampil, this)
        listViewSiswa.adapter = adapter

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        ambilDataSiswa()
    }

    private fun bukaFilePickerExcel() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // Hanya .xlsx
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Pilih File Excel"), FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Mengimpor data dari Excel...\nMohon tunggu...")
                    setCancelable(false)
                    show()
                }
                // Jalankan proses pembacaan dan upload di background
                lifecycleScope.launch(Dispatchers.IO) {
                    importSiswaFromExcel(uri, progressDialog)
                }
            }
        }
    }

    private suspend fun importSiswaFromExcel(uri: Uri, progressDialog: ProgressDialog) {
        var successCount = 0
        var failureCount = 0
        val failureDetails = mutableListOf<String>()

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Mulai dari baris kedua (indeks 1) untuk melewati header
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                // Asumsi urutan kolom: Nama, NIS, Email, Password, Kelas, Jurusan, Alamat, NoHP, NoHpOrtu
                val nama = row.getCell(0, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)?.stringCellValue ?: ""
                val nis = row.getCell(1)?.let { if (it.cellType == CellType.NUMERIC) it.numericCellValue.toLong().toString() else it.stringCellValue } ?: ""
                val email = row.getCell(2)?.stringCellValue ?: ""
                val password = row.getCell(3)?.stringCellValue ?: ""
                val kelas = row.getCell(4)?.stringCellValue ?: ""
                val jurusan = row.getCell(5)?.stringCellValue ?: ""
                val alamat = row.getCell(6)?.stringCellValue ?: ""
                val noHp = row.getCell(7)?.stringCellValue ?: ""
                val noHpOrtu = row.getCell(8)?.stringCellValue ?: ""

                if (nama.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    try {
                        // 1. Buat user di Firebase Auth
                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                        val uid = authResult.user?.uid ?: throw Exception("UID dari Auth null")

                        // 2. Siapkan data untuk Firestore
                        val siswaBaru = Siswa(uid, nama, nis, jurusan, kelas, alamat, email, password, noHp, noHpOrtu, "")

                        // 3. Simpan data ke Firestore dengan UID dari Auth
                        db.collection("siswa").document(uid).set(siswaBaru).await()
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                        failureDetails.add("Baris ${i + 1} ($nama): ${e.message}")
                    }
                }
            }
            workbook.close()
            inputStream?.close()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                showImportResultDialog("Error Membaca File", "Gagal membaca file Excel. Pastikan format file benar.\n\nError: ${e.message}")
            }
            return
        }

        withContext(Dispatchers.Main) {
            progressDialog.dismiss()
            val summaryMessage = "Berhasil: $successCount siswa\nGagal: $failureCount siswa\n\nDetail Kegagalan:\n${failureDetails.joinToString("\n")}"
            showImportResultDialog("Proses Impor Selesai", summaryMessage)
        }
    }

    private fun showImportResultDialog(title: String, message: String) {
        AlertDialog.Builder(this@DataSiswaActivity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                ambilDataSiswa() // Refresh data setelah dialog ditutup
            }
            .setCancelable(false)
            .show()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        searchView = findViewById(R.id.searchView)
        listViewSiswa = findViewById(R.id.listViewSiswa)
        fabTambahSiswa = findViewById(R.id.fabTambahSiswa)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        tvRowCount = findViewById(R.id.tvRowCount)
    }

    private fun setupListeners() {
        setupToolbar()
        setupSearchView()

        fabTambahSiswa.setOnClickListener {
            startActivity(Intent(this, TambahDataSiswaActivity::class.java))
        }

        listViewSiswa.setOnItemClickListener { _, _, position, _ ->
            val siswa = listTampil[position]
            showDetailDialog(siswa)
        }

        btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updateTampilanData()
            }
        }

        btnNext.setOnClickListener {
            val totalPages = ceil(listHasilFilter.size.toDouble() / ITEMS_PER_PAGE).toInt()
            if (currentPage < totalPages) {
                currentPage++
                updateTampilanData()
            }
        }
    }

    private fun setupToolbar() {
        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { onBackPressed() }

        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            // MODIFIKASI: Tambahkan case untuk action_import_excel
            when (item.itemId) {
                R.id.action_import_excel -> {
                    bukaFilePickerExcel()
                    true
                }
                R.id.filter_semua -> {
                    currentKelasFilter = null
                    currentPage = 1
                    filterDanTampilkanData(searchView.query.toString())
                    true
                }
                R.id.filter_kelas_x -> {
                    currentKelasFilter = "X"
                    currentPage = 1
                    filterDanTampilkanData(searchView.query.toString())
                    true
                }
                R.id.filter_kelas_xi -> {
                    currentKelasFilter = "XI"
                    currentPage = 1
                    filterDanTampilkanData(searchView.query.toString())
                    true
                }
                R.id.filter_kelas_xii -> {
                    currentKelasFilter = "XII"
                    currentPage = 1
                    filterDanTampilkanData(searchView.query.toString())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentPage = 1 // Reset ke halaman pertama setiap kali ada pencarian
                filterDanTampilkanData(newText)
                return true
            }
        })
    }

    private fun ambilDataSiswa() {
        db.collection("siswa")
            .get()
            .addOnSuccessListener { result ->
                semuaSiswa.clear()
                for (document in result) {
                    val siswa = document.toObject<Siswa>()
                    siswa.uid = document.id
                    semuaSiswa.add(siswa)
                }
                filterDanTampilkanData(searchView.query.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data siswa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterDanTampilkanData(query: String?) {
        listHasilFilter.clear()

        val hasilFilter = semuaSiswa.filter { siswa ->
            val cocokKelas = currentKelasFilter == null ||
                    siswa.kelas.startsWith("$currentKelasFilter ", ignoreCase = true) ||
                    siswa.kelas.equals(currentKelasFilter, ignoreCase = true)
            val cocokNama = query.isNullOrEmpty() || siswa.nama.contains(query, ignoreCase = true)
            cocokKelas && cocokNama
        }

        listHasilFilter.addAll(hasilFilter.sortedBy { it.nama })
        updateTampilanData()
    }

    private fun updateTampilanData() {
        listTampil.clear()

        val totalSiswa = listHasilFilter.size
        val startIndex = (currentPage - 1) * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, totalSiswa)

        if (startIndex < totalSiswa) {
            listTampil.addAll(listHasilFilter.subList(startIndex, endIndex))
        }

        adapter.notifyDataSetChanged()
        updatePaginationUI()
    }

    private fun updatePaginationUI() {
        val totalSiswaDiFilter = listHasilFilter.size
        val totalPages = if (totalSiswaDiFilter == 0) 1 else ceil(totalSiswaDiFilter.toDouble() / ITEMS_PER_PAGE).toInt()

        btnPrev.isEnabled = currentPage > 1
        btnNext.isEnabled = currentPage < totalPages

        tvPageInfo.text = "Halaman $currentPage dari $totalPages"

        val startIndex = (currentPage - 1) * ITEMS_PER_PAGE + 1
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE - 1, totalSiswaDiFilter)

        if (totalSiswaDiFilter == 0) {
            tvRowCount.text = "Tidak ada data siswa yang cocok"
        } else {
            tvRowCount.text = "Menampilkan $startIndex - $endIndex dari $totalSiswaDiFilter siswa"
        }
    }

    // -- Implementasi Listener dari Adapter --
    override fun onEditClicked(siswa: Siswa) {
        val intent = Intent(this, EditDataSiswaActivity::class.java)
        intent.putExtra("SISWA_ID", siswa.uid)
        startActivity(intent)
    }

    override fun onDeleteClicked(siswa: Siswa) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Apakah Anda yakin ingin menghapus data siswa bernama ${siswa.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("siswa").document(siswa.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                        ambilDataSiswa() // Ambil ulang data untuk konsistensi
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDetailDialog(siswa: Siswa) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_detail_siswa, null)
        builder.setView(view)

        // Bind views dari layout dialog
        val imgProfil = view.findViewById<ImageView>(R.id.detailImgProfil)
        val nama = view.findViewById<TextView>(R.id.detailNama)
        val nis = view.findViewById<TextView>(R.id.detailNis)
        val kelas = view.findViewById<TextView>(R.id.detailKelas)
        val jurusan = view.findViewById<TextView>(R.id.detailJurusan)
        val alamat = view.findViewById<TextView>(R.id.detailAlamat)
        val noHp = view.findViewById<TextView>(R.id.detailNoHp)
        val noHpOrtu = view.findViewById<TextView>(R.id.detailNoHpOrtu)
        val email = view.findViewById<TextView>(R.id.detailEmail)

        // Set data ke views
        nama.text = "Nama: ${siswa.nama}"
        nis.text = "NIS: ${siswa.nis}"
        kelas.text = "Kelas: ${siswa.kelas}"
        jurusan.text = "Jurusan: ${siswa.jurusan}"
        alamat.text = "Alamat: ${siswa.alamat}"
        noHp.text = "No. HP: ${siswa.noHp}"
        noHpOrtu.text = "No. HP Ortu: ${siswa.noHpOrtu}"
        email.text = "Email: ${siswa.email}"

        // Load gambar
        if (siswa.fotoProfilUrl.isNotEmpty()) {
            Glide.with(this).load(siswa.fotoProfilUrl).into(imgProfil)
        }

        builder.setPositiveButton("Tutup") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
