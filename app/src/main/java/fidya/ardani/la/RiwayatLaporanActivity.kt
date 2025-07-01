package fidya.ardani.la

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var listViewRiwayatLaporan: ListView
    private lateinit var rvRingkasanPoin: RecyclerView
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var laporanList: MutableList<Laporan>
    private lateinit var ringkasanList: MutableList<RingkasanPoin>
    private lateinit var laporanAdapter: BaseAdapter
    private lateinit var ringkasanAdapter: RingkasanPoinAdapter
    private val db = FirebaseFirestore.getInstance()
    private var completedQueries = 0

    // Deklarasi variabel UI untuk filter
    private lateinit var spinnerKategori: Spinner
    private lateinit var spinnerKelas: Spinner
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnApplyFilter: Button
    private lateinit var btnResetFilter: Button

    // NEW: Deklarasi untuk grafik
    private lateinit var chartKategori: PieChart
    private lateinit var chartBulan: BarChart
    private lateinit var chartKelas: HorizontalBarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi list
        laporanList = mutableListOf()
        ringkasanList = mutableListOf()

        // Cek email dari intent (untuk siswa) atau langsung load semua data (untuk guru)
        val userEmail = intent.getStringExtra("USER_EMAIL")

        // Pilih layout berdasarkan role
        if (userEmail != null) {
            // Layout untuk siswa
            setContentView(R.layout.activity_riwayat_laporan)
            listViewRiwayatLaporan = findViewById(R.id.list_view_riwayat_laporan)

            // Setup toolbar jika ada
            findViewById<MaterialToolbar>(R.id.topAppBar)?.let { toolbar ->
                toolbar.setNavigationOnClickListener { finish() }
            }

            // Setup adapter khusus untuk siswa
            laporanAdapter = RiwayatSiswaAdapter(this, laporanList)
            listViewRiwayatLaporan.adapter = laporanAdapter

            loadRiwayatLaporanSiswa(userEmail)
        } else {
            // Layout untuk guru BK (sekarang dengan filter)
            setContentView(R.layout.activity_riwayat_laporan_siswa)

            // Inisialisasi UI
            listViewRiwayatLaporan = findViewById(R.id.listViewRiwayat)
            rvRingkasanPoin = findViewById(R.id.rv_ringkasan_poin)
            spinnerKategori = findViewById(R.id.spinner_kategori)
            spinnerKelas = findViewById(R.id.spinner_kelas)
            spinnerBulan = findViewById(R.id.spinner_bulan)
            spinnerTahun = findViewById(R.id.spinner_tahun)
            btnApplyFilter = findViewById(R.id.btn_apply_filter)
            btnResetFilter = findViewById(R.id.btn_reset_filter)

            chartKategori = findViewById(R.id.chart_kategori)
            chartBulan = findViewById(R.id.chart_bulan)
            chartKelas = findViewById(R.id.chart_kelas)

            findViewById<MaterialToolbar>(R.id.topAppBar)?.setNavigationOnClickListener { finish() }

            // Setup Adapters
            laporanAdapter = LaporanAdapter(this, laporanList)
            listViewRiwayatLaporan.adapter = laporanAdapter

            // Setup RecyclerView dan Filter
            setupRecyclerView()
            setupFilters() // Fungsi baru untuk setup filter

            // Inisialisasi grafik (jika diperlukan)
            setupCharts()

            // Load data awal
//            loadRingkasanPoinSiswa()
            loadRiwayatLaporan()
        }
    }

    private fun setupCharts() {
        chartKategori.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(android.graphics.Color.BLACK)
            legend.isEnabled = false
        }
        chartBulan.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
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

    private fun setupRecyclerView() {
        rvRingkasanPoin.layoutManager = LinearLayoutManager(this)
        ringkasanAdapter = RingkasanPoinAdapter(ringkasanList)
        rvRingkasanPoin.adapter = ringkasanAdapter
        rvRingkasanPoin.isNestedScrollingEnabled = false
    }

    private fun setupFilters() {
        // Isi data ke dalam spinner
        populateSpinnerKategori()
        populateSpinnerKelas()
        populateSpinnerBulanTahun()

        // Set listener untuk tombol
        btnApplyFilter.setOnClickListener {
            applyFiltersAndLoadData()
        }

        btnResetFilter.setOnClickListener {
            // Reset semua spinner ke posisi awal
            spinnerKategori.setSelection(0)
            spinnerKelas.setSelection(0)
            spinnerBulan.setSelection(0)
            spinnerTahun.setSelection(0)
            // Muat ulang semua data
            loadRiwayatLaporan()
        }
    }

    private fun populateSpinnerKategori() {
        db.collection("data_poin").get().addOnSuccessListener { documents ->
            val kategoriList = mutableListOf("Semua Kategori")
            for (doc in documents) {
                doc.getString("nama")?.let { kategoriList.add(it) }
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriList.distinct())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategori.adapter = adapter
        }
    }

    private fun populateSpinnerKelas() {
        db.collection("siswa").get().addOnSuccessListener { documents ->
            val kelasList = mutableListOf("Semua Kelas")
            for (doc in documents) {
                doc.getString("kelas")?.let { kelasList.add(it) }
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kelasList.distinct())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKelas.adapter = adapter
        }
    }

    private fun populateSpinnerBulanTahun() {
        // Bulan
        val bulanList = mutableListOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val bulanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bulanList)
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBulan.adapter = bulanAdapter

        // Tahun
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val tahunList = mutableListOf("Semua Tahun")
        for (i in 0..3) {
            tahunList.add((currentYear - i).toString())
        }
        val tahunAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tahunList)
        tahunAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTahun.adapter = tahunAdapter
    }

    private fun applyFiltersAndLoadData() {
        val selectedKategori = spinnerKategori.selectedItem.toString()
        val selectedKelas = spinnerKelas.selectedItem.toString()
        val selectedBulan = spinnerBulan.selectedItemPosition // 0 = Semua, 1 = Jan, dst.
        val selectedTahun = spinnerTahun.selectedItem.toString()

        // Membangun query secara dinamis
        var query: Query = db.collection("laporan_pelanggaran")

        if (selectedKategori != "Semua Kategori") {
            query = query.whereEqualTo("poin_pelanggaran", selectedKategori)
        }

        // Filter kelas memerlukan query tambahan untuk mendapatkan daftar NIS
        if (selectedKelas != "Semua Kelas") {
            db.collection("siswa").whereEqualTo("kelas", selectedKelas).get()
                .addOnSuccessListener { siswaDocs ->
                    if (siswaDocs.isEmpty) {
                        Toast.makeText(this, "Tidak ada siswa di kelas $selectedKelas", Toast.LENGTH_SHORT).show()
                        laporanList.clear()
                        laporanAdapter.notifyDataSetChanged()
                        return@addOnSuccessListener
                    }
                    val nisList = siswaDocs.map { it.getString("nis") }
                    query.whereIn("nis", nisList).get().addOnSuccessListener { result ->
                        processLaporanResult(result, selectedBulan, selectedTahun)
                    }
                }
        } else {
            // Jika tidak ada filter kelas, langsung eksekusi query
            query.get().addOnSuccessListener { result ->
                processLaporanResult(result, selectedBulan, selectedTahun)
            }
        }
    }

    private fun processLaporanResult(result: com.google.firebase.firestore.QuerySnapshot, filterBulan: Int, filterTahun: String) {
        val filteredLaporan = mutableListOf<Laporan>()

        // Langkah 1: Saring laporan berdasarkan tanggal (jika ada filter tanggal)
        result.forEach { document ->
            val tanggalString = document.getString("tanggal_pelanggaran") ?: ""
            var passFilter = true

            if (tanggalString.isNotBlank() && (filterBulan != 0 || filterTahun != "Semua Tahun")) {
                try {
                    val date = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).parse(tanggalString)
                    val cal = Calendar.getInstance()
                    cal.time = date
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
                    namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui",
                    kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui",
                    tanggalPelanggaran = tanggalString,
                    kelas = document.getString("kelas") ?: "Lainnya", // Pastikan mengambil data kelas
                    guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui",
                    fotoBukti = document.getString("foto_bukti")
                ))
            }
        }

        // Jika setelah filter tidak ada data, kosongkan semua list dan berhenti
        if (filteredLaporan.isEmpty()) {
            laporanList.clear()
            ringkasanList.clear()
            laporanAdapter.notifyDataSetChanged()
            ringkasanAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Tidak ada data yang cocok dengan filter.", Toast.LENGTH_SHORT).show()
            return
        }

        // Langkah 2: Ambil nilai poin untuk setiap laporan yang sudah difilter
        var completedQueries = 0
        for (laporan in filteredLaporan) {
            db.collection("data_poin")
                .whereEqualTo("nama", laporan.kategoriPelanggaran)
                .get()
                .addOnCompleteListener { task -> // Gunakan onCompleteListener agar counter selalu jalan
                    if (task.isSuccessful && task.result != null && !task.result.isEmpty) {
                        laporan.jumlahPoin = task.result.documents[0].getLong("jumlah")?.toInt() ?: 0
                    }

                    completedQueries++

                    // Langkah 3: Jika semua query poin selesai, baru update UI
                    if (completedQueries == filteredLaporan.size) {
                        filteredLaporan.sortByDescending { it.tanggalPelanggaran }

                        // Update daftar laporan utama
                        laporanList.clear()
                        laporanList.addAll(filteredLaporan)
                        laporanAdapter.notifyDataSetChanged()

                        // Update ringkasan poin berdasarkan data yang sudah difilter
                        updateRingkasanPoin(laporanList)
                        updateChartKategori(laporanList)
                        updateChartBulan(laporanList)
                        updateChartKelas(laporanList)
                    }
                }
        }
    }

    private fun updateChartKategori(laporan: List<Laporan>) {
        val entries = laporan.groupBy { it.kategoriPelanggaran }
            .mapValues { it.value.size }
            .map { PieEntry(it.value.toFloat(), it.key.take(20)) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.WHITE
        }
        chartKategori.data = PieData(dataSet)
        chartKategori.animateY(1000)
    }

    private fun updateChartBulan(laporan: List<Laporan>) {
        val monthNames = DateFormatSymbols().shortMonths
        val monthCounts = IntArray(12)
        laporan.forEach {
            try {
                val date = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).parse(it.tanggalPelanggaran)
                val cal = Calendar.getInstance()
                date?.let { d ->
                    cal.time = d
                    monthCounts[cal.get(Calendar.MONTH)]++
                }
            } catch (e: Exception) { /* abaikan */ }
        }

        val entries = monthCounts.mapIndexed { index, count -> BarEntry(index.toFloat(), count.toFloat()) }

        chartBulan.xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        chartBulan.xAxis.granularity = 1f

        val dataSet = BarDataSet(entries, "").apply {
            colors = ColorTemplate.VORDIPLOM_COLORS.toList()
            setDrawValues(true)
        }
        chartBulan.data = BarData(dataSet)
        chartBulan.animateY(1000)
    }

    private fun updateChartKelas(laporan: List<Laporan>) {
        val groupedByKelas = laporan.groupBy { it.kelas }
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        groupedByKelas.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.size.toFloat()))
            labels.add(entry.key)
        }

        chartKelas.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartKelas.xAxis.labelCount = labels.size

        val dataSet = BarDataSet(entries, "").apply {
            colors = ColorTemplate.PASTEL_COLORS.toList()
            setDrawValues(true)
        }
        chartKelas.data = BarData(dataSet)
        chartKelas.animateY(1000)
    }

    private fun updateRingkasanPoin(laporanDenganPoin: List<Laporan>) {
        ringkasanList.clear()

        // Kelompokkan laporan berdasarkan nama siswa, lalu jumlahkan poinnya
        val poinPerSiswa = laporanDenganPoin
            .groupBy { it.namaSiswa }
            .mapValues { entry ->
                entry.value.sumOf { it.jumlahPoin }
            }

        // Ubah hasil map menjadi list RingkasanPoin
        poinPerSiswa.forEach { (nama, totalPoin) ->
            if (totalPoin > 0) { // Hanya tampilkan siswa yang punya poin
                ringkasanList.add(RingkasanPoin(nama, totalPoin))
            }
        }

        // Urutkan dari poin tertinggi dan update adapter
        ringkasanList.sortByDescending { it.totalPoin }
        ringkasanAdapter.notifyDataSetChanged()
    }

    private fun loadRingkasanPoinSiswa() {
        db.collection("siswa").get()
            .addOnSuccessListener { siswaDocuments ->
                for (siswaDoc in siswaDocuments) {
                    val namaSiswa = siswaDoc.getString("nama") ?: continue
                    val nis = siswaDoc.getString("nis") ?: continue

                    // Hitung total poin untuk setiap siswa
                    db.collection("laporan_pelanggaran")
                        .whereEqualTo("nis", nis)
                        .get()
                        .addOnSuccessListener { laporanDocs ->
                            var totalPoin = 0
                            var processedReports = 0
                            val totalReports = laporanDocs.size()

                            for (laporanDoc in laporanDocs) {
                                val poinPelanggaran = laporanDoc.getString("poin_pelanggaran") ?: continue

                                db.collection("data_poin")
                                    .whereEqualTo("nama", poinPelanggaran)
                                    .get()
                                    .addOnSuccessListener { poinDocs ->
                                        if (!poinDocs.isEmpty) {
                                            val poin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                            totalPoin += poin
                                        }

                                        processedReports++
                                        if (processedReports == totalReports) {
                                            ringkasanList.add(RingkasanPoin(namaSiswa, totalPoin))
                                            ringkasanList.sortByDescending { it.totalPoin }
                                            ringkasanAdapter.notifyDataSetChanged()
                                        }
                                    }
                            }
                        }
                }
            }
    }

    private fun loadRiwayatLaporanSiswa(email: String) {
        // Cari NIS siswa berdasarkan email
        db.collection("siswa")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val siswa = documents.documents[0]
                    val nis = siswa.getString("nis") ?: return@addOnSuccessListener

                    // Ambil riwayat pelanggaran berdasarkan NIS
                    db.collection("laporan_pelanggaran")
                        .whereEqualTo("nis", nis)
                        .get()
                        .addOnSuccessListener { result ->
                            val tempList = mutableListOf<Laporan>()
                            completedQueries = 0

                            if (result.isEmpty) {
                                laporanList.clear()
                                laporanAdapter.notifyDataSetChanged()
                                return@addOnSuccessListener
                            }

                            for (document in result) {
                                val namaSiswa = document.getString("nama_siswa") ?: "Tidak Diketahui"
                                val kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                                val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                                val guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui"
                                val fotoBukti = document.getString("foto_bukti")

                                val laporan = Laporan(
                                    namaSiswa = namaSiswa,
                                    kategoriPelanggaran = kategoriPelanggaran,
                                    tanggalPelanggaran = tanggalPelanggaran,
                                    guruPiket = guruPiket,
                                    fotoBukti = fotoBukti
                                )
                                tempList.add(laporan)

                                // Ambil jumlah poin
                                db.collection("data_poin")
                                    .whereEqualTo("nama", kategoriPelanggaran)
                                    .get()
                                    .addOnSuccessListener { poinDocs ->
                                        if (!poinDocs.isEmpty) {
                                            val jumlahPoin = poinDocs.documents[0].getLong("jumlah")?.toInt() ?: 0
                                            laporan.jumlahPoin = jumlahPoin
                                        }

                                        completedQueries++
                                        // Setelah semua query selesai
                                        if (completedQueries == result.size()) {
                                            tempList.sortByDescending { it.tanggalPelanggaran }
                                            laporanList.clear()
                                            laporanList.addAll(tempList)
                                            laporanAdapter.notifyDataSetChanged()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        completedQueries++
                                        Toast.makeText(this, "Gagal mengambil data poin: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal mengambil riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRiwayatLaporan() {
        db.collection("laporan_pelanggaran").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    laporanList.clear()
                    ringkasanList.clear()
                    laporanAdapter.notifyDataSetChanged()
                    ringkasanAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }
                // Kirim semua hasil ke prosesor utama
                processLaporanResult(result, 0, "Semua Tahun")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Laporan(
        val namaSiswa: String,
        val kategoriPelanggaran: String,
        val tanggalPelanggaran: String,
        val kelas: String = "Lainnya",
        val guruPiket: String = "",
        val fotoBukti: String? = null,
        var jumlahPoin: Int = 0
    )

    data class RingkasanPoin(
        val namaSiswa: String,
        val totalPoin: Int
    )

    data class LaporanExcel(
        val namaSiswa: String,
        val nis: String,
        val guruPelapor: String,
        val kategoriPelanggaran: String,
        val keterangan: String,
        val jumlahPoin: Int,
        val tanggalPelanggaran: String,
        val sudahDiberiSP: Boolean,
        val fotoBukti: String
    )
}
