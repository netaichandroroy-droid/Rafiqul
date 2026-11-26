package com.example.ai

import android.util.Log
import com.example.data.AiCallRecord
import com.example.data.AiCallStatus
import com.example.data.AiCallCenterPrefsStore
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToLong

data class AiCallQueueItem(
    val memberId: String,
    val memberName: String,
    val code: String,
    val area: String,
    val depot: String,
    val rawPhone: String,
    val dialNumber: String,
    val totalSales: Double,
    val approvedDeposit: Double,
    val duesAmount: Double,
    val callStatus: AiCallStatus,
    val lastCalledAtMs: Long,
    val scriptPreview: String
) {
    val hasCallablePhone: Boolean
        get() = dialNumber.isNotBlank()
}

data class AiGeneratedCallScriptResult(
    val script: String,
    val audioBytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AiGeneratedCallScriptResult) return false
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

class AiCallCenterEngine {

    companion object {
        private const val TAG = "AiCallCenterEngine"
        private const val SCRIPT_MODEL = "gemini-3.8-flash"
        private const val TTS_MODEL = "gemini-3.1-flash-tts-preview"
        private const val TIMEOUT_MS = 60_000L

        fun buildDueQueue(
            members: List<Member>,
            monthlySales: List<MonthlySalesRecord>,
            submissions: List<MemberTakaJoma>,
            savedRecords: Map<String, AiCallRecord>
        ): List<AiCallQueueItem> {
            val memberById = members.associateBy { it.id }
            val summaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
                members = members,
                monthlySales = monthlySales,
                submissions = submissions
            )
            return summaries
                .filter { it.dues > 0.0 }
                .sortedByDescending { it.dues }
                .map { summary ->
                    val member = memberById[summary.memberId]
                    val rawPhone = member?.phone?.trim().orEmpty()
                    val dialNumber = AiCallCenterPrefsStore.sanitizeDialNumber(rawPhone)
                    val record = savedRecords[summary.memberId]
                    val effectiveStatus = when {
                        dialNumber.isEmpty() -> AiCallStatus.NO_PHONE
                        record != null -> record.status
                        else -> AiCallStatus.PENDING
                    }
                    AiCallQueueItem(
                        memberId = summary.memberId,
                        memberName = summary.name,
                        code = summary.code,
                        area = summary.area,
                        depot = summary.depot,
                        rawPhone = rawPhone,
                        dialNumber = dialNumber,
                        totalSales = summary.totalSales,
                        approvedDeposit = summary.approvedJoma,
                        duesAmount = summary.dues,
                        callStatus = effectiveStatus,
                        lastCalledAtMs = record?.updatedAtMs ?: 0L,
                        scriptPreview = record?.scriptPreview.orEmpty()
                    )
                }
        }

        private fun sanitizeUserFacingError(raw: String?): String {
            val msg = raw.orEmpty()
            val containsExternalName = listOf("firebase", "gemini", "google", "app check", "api key")
                .any { msg.contains(it, ignoreCase = true) }
            return if (msg.isBlank() || containsExternalName) {
                "AI voice service could not connect right now. Please check your internet connection and try again."
            } else {
                msg
            }
        }
    }

    private val callScriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SCRIPT_MODEL,
            systemInstruction = content {
                text(
                    "You are a warm, polite female AI Call Center representative for Rangpur Depot. " +
                        "Write a short, courteous, natural 2-to-3 sentence phone call greeting reminding the member " +
                        "about their Monday dues balance and requesting deposit confirmation. " +
                        "Introduce yourself using the female agent name provided. " +
                        "Never mention any external AI provider, model, or third-party service name. " +
                        "Use only the exact name, area, and BDT figures provided. Do not use markdown or bullet symbols."
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

    suspend fun generateDueCallAudio(
        item: AiCallQueueItem,
        voiceOption: FemaleAiVoiceOption,
        onProgress: (String) -> Unit = {}
    ): Result<AiGeneratedCallScriptResult> {
        return try {
            onProgress("${voiceOption.agentName} (Female AI) preparing call for ${item.memberName}...")
            val prompt = buildString {
                appendLine("Compose a polite Monday dues reminder phone call message in clear spoken English.")
                appendLine("Female Agent Name: ${voiceOption.agentName}")
                appendLine("Member Name: ${item.memberName}")
                if (item.code.isNotBlank()) appendLine("Member Code: ${item.code}")
                if (item.area.isNotBlank()) appendLine("Area: ${item.area}")
                appendLine("Depot: ${item.depot.ifBlank { "Rangpur Depot" }}")
                appendLine("Total TP+VAT Sales: ${item.totalSales.roundToLong()} BDT")
                appendLine("Approved Deposit: ${item.approvedDeposit.roundToLong()} BDT")
                appendLine("Pending Monday Dues: ${item.duesAmount.roundToLong()} BDT")
                appendLine("Keep it warm, courteous, and brief (under 45 words) so it sounds natural spoken by a female representative on a phone call.")
            }

            val scriptResponse = withTimeout(TIMEOUT_MS) {
                callScriptModel.generateContent(prompt)
            }
            val script = scriptResponse.text?.trim().orEmpty()
            if (script.isEmpty()) {
                throw IllegalStateException("AI call script could not be prepared.")
            }

            onProgress("${voiceOption.agentName} (Female AI) generating voice for ${item.memberName}...")
            val ttsModel = when (voiceOption) {
                FemaleAiVoiceOption.AOEDE -> aoedeTtsModel
                FemaleAiVoiceOption.KORE -> koreTtsModel
            }
            val ttsPrompt = "Speak the following phone call reminder in a warm, natural, polite female voice:\n\n$script"
            val ttsResponse = withTimeout(TIMEOUT_MS) {
                ttsModel.generateContent(ttsPrompt)
            }
            val audioPart = ttsResponse.inlineDataParts.firstOrNull { part ->
                part.inlineData.isNotEmpty() &&
                    (part.mimeType.startsWith("audio/", ignoreCase = true) ||
                        part.mimeType.isBlank())
            } ?: throw IllegalStateException("AI call voice audio could not be generated.")

            Result.success(
                AiGeneratedCallScriptResult(
                    script = script,
                    audioBytes = audioPart.inlineData,
                    mimeType = audioPart.mimeType
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI call generation timed out", e)
            Result.failure(
                IllegalStateException("AI call voice preparation timed out. Please try again.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI call generation failed", e)
            Result.failure(IllegalStateException(sanitizeUserFacingError(e.localizedMessage), e))
        }
    }
}
