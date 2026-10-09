package com.example.basic.apk

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

data class ApkValidationReport(
    val isValid: Boolean,
    val isV1Verified: Boolean,
    val isV2Verified: Boolean,
    val isV3Verified: Boolean,
    val isV4Verified: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    fun summary(): String {
        val sb = StringBuilder()
        if (isValid) {
            sb.appendLine("Status: SIGNATURE VALID & COMPLIANT")
            sb.appendLine("✓ v1 Scheme (JAR): $isV1Verified")
            sb.appendLine("✓ v2 Scheme (APK Block): $isV2Verified")
            sb.appendLine("✓ v3 Scheme (Android 9+): $isV3Verified")
        } else {
            sb.appendLine("Status: INVALID SIGNATURE")
            if (errors.isEmpty()) {
                sb.appendLine("✗ Unverified APK signatures")
            } else {
                errors.forEach { sb.appendLine("✗ $it") }
            }
        }
        if (warnings.isNotEmpty()) {
            sb.appendLine("Warnings:")
            warnings.forEach { sb.appendLine("! $it") }
        }
        return sb.toString().trim()
    }
}

/**
 * Official Google Android ApkSigner engine integration.
 * Produces authentic Android v1, v2, and v3 signatures recognized across all Android versions.
 * Using minSdkVersion = 21 ensures v1 (JAR signing) is always generated and fully verified alongside v2 and v3.
 */
object ApkSignerHelper {

    fun signApk(
        unsignedApk: File,
        signedApk: File,
        keystoreStream: InputStream,
        keystorePass: String = "android",
        keyAlias: String = "androiddebugkey",
        keyPass: String = "android",
        minSdkVersion: Int = 21
    ) {
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(keystoreStream, keystorePass.toCharArray())
        val privateKey = ks.getKey(keyAlias, keyPass.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(keyAlias) as X509Certificate

        val signerConfig = ApkSigner.SignerConfig.Builder(
            keyAlias,
            privateKey,
            listOf(cert)
        ).build()

        val signer = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .setMinSdkVersion(minSdkVersion)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()

        signer.sign()
    }

    /**
     * Validates and diagnoses any issues with an APK before installation.
     */
    fun validateApk(apkFile: File, minTargetSdk: Int = 21): ApkValidationReport {
        val errorList = mutableListOf<String>()
        val warningList = mutableListOf<String>()

        if (!apkFile.exists() || apkFile.length() < 1000) {
            return ApkValidationReport(
                isValid = false,
                isV1Verified = false,
                isV2Verified = false,
                isV3Verified = false,
                isV4Verified = false,
                errors = listOf("APK file does not exist or is empty (${apkFile.length()} bytes)"),
                warnings = emptyList()
            )
        }

        return try {
            val verifier = ApkVerifier.Builder(apkFile)
                .setMinCheckedPlatformVersion(minTargetSdk)
                .build()

            val result = verifier.verify()

            val allErrors = result.allErrors
            for (i in 0 until allErrors.size) {
                val err = allErrors[i]
                errorList.add(err.toString())
            }

            val allWarnings = result.warnings
            for (i in 0 until allWarnings.size) {
                val warn = allWarnings[i]
                warningList.add(warn.toString())
            }

            ApkValidationReport(
                isValid = result.isVerified,
                isV1Verified = result.isVerifiedUsingV1Scheme,
                isV2Verified = result.isVerifiedUsingV2Scheme,
                isV3Verified = result.isVerifiedUsingV3Scheme,
                isV4Verified = result.isVerifiedUsingV4Scheme,
                errors = errorList,
                warnings = warningList
            )
        } catch (e: Exception) {
            ApkValidationReport(
                isValid = false,
                isV1Verified = false,
                isV2Verified = false,
                isV3Verified = false,
                isV4Verified = false,
                errors = listOf("Verification exception: ${e.message}"),
                warnings = emptyList()
            )
        }
    }
}
