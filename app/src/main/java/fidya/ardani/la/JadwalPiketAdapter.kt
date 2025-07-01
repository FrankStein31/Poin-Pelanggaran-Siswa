package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class JadwalPiketAdapter(
    private val context: Context,
    private val displayList: List<Any> // Menggunakan List<Any> untuk menampung Header (String) dan Item (JadwalPiketModel)
) : BaseAdapter() {

    // Definisikan tipe untuk view
    private val TYPE_ITEM = 0
    private val TYPE_HEADER = 1

    override fun getCount(): Int = displayList.size

    override fun getItem(position: Int): Any = displayList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2 // Ada 2 tipe view: item dan header

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is JadwalPiketModel) TYPE_ITEM else TYPE_HEADER
    }

    override fun isEnabled(position: Int): Boolean {
        // Menonaktifkan klik pada header
        return getItemViewType(position) == TYPE_ITEM
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        when (getItemViewType(position)) {
            TYPE_ITEM -> {
                // Proses untuk item jadwal
                if (view == null) {
                    view = LayoutInflater.from(context).inflate(R.layout.item_jadwal_piket, parent, false)
                }
                val hariTextView: TextView = view!!.findViewById(R.id.hariTextView)
                val jamTextView: TextView = view.findViewById(R.id.jamTextView)
                val guruTextView: TextView = view.findViewById(R.id.guruTextView)

                val jadwal = getItem(position) as JadwalPiketModel
                hariTextView.text = jadwal.hari
                jamTextView.text = jadwal.jam
                guruTextView.text = jadwal.guruNama
            }
            TYPE_HEADER -> {
                // Proses untuk header minggu
                if (view == null) {
                    view = LayoutInflater.from(context).inflate(R.layout.item_jadwal_header, parent, false)
                }
                val headerTextView: TextView = view!!.findViewById(R.id.headerTextView)
                val headerText = getItem(position) as String
                headerTextView.text = headerText
            }
        }
        return view!!
    }
}
