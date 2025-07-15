package fidya.ardani.la

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DetailPelanggaranActivity : AppCompatActivity() {

    private lateinit var tvJudulPelanggaran: TextView
    private lateinit var listViewPelanggaran: ListView
    private lateinit var btnCetakSP: Button
    private lateinit var adapter: DetailPelanggaranAdapter
    private val dataPelanggaran = ArrayList<RiwayatLaporanActivity.Laporan>()
    private val db = FirebaseFirestore.getInstance()
    private var completedQueries = 0
    private var currentNamaSiswa: String = ""
    private var currentNis: String = ""
    private var currentKelas: String = ""
    private var isSuratSudahDicetak = false // Flag untuk mengecek apakah surat sudah dicetak

    private lateinit var btnHubungiSiswa: Button
    private lateinit var btnHubungiOrtu: Button
    private var noHpSiswa: String = ""
    private var noHpOrtu: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pelanggaran)

        tvJudulPelanggaran = findViewById(R.id.tvJudulPelanggaran)
        listViewPelanggaran = findViewById(R.id.listViewPelanggaran)
        btnCetakSP = findViewById(R.id.btnCetakSP)

        btnHubungiSiswa = findViewById(R.id.btn_hubungi_siswa)
        btnHubungiOrtu = findViewById(R.id.btn_hubungi_ortu)

        val namaSiswa = intent.getStringExtra("nama_siswa")
        if (namaSiswa.isNullOrEmpty()) {
            Toast.makeText(this, "Nama siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentNamaSiswa = namaSiswa
        tvJudulPelanggaran.text = "Daftar Pelanggaran: $namaSiswa"
        adapter = DetailPelanggaranAdapter(this, dataPelanggaran)
        listViewPelanggaran.adapter = adapter

        // Setup tombol cetak
        btnCetakSP.setOnClickListener {
            cetakSuratPeringatan()
        }
        // Setup tombol hubungi siswa dan orang tua
        btnHubungiSiswa.setOnClickListener {
            showContactOptions(noHpSiswa, "Siswa ($currentNamaSiswa)")
        }
        btnHubungiOrtu.setOnClickListener {
            showContactOptions(noHpOrtu, "Orang Tua ($currentNamaSiswa)")
        }

        // Ambil data siswa terlebih dahulu untuk mendapatkan NIS dan kelas
        ambilDataSiswa(namaSiswa)

        // Cek status surat peringatan
        cekStatusSuratPeringatan(namaSiswa)
    }

    private fun cekStatusSuratPeringatan(namaSiswa: String) {
        db.collection("surat_peringatan")
            .whereEqualTo("nama_siswa", namaSiswa)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Surat sudah pernah dicetak - ambil data terakhir untuk ditampilkan
                    val dokumenTerakhir = documents.documents.last()
                    val tanggalCetak = dokumenTerakhir.getString("tanggal_cetak") ?: "Tidak diketahui"
                    val totalPoin = dokumenTerakhir.getLong("total_poin")?.toInt() ?: 0

                    isSuratSudahDicetak = true
                    updateButtonState()

                    // Tampilkan pop-up notifikasi
                    showSuratSudahDicetakDialog(namaSiswa, tanggalCetak, totalPoin)
                }
            }
            .addOnFailureListener {
                // Jika gagal mengecek, anggap belum dicetak
                isSuratSudahDicetak = false
            }
    }

    private fun showSuratSudahDicetakDialog(namaSiswa: String, tanggalCetak: String, totalPoin: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Informasi Surat Peringatan")
        builder.setMessage(
            "Surat peringatan untuk siswa \"$namaSiswa\" sudah pernah dicetak!\n\n" +
            "📅 Tanggal Cetak: $tanggalCetak\n" +
            "📊 Total Poin: $totalPoin\n\n" +
            "Tidak dapat mencetak surat peringatan yang sama untuk siswa ini."
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateButtonState() {
        val totalPoin = dataPelanggaran.sumOf { it.jumlahPoin }
        
        if (isSuratSudahDicetak) {
            btnCetakSP.isEnabled = false
            btnCetakSP.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            btnCetakSP.text = "Surat Sudah Dicetak"
            btnCetakSP.alpha = 0.5f
        } else if (totalPoin < 50) {
            btnCetakSP.isEnabled = false
            btnCetakSP.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            btnCetakSP.text = "Poin Belum Mencukupi ($totalPoin/50)"
            btnCetakSP.alpha = 0.5f
        } else {
            btnCetakSP.isEnabled = true
            btnCetakSP.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
            btnCetakSP.text = "Cetak Surat Peringatan"
            btnCetakSP.alpha = 1.0f
        }
    }

    private fun ambilDataSiswa(namaSiswa: String) {
        db.collection("siswa")
            .whereEqualTo("nama", namaSiswa)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val siswa = documents.documents[0]
                    currentNis = siswa.getString("nis") ?: ""
                    currentKelas = siswa.getString("kelas") ?: ""

                    noHpSiswa = siswa.getString("noHp") ?: ""
                    noHpOrtu = siswa.getString("noHpOrtu") ?: ""

                    // --- UPDATE TAMPILAN TOMBOL ---
                    updateContactButtonsState()
                }
                // Setelah mendapat data siswa, ambil data pelanggaran
                ambilDataPelanggaran(namaSiswa)
            }
            .addOnFailureListener {
                // Jika gagal ambil data siswa, tetap lanjutkan ambil pelanggaran
                ambilDataPelanggaran(namaSiswa)
            }
    }

    // Fungsi untuk menampilkan atau menyembunyikan tombol kontak
    private fun updateContactButtonsState() {
        if (noHpSiswa.isNotBlank()) {
            btnHubungiSiswa.visibility = View.VISIBLE
        } else {
            btnHubungiSiswa.visibility = View.GONE
        }

        if (noHpOrtu.isNotBlank()) {
            btnHubungiOrtu.visibility = View.VISIBLE
        } else {
            btnHubungiOrtu.visibility = View.GONE
        }
    }

    // Fungsi untuk menampilkan dialog pilihan (Telepon atau WhatsApp)
    private fun showContactOptions(phoneNumber: String, contactPerson: String) {
        val options = arrayOf("Telepon (Panggilan Biasa)", "Kirim Pesan WhatsApp")

        AlertDialog.Builder(this)
            .setTitle("Hubungi $contactPerson")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Panggilan Telepon
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        startActivity(intent)
                    }
                    1 -> { // Pesan WhatsApp
                        val formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber)
                        val message = if (contactPerson.contains("Siswa")) {
                            generateMessageForStudent()
                        } else {
                            generateMessageForParent()
                        }
                        val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(url)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Aplikasi WhatsApp tidak terpasang.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun generateMessageForStudent(): String {
        val totalPoin = dataPelanggaran.sumOf { it.jumlahPoin }
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return "Halo! Poin pelanggaran kamu telah mencapai $totalPoin poin. " +
               "Kamu akan mengikuti bimbingan konseling pada hari ini ($currentDate) " +
               "jam $currentTime di ruangan BK menemui guru BK."
    }

    private fun generateMessageForParent(): String {
        val totalPoin = dataPelanggaran.sumOf { it.jumlahPoin }
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return "Halo! Siswa bernama $currentNamaSiswa kelas $currentKelas telah mencapai $totalPoin poin pelanggaran. " +
               "Siswa akan mengikuti bimbingan konseling pada hari ini tanggal $currentDate jam $currentTime dengan guru BK."
    }

    // Fungsi helper untuk memformat nomor HP untuk WhatsApp (mengganti '0' dengan '62')
    private fun formatPhoneNumberForWhatsApp(number: String): String {
        val cleanNumber = number.replace(Regex("[^0-9]"), "") // Hapus karakter selain angka
        return if (cleanNumber.startsWith("0")) {
            "62" + cleanNumber.substring(1)
        } else if (cleanNumber.startsWith("62")) {
            cleanNumber
        } else {
            "62$cleanNumber" // Asumsikan nomor lokal tanpa 0
        }
    }

    private fun ambilDataPelanggaran(namaSiswa: String) {
        db.collection("laporan_pelanggaran")
            .whereEqualTo("nama_siswa", namaSiswa)
            .get()
            .addOnSuccessListener { result ->
                val tempList = mutableListOf<RiwayatLaporanActivity.Laporan>()
                completedQueries = 0

                if (result.isEmpty) {
                    dataPelanggaran.clear()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "Tidak ada pelanggaran tercatat.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val nama = document.getString("nama_siswa") ?: namaSiswa
                    val kategoriPelanggaran = document.getString("poin_pelanggaran") ?: "Tidak Diketahui"
                    val tanggalPelanggaran = document.getString("tanggal_pelanggaran") ?: "Tidak Diketahui"
                    val guruPiket = document.getString("guru_piket") ?: "Tidak Diketahui"
                    val fotoBukti = document.getString("foto_bukti")

                    val laporan = RiwayatLaporanActivity.Laporan(
                        namaSiswa = nama,
                        kategoriPelanggaran = kategoriPelanggaran,
                        tanggalPelanggaran = tanggalPelanggaran,
                        guruPiket = guruPiket,
                        fotoBukti = fotoBukti
                    )
                    tempList.add(laporan)

                    // Ambil jumlah poin dari collection data_poin berdasarkan nama pelanggaran
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
                                // Urutkan berdasarkan tanggal (terbaru dulu)
                                tempList.sortByDescending { it.tanggalPelanggaran }
                                dataPelanggaran.clear()
                                dataPelanggaran.addAll(tempList)
                                adapter.notifyDataSetChanged()
                                updateButtonState() // Update status tombol setelah data dimuat
                            }
                        }
                        .addOnFailureListener { e ->
                            completedQueries++
                            if (completedQueries == result.size()) {
                                tempList.sortByDescending { it.tanggalPelanggaran }
                                dataPelanggaran.clear()
                                dataPelanggaran.addAll(tempList)
                                adapter.notifyDataSetChanged()
                                updateButtonState() // Update status tombol setelah data dimuat
                            }
                            Toast.makeText(this, "Gagal mengambil data poin: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cetakSuratPeringatan() {
        if (isSuratSudahDicetak) {
            showKlikUlangDialog()
            return
        }

        if (currentNamaSiswa.isEmpty()) {
            Toast.makeText(this, "Data siswa tidak lengkap", Toast.LENGTH_SHORT).show()
            return
        }

        // Hitung total poin dari data yang sudah dimuat
        val totalPoin = dataPelanggaran.sumOf { it.jumlahPoin }

        if (totalPoin < 50) {
            showPoinTidakCukupDialog(totalPoin)
            return
        }

        if (totalPoin >= 50) {
            showKonfirmasiCetakDialog(totalPoin)
        }
    }

    private fun showKlikUlangDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Tidak Dapat Mencetak")
        builder.setMessage(
            "Surat peringatan untuk siswa \"$currentNamaSiswa\" sudah pernah dicetak sebelumnya!\n\n" +
            "Sistem tidak mengizinkan pencetakan surat peringatan yang duplikat untuk siswa yang sama."
        )
        builder.setPositiveButton("Mengerti") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()
    }

    private fun showPoinTidakCukupDialog(totalPoin: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Poin Tidak Mencukupi")
        builder.setMessage(
            "Total poin pelanggaran siswa \"$currentNamaSiswa\" adalah $totalPoin poin.\n\n" +
            "Untuk dapat mencetak surat peringatan, siswa harus memiliki minimal 50 poin pelanggaran.\n\n" +
            "Kekurangan: ${50 - totalPoin} poin"
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()
    }

    private fun showKonfirmasiCetakDialog(totalPoin: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📄 Konfirmasi Pencetakan")
        builder.setMessage(
            "Akan mencetak surat peringatan untuk:\n\n" +
            "👤 Nama: $currentNamaSiswa\n" +
            "🆔 NIS: $currentNis\n" +
            "🏫 Kelas: $currentKelas\n" +
            "📊 Total Poin: $totalPoin\n\n" +
            "Apakah Anda yakin ingin melanjutkan?"
        )
        builder.setPositiveButton("Ya, Cetak") { dialog, _ ->
            dialog.dismiss()
            generateAndShareSP(currentNamaSiswa, currentNis, currentKelas, totalPoin)
        }
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()
    }

    private fun generateAndShareSP(namaSiswa: String, nis: String, kelas: String, totalPoin: Int) {
        try {
            val pdfGenerator = PDFGenerator(this)
            val file = pdfGenerator.generateSuratPeringatan(namaSiswa, nis, kelas, totalPoin)

            // Share PDF menggunakan FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Simpan record surat peringatan ke Firestore
            simpanRecordSuratPeringatan(namaSiswa, nis, kelas, totalPoin)

            startActivity(intent)
        } catch (e: Exception) {
            showErrorDialog("Gagal membuat PDF: ${e.message}")
        }
    }

    private fun simpanRecordSuratPeringatan(namaSiswa: String, nis: String, kelas: String, totalPoin: Int) {
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val suratData = hashMapOf(
            "nama_siswa" to namaSiswa,
            "nis" to nis,
            "kelas" to kelas,
            "total_poin" to totalPoin,
            "tanggal_cetak" to currentDate,
            "status" to "dicetak",
            "status_notifikasi" to "belum_dibaca" // Field baru untuk tracking notifikasi
        )

        db.collection("surat_peringatan")
            .add(suratData)
            .addOnSuccessListener {
                // Update status button setelah berhasil menyimpan
                isSuratSudahDicetak = true
                updateButtonState()

                // Tampilkan dialog sukses
                showSuksesDialog(namaSiswa, totalPoin, currentDate)
            }
            .addOnFailureListener { e ->
                showErrorDialog("Gagal menyimpan record surat: ${e.message}")
            }
    }

    private fun showSuksesDialog(namaSiswa: String, totalPoin: Int, tanggalCetak: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("✅ Surat Berhasil Dicetak!")
        builder.setMessage(
            "Surat peringatan telah berhasil dicetak dan disimpan!\n\n" +
            "📋 Detail:\n" +
            "👤 Nama: $namaSiswa\n" +
            "📊 Total Poin: $totalPoin\n" +
            "📅 Tanggal Cetak: $tanggalCetak\n\n" +
            "⚠️ Siswa akan menerima notifikasi peringatan saat membuka aplikasi.\n\n" +
            "Surat peringatan tidak dapat dicetak ulang untuk siswa ini."
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Terjadi Kesalahan")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()
    }
}
