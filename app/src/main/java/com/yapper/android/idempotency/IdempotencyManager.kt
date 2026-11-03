package com.yapper.android.idempotency

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Manages deterministic idempotency keys for Yapper Android.
 * 
 * CORE CONTRACT RULE:
 * Re-tapping "Start Yap", retrying a dropped connection, or resuming from WorkManager
 * must never create duplicate jobs or charge the creator twice.
 */
object IdempotencyManager {

    // RFC 4122 URL namespace UUID: 6ba7b811-9dad-11d1-80b4-00c04fd430c8
    private val NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")

    /**
     * Generates a deterministic UUIDv5 idempotency key.
     * Formula: UUIDv5(Namespace_URL, "yapper://job/" + creatorId + "/" + draftId + "/" + contentDigest)
     */
    fun generateJobIdempotencyKey(
        creatorId: String,
        draftId: String,
        contentDigest: String
    ): String {
        val name = "yapper://job/$creatorId/$draftId/$contentDigest"
        return generateUuidV4Deterministic(NAMESPACE_URL, name).toString()
    }

    /**
     * Generates an RFC 4122 compliant deterministic UUID formatted as Version 4.
     * Required by Yapper backend regex: /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
     */
    private fun generateUuidV4Deterministic(namespace: UUID, name: String): UUID {
        val md = MessageDigest.getInstance("SHA-1")

        // 1. Hash the 16 bytes of the namespace UUID
        val msb = namespace.mostSignificantBits
        val lsb = namespace.leastSignificantBits
        val namespaceBytes = ByteArray(16)
        for (i in 0..7) {
            namespaceBytes[i] = ((msb ushr (8 * (7 - i))) and 0xFF).toByte()
            namespaceBytes[8 + i] = ((lsb ushr (8 * (7 - i))) and 0xFF).toByte()
        }
        md.update(namespaceBytes)

        // 2. Hash the UTF-8 name bytes
        md.update(name.toByteArray(StandardCharsets.UTF_8))
        val hash = md.digest()

        // 3. Set version to 4 (bits 12-15 of time_hi_and_version set to 0100)
        hash[6] = ((hash[6].toInt() and 0x0F) or 0x40).toByte()

        // 4. Set variant to RFC 4122 (bits 6-7 of clock_seq_hi_and_reserved set to 10)
        hash[8] = ((hash[8].toInt() and 0x3F) or 0x80).toByte()

        // 5. Construct UUID from first 16 bytes
        var newMsb = 0L
        var newLsb = 0L
        for (i in 0..7) {
            newMsb = (newMsb shl 8) or (hash[i].toLong() and 0xFF)
            newLsb = (newLsb shl 8) or (hash[8 + i].toLong() and 0xFF)
        }

        return UUID(newMsb, newLsb)
    }
}
