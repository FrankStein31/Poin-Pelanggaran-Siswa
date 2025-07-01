package fidya.ardani.la

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

data class Guru(
    @get:Exclude
    var id: String = "", // Gunakan var dan Exclude agar bisa diisi dari ID dokumen

    var nama: String = "",
    var nip: String = "",
    var alamat: String = "",
    var email: String = "",
    var jadwalPiket: String = "",
    var noHp: String = "",
    var fotoProfilUrl: String = ""
)

@Parcelize // Tambahkan anotasi ini
data class JadwalPiketModel(
    val id: String,
    val tanggal: String,
    val hari: String,
    val jam: String,
    val guruId: String,
    var guruNama: String
) : Parcelable // Implementasikan Parcelable

data class JadwalPiketTersimpan(val docId: String, val guruId: String)
