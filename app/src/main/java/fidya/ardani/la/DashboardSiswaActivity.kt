package fidya.ardani.la

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardSiswaActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var imgProfile: ImageView
    private lateinit var txtTotalPoin: TextView
    private lateinit var txtPeringatan: TextView
    private lateinit var cardDataPoinSaya: CardView
    private lateinit var cardRiwayat: CardView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_siswa)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setClickListeners()
        loadUserData()
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

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Ambil data siswa untuk mendapatkan NIS
            db.collection("siswa")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val siswa = documents.documents[0]
                        val nis = siswa.getString("nis") ?: return@addOnSuccessListener

                        // Hitung total poin pelanggaran
                        db.collection("laporan_pelanggaran")
                            .whereEqualTo("nis", nis)
                            .get()
                            .addOnSuccessListener { laporanDocuments ->
                                var totalPoin = 0
                                for (doc in laporanDocuments) {
                                    val poinPelanggaran = doc.getString("poin_pelanggaran") ?: ""
                                    // Ambil nilai poin dari koleksi data_poin
                                    db.collection("data_poin")
                                        .whereEqualTo("nama", poinPelanggaran)
                                        .get()
                                        .addOnSuccessListener { poinDocuments ->
                                            if (!poinDocuments.isEmpty) {
                                                val poinDoc = poinDocuments.documents[0]
                                                val jumlahPoin = poinDoc.getLong("jumlah")?.toInt() ?: 0
                                                totalPoin += jumlahPoin
                                                txtTotalPoin.text = totalPoin.toString()
                                                
                                                // Set peringatan berdasarkan total poin
                                                val peringatan = when {
                                                    totalPoin >= 100 -> "3"
                                                    totalPoin >= 50 -> "2"
                                                    totalPoin >= 25 -> "1"
                                                    else -> "0"
                                                }
                                                txtPeringatan.text = peringatan
                                            }
                                        }
                                }
                            }
                    }
                }
        }
    }

    override fun onClick(view: View) {
        val intent = when (view.id) {
            R.id.imgProfile -> Intent(this, ProfileActivity::class.java)
            R.id.cardDataPoinSaya -> Intent(this, DataPoinSiswaActivity::class.java)
            R.id.cardRiwayat -> Intent(this, RiwayatLaporanActivity::class.java).apply {
                putExtra("USER_EMAIL", auth.currentUser?.email)
            }
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}