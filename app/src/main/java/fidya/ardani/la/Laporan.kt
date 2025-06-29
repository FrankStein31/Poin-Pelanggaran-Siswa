package fidya.ardani.la

data class Laporan(
        val namaSiswa: String,
        val kategoriPelanggaran: String,
        val tanggalPelanggaran: String,
        val guruPiket: String = "",
        val fotoBukti: String? = null,
        var jumlahPoin: Int = 0
)

