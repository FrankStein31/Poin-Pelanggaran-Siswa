package fidya.ardani.la

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class LaporanAdapter(
    private val context: Context,
    private val dataList: List<RiwayatLaporanActivity.Laporan>
) : BaseAdapter() {

    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): Any = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_riwayat_laporan, parent, false)

        val laporan = getItem(position) as RiwayatLaporanActivity.Laporan

        val tvNamaSiswa = view.findViewById<TextView>(R.id.tv_nama_siswa)
        val tvPoinPelanggaran = view.findViewById<TextView>(R.id.tv_poin_pelanggaran)
        val tvKeteranganPelanggaran = view.findViewById<TextView>(R.id.tv_keterangan_pelanggaran)
        val tvTanggalPelanggaran = view.findViewById<TextView>(R.id.tv_tanggal_pelanggaran)
        val tvGuruPiket = view.findViewById<TextView>(R.id.tv_guru_piket)
        val imgBuktiPelanggaran = view.findViewById<ImageView>(R.id.img_bukti_pelanggaran)

        tvNamaSiswa.text = laporan.namaSiswa
        tvPoinPelanggaran.text = "Poin: ${laporan.jumlahPoin}"
        tvKeteranganPelanggaran.text = "Pelanggaran: ${laporan.kategoriPelanggaran}"
        tvTanggalPelanggaran.text = "Tanggal: ${laporan.tanggalPelanggaran}"
        tvGuruPiket.text = "Pelapor: ${laporan.guruPiket}"

        // Load foto bukti menggunakan Glide
        if (!laporan.fotoBukti.isNullOrEmpty()) {
            imgBuktiPelanggaran.visibility = View.VISIBLE
            Glide.with(context)
                .load(laporan.fotoBukti)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imgBuktiPelanggaran)
        } else {
            imgBuktiPelanggaran.visibility = View.GONE
        }

        return view
    }
}
