package org.heroism.herocompass.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortableBackupCryptoTest {
    private val passphrase = "menyala-backup-kuat".toCharArray()

    @Test
    fun encryptedBackupRoundTripsAllSupportedRecords() {
        val snapshot = PortableBackupSnapshot(
            createdAtEpochMillis = 1234L,
            children = listOf(BackupChild("child-1", "Ayu Lestari", "2015-04-12", "P", false)),
            assessments = listOf(BackupAssessment("future", "a-1", "child-1", "PRE_TEST", listOf(1, null, 4), null, null)),
            interventions = listOf(BackupIntervention("i-1", "child-1", "2026-11-22", "Sesi refleksi", "Catatan")),
        )

        val encrypted = PortableBackupCrypto.encrypt(snapshot, passphrase)
        assertTrue(encrypted is BackupCryptoResult.Success)
        val bytes = (encrypted as BackupCryptoResult.Success).bytes
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("Ayu Lestari"))
        val decrypted = PortableBackupCrypto.decrypt(bytes, passphrase)
        assertTrue(decrypted is BackupCryptoResult.Success)
        val restored = PortableBackupCrypto.decodeSnapshot((decrypted as BackupCryptoResult.Success).bytes)

        assertEquals(snapshot, restored)
    }

    @Test
    fun wrongPassphraseAndTamperingAreRejected() {
        val snapshot = PortableBackupSnapshot(1, emptyList(), emptyList(), emptyList())
        val bytes = (PortableBackupCrypto.encrypt(snapshot, passphrase) as BackupCryptoResult.Success).bytes
        assertEquals(
            BackupCryptoResult.AuthenticationFailed,
            PortableBackupCrypto.decrypt(bytes, "bukan-passphrase-benar".toCharArray()),
        )
        val damaged = bytes.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertEquals(BackupCryptoResult.AuthenticationFailed, PortableBackupCrypto.decrypt(damaged, passphrase))
        assertEquals(BackupCryptoResult.InvalidPassphrase, PortableBackupCrypto.encrypt(snapshot, "singkat".toCharArray()))
    }
}
