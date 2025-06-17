package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class RiwayatSiswaAdapter(
    private val context: Context,
    private val laporanList: List<RiwayatLaporanActivity.Laporan>
) : BaseAdapter() {

    override fun getCount(): Int = laporanList.size
    override fun getItem(position: Int): Any = laporanList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_riwayat_siswa, parent, false)

        val laporan = laporanList[position]
        
        view.findViewById<TextView>(R.id.tvPoin).text = "${laporan.jumlahPoin} Poin"
        view.findViewById<TextView>(R.id.tvPelanggaran).text = laporan.kategoriPelanggaran
        view.findViewById<TextView>(R.id.tvTanggal).text = laporan.tanggalPelanggaran
        view.findViewById<TextView>(R.id.tvPelapor).text = laporan.guruPiket

        // Load foto pelapor
        val imgPelapor = view.findViewById<CircleImageView>(R.id.imgPelapor)
        Glide.with(context)
            .load(R.drawable.ic_person)
            .into(imgPelapor)

        // Load foto bukti jika ada
        val imgBukti = view.findViewById<ImageView>(R.id.imgBukti)
        if (!laporan.fotoBukti.isNullOrEmpty()) {
            Glide.with(context)
                .load(laporan.fotoBukti)
                .into(imgBukti)
            imgBukti.visibility = View.VISIBLE
        } else {
            imgBukti.visibility = View.GONE
        }

        return view
    }
} 