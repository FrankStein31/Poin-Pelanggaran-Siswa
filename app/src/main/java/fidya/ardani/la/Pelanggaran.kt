package fidya.ardani.la.model

import java.util.Date

data class Pelanggaran(
    val id: Int,
    val kategori: String,
    val poin: Int,
    val tanggal: Date?,
    val keterangan: String?,
    val guruPelapor: String?
)