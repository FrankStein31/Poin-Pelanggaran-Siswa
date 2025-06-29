package fidya.ardani.la.model

import java.util.Date

data class Pelanggaran(
    val id: Int,
    val nama_siswa: String,        // Tambahkan ini
    val kategori: String,
    val poin: Int,
    val tanggal: Date?,
    val keterangan: String?,
    val guruPelapor: String?,
    val foto_bukti: String?        // Tambahkan ini juga
)
