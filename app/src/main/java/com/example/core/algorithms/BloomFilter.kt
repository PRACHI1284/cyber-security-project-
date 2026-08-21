package com.example.core.algorithms

import java.security.MessageDigest
import java.util.BitSet
import kotlin.math.abs

/**
 * A probabilistic data structure for extremely fast subset membership testing.
 * Used here to quickly discard safe applications without querying the main signature database.
 * 
 * Space complexity: O(m)
 * Time complexity: O(k) for insertion and lookup
 */
class BloomFilter(private val expectedElements: Int = 10000, private val falsePositiveProbability: Double = 0.01) {

    private val bitSetSize: Int = Math.ceil(-(expectedElements * Math.log(falsePositiveProbability)) / (Math.log(2.0) * Math.log(2.0))).toInt()
    private val hashFunctionsCount: Int = Math.round((bitSetSize / expectedElements.toDouble()) * Math.log(2.0)).toInt()

    private val bitSet = BitSet(bitSetSize)
    private val md5Digest = MessageDigest.getInstance("MD5")

    /**
     * Add a signature or package name to the bloom filter.
     */
    fun add(element: String) {
        val hashes = getHashes(element)
        for (hash in hashes) {
            bitSet.set(hash)
        }
    }

    /**
     * Test if a signature might be malicious. 
     * If false, it is definitely safe (99.9% certainty).
     * If true, it MIGHT be malicious and should be run through Aho-Corasick or database.
     */
    fun mightContain(element: String): Boolean {
        val hashes = getHashes(element)
        for (hash in hashes) {
            if (!bitSet.get(hash)) {
                return false
            }
        }
        return true
    }

    private fun getHashes(element: String): IntArray {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val digest = md5Digest.digest(bytes)
        
        // Use portions of the MD5 hash to simulate multiple independent hash functions
        val hashes = IntArray(hashFunctionsCount)
        var h1 = 0
        var h2 = 0

        if (digest.size >= 8) {
            h1 = (digest[0].toInt() and 0xFF) or
                 ((digest[1].toInt() and 0xFF) shl 8) or
                 ((digest[2].toInt() and 0xFF) shl 16) or
                 ((digest[3].toInt() and 0xFF) shl 24)
            h2 = (digest[4].toInt() and 0xFF) or
                 ((digest[5].toInt() and 0xFF) shl 8) or
                 ((digest[6].toInt() and 0xFF) shl 16) or
                 ((digest[7].toInt() and 0xFF) shl 24)
        }

        for (i in 0 until hashFunctionsCount) {
            val combinedHash = h1 + (i * h2)
            hashes[i] = abs(combinedHash) % bitSetSize
        }
        
        return hashes
    }
}
