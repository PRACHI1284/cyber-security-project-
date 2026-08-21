package com.example.core.algorithms

import java.util.LinkedList
import java.util.Queue

/**
 * Aho-Corasick algorithm for multi-pattern string matching.
 * Scans a target text (like an Android manifest or log) against thousands of known malicious signatures in O(N + M) time.
 */
class AhoCorasick {
    private val root = TrieNode()

    class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var failureLink: TrieNode? = null
        val output = mutableListOf<String>()
    }

    /**
     * Add a known malicious string signature to the dictionary.
     */
    fun addKeyword(keyword: String) {
        var current = root
        for (char in keyword) {
            current = current.children.computeIfAbsent(char) { TrieNode() }
        }
        current.output.add(keyword)
    }

    /**
     * Build the failure links. Must be called after all keywords are added.
     */
    fun buildFailureLinks() {
        val queue: Queue<TrieNode> = LinkedList()
        
        // Initialize depth 1 nodes failure links to root
        for (child in root.children.values) {
            child.failureLink = root
            queue.add(child)
        }

        while (queue.isNotEmpty()) {
            val current = queue.poll()

            for ((char, child) in current.children) {
                var failure = current.failureLink
                while (failure != null && !failure.children.containsKey(char)) {
                    failure = failure.failureLink
                }
                
                child.failureLink = failure?.children?.get(char) ?: root
                
                // Merge outputs from the failure link
                child.failureLink?.output?.let {
                    child.output.addAll(it)
                }
                
                queue.add(child)
            }
        }
    }

    /**
     * Search the text for any occurrences of the added keywords.
     * Returns a map of matched keywords and their starting indices in the text.
     */
    fun search(text: String): Map<String, List<Int>> {
        val results = mutableMapOf<String, MutableList<Int>>()
        var current = root

        for (i in text.indices) {
            val char = text[i]
            
            while (current != root && !current.children.containsKey(char)) {
                current = current.failureLink ?: root
            }
            
            current = current.children[char] ?: root
            
            for (match in current.output) {
                val matchStartIndex = i - match.length + 1
                results.computeIfAbsent(match) { mutableListOf() }.add(matchStartIndex)
            }
        }
        
        return results
    }
}
