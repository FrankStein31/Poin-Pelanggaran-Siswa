package fidya.ardani.la.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import fidya.ardani.la.Guru
import fidya.ardani.la.R

class GuruAdapter(
    private val context: Context,
    private val resource: Int,
    private val list: List<Guru>,
    private val onEdit: (Guru) -> Unit,
    private val onDelete: (Guru) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = list.size

    override fun getItem(position: Int): Any = list[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val guru = list[position]

        val txtNama: TextView = view.findViewById(R.id.txtNama)
        val txtNip: TextView = view.findViewById(R.id.txtNip)
        val txtEmail: TextView = view.findViewById(R.id.txtEmail)
        val txtAlamat: TextView = view.findViewById(R.id.txtAlamat)
        val txtJadwalPiket: TextView = view.findViewById(R.id.txtJadwalPiket)

        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)

        txtNama.text = guru.nama
        txtNip.text = "NIP: ${guru.nip}"
        txtEmail.text = "Email: ${guru.email}"
        txtAlamat.text = "Alamat: ${guru.alamat}"
        txtJadwalPiket.text = "Jadwal Piket: ${guru.jadwalPiket}"

        btnEdit.setOnClickListener {
            onEdit(guru)
        }

        btnDelete.setOnClickListener {
            onDelete(guru)
        }

        return view
    }
}