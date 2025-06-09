package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class LaporanAdapter(
    private val context: Context,
    private val dataSource: List<RiwayatLaporanActivity.Laporan>
) : BaseAdapter() {

    override fun getCount(): Int = dataSource.size

    override fun getItem(position: Int): Any = dataSource[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_riwayat_laporan, parent, false)

        val laporan = getItem(position) as RiwayatLaporanActivity.Laporan

        val tvNamaSiswa = view.findViewById<TextView>(R.id.tv_nama_siswa)
        val tvPoinPelanggaran = view.findViewById<TextView>(R.id.tv_poin_pelanggaran)
        val tvTanggalPelanggaran = view.findViewById<TextView>(R.id.tv_tanggal_pelanggaran)

        tvNamaSiswa.text = laporan.namaSiswa
        tvPoinPelanggaran.text = "Poin: ${laporan.jumlahPoin}"
        tvTanggalPelanggaran.text = "Tanggal: ${laporan.tanggalPelanggaran}"

        return view
    }
}
