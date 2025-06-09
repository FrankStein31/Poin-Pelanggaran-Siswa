package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import fidya.ardani.la.R

class ProfileActivity : AppCompatActivity() {

    private lateinit var txtNIS: TextView
    private lateinit var imgBack: ImageButton
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Mengambil data NIS dari intent jika ada
        val nis = intent.getStringExtra("NIS")
        if (nis != null) {
            txtNIS.text = nis
        }

        // Set listener untuk tombol kembali
        imgBack.setOnClickListener {
            finish()
        }

        // Set listener untuk tombol logout
        btnLogout.setOnClickListener {
            // Implementasi logout
            // Misalnya, hapus data sesi/preferensi

            // Kembali ke halaman login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}