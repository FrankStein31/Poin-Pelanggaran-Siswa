package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashboardSiswaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var imgProfile: ImageView
    private lateinit var txtTotalPoin: TextView
    private lateinit var txtPeringatan: TextView
    private lateinit var cardDataPoinSaya: CardView
    private lateinit var cardRiwayat: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_siswa)

        initViews()
        setClickListeners()
    }

    private fun initViews() {
        imgProfile = findViewById(R.id.imgProfile)
        txtTotalPoin = findViewById(R.id.txtTotalPoin)
        txtPeringatan = findViewById(R.id.txtPeringatan)
        cardDataPoinSaya = findViewById(R.id.cardDataPoinSaya)
        cardRiwayat = findViewById(R.id.cardRiwayat)
    }

    private fun setClickListeners() {
        imgProfile.setOnClickListener(this)
        cardDataPoinSaya.setOnClickListener(this)
        cardRiwayat.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val intent = when (view.id) {
            R.id.imgProfile -> Intent(this, ProfileActivity::class.java)
            R.id.cardDataPoinSaya -> Intent(this, DataPoinSiswaActivity::class.java)
            R.id.cardRiwayat -> Intent(this, RiwayatLaporanActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}