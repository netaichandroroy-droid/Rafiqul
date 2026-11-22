package com.example

import com.example.auth.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileUnitTest {

    @Test
    fun passwordHasher_usesUniqueSaltsAndVerifiesCorrectly() {
        val hash1 = PasswordHasher.hashPassword("MySecurePass123!")
        val hash2 = PasswordHasher.hashPassword("MySecurePass123!")

        assertTrue(hash1.startsWith("pbkdf2_sha256$120000$"))
        assertNotEquals(hash1, hash2)
        assertTrue(PasswordHasher.verifyPassword("MySecurePass123!", hash1))
        assertTrue(PasswordHasher.verifyPassword("MySecurePass123!", hash2))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword123!", hash1))
        assertFalse(PasswordHasher.verifyPassword("", hash1))
        assertFalse(PasswordHasher.verifyPassword("MySecurePass123!", "invalid_hash"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun passwordHasher_rejectsShortPassword() {
        PasswordHasher.hashPassword("1234567")
    }
}
