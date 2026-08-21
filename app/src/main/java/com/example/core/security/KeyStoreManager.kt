package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore backend for cryptography.
 * Replaces the insecure hardcoded ECB AES encryption with AES-256-GCM.
 */
object KeyStoreManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "VigilantGuardVaultKey"
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"
    private const val GCM_TAG_LENGTH = 128

    init {
        generateKeyIfNotExists()
    }

    private fun generateKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE)
            
            // Require user authentication for extra security (can be tied to biometrics)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setKeySize(256)
                // In a true production environment, setUserAuthenticationRequired(true) 
                // would mandate BiometricPrompt usage to unlock the key before each use.
                // .setUserAuthenticationRequired(true)
                // .setUserAuthenticationValidityDurationSeconds(60)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: Base64(IV + CipherText)
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and Encrypted Data to store together
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT).trim()
    }

    /**
     * Decrypts ciphertext (IV + CipherText in Base64) using AES-256-GCM.
     */
    fun decrypt(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        
        // GCM IV size is exactly 12 bytes
        val iv = ByteArray(12)
        val encryptedData = ByteArray(combined.size - 12)
        
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, encryptedData, 0, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getSecretKey()
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedData = cipher.doFinal(encryptedData)
        
        return String(decryptedData, Charsets.UTF_8)
    }
}
