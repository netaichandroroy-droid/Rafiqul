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
    val bnLabel: String,
    val subtitle: String
) {
    AOEDE(
        voiceId = "Aoede",
        bnLabel = "মধুর নারী কণ্ঠ",
        subtitle = "সুমধুর ও শান্ত কণ্ঠ"
    ),
    KORE(
        voiceId = "Kore",
        bnLabel = "স্পষ্ট নারী কণ্ঠ",
        subtitle = "পরিষ্কার ও দৃঢ় উচ্চারণ"
    )
}

enum class AiVoiceScope(val bnLabel: String) {
    ALL_MEMBERS("সকল মেম্বার"),
    ONLY_DUES("শুধু বকেয়া"),
    SINGLE_MEMBER("একজন মেম্বার")
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
                    "আপনি রফিকুল হিসাব খাতা অ্যাপের মিষ্টি ও মধুর কণ্ঠের নারী সহকারী। " +
                        "আপনাকে দেওয়া মেম্বারদের সেলস (টিপি প্লাস ভ্যাট), অনুমোদিত জমা টাকা এবং বকেয়া (ডিউস) হিসাব " +
                        "খুব সুন্দর, ভদ্র, প্রাণবন্ত এবং স্বাভাবিক বাংলা ভাষায় পাঠ করার উপযোগী সংক্ষিপ্ত বিবরণ আকারে লিখতে হবে। " +
                        "কোনো মার্কডাউন চিহ্ন (*, #, বুলেট পয়েন্ট) বা ইংরেজি কোড লিখবেন না। " +
                        "সহজ বাংলা পূর্ণবাক্যে লিখুন যাতে নারী কণ্ঠে সরাসরি পড়ে শোনানো যায়। " +
                        "শুধুমাত্র প্রদত্ত সঠিক তথ্য ও অঙ্ক ব্যবহার করবেন; নিজে থেকে কোনো সংখ্যা বানাবেন না।"
                )
            }
        )
    }

    private val aoedeTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.AOEDE.voiceId) }
    private val koreTtsModel by lazy { buildTtsModel(FemaleAiVoiceOption.KORE.voiceId) }

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
                IllegalStateException("এখনো কোনো মেম্বার বা সেলস তথ্য যোগ করা হয়নি।")
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
                IllegalStateException("বর্তমানে কোনো মেম্বারের বকেয়া (Dues) নেই।")
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
                throw IllegalStateException("AI বাংলা হিসাবের বিবরণ তৈরি করতে পারেনি।")
            }

            onStatus(AiVoiceStatus.GeneratingAudio)
            val ttsModel = when (voiceOption) {
                FemaleAiVoiceOption.AOEDE -> aoedeTtsModel
                FemaleAiVoiceOption.KORE -> koreTtsModel
            }
            val ttsPrompt = "নিচের বাংলা হিসাব বিবরণটি খুব মিষ্টি, শান্ত, আন্তরিক ও সুস্পষ্ট বাংলাদেশি নারী কণ্ঠে " +
                "স্বাভাবিক গতিতে পড়ে শোনান। বাড়তি কোনো ইংরেজি বা ভূমিকা যোগ করবেন না:\n\n$scriptText"

            val ttsResponse = withTimeout(TIMEOUT_MS) {
                ttsModel.generateContent(ttsPrompt)
            }
            val audioPart = ttsResponse.inlineDataParts.firstOrNull { part ->
                part.inlineData.isNotEmpty() &&
                    (part.mimeType.startsWith("audio/", ignoreCase = true) ||
                        part.mimeType.isBlank())
            } ?: throw IllegalStateException("AI ভয়েস অডিও পাওয়া যায়নি।")

            Result.success(
                AiVoiceResult(
                    script = scriptText,
                    audioBytes = audioPart.inlineData,
                    mimeType = audioPart.mimeType
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI voice generation timed out", e)
            Result.failure(
                IllegalStateException("AI ভয়েস তৈরিতে সময় বেশি লেগেছে (৬০ সেকেন্ড)। অনুগ্রহ করে আবার চেষ্টা করুন।", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI voice generation failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "AI ভয়েস তৈরি করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।",
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
        sb.appendLine("দয়া করে নিচের মেম্বার হিসাবের তথ্যগুলো ব্যবহার করে সুন্দর, মিষ্টি নারী কণ্ঠে বাংলায় শোনানোর জন্য একটি সংক্ষিপ্ত ও প্রাঞ্জল রিপোর্ট লিখুন।")
        sb.appendLine("সম্ভাষণ দিয়ে শুরু করুন, তারপর মোট সেলস (টিপি প্লাস ভ্যাট), মোট অনুমোদিত জমা টাকা এবং মোট বকেয়া জানান। এরপর প্রতিটি মেম্বারের নাম, সেলস, জমা এবং বকেয়া পরিষ্কার বাংলায় বলুন।")
        sb.appendLine()
        sb.appendLine("রিপোর্টের ধরন: ${scope.bnLabel}")
        sb.appendLine("মোট মেম্বার সংখ্যা: ${summaries.size} জন")
        sb.appendLine("সর্বমোট সেলস (টিপি প্লাস ভ্যাট): ${totals.totalSales.roundToLong()} টাকা")
        sb.appendLine("সর্বমোট অনুমোদিত জমা টাকা: ${totals.totalApprovedJoma.roundToLong()} টাকা")
        sb.appendLine("সর্বমোট বকেয়া (ডিউস): ${totals.totalDues.roundToLong()} টাকা")
        sb.appendLine()
        sb.appendLine("মেম্বার অনুযায়ী বিস্তারিত:")
        capList.forEachIndexed { index, m ->
            val areaPart = if (m.area.isNotBlank()) ", এরিয়া: ${m.area}" else ""
            val codePart = if (m.code.isNotBlank()) " (কোড ${m.code})" else ""
            val pendingPart = if (m.pendingJoma > 0.0) ", অপেক্ষমাণ জমা: ${m.pendingJoma.roundToLong()} টাকা" else ""
            val advancePart = if (m.advance > 0.0) ", অগ্রিম: ${m.advance.roundToLong()} টাকা" else ""
            sb.appendLine(
                "${index + 1}. নাম: ${m.name}$codePart$areaPart — " +
                    "টিপি প্লাস ভ্যাট সেলস: ${m.totalSales.roundToLong()} টাকা, " +
                    "অনুমোদিত জমা টাকা: ${m.approvedJoma.roundToLong()} টাকা, " +
                    "বকেয়া: ${m.dues.roundToLong()} টাকা$pendingPart$advancePart।"
            )
        }
        if (omittedCount > 0) {
            val remainingDues = summaries.drop(capList.size).sumOf { it.dues }.roundToLong()
            sb.appendLine("এছাড়া আরও $omittedCount জন মেম্বার রয়েছেন, যাদের সম্মিলিত বকেয়া $remainingDues টাকা।")
        }
        sb.appendLine()
        sb.appendLine("নির্দেশনা: সংখ্যা ও টাকার অঙ্কগুলো স্বাভাবিক বাংলা শব্দ বা স্পষ্ট বাংলা সংখ্যায় এমনভাবে লিখুন যেন নারী ভয়েস মডেল মধুরভাবে পড়তে পারে। কোনো বুলেট, তারকা বা মার্কডাউন চিহ্ন রাখবেন না।")
        return sb.toString()
    }
}
