package com.safesphere.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device adult-content classifier (English + Hinglish).
 *
 *   val score = classifier.classify("tu kya kar rahi hai")   // 0.0 .. 1.0, higher = more explicit
 *
 * Create it ONCE (loading the model is slow) and reuse it. Call from a background thread.
 * Requires in build.gradle.kts:  androidResources { noCompress += "tflite" }
 */
class TextClassifier(context: Context, numThreads: Int = 2) : Closeable {

    private val tokenizer = WordPieceTokenizer.fromAssets(context)
    private val interpreter: Interpreter
    private val idsInput: Int
    private val maskInput: Int
    private val lock = Any()

    /** The decision threshold chosen during training (stored in tokenizer_config.json). */
    val threshold: Float get() = tokenizer.config.threshold

    init {
        val options = Interpreter.Options().setNumThreads(numThreads)
        interpreter = Interpreter(loadModelFile(context), options)

        // The converter may reorder inputs, so find them by name instead of assuming an order.
        var ids = -1
        var mask = -1
        for (i in 0 until interpreter.inputTensorCount) {
            val name = interpreter.getInputTensor(i).name()
            if (name.contains("attention_mask")) mask = i else if (name.contains("input_ids")) ids = i
        }
        if (ids == -1 || mask == -1) { // fall back to the order we declared in convert_to_tflite.py
            ids = 0
            mask = 1
        }
        idsInput = ids
        maskInput = mask
    }

    /** Returns the probability (0..1) that [text] is adult-toned. Blank text returns 0. */
    fun classify(text: String): Float {
        if (text.isBlank()) return 0f
        val encoded = tokenizer.encode(text)

        val inputs = arrayOfNulls<Any>(2)
        inputs[idsInput] = arrayOf(encoded.inputIds)       // shape [1, maxLen], int32
        inputs[maskInput] = arrayOf(encoded.attentionMask) // shape [1, maxLen], int32
        val output = Array(1) { FloatArray(1) }            // shape [1, 1], float32

        synchronized(lock) { // Interpreter is not thread-safe
            interpreter.runForMultipleInputsOutputs(inputs, mapOf<Int, Any>(0 to output))
        }
        return output[0][0].coerceIn(0f, 1f)
    }

    /** Convenience: true when the score is above the trained threshold. */
    fun isExplicit(text: String): Boolean = classify(text) > threshold

    override fun close() = synchronized(lock) { interpreter.close() }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        context.assets.openFd(MODEL_FILE).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    companion object {
        const val MODEL_FILE = "hinglish_classifier.tflite"
    }
}
