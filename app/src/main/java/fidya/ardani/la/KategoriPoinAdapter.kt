package fidya.ardani.la

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KategoriPoinAdapter(private val kategoriList: List<KategoriPoin>) :
    RecyclerView.Adapter<KategoriPoinAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNamaKategori: TextView = view.findViewById(R.id.txtNamaKategori)
        val txtPoin: TextView = view.findViewById(R.id.txtPoin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori_poin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kategori = kategoriList[position]
        holder.txtNamaKategori.text = kategori.nama
        holder.txtPoin.text = kategori.poin.toString()
    }

    override fun getItemCount() = kategoriList.size
}
