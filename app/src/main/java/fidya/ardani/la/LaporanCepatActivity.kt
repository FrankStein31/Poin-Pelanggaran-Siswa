package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class LaporanCepatActivity : AppCompatActivity() {

    // --- Komponen UI Lama & Baru ---
    private lateinit var fabTambahLaporan: FloatingActionButton
    private lateinit var listViewLaporan: ListView
    private lateinit var spinnerKategori: Spinner
    private lateinit var spinnerKelas: Spinner
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnApplyFilter: Button
    private lateinit var btnResetFilter: Button
    private lateinit var chartKategori: PieChart
    private lateinit var chartBulan: BarChart
    private lateinit var chartKelas: HorizontalBarChart

    // --- Data & Adapters ---
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var laporanAdapter: LaporanAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_cepat)

        val namaGuru = intent.getStringExtra("nama_guru") ?: ""

        initViews()
        setupToolbar()
        setupListeners(namaGuru)
        setupCharts()
        setupFilters()

        loadLaporanAwal()
    }

    private fun initViews() {
        fabTambahLaporan = findViewById(R.id.btn_tambah_laporan)
        listViewLaporan = findViewById(R.id.list_view_laporan)
        spinnerKategori = findViewById(R.id.spinner_kategori)
        spinnerKelas = findViewById(R.id.spinner_kelas)
        spinnerBulan = findViewById(R.id.spinner_bulan)
        spinnerTahun = findViewById(R.id.spinner_tahun)
        btnApplyFilter = findViewById(R.id.btn_apply_filter)
        btnResetFilter = findViewById(R.id.btn_reset_filter)
        chartKategori = findViewById(R.id.chart_kategori)
        chartBulan = findViewById(R.id.chart_bulan)
        chartKelas = findViewById(R.id.chart_kelas)

        laporanList = mutableListOf()
        laporanAdapter = LaporanAdapter(this, laporanList)
        listViewLaporan.adapter = laporanAdapter
    }

    private fun setupToolbar() {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupListeners(namaGuru: String) {
        fabTambahLaporan.setOnClickListener {
            val intent = Intent(this, TambahLaporanCepatActivity::class.java)
            intent.putExtra("nama_guru", namaGuru)
            startActivity(intent)
        }

        btnApplyFilter.setOnClickListener { applyFiltersAndLoadData() }

        btnResetFilter.setOnClickListener {
            spinnerKategori.setSelection(0)
            spinnerKelas.setSelection(0)
            spinnerBulan.setSelection(0)
            spinnerTahun.setSelection(0)
            loadLaporanAwal()
        }
    }

    private fun setupCharts() {
        // Pengaturan Chart Kategori (PieChart)
        chartKategori.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(android.graphics.Color.BLACK)
            legend.isEnabled = false
        }

        // Pengaturan Chart Bulan (BarChart)
        chartBulan.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
        }

        // Pengaturan Chart Kelas (HorizontalBarChart)
        chartKelas.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun setupFilters() {
        // Mengisi data ke spinner dari Firestore atau data statis
        populateSpinnerWithHint(spinnerKategori, "data_poin", "nama", "Semua Kategori")
        populateSpinnerWithHint(spinnerKelas, "laporan_pelanggaran", "kelas", "Semua Kelas")
        populateSpinnerBulanTahun()
    }

    private fun populateSpinnerWithHint(spinner: Spinner, collection: String, field: String, hint: String) {
        db.collection(collection).get().addOnSuccessListener { documents ->
            val items = mutableListOf(hint)
            documents.mapNotNullTo(items) { it.getString(field) }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items.distinct())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun populateSpinnerBulanTahun() {
        val bulanList = mutableListOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        spinnerBulan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bulanList)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val tahunList = (0..3).map { (currentYear - it).toString() }.toMutableList().apply { add(0, "Semua Tahun") }
        spinnerTahun.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tahunList)
    }

    private fun loadLaporanAwal() {
        db.collection("laporan_pelanggaran").get()
            .addOnSuccessListener { result -> processLaporanResult(result, 0, "Semua Tahun") }
            .addOnFailureListener { e -> Toast.makeText(this, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun applyFiltersAndLoadData() {
        val selectedKategori = spinnerKategori.selectedItem.toString()
        val selectedKelas = spinnerKelas.selectedItem.toString()
        val selectedBulan = spinnerBulan.selectedItemPosition
        val selectedTahun = spinnerTahun.selectedItem.toString()

        var query: Query = db.collection("laporan_pelanggaran")

        if (selectedKategori != "Semua Kategori") {
            query = query.whereEqualTo("poin_pelanggaran", selectedKategori)
        }
        if (selectedKelas != "Semua Kelas") {
            query = query.whereEqualTo("kelas", selectedKelas)
        }

        query.get().addOnSuccessListener { result ->
            processLaporanResult(result, selectedBulan, selectedTahun)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal menerapkan filter: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processLaporanResult(result: QuerySnapshot, filterBulan: Int, filterTahun: String) {
        val filteredLaporan = mutableListOf<Laporan>()
        val dateFormat = SimpleDateFormat("d-M-yyyy", Locale.getDefault())

        for (document in result) {
            val tanggalString = document.getString("tanggal_pelanggaran") ?: ""
            var passFilter = true

            if (tanggalString.isNotBlank() && (filterBulan != 0 || filterTahun != "Semua Tahun")) {
                try {
                    val date = dateFormat.parse(tanggalString)
                    val cal = Calendar.getInstance().apply { time = date!! }
                    val bulanLaporan = cal.get(Calendar.MONTH) + 1
                    val tahunLaporan = cal.get(Calendar.YEAR).toString()

                    if (filterBulan != 0 && bulanLaporan != filterBulan) passFilter = false
                    if (filterTahun != "Semua Tahun" && tahunLaporan != filterTahun) passFilter = false
                } catch (e: Exception) {
                    passFilter = false
                }
            }

            if (passFilter) {
                filteredLaporan.add(Laporan(
                    namaSiswa = document.getString("nama_siswa") ?: "N/A",
                    nis = document.getString("nis") ?: "N/A",
                    jurusan = document.getString("jurusan") ?: "N/A",
                    kelas = document.getString("kelas") ?: "N/A",
                    poinPelanggaran = document.getString("poin_pelanggaran") ?: "N/A",
                    tanggalPelanggaran = tanggalString,
                    guruPiket = document.getString("guru_piket") ?: "N/A",
                    fotoBukti = document.getString("foto_bukti")
                ))
            }
        }

        laporanList.clear()
        laporanList.addAll(filteredLaporan.sortedByDescending { it.tanggalPelanggaran })
        laporanAdapter.notifyDataSetChanged()

        if (laporanList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data yang cocok dengan filter.", Toast.LENGTH_SHORT).show()
        }

        // Update semua chart
        updateChartKategori(laporanList)
        updateChartBulan(laporanList)
        updateChartKelas(laporanList)
    }

    private fun updateChartKategori(laporan: List<Laporan>) {
        val entries = laporan.groupBy { it.poinPelanggaran }
            .map { PieEntry(it.value.size.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Kategori Pelanggaran").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.WHITE
        }
        chartKategori.data = PieData(dataSet)
        chartKategori.invalidate() // refresh chart
        chartKategori.animateY(1000)
    }

    private fun updateChartBulan(laporan: List<Laporan>) {
        val monthNames = DateFormatSymbols.getInstance(Locale("id", "ID")).shortMonths
        val monthCounts = IntArray(12)
        val dateFormat = SimpleDateFormat("d-M-yyyy", Locale.getDefault())
        laporan.forEach {
            try {
                dateFormat.parse(it.tanggalPelanggaran)?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    monthCounts[cal.get(Calendar.MONTH)]++
                }
            } catch (e: Exception) { /* abaikan tanggal invalid */ }
        }
        val entries = monthCounts.mapIndexed { index, count -> BarEntry(index.toFloat(), count.toFloat()) }
        chartBulan.xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        chartBulan.xAxis.granularity = 1f
        val dataSet = BarDataSet(entries, "Laporan per Bulan").apply {
            colors = ColorTemplate.VORDIPLOM_COLORS.toList()
        }
        chartBulan.data = BarData(dataSet)
        chartBulan.invalidate()
        chartBulan.animateY(1000)
    }

    private fun updateChartKelas(laporan: List<Laporan>) {
        val groupedByKelas = laporan.groupBy { it.kelas }
        val labels = groupedByKelas.keys.toList()
        val entries = labels.mapIndexed { index, s -> BarEntry(index.toFloat(), groupedByKelas[s]?.size?.toFloat() ?: 0f) }
        chartKelas.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartKelas.xAxis.labelCount = labels.size
        val dataSet = BarDataSet(entries, "Pelanggaran per Kelas").apply {
            colors = ColorTemplate.PASTEL_COLORS.toList()
        }
        chartKelas.data = BarData(dataSet)
        chartKelas.invalidate()
        chartKelas.animateY(1000)
    }

    // Adapter dan Data Class tetap sama seperti kode asli Anda
    class LaporanAdapter(private val context: AppCompatActivity, private val laporanList: List<Laporan>) : BaseAdapter() {
        override fun getCount(): Int = laporanList.size
        override fun getItem(position: Int): Any = laporanList[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_laporan, parent, false)
            val laporan = laporanList[position]
            val tvNamaSiswa = view.findViewById<TextView>(R.id.tv_nama_siswa)
            val tvNis = view.findViewById<TextView>(R.id.tv_nis)
            val tvJurusanKelas = view.findViewById<TextView>(R.id.tv_jurusan_kelas)
            val tvPoinPelanggaran = view.findViewById<TextView>(R.id.tv_poin_pelanggaran)
            val tvTanggalPelanggaran = view.findViewById<TextView>(R.id.tv_tanggal_pelanggaran)
            val tvGuruPiket = view.findViewById<TextView>(R.id.tv_guru_piket)
            val imgBuktiPelanggaran = view.findViewById<ImageView>(R.id.img_bukti_pelanggaran)
            tvNamaSiswa.text = laporan.namaSiswa
            tvNis.text = "NIS: ${laporan.nis}"
            tvJurusanKelas.text = "Jurusan: ${laporan.jurusan} | Kelas: ${laporan.kelas}"
            tvPoinPelanggaran.text = "Pelanggaran: ${laporan.poinPelanggaran}"
            tvTanggalPelanggaran.text = "Tanggal: ${laporan.tanggalPelanggaran}"
            tvGuruPiket.text = "Guru Piket: ${laporan.guruPiket}"
            imgBuktiPelanggaran?.let {
                if (!laporan.fotoBukti.isNullOrEmpty()) {
                    it.visibility = View.VISIBLE
                    Glide.with(context).load(laporan.fotoBukti).placeholder(R.drawable.ic_menu_gallery).error(R.drawable.ic_menu_report_image).into(it)
                } else {
                    it.visibility = View.GONE
                }
            }
            return view
        }
    }

    data class Laporan(
        val namaSiswa: String, val nis: String, val jurusan: String, val kelas: String,
        val poinPelanggaran: String, val tanggalPelanggaran: String, val guruPiket: String,
        val fotoBukti: String? = null
    )
}
