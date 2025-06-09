package fidya.ardani.la

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore

class SiswaAdapter(private val context: Context, private val siswaList: List<Siswa>) : BaseAdapter() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getCount(): Int {
        return siswaList.size
    }

    override fun getItem(position: Int): Any {
        return siswaList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_siswa, parent, false)
        val siswa = siswaList[position]

        val tvNamaSiswa = view.findViewById<TextView>(R.id.tvNamaSiswa)
        val tvNIS = view.findViewById<TextView>(R.id.tvNIS)
        val tvJurusan = view.findViewById<TextView>(R.id.tvDetailJurusan)
        val tvKelas = view.findViewById<TextView>(R.id.tvDetailKelas)
        val tvAlamat = view.findViewById<TextView>(R.id.tvDetailAlamat)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        tvNamaSiswa.text = siswa.nama
        tvNIS.text = "NIS: ${siswa.nis}"
        tvJurusan.text = "Jurusan: ${siswa.jurusan}"
        tvKelas.text = "Kelas: ${siswa.kelas}"
        tvAlamat.text = "Alamat: ${siswa.alamat}"

        // Tombol Edit
        btnEdit.setOnClickListener {
            val intent = Intent(context, EditDataSiswaActivity::class.java)
            intent.putExtra("SISWA_ID", siswa.id)
            intent.putExtra("NAMA", siswa.nama)
            intent.putExtra("NIS", siswa.nis)
            intent.putExtra("JURUSAN", siswa.jurusan)
            intent.putExtra("KELAS", siswa.kelas)
            intent.putExtra("ALAMAT", siswa.alamat)
            context.startActivity(intent)
        }

        // Tombol Delete
        btnDelete.setOnClickListener {
            firestore.collection("siswa").document(siswa.id).delete()
                .addOnSuccessListener {
                    siswaList.toMutableList().remove(siswa)
                    notifyDataSetChanged()
                }
                .addOnFailureListener {
                    // Handle error
                }
        }

        return view
    }
}
