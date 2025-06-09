package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView

class DashboardGuruBkActivity : AppCompatActivity() {

    private lateinit var imgMenu: CircleImageView
    private lateinit var txtWelcome: TextView
    private lateinit var cardDataSiswa: CardView
    private lateinit var cardDataJurusan: CardView
    private lateinit var cardDataKelas: CardView
    private lateinit var cardJadwalPiket: CardView
    private lateinit var cardDataPoin: CardView
    private lateinit var cardRiwayatPelanggaran: CardView
    private lateinit var cardDaftarGuru: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_guru_bk)

        // Initialize views
        initializeViews()

        // Set welcome message with user's name (can be fetched from shared prefs or intent)
        setWelcomeMessage()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        // Profile image in header
        imgMenu = findViewById(R.id.imgMenu)

        // Menu cards
        cardDataSiswa = findViewById(R.id.cardDataSiswa)
        cardDataJurusan = findViewById(R.id.cardDataJurusan)
        cardDataKelas = findViewById(R.id.cardDataKelas)
        cardJadwalPiket = findViewById(R.id.cardJadwalPiket)
        cardDataPoin = findViewById(R.id.cardDataPoin)
        cardRiwayatPelanggaran = findViewById(R.id.cardRiwayatPelanggaran)
        cardDaftarGuru = findViewById(R.id.cardDaftarGuru)
    }

    private fun setWelcomeMessage() {
        // Get user name from shared preferences or login info
        val userName = getUserName()
        if (userName.isNotEmpty()) {
            txtWelcome.text = "Selamat Datang, $userName!"
        }
    }

    private fun getUserName(): String {
        // TODO: Implement method to get user name from SharedPreferences or other source
        // This is a placeholder implementation
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("user_name", "") ?: ""
    }

    private fun setupClickListeners() {
        // Profile menu click
        imgMenu.setOnClickListener {
            // TODO: Show profile options menu or navigate to profile screen
            Toast.makeText(this, "Menu Profil", Toast.LENGTH_SHORT).show()
            // Example: startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Card click listeners
        cardDataSiswa.setOnClickListener {
            navigateToActivity("DataSiswaActivity")
        }

        cardDataJurusan.setOnClickListener {
            navigateToActivity("DataJurusanActivity")
        }

        cardDataKelas.setOnClickListener {
            navigateToActivity("DataKelasActivity")
        }

        cardJadwalPiket.setOnClickListener {
            navigateToActivity("JadwalPiketActivity")
        }

        cardDataPoin.setOnClickListener {
            navigateToActivity("DataPoinActivity")
        }

        cardRiwayatPelanggaran.setOnClickListener {
            navigateToActivity("RiwayatLaporanActivity")
        }

        cardDaftarGuru.setOnClickListener {
            navigateToActivity("DataGuruPiketActivity")
        }
    }

    private fun navigateToActivity(activityName: String) {
        try {
            // Dynamically create the class from the string name
            val className = "fidya.ardani.la.$activityName" // Change this to your actual package name
            val activityClass = Class.forName(className)
            val intent = Intent(this, activityClass)
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            // If the activity hasn't been created yet
            Toast.makeText(this, "Halaman $activityName belum tersedia", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Optional: Handle back button press
    override fun onBackPressed() {
        // Show confirmation dialog before exiting
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        // Use AlertDialog to confirm exit
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Keluar")
        builder.setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")

        builder.setPositiveButton("Ya") { _, _ ->
            // Close the app
            finishAffinity()
        }

        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}