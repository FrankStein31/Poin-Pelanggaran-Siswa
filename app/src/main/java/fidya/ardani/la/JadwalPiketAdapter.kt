package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class JadwalPiketAdapter(
    private val context: Context,
    private val jadwalList: List<JadwalPiketModel>,
    private val guruList: List<Guru>
) : BaseAdapter() {

    override fun getCount(): Int = jadwalList.size

    override fun getItem(position: Int): Any = jadwalList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_jadwal_piket, parent, false)

        val hariTextView: TextView = view.findViewById(R.id.hariTextView)
        val jamTextView: TextView = view.findViewById(R.id.jamTextView)
        val guruTextView: TextView = view.findViewById(R.id.guruTextView)

        val jadwal = jadwalList[position]

        hariTextView.text = jadwal.hari
        jamTextView.text = jadwal.jam
        guruTextView.text = jadwal.guruNama

        return view
    }
}
