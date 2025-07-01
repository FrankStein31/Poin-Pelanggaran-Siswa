package fidya.ardani.la.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import fidya.ardani.la.Guru
import fidya.ardani.la.R

class GuruAdapter(
    private val context: Context,
    private val list: List<Guru>,
    private val listener: AdapterListener
) : BaseAdapter() {

    interface AdapterListener {
        fun onEdit(guru: Guru)
        fun onDelete(guru: Guru)
    }

    override fun getCount(): Int = list.size
    override fun getItem(position: Int): Any = list[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_guru, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val guru = list[position]

        holder.txtNama.text = guru.nama
        holder.txtNip.text = "NIP: ${guru.nip}"
        holder.txtJadwalPiket.text = "Piket: ${guru.jadwalPiket}"

        if (guru.fotoProfilUrl.isNotEmpty()) {
            Glide.with(context).load(guru.fotoProfilUrl).placeholder(R.drawable.ic_person).into(holder.imgProfil)
        } else {
            holder.imgProfil.setImageResource(R.drawable.ic_person)
        }

        holder.btnEdit.setOnClickListener { listener.onEdit(guru) }
        holder.btnDelete.setOnClickListener { listener.onDelete(guru) }

        return view
    }

    private class ViewHolder(view: View) {
        val txtNama: TextView = view.findViewById(R.id.txtNama)
        val txtNip: TextView = view.findViewById(R.id.txtNip)
        val txtJadwalPiket: TextView = view.findViewById(R.id.txtJadwalPiket)
        val imgProfil: CircleImageView = view.findViewById(R.id.imgProfilGuru)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}
