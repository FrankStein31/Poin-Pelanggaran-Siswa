package fidya.ardani.la

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PoinAdapter(
    private val listPoin: List<PoinModel>,
    private val listener: PoinAdapterListener
) : RecyclerView.Adapter<PoinAdapter.PoinViewHolder>() {

    interface PoinAdapterListener {
        fun onItemClick(poin: PoinModel, position: Int)
    }

    inner class PoinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvJurusan: TextView = itemView.findViewById(R.id.tvJurusan)
        private val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        private val tvPelanggaran: TextView = itemView.findViewById(R.id.tvPelanggaran)

        fun bind(poin: PoinModel) {
            tvNama.text = poin.nama
            tvJurusan.text = poin.jurusan
            tvKelas.text = poin.kelas
            tvPelanggaran.text = poin.pelanggaran

            itemView.setOnClickListener {
                listener.onItemClick(poin, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poin, parent, false)
        return PoinViewHolder(view)
    }

    override fun onBindViewHolder(holder: PoinViewHolder, position: Int) {
        holder.bind(listPoin[position])
    }

    override fun getItemCount(): Int = listPoin.size
}