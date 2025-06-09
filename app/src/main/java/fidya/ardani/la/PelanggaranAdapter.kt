package fidya.ardani.la.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fidya.ardani.la.model.Pelanggaran // Tambahkan import untuk model Pelanggaran
import fidya.ardani.la.R // Tambahkan import untuk resource R
import java.text.SimpleDateFormat
import java.util.Locale

class PelanggaranAdapter(private var pelanggaranList: List<Pelanggaran>) :
    RecyclerView.Adapter<PelanggaranAdapter.PelanggaranViewHolder>() {

    class PelanggaranViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtKategori: TextView = itemView.findViewById(R.id.txtKategori)
        val txtPoin: TextView = itemView.findViewById(R.id.txtPoin)
        val txtTanggal: TextView = itemView.findViewById(R.id.txtTanggal)
        val txtKeterangan: TextView = itemView.findViewById(R.id.txtKeterangan)
        val txtGuruPelapor: TextView = itemView.findViewById(R.id.txtGuruPelapor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PelanggaranViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pelanggaran, parent, false)
        return PelanggaranViewHolder(view)
    }

    override fun onBindViewHolder(holder: PelanggaranViewHolder, position: Int) {
        val pelanggaran = pelanggaranList[position]

        holder.txtKategori.text = pelanggaran.kategori
        holder.txtPoin.text = pelanggaran.poin.toString()

        // Format tanggal jika tidak null
        pelanggaran.tanggal?.let { date ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            holder.txtTanggal.text = dateFormat.format(date)
        } ?: run {
            holder.txtTanggal.text = "-"
        }

        holder.txtKeterangan.text = pelanggaran.keterangan ?: "-"
        holder.txtGuruPelapor.text = pelanggaran.guruPelapor ?: "-"
    }

    override fun getItemCount(): Int = pelanggaranList.size

    // Fungsi untuk memperbarui data
    fun updateData(newPelanggaranList: List<Pelanggaran>) {
        pelanggaranList = newPelanggaranList
        notifyDataSetChanged()
    }
}