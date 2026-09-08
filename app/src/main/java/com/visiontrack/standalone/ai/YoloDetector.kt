package com.visiontrack.standalone.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.visiontrack.standalone.model.Detection
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class YoloDetector(private val context: Context) : AutoCloseable {
    private val inputSize = 640

    private val labels: List<String> by lazy {
        context.assets.open("coco80.txt")
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    }

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null

    fun isModelAvailable(): Boolean = try {
        context.assets.open("yolo26n.onnx").close()
        true
    } catch (_: Exception) {
        false
    }

    private fun ensureSession(): OrtSession {
        session?.let { return it }

        if (!isModelAvailable()) {
            error("Model missing. Run tools\\prepare_model.ps1 and rebuild the APK.")
        }

        val bytes = context.assets.open("yolo26n.onnx").use { it.readBytes() }
        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(4)
        }

        return env.createSession(bytes, options).also { session = it }
    }

    fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.35f,
        iouThreshold: Float = 0.45f
    ): List<Detection> {
        val session = ensureSession()
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Ultralytics ONNX input is NCHW: [1, 3, 640, 640]
        val data = FloatArray(1 * 3 * inputSize * inputSize)
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val plane = inputSize * inputSize
        for (i in pixels.indices) {
            val pixel = pixels[i]
            data[i] = ((pixel shr 16) and 0xFF) / 255f
            data[plane + i] = ((pixel shr 8) and 0xFF) / 255f
            data[2 * plane + i] = (pixel and 0xFF) / 255f
        }

        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape).use { input ->
            val inputName = session.inputNames.first()
            session.run(mapOf(inputName to input)).use { results ->
                val value = results[0].value
                val raw = convertOutput(value)
                val candidates = parseOutput(raw, confidenceThreshold)
                val selected = nonMaxSuppression(candidates, iouThreshold)

                val sx = bitmap.width / inputSize.toFloat()
                val sy = bitmap.height / inputSize.toFloat()

                return selected.map {
                    it.copy(
                        left = it.left * sx,
                        top = it.top * sy,
                        right = it.right * sx,
                        bottom = it.bottom * sy
                    )
                }
            }
        }
    }

    private fun convertOutput(value: Any): Array<FloatArray> {
        // Typical YOLO26 ONNX output:
        // [1, 84, 8400] when exported with the default one-to-many head.
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is Array<*> -> {
                val outer = value as Array<*>
                if (outer.size != 1) {
                    error("Unexpected ONNX output batch size: ${outer.size}")
                }
                val first = outer[0]
                when (first) {
                    is Array<*> -> first.map { row ->
                        when (row) {
                            is FloatArray -> row
                            else -> error("Unexpected ONNX output row type: ${row?.javaClass}")
                        }
                    }.toTypedArray()
                    else -> error("Unexpected ONNX output structure: ${first?.javaClass}")
                }
            }
            else -> error("Unexpected ONNX output type: ${value.javaClass}")
        }
    }

    private fun parseOutput(raw: Array<FloatArray>, threshold: Float): List<Detection> {
        if (raw.isEmpty()) return emptyList()

        val rows = raw.size
        val cols = raw[0].size

        // End-to-end style output: [N, 6] = x1,y1,x2,y2,score,class_id
        if (cols == 6) {
            return raw.mapNotNull { row ->
                val score = row[4]
                if (score < threshold) return@mapNotNull null
                val classId = row[5].toInt().coerceIn(0, labels.lastIndex)
                Detection(
                    labels[classId],
                    score,
                    row[0], row[1], row[2], row[3]
                )
            }
        }

        // Standard Ultralytics detection output:
        // [84, N] or [N, 84]
        val featuresFirst = rows in 80..100 && cols > rows
        val numFeatures = if (featuresFirst) rows else cols
        val numBoxes = if (featuresFirst) cols else rows

        if (numFeatures < 84) {
            error("Unexpected YOLO ONNX output dimensions: [$rows, $cols]")
        }

        val detections = ArrayList<Detection>()

        for (i in 0 until numBoxes) {
            fun v(feature: Int): Float =
                if (featuresFirst) raw[feature][i] else raw[i][feature]

            val cx = v(0)
            val cy = v(1)
            val w = v(2)
            val h = v(3)

            var bestClass = -1
            var bestScore = 0f

            val classCount = min(labels.size, numFeatures - 4)
            for (c in 0 until classCount) {
                val score = v(4 + c)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            if (bestClass >= 0 && bestScore >= threshold) {
                detections += Detection(
                    label = labels[bestClass],
                    confidence = bestScore,
                    left = cx - w / 2f,
                    top = cy - h / 2f,
                    right = cx + w / 2f,
                    bottom = cy + h / 2f
                )
            }
        }

        return detections
    }

    private fun nonMaxSuppression(
        input: List<Detection>,
        iouThreshold: Float
    ): List<Detection> {
        val sorted = input.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected += best

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (candidate.label == best.label && iou(best, candidate) > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selected.take(50)
    }

    private fun iou(a: Detection, b: Detection): Float {
        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)
        val x2 = min(a.right, b.right)
        val y2 = min(a.bottom, b.bottom)

        val intersection = max(0f, x2 - x1) * max(0f, y2 - y1)
        val areaA = max(0f, a.right - a.left) * max(0f, a.bottom - a.top)
        val areaB = max(0f, b.right - b.left) * max(0f, b.bottom - b.top)

        return intersection / max(1e-6f, areaA + areaB - intersection)
    }

    override fun close() {
        session?.close()
        session = null
    }
}
