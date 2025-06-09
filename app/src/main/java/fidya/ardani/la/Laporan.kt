package fidya.ardani.la

data class Laporan(
    val namaSiswa: String,
    val kategoriPelanggaran: String,
    val tanggalPelanggaran: String,
    var jumlahPoin: Int = 0
)
