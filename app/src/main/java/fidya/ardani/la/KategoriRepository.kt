package fidya.ardani.la

import android.content.Context

class KategoriRepository(private val context: Context) {
    fun getAllKategori(): List<KategoriPelanggaran> {
        // In a real app, this would fetch from an API or local database
        // For demo purposes, return mock data
        return listOf(
            KategoriPelanggaran(1, "Terlambat", "Keterlambatan masuk sekolah", 5),
            KategoriPelanggaran(2, "Seragam", "Pelanggaran aturan seragam", 10),
            KategoriPelanggaran(3, "Kehadiran", "Alpha tanpa keterangan", 15),
            KategoriPelanggaran(4, "Attitude", "Sikap tidak sopan", 20),
            KategoriPelanggaran(5, "Kebersihan", "Membuang sampah sembarangan", 5)
        )
    }

    fun getKategoriById(id: Int): KategoriPelanggaran? {
        return getAllKategori().find { it.id == id }
    }

    fun searchKategori(query: String): List<KategoriPelanggaran> {
        return getAllKategori().filter {
            it.nama.contains(query, ignoreCase = true) ||
            it.deskripsi.contains(query, ignoreCase = true)
        }
    }
}