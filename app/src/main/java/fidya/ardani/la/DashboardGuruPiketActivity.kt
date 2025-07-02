package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashboardGuruPiketActivity : AppCompatActivity() {
    private lateinit var imgMenu: ImageView
    private lateinit var cardQuickReport: CardView
    private lateinit var cardPiketHariIni: CardView
    private lateinit var txtTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_guru_piket)

        // Inisialisasi view yang ada di XML
        imgMenu = findViewById(R.id.imgMenu)
        cardQuickReport = findViewById(R.id.cardQuickReport)
        cardPiketHariIni = findViewById(R.id.cardPiketHariIni)
        txtTitle = findViewById(R.id.txtDashboardTitle)

        val namaGuru = intent.getStringExtra("nama_guru") ?: ""

        // Aksi Klik
        imgMenu.setOnClickListener {
            Toast.makeText(this, "Profil Guru / Menu", Toast.LENGTH_SHORT).show()
            // Bisa diarahkan ke activity profil atau menu
            try {
                startActivity(Intent(this, ProfileActivity::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        cardQuickReport.setOnClickListener {
            val intent = Intent(this, LaporanCepatActivity::class.java)
            intent.putExtra("nama_guru", namaGuru)
            startActivity(intent)
        }

        cardPiketHariIni.setOnClickListener {
            try {
//                startActivity(Intent(this, PiketHariIniActivity::class.java))
                startActivity(Intent(this, JadwalSayaActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(
                    this, "Fitur Laporan Cepat sedang dalam pengembangan",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
}
