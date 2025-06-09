package fidya.ardani.la

data class Siswa(
    val nama: String,
    val nis: String,
    val jurusan: String,
    val kelas: String,
    val alamat: String,
    val id: String,
    val password: String
    // Menyimpan ID siswa untuk keperluan edit/delete
)