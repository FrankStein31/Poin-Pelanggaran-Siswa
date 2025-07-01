package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class SiswaAdapter(
    private val context: Context,
    private val siswaList: List<Siswa>,
    private val listener: SiswaAdapterListener // Listener untuk komunikasi ke Activity
) : BaseAdapter() {

    // Interface untuk mengirim event klik ke Activity
    interface SiswaAdapterListener {
        fun onEditClicked(siswa: Siswa)
        fun onDeleteClicked(siswa: Siswa)
    }

    override fun getCount(): Int = siswaList.size
    override fun getItem(position: Int): Any = siswaList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            // Menggunakan layout item_siswa_card.xml yang sudah diperbarui
            view = LayoutInflater.from(context).inflate(R.layout.item_siswa, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val siswa = siswaList[position]

        holder.tvNama.text = siswa.nama
        holder.tvNis.text = "NIS: ${siswa.nis}"
        holder.tvJurusan.text = "Jurusan: ${siswa.jurusan}"
        holder.tvKelas.text = "Kelas: ${siswa.kelas}"

        // Load gambar profil menggunakan Glide
        if (siswa.fotoProfilUrl.isNotEmpty()) {
            Glide.with(context)
                .load(siswa.fotoProfilUrl)
                .placeholder(R.drawable.ic_person) // Gambar placeholder
                .error(R.drawable.ic_person)       // Gambar jika ada error
                .into(holder.imgProfil)
        } else {
            holder.imgProfil.setImageResource(R.drawable.ic_person)
        }

        // Set listener untuk tombol edit dan delete
        holder.btnEdit.setOnClickListener { listener.onEditClicked(siswa) }
        holder.btnDelete.setOnClickListener { listener.onDeleteClicked(siswa) }

        return view
    }

    // ViewHolder Pattern untuk performa optimal
    private class ViewHolder(view: View) {
        // Teks
        val tvNama: TextView = view.findViewById(R.id.tvNamaSiswa)
        val tvNis: TextView = view.findViewById(R.id.tvNIS)
        val tvJurusan: TextView = view.findViewById(R.id.tvDetailJurusan)
        val tvKelas: TextView = view.findViewById(R.id.tvDetailKelas)

        // Gambar
        val imgProfil: CircleImageView = view.findViewById(R.id.imgProfil)

        // Tombol
        // PERBAIKAN DI SINI: Ubah tipe dari ImageButton menjadi Button
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }
}
