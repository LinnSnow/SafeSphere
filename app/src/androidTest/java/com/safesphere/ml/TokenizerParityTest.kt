package com.safesphere.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the Kotlin tokenizer produces EXACTLY the ids Python produced.
 * Python wrote assets/tokenizer/tokenizer_parity_tests.json (train_classifier.py).
 * Run: Android Studio > right-click this file > Run (needs an emulator/phone).
 */
@RunWith(AndroidJUnit4::class)
class TokenizerParityTest {

    @Test
    fun kotlinTokenizerMatchesPython() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tokenizer = WordPieceTokenizer.fromAssets(context)

        val cases = JSONArray(context.assets.open("tokenizer/tokenizer_parity_tests.json").bufferedReader().readText())
        assertTrue("parity file is empty", cases.length() > 0)

        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val text = case.getString("text")
            val expected = case.getJSONArray("input_ids").let { arr -> IntArray(arr.length()) { arr.getInt(it) } }
            val actual = tokenizer.encode(text).inputIds
            assertEquals("length differs for: \"$text\"", expected.size, actual.size)
            for (j in expected.indices) {
                assertEquals("id mismatch at position $j for: \"$text\"", expected[j], actual[j])
            }
        }
    }
}
