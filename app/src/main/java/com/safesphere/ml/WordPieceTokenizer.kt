package com.safesphere.ml

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Values written by train_classifier.py into assets/tokenizer/tokenizer_config.json */
data class TokenizerConfig(
    val maxLen: Int,
    val doLowerCase: Boolean,
    val clsId: Int,
    val sepId: Int,
    val padId: Int,
    val unkId: Int,
    val threshold: Float,
) {
    companion object {
        fun fromJson(json: String): TokenizerConfig {
            val o = JSONObject(json)
            return TokenizerConfig(
                maxLen = o.getInt("max_len"),
                doLowerCase = o.getBoolean("do_lower_case"),
                clsId = o.getInt("cls_id"),
                sepId = o.getInt("sep_id"),
                padId = o.getInt("pad_id"),
                unkId = o.getInt("unk_id"),
                threshold = o.getDouble("threshold").toFloat(),
            )
        }
    }
}

/**
 * A Kotlin re-implementation of the BERT WordPiece tokenizer (what Hugging Face does in Python).
 * It MUST give the same ids as Python - run TokenizerParityTest to prove it.
 *
 * Pipeline: clean text -> (space out CJK) -> split on whitespace -> lowercase + strip accents
 *           -> split punctuation -> WordPiece (longest-match-first) -> [CLS] ... [SEP] + padding
 */
class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    val config: TokenizerConfig,
) {
    class Encoded(val inputIds: IntArray, val attentionMask: IntArray)

    fun encode(text: String): Encoded {
        val maxBody = config.maxLen - 2 // room for [CLS] and [SEP]
        val pieces = ArrayList<Int>()
        for (word in basicTokenize(text)) {
            wordPiece(word, pieces)
            if (pieces.size >= maxBody) break
        }
        val body = if (pieces.size > maxBody) pieces.subList(0, maxBody) else pieces

        val ids = IntArray(config.maxLen) { config.padId }
        val mask = IntArray(config.maxLen)
        ids[0] = config.clsId
        mask[0] = 1
        for (i in body.indices) {
            ids[i + 1] = body[i]
            mask[i + 1] = 1
        }
        ids[body.size + 1] = config.sepId
        mask[body.size + 1] = 1
        return Encoded(ids, mask)
    }

    // ---------------------------------------------------------------- basic tokenizer
    private fun basicTokenize(text: String): List<String> {
        val spaced = addSpacesAroundCjk(cleanText(text))
        val out = ArrayList<String>()
        for (token in spaced.split(' ')) {
            if (token.isEmpty()) continue
            var t = token
            if (config.doLowerCase) {
                t = stripAccents(t.lowercase(Locale.ROOT))
            }
            out.addAll(splitOnPunctuation(t))
        }
        return out
    }

    private fun cleanText(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == 0 || cp == 0xFFFD || isControl(cp)) continue
            if (isWhitespace(cp)) sb.append(' ') else sb.appendCodePoint(cp)
        }
        return sb.toString()
    }

    private fun addSpacesAroundCjk(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (isCjk(cp)) sb.append(' ').appendCodePoint(cp).append(' ') else sb.appendCodePoint(cp)
        }
        return sb.toString()
    }

    private fun stripAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var i = 0
        while (i < decomposed.length) {
            val cp = decomposed.codePointAt(i)
            i += Character.charCount(cp)
            if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) sb.appendCodePoint(cp)
        }
        return sb.toString()
    }

    private fun splitOnPunctuation(token: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < token.length) {
            val cp = token.codePointAt(i)
            i += Character.charCount(cp)
            if (isPunctuation(cp)) {
                if (current.isNotEmpty()) {
                    out.add(current.toString())
                    current.setLength(0)
                }
                out.add(String(Character.toChars(cp)))
            } else {
                current.appendCodePoint(cp)
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    // ------------------------------------------------------------------- WordPiece
    private fun wordPiece(word: String, out: MutableList<Int>) {
        if (word.codePointCount(0, word.length) > MAX_CHARS_PER_WORD) {
            out.add(config.unkId)
            return
        }
        val found = ArrayList<Int>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var match = -1
            while (start < end) {
                var sub = word.substring(start, end)
                if (start > 0) sub = "##$sub"
                val id = vocab[sub]
                if (id != null) {
                    match = id
                    break
                }
                end = word.offsetByCodePoints(end, -1)
            }
            if (match == -1) { // no piece fits -> the WHOLE word becomes [UNK]
                out.add(config.unkId)
                return
            }
            found.add(match)
            start = end
        }
        out.addAll(found)
    }

    // --------------------------------------------------------------- char classes
    private fun isWhitespace(cp: Int): Boolean =
        cp == ' '.code || cp == '\t'.code || cp == '\n'.code || cp == '\r'.code ||
            Character.getType(cp) == Character.SPACE_SEPARATOR.toInt()

    private fun isControl(cp: Int): Boolean {
        if (cp == '\t'.code || cp == '\n'.code || cp == '\r'.code) return false
        return when (Character.getType(cp)) {
            Character.CONTROL.toInt(), Character.FORMAT.toInt(), Character.SURROGATE.toInt(),
            Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt() -> true
            else -> false
        }
    }

    private fun isPunctuation(cp: Int): Boolean {
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return when (Character.getType(cp)) {
            Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt() -> true
            else -> false
        }
    }

    private fun isCjk(cp: Int): Boolean =
        cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
            cp in 0x2A700..0x2B73F || cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF ||
            cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F

    companion object {
        private const val MAX_CHARS_PER_WORD = 100
        private const val VOCAB_PATH = "tokenizer/vocab.txt"
        private const val CONFIG_PATH = "tokenizer/tokenizer_config.json"

        /** Loads vocab.txt + tokenizer_config.json from app/src/main/assets/tokenizer/ */
        fun fromAssets(context: Context): WordPieceTokenizer {
            val config = context.assets.open(CONFIG_PATH).bufferedReader().use {
                TokenizerConfig.fromJson(it.readText())
            }
            val vocab = HashMap<String, Int>(40_000)
            context.assets.open(VOCAB_PATH).bufferedReader().useLines { lines ->
                var index = 0
                for (line in lines) {
                    vocab[line.trimEnd('\r', '\n')] = index++
                }
            }
            return WordPieceTokenizer(vocab, config)
        }
    }
}
