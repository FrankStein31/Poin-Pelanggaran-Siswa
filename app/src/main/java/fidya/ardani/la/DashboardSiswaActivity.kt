package fidya.ardani.la

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardSiswaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var imgProfile: ImageView
    private lateinit var txtTotalPoin: TextView
    private lateinit var txtPeringatan: TextView
    private lateinit var cardDataPoinSaya: CardView
    private lateinit var cardRiwayat: CardView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables untuk data siswa saat ini
    private var currentStudentNIS: String = ""
    private var currentStudentName: String = ""
    private var currentViolationPoints: Int = 0
    private var currentWarningDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_siswa)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setClickListeners()

        // Debug collections terlebih dahulu
        debugFirestoreCollections()

        loadUserData()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.imgProfile -> {
                android.util.Log.d("Dashboard", "Profile image diklik")
                showProfileOptions()
            }

            R.id.cardDataPoinSaya -> {
                android.util.Log.d("Dashboard", "Card Data Poin diklik")
                val intent = Intent(this, DataPoinSiswaActivity::class.java)
                startActivity(intent)
            }

            R.id.cardRiwayat -> {
                android.util.Log.d("Dashboard", "Card Riwayat diklik")
                showRiwayatPelanggaran()
            }
        }
    }

    private fun initViews() {
        try {
            imgProfile = findViewById(R.id.imgProfile)
            txtTotalPoin = findViewById(R.id.txtTotalPoin)
            txtPeringatan = findViewById(R.id.txtPeringatan)
            cardDataPoinSaya = findViewById(R.id.cardDataPoinSaya)
            cardRiwayat = findViewById(R.id.cardRiwayat)

            // Set TextView sebagai clickable dengan background untuk memberikan feedback visual
            txtPeringatan.isClickable = true
            txtPeringatan.isFocusable = true
            txtPeringatan.background = resources.getDrawable(android.R.drawable.list_selector_background, null)

            txtTotalPoin.isClickable = true
            txtTotalPoin.isFocusable = true
            txtTotalPoin.background = resources.getDrawable(android.R.drawable.list_selector_background, null)

            android.util.Log.d("Dashboard", "All views initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error initializing views: ${e.message}")
        }
    }

    private fun setClickListeners() {
        try {
            imgProfile.setOnClickListener(this)
            cardDataPoinSaya.setOnClickListener(this)
            cardRiwayat.setOnClickListener(this)

            // Set click listener untuk peringatan
            txtPeringatan.setOnClickListener { view ->
                android.util.Log.d("Dashboard", "txtPeringatan diklik melalui lambda!")
                showDetailPelanggaran()
            }

            // Set click listener untuk total poin
            txtTotalPoin.setOnClickListener { view ->
                android.util.Log.d("Dashboard", "txtTotalPoin diklik!")
                val intent = Intent(this, DataPoinSiswaActivity::class.java)
                startActivity(intent)
            }

            android.util.Log.d("Dashboard", "All click listeners set successfully")
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error setting click listeners: ${e.message}")
        }
    }

    // Method untuk mendapatkan nama siswa saat ini
    private fun getCurrentStudentName(): String {
        return currentStudentName.ifEmpty { "Tidak diketahui" }
    }

    // Method untuk mendapatkan NIS siswa saat ini
    private fun getCurrentStudentNIS(): String {
        return currentStudentNIS.ifEmpty { "Tidak diketahui" }
    }

    // Method untuk mendapatkan total poin pelanggaran saat ini
    private fun getCurrentViolationPoints(): Int {
        return currentViolationPoints
    }

    // Method untuk mendapatkan tanggal peringatan saat ini
    private fun getCurrentWarningDate(): String {
        return currentWarningDate.ifEmpty { "Tidak ada" }
    }

    private fun showProfileOptions() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("👤 PROFIL SISWA")
            builder.setMessage(
                """
            👤 Nama: ${getCurrentStudentName()}
            🆔 NIS: ${getCurrentStudentNIS()}
            📧 Email: ${auth.currentUser?.email ?: "Tidak diketahui"}
            📊 Total Poin Pelanggaran: ${getCurrentViolationPoints()}
        """.trimIndent()
            )

            builder.setPositiveButton("Logout") { _, _ ->
                showLogoutConfirmation()
            }

            builder.setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error showing profile options: ${e.message}")
            showErrorDialog("Gagal menampilkan profil: ${e.message}")
        }
    }

    private fun showLogoutConfirmation() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("🚪 LOGOUT")
            builder.setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")

            builder.setPositiveButton("Ya, Logout") { _, _ ->
                try {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    android.util.Log.e("Dashboard", "Error during logout: ${e.message}")
                    showErrorDialog("Gagal logout: ${e.message}")
                }
            }

            builder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error showing logout confirmation: ${e.message}")
        }
    }

    private fun loadUserData() {
        try {
            val currentUser = auth.currentUser
            android.util.Log.d("Dashboard", "Current user: ${currentUser?.email}")

            currentUser?.let { user ->
                db.collection("siswa")
                    .whereEqualTo("email", user.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        try {
                            android.util.Log.d("Dashboard", "Dokumen siswa ditemukan: ${documents.size()}")

                            if (!documents.isEmpty) {
                                val siswa = documents.documents[0]
                                val nis = siswa.getString("nis")
                                val namaSiswa = siswa.getString("nama")

                                android.util.Log.d("Dashboard", "Data siswa - NIS: $nis, Nama: $namaSiswa")

                                if (nis != null && namaSiswa != null) {
                                    currentStudentNIS = nis
                                    currentStudentName = namaSiswa

                                    hitungTotalPoinDenganNotifikasi(nis, namaSiswa)
                                    cekSuratPeringatanBaru(namaSiswa, nis)
                                } else {
                                    android.util.Log.e("Dashboard", "NIS atau nama siswa null")
                                    showErrorDialog("Data siswa tidak lengkap")
                                }
                            } else {
                                android.util.Log.e("Dashboard", "Tidak ada dokumen siswa dengan email: ${user.email}")
                                showErrorDialog("Data siswa tidak ditemukan")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Dashboard", "Error processing siswa data: ${e.message}")
                            showErrorDialog("Error memproses data siswa: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("Dashboard", "Error mengambil data siswa: ${e.message}")
                        showErrorDialog("Gagal mengambil data siswa: ${e.message}")
                    }
            } ?: run {
                android.util.Log.e("Dashboard", "Current user is null")
                showErrorDialog("User tidak ditemukan. Silakan login ulang.")
            }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in loadUserData: ${e.message}")
            showErrorDialog("Error memuat data user: ${e.message}")
        }
    }

    private fun cekSuratPeringatanBaru(namaSiswa: String, nis: String) {
        try {
            android.util.Log.d("Dashboard", "Mulai cek surat peringatan untuk: $namaSiswa")

            // Cek dengan surat_teguran (nama collection yang benar berdasarkan kode)
            db.collection("surat_teguran")
                .whereEqualTo("nama_siswa", namaSiswa)
                .orderBy("tanggal_cetak", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    android.util.Log.d("Dashboard", "Dokumen surat teguran dengan nama $namaSiswa: ${documents.size()}")

                    if (!documents.isEmpty) {
                        val dokumen = documents.documents[0]
                        val totalPoin = dokumen.getLong("total_poin")?.toInt() ?: 0
                        val tanggalCetak = dokumen.getString("tanggal_cetak") ?: ""
                        val documentId = dokumen.id

                        android.util.Log.d("Dashboard", "Surat teguran ditemukan - totalPoin: $totalPoin, tanggal: $tanggalCetak")
                        currentWarningDate = tanggalCetak
                        showNotifikasiPeringatan(namaSiswa, totalPoin, tanggalCetak, documentId)
                    } else {
                        android.util.Log.d("Dashboard", "Tidak ada surat teguran dengan nama, coba dengan NIS")
                        cekSuratPeringatanDenganNIS(namaSiswa, nis)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error query surat_teguran dengan nama: ${e.message}")
                    cekSuratPeringatanDenganNIS(namaSiswa, nis)
                }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in cekSuratPeringatanBaru: ${e.message}")
        }
    }

    private fun cekSuratPeringatanDenganNIS(namaSiswa: String, nis: String) {
        try {
            android.util.Log.d("Dashboard", "Cek surat teguran dengan NIS: $nis")

            db.collection("surat_teguran")
                .whereEqualTo("nis", nis)
                .orderBy("tanggal_cetak", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    android.util.Log.d("Dashboard", "Dokumen surat teguran dengan NIS $nis: ${documents.size()}")

                    if (!documents.isEmpty) {
                        val dokumen = documents.documents[0]
                        val totalPoin = dokumen.getLong("total_poin")?.toInt() ?: 0
                        val tanggalCetak = dokumen.getString("tanggal_cetak") ?: ""
                        val documentId = dokumen.id

                        android.util.Log.d("Dashboard", "Surat teguran ditemukan dengan NIS - totalPoin: $totalPoin")
                        currentWarningDate = tanggalCetak
                        showNotifikasiPeringatan(namaSiswa, totalPoin, tanggalCetak, documentId)
                    } else {
                        android.util.Log.d("Dashboard", "Tidak ada surat teguran untuk NIS: $nis")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error query surat teguran dengan NIS: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in cekSuratPeringatanDenganNIS: ${e.message}")
        }
    }

    private fun showNotifikasiPeringatan(namaSiswa: String, totalPoin: Int, tanggalCetak: String, documentId: String) {
        try {
            android.util.Log.d("Dashboard", "Menampilkan notifikasi peringatan untuk $namaSiswa dengan poin $totalPoin")

            currentViolationPoints = totalPoin

            val levelPeringatan = when {
                totalPoin >= 100 -> "TINGKAT 3 - SANGAT SERIUS"
                totalPoin >= 50 -> "TINGKAT 2 - SERIUS"
                else -> "TINGKAT 1 - PERINGATAN"
            }

            val pesanPeringatan = when {
                totalPoin >= 100 -> "Anda telah melakukan pelanggaran yang sangat serius! Segera temui guru BK untuk konsultasi lebih lanjut."
                totalPoin >= 50 -> "Anda telah menerima surat peringatan resmi. Harap memperbaiki perilaku dan menaati peraturan sekolah."
                else -> "Anda telah menerima peringatan. Mohon untuk lebih memperhatikan kedisiplinan."
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("🚨 TEGURAN RESMI")
            builder.setMessage(
                "📋 SURAT TEGURAN AKTIF\n\n" +
                        "👤 Nama: $namaSiswa\n" +
                        "📊 Total Poin Pelanggaran: $totalPoin\n" +
                        "⚠️ Level: $levelPeringatan\n" +
                        "📅 Tanggal Surat: $tanggalCetak\n\n" +
                        "📝 Pesan:\n$pesanPeringatan\n\n" +
                        "⚠️ PENTING: Surat Teguran ini masih berlaku dan akan terus ditampilkan setiap login hingga Anda menyelesaikan masalah ini dengan guru BK."
            )

            builder.setPositiveButton("Saya Mengerti") { dialog, _ ->
                dialog.dismiss()
            }

//            builder.setNegativeButton("Hubungi BK") { dialog, _ ->
//                dialog.dismiss()
//                tampilkanInfoKontakBK()
//            }

            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()

            android.util.Log.d("Dashboard", "Dialog notifikasi ditampilkan")
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error showing notification: ${e.message}")
        }
    }

    private fun tampilkanInfoKontakBK() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("📞 KONTAK BIMBINGAN KONSELING")
            builder.setMessage(
                """
            Untuk berkonsultasi mengenai surat peringatan, Anda dapat menghubungi:
            
            👩‍🏫 Ibu Sari Dewi, S.Pd
            📱 WhatsApp: 081259183393
            🏢 Ruang BK: Lantai 2, Ruang 201
            
            ⏰ Jam Konsultasi:
            Senin - Jumat: 08.00 - 15.00
            Sabtu: 08.00 - 12.00
            
            💡 Tips:
            - Buat janji terlebih dahulu
            - Siapkan pertanyaan yang ingin disampaikan
            - Bawa kartu pelajar sebagai identitas
            """.trimIndent()
            )

            builder.setPositiveButton("Hubungi WhatsApp") { _, _ ->
                hubungiBKViaWhatsApp()
            }

            builder.setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error showing contact info: ${e.message}")
            showErrorDialog("Gagal menampilkan info kontak: ${e.message}")
        }
    }
private fun hubungiBKViaWhatsApp() {
    try {
        val nomorBK = "6281259183393"
        val pesanOtomatis = "Halo Bu/Pak BK, saya ${getCurrentStudentName()} (NIS: ${getCurrentStudentNIS()}) " +
                "ingin berkonsultasi mengenai surat peringatan yang saya terima. " +
                "Total poin pelanggaran saya saat ini: ${getCurrentViolationPoints()}."

        val url = "https://wa.me/$nomorBK?text=${Uri.encode(pesanOtomatis)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent) // langsung jalankan tanpa cek resolveActivity()
    } catch (e: Exception) {
        android.util.Log.e("Dashboard", "Error opening WhatsApp: ${e.message}")
        Toast.makeText(this, "Gagal membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


    private fun hitungTotalPoinDenganNotifikasi(nis: String, namaSiswa: String) {
        try {
            android.util.Log.d("Dashboard", "Mulai hitung total poin untuk NIS: $nis")

            db.collection("laporan_pelanggaran")
                .whereEqualTo("nis", nis)
                .get()
                .addOnSuccessListener { laporanDocuments ->
                    try {
                        android.util.Log.d("Dashboard", "Laporan pelanggaran ditemukan: ${laporanDocuments.size()}")

                        var totalPoin = 0
                        var completedQueries = 0
                        val totalQueries = laporanDocuments.size()

                        if (totalQueries == 0) {
                            runOnUiThread {
                                txtTotalPoin.text = "0"
                                txtPeringatan.text = "0"
                            }
                            currentViolationPoints = 0
                            android.util.Log.d("Dashboard", "Tidak ada laporan pelanggaran")
                            return@addOnSuccessListener
                        }

                        for (doc in laporanDocuments) {
                            val poinPelanggaran = doc.getString("poin_pelanggaran") ?: ""
                            android.util.Log.d("Dashboard", "Processing pelanggaran: $poinPelanggaran")

                            db.collection("data_poin")
                                .whereEqualTo("nama", poinPelanggaran)
                                .get()
                                .addOnSuccessListener { poinDocuments ->
                                    try {
                                        if (!poinDocuments.isEmpty) {
                                            val poinDoc = poinDocuments.documents[0]
                                            val jumlahPoin = poinDoc.getLong("jumlah")?.toInt() ?: 0
                                            totalPoin += jumlahPoin
                                            android.util.Log.d("Dashboard", "Poin $poinPelanggaran: $jumlahPoin, Total: $totalPoin")
                                        }

                                        completedQueries++
                                        if (completedQueries == totalQueries) {
                                            runOnUiThread {
                                                txtTotalPoin.text = totalPoin.toString()
                                                currentViolationPoints = totalPoin
                                                val peringatan = when {
                                                    totalPoin >= 100 -> "3"
                                                    totalPoin >= 50 -> "2"
                                                    totalPoin >= 25 -> "1"
                                                    else -> "0"
                                                }
                                                txtPeringatan.text = peringatan
                                            }

                                            android.util.Log.d("Dashboard", "Total poin final: $totalPoin")

                                            if (totalPoin >= 25) {
                                                buatSuratPeringatanOtomatis(nis, namaSiswa, totalPoin)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Dashboard", "Error in success callback: ${e.message}")
                                        completedQueries++
                                    }
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("Dashboard", "Error get data_poin untuk $poinPelanggaran: ${e.message}")
                                    completedQueries++
                                    if (completedQueries == totalQueries) {
                                        runOnUiThread {
                                            txtTotalPoin.text = totalPoin.toString()
                                            currentViolationPoints = totalPoin
                                            val peringatan = when {
                                                totalPoin >= 100 -> "3"
                                                totalPoin >= 50 -> "2"
                                                totalPoin >= 25 -> "1"
                                                else -> "0"
                                            }
                                            txtPeringatan.text = peringatan
                                        }

                                        if (totalPoin >= 25) {
                                            buatSuratPeringatanOtomatis(nis, namaSiswa, totalPoin)
                                        }
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Dashboard", "Error processing laporan documents: ${e.message}")
                        runOnUiThread {
                            txtTotalPoin.text = "Error"
                            txtPeringatan.text = "Error"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error get laporan_pelanggaran: ${e.message}")
                    runOnUiThread {
                        txtTotalPoin.text = "Error"
                        txtPeringatan.text = "Error"
                    }
                    currentViolationPoints = 0
                }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in hitungTotalPoinDenganNotifikasi: ${e.message}")
        }
    }

    private fun buatSuratPeringatanOtomatis(nis: String, namaSiswa: String, totalPoin: Int) {
        try {
            android.util.Log.d("Dashboard", "Cek apakah perlu buat surat teguran otomatis")

            db.collection("surat_teguran")
                .whereEqualTo("nis", nis)
                .whereEqualTo("total_poin", totalPoin.toLong())
                .get()
                .addOnSuccessListener { existingDocuments ->
                    try {
                        if (existingDocuments.isEmpty) {
                            val tanggalSekarang = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            val suratPeringatan = hashMapOf(
                                "nis" to nis,
                                "nama_siswa" to namaSiswa,
                                "total_poin" to totalPoin.toLong(),
                                "tanggal_cetak" to tanggalSekarang,
                                "status_notifikasi" to "belum_dibaca"
                            )

                            db.collection("surat_teguran")
                                .add(suratPeringatan)
                                .addOnSuccessListener { documentReference ->
                                    android.util.Log.d("Dashboard", "Surat teguran otomatis dibuat: ${documentReference.id}")
                                    currentWarningDate = tanggalSekarang
                                    showNotifikasiPeringatan(namaSiswa, totalPoin, tanggalSekarang, documentReference.id)
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("Dashboard", "Error membuat surat teguran otomatis: ${e.message}")
                                }
                        } else {
                            android.util.Log.d("Dashboard", "Surat Teguran sudah ada untuk total poin: $totalPoin")
                            val dokumen = existingDocuments.documents[0]
                            val tanggalCetak = dokumen.getString("tanggal_cetak") ?: ""
                            currentWarningDate = tanggalCetak
                            showNotifikasiPeringatan(namaSiswa, totalPoin, tanggalCetak, dokumen.id)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Dashboard", "Error in success callback buatSuratPeringatanOtomatis: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error cek existing surat teguran: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in buatSuratPeringatanOtomatis: ${e.message}")
        }
    }

    private fun debugFirestoreCollections() {
        try {
            android.util.Log.d("Debug", "=== DEBUGGING FIRESTORE COLLECTIONS ===")

            db.collection("siswa").limit(5).get().addOnSuccessListener { documents ->
                android.util.Log.d("Debug", "Collection siswa: ${documents.size()} documents")
                for (doc in documents) {
                    android.util.Log.d("Debug", "Siswa doc: ${doc.data}")
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("Debug", "Error siswa collection: ${e.message}")
            }

            db.collection("surat_teguran").limit(5).get().addOnSuccessListener { documents ->
                android.util.Log.d("Debug", "Collection surat_teguran: ${documents.size()} documents")
                for (doc in documents) {
                    android.util.Log.d("Debug", "Surat teguran doc: ${doc.data}")
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("Debug", "Error surat_teguran collection: ${e.message}")
            }

            db.collection("laporan_pelanggaran").limit(5).get().addOnSuccessListener { documents ->
                android.util.Log.d("Debug", "Collection laporan_pelanggaran: ${documents.size()} documents")
                for (doc in documents) {
                    android.util.Log.d("Debug", "Laporan pelanggaran doc: ${doc.data}")
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("Debug", "Error laporan_pelanggaran collection: ${e.message}")
            }

            db.collection("data_poin").limit(5).get().addOnSuccessListener { documents ->
                android.util.Log.d("Debug", "Collection data_poin: ${documents.size()} documents")
                for (doc in documents) {
                    android.util.Log.d("Debug", "Data poin doc: ${doc.data}")
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("Debug", "Error data_poin collection: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("Debug", "Error in debugFirestoreCollections: ${e.message}")
        }
    }

    private fun showDetailPelanggaran() {
        android.util.Log.d("Dashboard", "showDetailPelanggaran() dipanggil")
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            android.util.Log.d("Dashboard", "User email: ${user.email}")
            db.collection("siswa")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    android.util.Log.d("Dashboard", "Siswa documents found: ${documents.size()}")
                    if (!documents.isEmpty) {
                        val siswa = documents.documents[0]
                        val nis = siswa.getString("nis") ?: return@addOnSuccessListener
                        android.util.Log.d("Dashboard", "NIS found: $nis")

                        getDetailPelanggaranSiswa(nis)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error getting siswa: ${e.message}")
                    showErrorDialog("Gagal memuat data siswa: ${e.message}")
                }
        } ?: run {
            android.util.Log.e("Dashboard", "Current user is null")
            showErrorDialog("User tidak ditemukan. Silakan login ulang.")
        }
    }
    private fun getDetailPelanggaranSiswa(nis: String) {
        db.collection("laporan_pelanggaran")
            .whereEqualTo("nis", nis)
            .get()
            .addOnSuccessListener { laporanDocuments ->
                if (laporanDocuments.isEmpty) {
                    showNoViolationDialog()
                    return@addOnSuccessListener
                }

                val detailList = mutableListOf<String>()
                var processedCount = 0
                val totalCount = laporanDocuments.size()

                for (doc in laporanDocuments) {
                    val poinPelanggaran = doc.getString("poin_pelanggaran") ?: ""
                    val guruPelapor = doc.getString("guru_piket") ?: "Tidak diketahui"
                    val tanggal = doc.getString("tanggal_pelanggaran") ?: "Tidak diketahui"

                    db.collection("data_poin")
                        .whereEqualTo("nama", poinPelanggaran)
                        .get()
                        .addOnSuccessListener { poinDocuments ->
                            var jumlahPoin = 0
                            if (!poinDocuments.isEmpty) {
                                val poinDoc = poinDocuments.documents[0]
                                jumlahPoin = poinDoc.getLong("jumlah")?.toInt() ?: 0
                            }

                            val detail = """
                            📋 Pelanggaran: $poinPelanggaran
                            📊 Poin: $jumlahPoin
                            👨‍🏫 Guru Pelapor: $guruPelapor
                            📅 Tanggal: $tanggal
                       
                            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        """.trimIndent()

                            detailList.add(detail)
                            processedCount++

                            android.util.Log.d(
                                "Dashboard",
                                "Processed $processedCount of $totalCount violations"
                            )

                            // Jika semua data sudah diproses, tampilkan dialog
                            if (processedCount == totalCount) {
                                showDetailPelanggaranDialog(detailList)
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e(
                                "Dashboard",
                                "Error getting data_poin for $poinPelanggaran: ${e.message}"
                            )

                            // Tetap tambahkan detail meski gagal mendapat poin
                            val detail = """
                            📋 Pelanggaran: $poinPelanggaran
                            📊 Poin: Error (tidak dapat memuat)
                            👨‍🏫 Guru Pelapor: $guruPelapor
                            📅 Tanggal: $tanggal
                        
                            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        """.trimIndent()

                            detailList.add(detail)
                            processedCount++

                            // Jika semua data sudah diproses, tampilkan dialog
                            if (processedCount == totalCount) {
                                showDetailPelanggaranDialog(detailList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Dashboard", "Error getting laporan_pelanggaran: ${e.message}")
                showErrorDialog("Gagal memuat data pelanggaran: ${e.message}")
            }
    }

    private fun showDetailPelanggaranDialog(detailList: List<String>) {
        val totalPoin = calculateTotalPoints(detailList)
        val jumlahPelanggaran = detailList.size

        val builder = AlertDialog.Builder(this)
        builder.setTitle("📋 DETAIL PELANGGARAN ANDA")

        val message = buildString {
            append("👤 Nama: ${getCurrentStudentName()}\n")
            append("🆔 NIS: ${getCurrentStudentNIS()}\n")
            append("📊 Total Poin: $totalPoin\n")
            append("📈 Jumlah Pelanggaran: $jumlahPelanggaran\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

            append("📝 RINCIAN PELANGGARAN:\n\n")

            detailList.forEachIndexed { index, detail ->
                append("${index + 1}. $detail\n")
            }

            append("\n⚠️ CATATAN:\n")
            when {
                totalPoin >= 200 -> append("Status: PERINGATAN TINGKAT 3 - SANGAT SERIUS\nSegera temui guru BK untuk konsultasi!")
                totalPoin >= 100 -> append("Status: PERINGATAN TINGKAT 2 - SERIUS\nHarap memperbaiki perilaku dan menaati peraturan sekolah.")
                totalPoin >= 50 -> append("Status: PERINGATAN TINGKAT 1\nMohon untuk lebih memperhatikan kedisiplinan.")
                else -> append("Status: DALAM BATAS NORMAL\nTetap jaga kedisiplinan dan perilaku yang baik.")
            }
        }

        builder.setMessage(message)

        builder.setPositiveButton("Tutup") { dialog, _ ->
            dialog.dismiss()
        }

        // Tambahkan tombol "Hubungi BK" jika poin >= 25
//        if (totalPoin >= 50) {
//            builder.setNegativeButton("Hubungi BK") { dialog, _ ->
//                dialog.dismiss()
//                tampilkanInfoKontakBK()
//            }
//        }

        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()

        android.util.Log.d(
            "Dashboard",
            "Detail pelanggaran dialog shown with $jumlahPelanggaran violations and $totalPoin points"
        )
    }

    private fun showNoViolationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("✅ TIDAK ADA PELANGGARAN")
        builder.setMessage(
            """
        🎉 Selamat!
        
        👤 ${getCurrentStudentName()}
        🆔 NIS: ${getCurrentStudentNIS()}
        
        Anda belum memiliki catatan pelanggaran apapun.
        Pertahankan kedisiplinan dan perilaku yang baik!
        
        💡 Tips:
        • Selalu datang tepat waktu
        • Patuhi tata tertib sekolah
        • Hormati guru dan teman
        • Jaga kebersihan lingkungan
    """.trimIndent()
        )

        builder.setPositiveButton("Terima Kasih") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun calculateTotalPoints(detailList: List<String>): Int {
        var total = 0
        for (detail in detailList) {
            // Extract poin dari string detail
            val poinPattern = "📊 Poin: (\\d+)".toRegex()
            val matchResult = poinPattern.find(detail)
            if (matchResult != null) {
                total += matchResult.groupValues[1].toIntOrNull() ?: 0
            }
        }
        return total
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Error")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    // Tambahkan method ini di dalam class DashboardSiswaActivity
    private fun showRiwayatPelanggaran() {
        android.util.Log.d("Dashboard", "showRiwayatPelanggaran() dipanggil")

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("siswa")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val siswa = documents.documents[0]
                        val nis = siswa.getString("nis") ?: return@addOnSuccessListener
                        val namaSiswa = siswa.getString("nama") ?: return@addOnSuccessListener

                        getRiwayatPelanggaranSiswa(nis, namaSiswa)
                    } else {
                        showErrorDialog("Data siswa tidak ditemukan")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Dashboard", "Error getting siswa: ${e.message}")
                    showErrorDialog("Gagal memuat data siswa: ${e.message}")
                }
        } ?: showErrorDialog("User tidak ditemukan. Silakan login ulang.")
    }

    private fun getRiwayatPelanggaranSiswa(nis: String, namaSiswa: String) {
    android.util.Log.d("Dashboard", "Mengambil riwayat pelanggaran untuk NIS: $nis")

    // Remove orderBy to avoid index requirement
    db.collection("laporan_pelanggaran")
        .whereEqualTo("nis", nis)
        .get()
        .addOnSuccessListener { laporanDocuments ->
            if (laporanDocuments.isEmpty) {
                showNoHistoryDialog(namaSiswa)
                return@addOnSuccessListener
            }

            val riwayatList = mutableListOf<RiwayatPelanggaran>()
            var processedCount = 0
            val totalCount = laporanDocuments.size()

            for (doc in laporanDocuments) {
                val poinPelanggaran = doc.getString("poin_pelanggaran") ?: ""
                val guruPelapor = doc.getString("guru_piket") ?: "Tidak diketahui"
                val tanggal = doc.getString("tanggal_pelanggaran") ?: "Tidak diketahui"

                // Get point value from data_poin collection
                db.collection("data_poin")
                    .whereEqualTo("nama", poinPelanggaran)
                    .get()
                    .addOnSuccessListener { poinDocuments ->
                        var jumlahPoin = 0
                        if (!poinDocuments.isEmpty) {
                            val poinDoc = poinDocuments.documents[0]
                            jumlahPoin = poinDoc.getLong("jumlah")?.toInt() ?: 0
                        }

                        val riwayat = RiwayatPelanggaran(
                            pelanggaran = poinPelanggaran,
                            poin = jumlahPoin,
                            guruPelapor = guruPelapor,
                            tanggal = tanggal
                        )

                        riwayatList.add(riwayat)
                        processedCount++

                        // If all data processed, sort by date and show dialog
                        if (processedCount == totalCount) {
                            // Sort by date in descending order (newest first)
                            val sortedList = riwayatList.sortedByDescending {
                                try {
                                    // Try to parse the date for proper sorting
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.tanggal)
                                } catch (e: Exception) {
                                    // If date parsing fails, use current date
                                    Date()
                                }
                            }
                            showRiwayatDialog(namaSiswa, nis, sortedList)
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("Dashboard", "Error getting data_poin: ${e.message}")

                        // Still add the record even if point retrieval fails
                        val riwayat = RiwayatPelanggaran(
                            pelanggaran = poinPelanggaran,
                            poin = 0,
                            guruPelapor = guruPelapor,
                            tanggal = tanggal
                        )

                        riwayatList.add(riwayat)
                        processedCount++

                        if (processedCount == totalCount) {
                            // Sort by date in descending order
                            val sortedList = riwayatList.sortedByDescending {
                                try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.tanggal)
                                } catch (e: Exception) {
                                    Date()
                                }
                            }
                            showRiwayatDialog(namaSiswa, nis, sortedList)
                        }
                    }
            }
        }
        .addOnFailureListener { e ->
            android.util.Log.e("Dashboard", "Error getting riwayat pelanggaran: ${e.message}")
            showErrorDialog("Gagal memuat riwayat pelanggaran: ${e.message}")
        }
}
    private fun showRiwayatDialog(
        namaSiswa: String,
        nis: String,
        riwayatList: List<RiwayatPelanggaran>
    ) {
        val totalPoin = riwayatList.sumOf { it.poin }
        val jumlahPelanggaran = riwayatList.size

        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 RIWAYAT PELANGGARAN")

        val message = buildString {
            append("👤 Nama: $namaSiswa\n")
            append("🆔 NIS: $nis\n")
            append("📊 Total Poin: $totalPoin\n")
            append("📈 Jumlah Pelanggaran: $jumlahPelanggaran\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

            append("📝 RIWAYAT PELANGGARAN (Terbaru ke Terlama):\n\n")

            riwayatList.forEachIndexed { index, riwayat ->
                append("${index + 1}. 📋 ${riwayat.pelanggaran}\n")
                append("   📊 Poin: ${riwayat.poin}\n")
                append("   👨‍🏫 Guru: ${riwayat.guruPelapor}\n")
                append("   📅 Tanggal: ${riwayat.tanggal}\n")
                append("   ────────────────────────────────────\n")
            }

            append("\n⚠️ STATUS SAAT INI:\n")
            when {
                totalPoin >= 200 -> append("🔴 PERINGATAN TINGKAT 3 - SANGAT SERIUS")
                totalPoin >= 100 -> append("🟠 PERINGATAN TINGKAT 2 - SERIUS")
                totalPoin >= 50 -> append("🟡 PERINGATAN TINGKAT 1")
                else -> append("🟢 DALAM BATAS NORMAL")
            }
        }

        builder.setMessage(message)

        builder.setPositiveButton("Tutup") { dialog, _ ->
            dialog.dismiss()
        }

        // Tambahkan tombol "Detail Lengkap" untuk melihat detail per pelanggaran
        builder.setNeutralButton("Detail Lengkap") { dialog, _ ->
            dialog.dismiss()

            // PERUBAHAN: Buka Activity baru dan kirim data siswa
            val intent = Intent(this, RiwayatDetailActivity::class.java)
            intent.putExtra("STUDENT_NIS", nis)
            intent.putExtra("STUDENT_NAME", namaSiswa)
            startActivity(intent)
        }

        // Tambahkan tombol "Hubungi BK" jika poin >= 25
//        if (totalPoin >= 50) {
//            builder.setNegativeButton("Hubungi BK") { dialog, _ ->
//                dialog.dismiss()
//                tampilkanInfoKontakBK()
//            }
//        }

        builder.setCancelable(true)

        val dialog = builder.create()
        dialog.show()

        android.util.Log.d("Dashboard", "Riwayat dialog shown with $jumlahPelanggaran violations")
    }

    private fun showNoHistoryDialog(namaSiswa: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("✅ TIDAK ADA RIWAYAT")
        builder.setMessage(
            """
        🎉 Excellent!
        
        👤 $namaSiswa
        🆔 NIS: ${getCurrentStudentNIS()}
        
        Anda tidak memiliki riwayat pelanggaran apapun.
        Pertahankan kedisiplinan dan prestasi yang baik!
        
        💡 Tetap jaga:
        • Kedisiplinan waktu
        • Etika dan sopan santun
        • Kebersihan lingkungan
        • Prestasi akademik
    """.trimIndent()
        )

        builder.setPositiveButton("Terima Kasih") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    // Data class untuk menyimpan riwayat pelanggaran
    data class RiwayatPelanggaran(
        val pelanggaran: String,
        val poin: Int,
        val guruPelapor: String,
        val tanggal: String
    )
}
