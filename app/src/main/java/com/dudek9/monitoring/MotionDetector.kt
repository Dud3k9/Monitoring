import java.io.File

class MotionDetector {

    companion object {

        fun motinPercent(array1: ByteArray, array2: ByteArray): Double {
            var negative: Double = 0.0
            for (i in 0..array1.size - 1) {
                if (Math.abs(array1[i] - array2[i]) > 5)
                    negative++
            }
            return (negative / array1.size * 100)
        }
    }

}