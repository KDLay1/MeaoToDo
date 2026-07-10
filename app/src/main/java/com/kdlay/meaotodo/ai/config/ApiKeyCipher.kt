package com.kdlay.meaotodo.ai.config

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedApiKey(
    val cipherText: String,
    val initializationVector: String
)

class ApiKeyCipher {
    fun encrypt(value: String): EncryptedApiKey {
        require(value.isNotBlank()) { "API Key 不能为空" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return EncryptedApiKey(
            cipherText = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
            initializationVector = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(encrypted: EncryptedApiKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(encrypted.initializationVector, Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(Base64.decode(encrypted.cipherText, Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    fun deleteKey() {
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = keyStore()
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "meaotodo.ai.api_key.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
