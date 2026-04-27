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

        return suspendCancellableCoroutine { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { translated ->
                            translator.close()
                            cont.resume(translated)
                        }
                        .addOnFailureListener {
                            translator.close()
                            cont.resume(text)
                        }
                }
                .addOnFailureListener {
                    translator.close()
                    cont.resume(text)
                }

            cont.invokeOnCancellation { translator.close() }
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

        return suspendCancellableCoroutine { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    val results = arrayOfNulls<String>(texts.size)
                    var remaining = texts.size

                    if (remaining == 0) {
                        translator.close()
                        cont.resume(emptyList())
                        return@addOnSuccessListener
                    }

                    texts.forEachIndexed { index, text ->
                        if (text.isNullOrBlank()) {
                            results[index] = text
                            remaining--
                            if (remaining == 0) {
                                translator.close()
                                cont.resume(results.toList())
                            }
                        } else {
                            translator.translate(text)
                                .addOnSuccessListener { translated ->
                                    results[index] = translated
                                    remaining--
                                    if (remaining == 0) {
                                        translator.close()
                                        cont.resume(results.toList())
                                    }
                                }
                                .addOnFailureListener {
                                    results[index] = text
                                    remaining--
                                    if (remaining == 0) {
                                        translator.close()
                                        cont.resume(results.toList())
                                    }
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    translator.close()
                    cont.resume(texts.toList())
                }

            cont.invokeOnCancellation { translator.close() }
        }
    }
}