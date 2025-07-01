package fidya.ardani.la

import com.google.firebase.firestore.Exclude

data class Siswa(
    @get:Exclude
    var uid: String = "",

    var nama: String = "",
    var nis: String = "",
    var jurusan: String = "",
    var kelas: String = "",
    var alamat: String = "",
    var email: String = "",
    var password: String = "",
    var noHp: String = "",
    var noHpOrtu: String = "",
    var fotoProfilUrl: String = ""
    // Menyimpan ID siswa untuk keperluan edit/delete
)
