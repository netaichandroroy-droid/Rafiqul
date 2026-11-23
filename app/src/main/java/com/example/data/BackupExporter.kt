package com.example.data

import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.ImportedTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.OnlinePaymentAccount
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import com.google.firebase.Timestamp
import java.util.UUID

data class FullAppBackupData(
    val formatVersion: Int = 2,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val isFullSnapshot: Boolean = true,
    val transactions: List<HisabTransaction> = emptyList(),
    val members: List<Member> = emptyList(),
    val memberTakaJoma: List<MemberTakaJoma> = emptyList(),
    val monthlySales: List<MonthlySalesRecord> = emptyList(),
    val onlinePaymentConfig: OnlinePaymentConfig? = null,
    val memberAppDownloadUrl: String? = null
) {
    fun buildBanglaSummary(): String {
        val parts = listOf(
            "${transactions.size} transactions",
            "${members.size} members",
            "${memberTakaJoma.size} deposits",
            "${monthlySales.size} sales"
        )
        return parts.joinToString(" • ")
    }
}

object BackupExporter {

    private val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    fun buildCsv(transactions: List<HisabTransaction>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append("Type,Amount,Category,Note,Date,Account\n")
        for (t in transactions) {
            val fields = listOf(
                t.type,
                t.amount.toString(),
                t.category,
                t.note,
                t.date,
                t.account
            )
            sb.append(fields.joinToString(",") { value ->
                "\"" + value.replace("\"", "\"\"") + "\""
            })
            sb.append('\n')
        }
        return sb.toString()
    }

    fun buildJsonBackup(transactions: List<HisabTransaction>): String {
        return MiniJson.encode(transactions.map { transactionToMap(it) }, indent = 2)
    }

    fun buildFullAppBackupJson(
        transactions: List<HisabTransaction>,
        members: List<Member>,
        memberTakaJoma: List<MemberTakaJoma>,
        monthlySales: List<MonthlySalesRecord>,
        onlinePaymentConfig: OnlinePaymentConfig,
        memberAppDownloadUrl: String = ""
    ): String {
        val root = linkedMapOf<String, Any?>(
            "formatVersion" to 2,
            "exportedAtMs" to System.currentTimeMillis(),
            "transactions" to transactions.map { transactionToMap(it) },
            "members" to members.map { m ->
                linkedMapOf<String, Any?>(
                    "id" to m.id,
                    "name" to m.name,
                    "designation" to m.designation,
                    "phone" to m.phone,
                    "depot" to m.depot,
                    "code" to m.code,
                    "area" to m.area,
                    "isActive" to m.isActive,
                    "createdAtMs" to m.createdAtMs,
                    "passwordHash" to m.passwordHash,
                    "isDefaultPassword" to m.isDefaultPassword,
                    "email" to m.email,
                    "address" to m.address,
                    "joiningDate" to m.joiningDate,
                    "profilePhotoPath" to m.profilePhotoPath,
                    "salesAmount" to m.salesAmount
                )
            },
            "memberTakaJoma" to memberTakaJoma.map { entry ->
                linkedMapOf<String, Any?>(
                    "id" to entry.id,
                    "memberId" to entry.memberId,
                    "name" to entry.name,
                    "code" to entry.code,
                    "depot" to entry.depot,
                    "designation" to entry.designation,
                    "area" to entry.area,
                    "bankName" to entry.bankName,
                    "amount" to entry.amount,
                    "date" to entry.date,
                    "note" to entry.note,
                    "photoPath" to entry.photoPath,
                    "slipHash" to entry.slipHash,
                    "submittedByRole" to entry.submittedByRole,
                    "createdAtMs" to entry.createdAtMs,
                    "approvalStatus" to entry.resolvedStatus.wireValue,
                    "reviewedAtMs" to entry.reviewedAtMs
                )
            },
            "monthlySales" to monthlySales.map { rec ->
                linkedMapOf<String, Any?>(
                    "id" to rec.id,
                    "yearMonth" to rec.yearMonth,
                    "code" to rec.code,
                    "name" to rec.name,
                    "tpSales" to rec.tpSales,
                    "tpPlusVat" to rec.tpPlusVat,
                    "createdAtMs" to rec.createdAtMs
                )
            },
            "onlinePaymentConfig" to linkedMapOf<String, Any?>(
                "isOnlinePaymentEnabled" to onlinePaymentConfig.isOnlinePaymentEnabled,
                "accounts" to onlinePaymentConfig.accounts.map { acc ->
                    linkedMapOf<String, Any?>(
                        "id" to acc.id,
                        "channel" to acc.channel.name,
                        "accountNumber" to acc.accountNumber,
                        "accountType" to acc.accountType,
                        "isEnabled" to acc.isEnabled
                    )
                }
            ),
            "memberAppDownloadUrl" to memberAppDownloadUrl.trim()
        )
        return MiniJson.encode(root, indent = 2)
    }

    fun parseFullAppBackupJson(jsonText: String): Result<FullAppBackupData> {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please select a valid JSON backup file."))
        }

        if (trimmed.startsWith("[")) {
            val legacy = parseJsonBackup(trimmed)
            return legacy.map { items ->
                val now = Timestamp.now()
                FullAppBackupData(
                    formatVersion = 1,
                    isFullSnapshot = false,
                    transactions = items.map { item ->
                        HisabTransaction(
                            id = UUID.randomUUID().toString(),
                            userId = LocalTransactionStore.LOCAL_USER_ID,
                            type = item.type.wireValue,
                            amount = item.amount,
                            category = item.category,
                            note = item.note,
                            date = item.date,
                            account = item.account.wireValue,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                )
            }
        }

        return try {
            @Suppress("UNCHECKED_CAST")
            val root = MiniJson.parse(trimmed) as? Map<String, Any?>
                ?: return Result.failure(IllegalArgumentException("Please select a valid JSON backup file."))

            val formatVersion = root.intValue("formatVersion", 2)
            val exportedAtMs = root.longValue("exportedAtMs", System.currentTimeMillis())

            val transactions = root.objectList("transactions").mapNotNull { obj ->
                val amount = obj.doubleValue("amount", -1.0)
                val date = obj.stringValue("date").trim()
                if (amount <= 0.0 || amount > 1_000_000_000.0) return@mapNotNull null
                if (!dateRegex.matches(date)) return@mapNotNull null

                val txType = TxType.fromRaw(obj.stringValue("type", "expense"))
                val accType = AccountType.fromRaw(obj.stringValue("account", "cash"))
                val category = obj.stringValue("category").trim().take(60).ifBlank { txType.bnLabel }
                val note = obj.stringValue("note").trim().take(200).ifBlank { category }
                val createdAtMs = obj.longValue(
                    "createdAtMs",
                    obj.longValue("createdAt", System.currentTimeMillis())
                )
                val updatedAtMs = obj.longValue("updatedAtMs", createdAtMs)
                val id = obj.stringValue("id").trim().ifBlank { UUID.randomUUID().toString() }
                val userId = obj.stringValue("userId").trim().ifBlank { LocalTransactionStore.LOCAL_USER_ID }

                HisabTransaction(
                    id = id,
                    userId = userId,
                    type = txType.wireValue,
                    amount = amount,
                    category = category,
                    note = note,
                    date = date,
                    account = accType.wireValue,
                    createdAt = msToTimestamp(createdAtMs),
                    updatedAt = msToTimestamp(updatedAtMs)
                )
            }

            val members = root.objectList("members").mapNotNull { obj ->
                val name = obj.stringValue("name").trim()
                if (name.isEmpty()) return@mapNotNull null
                Member(
                    id = obj.stringValue("id").trim().ifBlank { UUID.randomUUID().toString() },
                    name = name,
                    designation = obj.stringValue("designation").trim(),
                    phone = obj.stringValue("phone").trim(),
                    depot = obj.stringValue("depot").trim(),
                    code = obj.stringValue("code").trim(),
                    area = obj.stringValue("area").trim(),
                    isActive = obj.booleanValue("isActive", true),
                    createdAtMs = obj.longValue("createdAtMs", System.currentTimeMillis()),
                    passwordHash = obj.stringValue("passwordHash"),
                    isDefaultPassword = obj.booleanValue("isDefaultPassword", true),
                    email = obj.stringValue("email").trim(),
                    address = obj.stringValue("address").trim(),
                    joiningDate = obj.stringValue("joiningDate").trim(),
                    profilePhotoPath = obj.stringValue("profilePhotoPath").trim(),
                    salesAmount = obj.doubleValue("salesAmount", 0.0)
                        .takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0
                )
            }

            val takaJomaList = root.objectList("memberTakaJoma").mapNotNull { obj ->
                val name = obj.stringValue("name").trim()
                val amount = obj.doubleValue("amount", 0.0)
                if (name.isEmpty() || amount.isNaN() || amount <= 0.0) return@mapNotNull null
                MemberTakaJoma(
                    id = obj.stringValue("id").trim().ifBlank { UUID.randomUUID().toString() },
                    memberId = obj.stringValue("memberId").trim(),
                    name = name,
                    code = obj.stringValue("code").trim(),
                    depot = obj.stringValue("depot").trim(),
                    designation = obj.stringValue("designation").trim(),
                    area = obj.stringValue("area").trim(),
                    bankName = obj.stringValue("bankName").trim(),
                    amount = amount,
                    date = obj.stringValue("date").trim(),
                    note = obj.stringValue("note").trim(),
                    photoPath = obj.stringValue("photoPath").trim(),
                    slipHash = obj.stringValue("slipHash").trim(),
                    submittedByRole = obj.stringValue("submittedByRole", "MEMBER").ifBlank { "MEMBER" },
                    createdAtMs = obj.longValue("createdAtMs", System.currentTimeMillis()),
                    approvalStatus = TakaJomaStatus.fromRaw(
                        obj.stringValue("approvalStatus", TakaJomaStatus.APPROVED.wireValue)
                    ).wireValue,
                    reviewedAtMs = obj.longValue("reviewedAtMs", 0L)
                )
            }

            val monthlySales = root.objectList("monthlySales").mapNotNull { obj ->
                val yearMonth = obj.stringValue("yearMonth").trim()
                val code = obj.stringValue("code").trim()
                val name = obj.stringValue("name").trim()
                if (yearMonth.isEmpty() || (code.isEmpty() && name.isEmpty())) return@mapNotNull null
                MonthlySalesRecord(
                    id = obj.stringValue("id").trim().ifBlank { UUID.randomUUID().toString() },
                    yearMonth = yearMonth,
                    code = code,
                    name = name.ifBlank { code },
                    tpSales = obj.doubleValue("tpSales", 0.0)
                        .takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0,
                    tpPlusVat = obj.doubleValue("tpPlusVat", 0.0)
                        .takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0,
                    createdAtMs = obj.longValue("createdAtMs", System.currentTimeMillis())
                )
            }

            @Suppress("UNCHECKED_CAST")
            val paymentObj = root["onlinePaymentConfig"] as? Map<String, Any?>
            val paymentConfig = if (paymentObj != null) {
                val byId = LinkedHashMap<String, OnlinePaymentAccount>()
                OnlinePaymentConfig.defaultAccounts().forEach { byId[it.id] = it }
                paymentObj.objectList("accounts").forEach { accObj ->
                    val id = accObj.stringValue("id").trim()
                    val channel = PaymentChannel.entries.firstOrNull {
                        it.name == accObj.stringValue("channel")
                    } ?: return@forEach
                    if (id.isEmpty()) return@forEach
                    byId[id] = OnlinePaymentAccount(
                        id = id,
                        channel = channel,
                        accountNumber = accObj.stringValue("accountNumber").trim(),
                        accountType = accObj.stringValue("accountType", "Personal")
                            .trim().ifBlank { "Personal" },
                        isEnabled = accObj.booleanValue("isEnabled", false)
                    )
                }
                OnlinePaymentConfig(
                    isOnlinePaymentEnabled = paymentObj.booleanValue("isOnlinePaymentEnabled", true),
                    accounts = byId.values.toList()
                )
            } else {
                null
            }

            val memberAppDownloadUrl = if (root.containsKey("memberAppDownloadUrl")) {
                root.stringValue("memberAppDownloadUrl").trim()
            } else {
                null
            }

            val hasRecognizedStructure = root.containsKey("transactions") ||
                root.containsKey("members") ||
                root.containsKey("memberTakaJoma") ||
                root.containsKey("monthlySales") ||
                root.containsKey("onlinePaymentConfig")

            if (!hasRecognizedStructure) {
                Result.failure(IllegalArgumentException("Please select a valid JSON backup file."))
            } else {
                Result.success(
                    FullAppBackupData(
                        formatVersion = formatVersion,
                        exportedAtMs = exportedAtMs,
                        isFullSnapshot = true,
                        transactions = transactions,
                        members = members,
                        memberTakaJoma = takaJomaList,
                        monthlySales = monthlySales,
                        onlinePaymentConfig = paymentConfig,
                        memberAppDownloadUrl = memberAppDownloadUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Please select a valid JSON backup.", e))
        }
    }

    fun parseJsonBackup(jsonText: String): Result<List<ImportedTransaction>> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val array = MiniJson.parse(jsonText.trim()) as? List<Any?>
                ?: return Result.failure(IllegalArgumentException("Please select a valid JSON backup file."))
            val parsed = mutableListOf<ImportedTransaction>()
            for (item in array) {
                @Suppress("UNCHECKED_CAST")
                val obj = item as? Map<String, Any?> ?: continue
                val amount = obj.doubleValue("amount", -1.0)
                val date = obj.stringValue("date").trim()
                val rawType = obj.stringValue("type", "expense")
                val rawAccount = obj.stringValue("account", "cash")
                val category = obj.stringValue("category").trim()
                val note = obj.stringValue("note").trim()

                if (amount <= 0.0 || amount > 1_000_000_000.0) continue
                if (!dateRegex.matches(date)) continue

                val txType = TxType.fromRaw(rawType)
                val accType = AccountType.fromRaw(rawAccount)
                val safeCategory = category.take(60).ifBlank { txType.bnLabel }
                val safeNote = note.take(200).ifBlank { safeCategory }

                parsed.add(
                    ImportedTransaction(
                        type = txType,
                        amount = amount,
                        category = safeCategory,
                        note = safeNote,
                        date = date,
                        account = accType
                    )
                )
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalArgumentException("Please select a valid JSON backup file."))
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Please select a valid JSON backup.", e))
        }
    }

    private fun transactionToMap(t: HisabTransaction): Map<String, Any?> {
        val createdMs = t.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = t.updatedAt?.toDate()?.time ?: createdMs
        return linkedMapOf(
            "id" to t.id,
            "userId" to t.userId,
            "type" to t.type,
            "amount" to t.amount,
            "category" to t.category,
            "note" to t.note,
            "date" to t.date,
            "account" to t.account,
            "createdAtMs" to createdMs,
            "updatedAtMs" to updatedMs
        )
    }

    private fun msToTimestamp(ms: Long): Timestamp {
        val safeMs = ms.coerceAtLeast(0L)
        return Timestamp(safeMs / 1000L, ((safeMs % 1000L) * 1_000_000L).toInt())
    }
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.objectList(key: String): List<Map<String, Any?>> {
    val raw = this[key] as? List<Any?> ?: return emptyList()
    return raw.mapNotNull { it as? Map<String, Any?> }
}

private fun Map<String, Any?>.stringValue(key: String, default: String = ""): String {
    return when (val v = this[key]) {
        null -> default
        is String -> v
        else -> v.toString()
    }
}

private fun Map<String, Any?>.booleanValue(key: String, default: Boolean): Boolean {
    return when (val v = this[key]) {
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull() ?: default
        else -> default
    }
}

private fun Map<String, Any?>.intValue(key: String, default: Int): Int {
    return when (val v = this[key]) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }
}

private fun Map<String, Any?>.longValue(key: String, default: Long): Long {
    return when (val v = this[key]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: default
        else -> default
    }
}

private fun Map<String, Any?>.doubleValue(key: String, default: Double): Double {
    return when (val v = this[key]) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: default
        else -> default
    }
}

private object MiniJson {
    fun encode(value: Any?, indent: Int = 2): String {
        val sb = StringBuilder()
        writeValue(sb, value, indent, 0)
        return sb.toString()
    }

    private fun writeValue(sb: StringBuilder, value: Any?, indent: Int, depth: Int) {
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(value)
            is Int, is Long, is Short, is Byte -> sb.append(value.toString())
            is Double -> {
                if (value.isNaN() || value.isInfinite()) sb.append("0.0")
                else if (value == value.toLong().toDouble()) sb.append(value.toLong()).append(".0")
                else sb.append(value.toString())
            }
            is Float -> {
                if (value.isNaN() || value.isInfinite()) sb.append("0.0") else sb.append(value.toString())
            }
            is Number -> sb.append(value.toString())
            is String -> writeString(sb, value)
            is Map<*, *> -> {
                if (value.isEmpty()) {
                    sb.append("{}")
                    return
                }
                sb.append('{')
                val nextDepth = depth + 1
                var first = true
                for ((k, v) in value) {
                    if (!first) sb.append(',')
                    first = false
                    sb.append('\n').append(" ".repeat(indent * nextDepth))
                    writeString(sb, k.toString())
                    sb.append(": ")
                    writeValue(sb, v, indent, nextDepth)
                }
                sb.append('\n').append(" ".repeat(indent * depth)).append('}')
            }
            is Iterable<*> -> {
                val list = value.toList()
                if (list.isEmpty()) {
                    sb.append("[]")
                    return
                }
                sb.append('[')
                val nextDepth = depth + 1
                list.forEachIndexed { idx, item ->
                    if (idx > 0) sb.append(',')
                    sb.append('\n').append(" ".repeat(indent * nextDepth))
                    writeValue(sb, item, indent, nextDepth)
                }
                sb.append('\n').append(" ".repeat(indent * depth)).append(']')
            }
            else -> writeString(sb, value.toString())
        }
    }

    private fun writeString(sb: StringBuilder, str: String) {
        sb.append('"')
        for (ch in str) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
    }

    fun parse(input: String): Any? {
        val parser = Parser(input)
        val result = parser.readValue()
        parser.skipWhitespace()
        if (!parser.isAtEnd()) {
            throw IllegalArgumentException("Unexpected trailing JSON content")
        }
        return result
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= src.length

        fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        fun readValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) throw IllegalArgumentException("Unexpected end of JSON")
            return when (src[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> readNull()
                '-', in '0'..'9' -> readNumber()
                else -> throw IllegalArgumentException("Unexpected char '${src[pos]}' at $pos")
            }
        }

        private fun readObject(): Map<String, Any?> {
            pos++ // '{'
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                if (pos >= src.length || src[pos] != '"') {
                    throw IllegalArgumentException("Expected string key at $pos")
                }
                val key = readString()
                skipWhitespace()
                if (pos >= src.length || src[pos] != ':') {
                    throw IllegalArgumentException("Expected ':' at $pos")
                }
                pos++
                map[key] = readValue()
                skipWhitespace()
                if (pos >= src.length) throw IllegalArgumentException("Unterminated object")
                when (src[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun readArray(): List<Any?> {
            pos++ // '['
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(readValue())
                skipWhitespace()
                if (pos >= src.length) throw IllegalArgumentException("Unterminated array")
                when (src[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun readString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) throw IllegalArgumentException("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) throw IllegalArgumentException("Invalid unicode escape")
                                val hex = src.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Invalid escape '\\$esc'")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun readBoolean(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else if (src.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at $pos")
            }
        }

        private fun readNull(): Any? {
            if (src.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at $pos")
        }

        private fun readNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            var isFloat = false
            while (pos < src.length) {
                val c = src[pos]
                if (c in '0'..'9') {
                    pos++
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    if (c == '-' || c == '+') {
                        val prev = src[pos - 1]
                        if (prev != 'e' && prev != 'E') break
                    }
                    isFloat = true
                    pos++
                } else {
                    break
                }
            }
            val token = src.substring(start, pos)
            return if (isFloat) {
                token.toDouble()
            } else {
                token.toLongOrNull() ?: token.toDouble()
            }
        }
    }
}
