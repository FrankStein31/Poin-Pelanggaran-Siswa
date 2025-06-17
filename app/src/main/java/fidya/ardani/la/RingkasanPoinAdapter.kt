package fidya.ardani.la

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RingkasanPoinAdapter(private val dataList: List<RiwayatLaporanActivity.RingkasanPoin>) : 
    RecyclerView.Adapter<RingkasanPoinAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaSiswa: TextView = view.findViewById(R.id.tv_nama_siswa)
        val tvTotalPoin: TextView = view.findViewById(R.id.tv_total_poin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ringkasan_poin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.tvNamaSiswa.text = item.namaSiswa
        holder.tvTotalPoin.text = "${item.totalPoin} Poin"
        
        // Warna merah jika poin >= 50
        if (item.totalPoin >= 50) {
            holder.tvTotalPoin.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        } else {
            holder.tvTotalPoin.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }

    override fun getItemCount() = dataList.size
} 