package fidya.ardani.la

import java.util.Date

data class PelanggaranActivity(
    val id: String,
    val siswaId: String,
    val kategoriId: String,
    val kategoriNama: String,
    val poin: Int,
    val tanggal: Date,
    val keterangan: String,
    val guruPelapor: String
)