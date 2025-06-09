package fidya.ardani.la

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PelanggaranRepository(private val context: Context) {

    fun getPelanggaranBySiswaId(siswaId: String): List<PelanggaranActivity> {
        // In a real app, this would fetch from an API or local database
        // For demo purposes, return mock data
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        return listOf(
            PelanggaranActivity(
                id = "P1",
                siswaId = siswaId,
                kategoriId = "K1",
                kategoriNama = "Terlambat masuk sekolah",
                poin = 5,
                tanggal = formatter.parse("12-04-2023") ?: Date(),
                keterangan = "Terlambat 30 menit",
                guruPelapor = "Bpk. Ahmad"
            ),
            PelanggaranActivity(
                id = "P2",
                siswaId = siswaId,
                kategoriId = "K2",
                kategoriNama = "Tidak memakai atribut lengkap",
                poin = 3,
                tanggal = formatter.parse("10-04-2023") ?: Date(),
                keterangan = "Tidak memakai dasi",
                guruPelapor = "Ibu Siti"
            ),
            PelanggaranActivity(
                id = "P3",
                siswaId = siswaId,
                kategoriId = "K3",
                kategoriNama = "Tidak mengerjakan PR",
                poin = 5,
                tanggal = formatter.parse("05-04-2023") ?: Date(),
                keterangan = "PR Matematika",
                guruPelapor = "Bpk. Rudi"
            ),
            PelanggaranActivity(
                id = "P4",
                siswaId = siswaId,
                kategoriId = "K4",
                kategoriNama = "Membuat keributan di kelas",
                poin = 10,
                tanggal = formatter.parse("28-03-2023") ?: Date(),
                keterangan = "Mengganggu proses belajar",
                guruPelapor = "Ibu Diana"
            ),
            PelanggaranActivity(
                id = "P5",
                siswaId = siswaId,
                kategoriId = "K5",
                kategoriNama = "Membolos pelajaran",
                poin = 15,
                tanggal = formatter.parse("15-03-2023") ?: Date(),
                keterangan = "Tidak mengikuti pelajaran Fisika",
                guruPelapor = "Bpk. Hendra"
            )
        )
    }
}