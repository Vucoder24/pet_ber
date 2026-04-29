package com.nvv.petber.utils

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object TranslationUtils {

    private fun getTargetLanguage(): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "vi" -> TranslateLanguage.VIETNAMESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "zh" -> TranslateLanguage.CHINESE
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "es" -> TranslateLanguage.SPANISH
            "th" -> TranslateLanguage.THAI
            "pt" -> TranslateLanguage.PORTUGUESE
            else -> TranslateLanguage.ENGLISH
        }
    }


    suspend fun translate(text: String?): String? {
        if (text.isNullOrBlank()) return text
        val targetLang = getTargetLanguage()
        if (targetLang == TranslateLanguage.ENGLISH) return text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)
        var isClosed = false

        fun safeClose() {
            if (!isClosed) {
                isClosed = true
                translator.close()
            }
        }

        return suspendCancellableCoroutine { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    if (isClosed) return@addOnSuccessListener  // ← guard
                    translator.translate(text)
                        .addOnSuccessListener { translated ->
                            safeClose()
                            if (cont.isActive) cont.resume(translated)
                        }
                        .addOnFailureListener {
                            safeClose()
                            if (cont.isActive) cont.resume(text)
                        }
                }
                .addOnFailureListener {
                    safeClose()
                    if (cont.isActive) cont.resume(text)
                }

            cont.invokeOnCancellation { safeClose() }
        }
    }


    suspend fun translateAll(vararg texts: String?): List<String?> {
        val targetLang = getTargetLanguage()
        if (targetLang == TranslateLanguage.ENGLISH) return texts.toList()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)

        var isClosed = false

        fun safeClose() {
            if (!isClosed) {
                isClosed = true
                translator.close()
            }
        }

        return suspendCancellableCoroutine { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    val results = arrayOfNulls<String>(texts.size)
                    var remaining = texts.size

                    if (remaining == 0) {
                        safeClose()
                        cont.resume(emptyList())
                        return@addOnSuccessListener
                    }

                    texts.forEachIndexed { index, text ->
                        if (isClosed) return@forEachIndexed

                        if (text.isNullOrBlank()) {
                            results[index] = text
                            remaining--
                            if (remaining == 0) {
                                safeClose()
                                if (cont.isActive) cont.resume(results.toList())
                            }
                        } else {
                            translator.translate(text)
                                .addOnSuccessListener { translated ->
                                    if (isClosed) return@addOnSuccessListener
                                    results[index] = translated
                                    remaining--
                                    if (remaining == 0) {
                                        safeClose()
                                        if (cont.isActive) cont.resume(results.toList())
                                    }
                                }
                                .addOnFailureListener {
                                    if (isClosed) return@addOnFailureListener
                                    results[index] = text
                                    remaining--
                                    if (remaining == 0) {
                                        safeClose()
                                        if (cont.isActive) cont.resume(results.toList())
                                    }
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    safeClose()
                    if (cont.isActive) cont.resume(texts.toList())
                }

            cont.invokeOnCancellation { safeClose() }
        }
    }
}