package com.example.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberSalesDuesSummary
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong

enum class FemaleAiVoiceOption(
    val voiceId: String,
    val agentName: String,
    val bnLabel: String,
    val subtitle: String,
    val isMale: Boolean = false
) {
    AOEDE(
        voiceId = "Aoede",
        agentName = "Nadia",
        bnLabel = "Nadia (Female)",
        subtitle = "Warm female AI speaker",
        isMale = false
    ),
    KORE(
        voiceId = "Kore",
        agentName = "Sadia",
        bnLabel = "Sadia (Female)",
        subtitle = "Clear female AI speaker",
        isMale = false
    ),
    RAFIQUL(
        voiceId = "Charon",
        agentName = "Md.Rafiqul",
        bnLabel = "Md.Rafiqul",
        subtitle = "Male AI speaker",
        isMale = true
    ),
    NITAI(
        voiceId = "Puck",
        agentName = "Nitai Chondro Roy",
        bnLabel = "Nitai Chondro Roy",
        subtitle = "Male AI speaker",
        isMale = true
    ),
    NIKHIL(
        voiceId = "Fenrir",
        agentName = "Nikhil Chondro Roy",
        bnLabel = "Nikhil Chondro Roy",
        subtitle = "Male AI speaker",
        isMale = true
    );

    val genderLabel: String
        get() = if (isMale) "Male AI" else "Female AI"
}

enum class AiVoiceScope(val bnLabel: String) {
    ALL_MEMBERS("All Members"),
    ONLY_DUES("Dues Only"),
    SINGLE_MEMBER("Single Member")
}

sealed interface AiVoiceStatus {
    data object Idle : AiVoiceStatus
    data object ComposingScript : AiVoiceStatus
    data object GeneratingAudio : AiVoiceStatus
    data object Playing : AiVoiceStatus
    data object Ready : AiVoiceStatus
    data class Error(val message: String) : AiVoiceStatus
}

data class AiVoiceResult(
    val script: String,
    val audioBytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AiVoiceResult) return false
        return script == other.script &&
            mimeType == other.mimeType &&
            audioBytes.contentEquals(other.audioBytes)
    }

    override fun hashCode(): Int {
        var result = script.hashCode()
        result = 31 * result + audioBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

class BanglaAiVoiceAssistant {

    companion object {
        private const val TAG = "BanglaAiVoiceAssistant"
        private const val SCRIPT_MODEL = "gemini-3.8-flash"
        private const val TTS_MODEL = "gemini-3.1-flash-tts-preview"
        private const val TIMEOUT_MS = 60_000L
    }

    private val scriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SCRIPT_MODEL,
            systemInstruction = content {
                text(
                    "You are a warm and polite female voice assistant for the Rafiqul Islam ledger app. " +
                        "Write a concise, natural, and easy-to-read English spoken summary from the provided member " +
                        "TP+VAT sales, approved deposits, and dues figures. " +
                        "Do not use any markdown symbols (*, #, bullet points) or code blocks. " +
                        "Write in clear complete English sentences suitable for reading aloud smoothly. " +
                        "Use only the exact figures and facts provided; never invent or alter any numbers."
                )
            }
        )
    }

    private val aoedeTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.AOEDE.voiceId) }
    private val koreTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.KORE.voiceId) }
    private val rafiqulTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.RAFIQUL.voiceId) }
    private val nitaiTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.NITAI.voiceId) }
    private val nikhilTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.NIKHIL.voiceId) }

    @OptIn(PublicPreviewAPI::class)
    private fun buildTtsModel(voiceName: String) = Firebase.ai.generativeModel(
        modelName = TTS_MODEL,
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO)
            speechConfig = SpeechConfig(voice = Voice(voiceName))
        }
    )

    suspend fun generateVoiceReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        scope: AiVoiceScope,
        selectedMemberId: String?,
        voiceOption: FemaleAiVoiceOption,
        onStatus: (AiVoiceStatus) -> Unit
    ): Result<AiVoiceResult> {
        val allSummaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions
        )

        if (allSummaries.isEmpty()) {
            return Result.failure(
                IllegalStateException("No members or sales records have been added yet.")
            )
        }

        val targetSummaries = when (scope) {
            AiVoiceScope.ALL_MEMBERS -> allSummaries
            AiVoiceScope.ONLY_DUES -> allSummaries.filter { it.dues > 0.0 }
            AiVoiceScope.SINGLE_MEMBER -> {
                val chosen = allSummaries.firstOrNull { it.memberId == selectedMemberId }
                    ?: allSummaries.first()
                listOf(chosen)
            }
        }

        if (targetSummaries.isEmpty()) {
            return Result.failure(
                IllegalStateException("No members currently have any dues.")
            )
        }

        return try {
            onStatus(AiVoiceStatus.ComposingScript)
            val dataPrompt = buildBanglaDataPrompt(scope, targetSummaries)
            val scriptResponse = withTimeout(TIMEOUT_MS) {
                scriptModel.generateContent(dataPrompt)
            }
            val scriptText = scriptResponse.text?.trim().orEmpty()
            if (scriptText.isEmpty()) {
                throw IllegalStateException("Could not generate the voice summary script.")
            }

            onStatus(AiVoiceStatus.GeneratingAudio)
            val ttsModel = when (voiceOption) {
                FemaleAiVoiceOption.AOEDE -> aoedeTtsModel
                FemaleAiVoiceOption.KORE -> koreTtsModel
                FemaleAiVoiceOption.RAFIQUL -> rafiqulTtsModel
                FemaleAiVoiceOption.NITAI -> nitaiTtsModel
                FemaleAiVoiceOption.NIKHIL -> nikhilTtsModel
            }
            val voiceGender = if (voiceOption.isMale) "male" else "female"
            val ttsPrompt = "Please read the following English ledger summary aloud in a warm, calm, clear, " +
                "and natural $voiceGender voice as ${voiceOption.agentName} at a steady pace. Do not add any extra introduction:\n\n$scriptText"

            val ttsResponse = withTimeout(TIMEOUT_MS) {
                ttsModel.generateContent(ttsPrompt)
            }
            val audioPart = ttsResponse.inlineDataParts.firstOrNull { part ->
                part.inlineData.isNotEmpty() &&
                    (part.mimeType.startsWith("audio/", ignoreCase = true) ||
                        part.mimeType.isBlank())
            } ?: throw IllegalStateException("Could not generate voice audio.")

            Result.success(
                AiVoiceResult(
                    script = scriptText,
                    audioBytes = audioPart.inlineData,
                    mimeType = audioPart.mimeType
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Voice generation timed out", e)
            Result.failure(
                IllegalStateException("Voice generation took too long (60s). Please try again.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Voice generation failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Could not generate voice summary. Please check your internet connection.",
                    e
                )
            )
        }
    }

    suspend fun playPcmAudio(audioBytes: ByteArray, mimeType: String) {
        withContext(Dispatchers.IO) {
            if (audioBytes.isEmpty()) return@withContext

            val isWav = mimeType.contains("wav", ignoreCase = true) ||
                (audioBytes.size >= 44 &&
                    audioBytes[0] == 'R'.code.toByte() &&
                    audioBytes[1] == 'I'.code.toByte() &&
                    audioBytes[2] == 'F'.code.toByte() &&
                    audioBytes[3] == 'F'.code.toByte())

            val sampleRate = when {
                isWav && audioBytes.size >= 28 -> {
                    ((audioBytes[24].toInt() and 0xFF)) or
                        ((audioBytes[25].toInt() and 0xFF) shl 8) or
                        ((audioBytes[26].toInt() and 0xFF) shl 16) or
                        ((audioBytes[27].toInt() and 0xFF) shl 24)
                }
                else -> parseSampleRateFromMime(mimeType)
            }.coerceIn(8_000, 48_000)

            val pcmOffset = if (isWav) 44 else 0
            val pcmLength = (audioBytes.size - pcmOffset).coerceAtLeast(0)
            if (pcmLength <= 0) return@withContext

            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBufSize * 2)
                .build()

            try {
                track.play()
                var offset = pcmOffset
                val chunkSize = minBufSize
                while (offset < audioBytes.size) {
                    coroutineContext.ensureActive()
                    val toWrite = minOf(chunkSize, audioBytes.size - offset)
                    val written = track.write(audioBytes, offset, toWrite)
                    if (written <= 0) break
                    offset += written
                }
                // Drain remaining buffer briefly only if still active
                coroutineContext.ensureActive()
                track.stop()
            } finally {
                runCatching {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause()
                        track.flush()
                    }
                    track.release()
                }
            }
        }
    }

    suspend fun playCallRingbackTone(pulses: Int = 2) {
        withContext(Dispatchers.IO) {
            val toneGen = runCatching {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 58)
            }.getOrNull() ?: return@withContext
            try {
                repeat(pulses.coerceIn(1, 3)) { idx ->
                    coroutineContext.ensureActive()
                    runCatching {
                        toneGen.startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE, 420)
                    }
                    kotlinx.coroutines.delay(460)
                    runCatching { toneGen.stopTone() }
                    if (idx < pulses - 1) {
                        kotlinx.coroutines.delay(240)
                    }
                }
            } finally {
                runCatching {
                    toneGen.stopTone()
                    toneGen.release()
                }
            }
        }
    }

    private fun parseSampleRateFromMime(mimeType: String): Int {
        val match = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE).find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24_000
    }

    private fun buildBanglaDataPrompt(
        scope: AiVoiceScope,
        summaries: List<MemberSalesDuesSummary>
    ): String {
        val totals = MemberSalesDuesCalculator.calculateTotals(summaries)
        val capList = summaries.take(18)
        val omittedCount = (summaries.size - capList.size).coerceAtLeast(0)

        val sb = StringBuilder()
        sb.appendLine("Please write a brief, clear, and pleasant spoken English summary using the member ledger details below.")
        sb.appendLine("Start with a polite greeting, then state Total TP+VAT sales, Total Approved Deposit, and Total Dues. Afterwards, summarize each member's name, sales, deposit, and dues clearly.")
        sb.appendLine()
        sb.appendLine("Report scope: ${scope.bnLabel}")
        sb.appendLine("Total members: ${summaries.size}")
        sb.appendLine("Total TP+VAT sales: ${totals.totalSales.roundToLong()} BDT")
        sb.appendLine("Total approved deposit: ${totals.totalApprovedJoma.roundToLong()} BDT")
        sb.appendLine("Total dues: ${totals.totalDues.roundToLong()} BDT")
        sb.appendLine()
        sb.appendLine("Member breakdown:")
        capList.forEachIndexed { index, m ->
            val areaPart = if (m.area.isNotBlank()) ", Area: ${m.area}" else ""
            val codePart = if (m.code.isNotBlank()) " (Code ${m.code})" else ""
            val pendingPart = if (m.pendingJoma > 0.0) ", Pending deposit: ${m.pendingJoma.roundToLong()} BDT" else ""
            val advancePart = if (m.advance > 0.0) ", Advance: ${m.advance.roundToLong()} BDT" else ""
            sb.appendLine(
                "${index + 1}. Name: ${m.name}$codePart$areaPart — " +
                    "TP+VAT sales: ${m.totalSales.roundToLong()} BDT, " +
                    "Approved deposit: ${m.approvedJoma.roundToLong()} BDT, " +
                    "Dues: ${m.dues.roundToLong()} BDT$pendingPart$advancePart."
            )
        }
        if (omittedCount > 0) {
            val remainingDues = summaries.drop(capList.size).sumOf { it.dues }.roundToLong()
            sb.appendLine("Additionally, there are $omittedCount more members with combined dues of $remainingDues BDT.")
        }
        sb.appendLine()
        sb.appendLine("Instruction: Write amounts as clear spoken English phrases without any bullet points, asterisks, or markdown symbols.")
        return sb.toString()
    }
}
