package `in`.org.projecteka.jataayu.consent

import org.apache.commons.io.FileUtils.readFileToString
import java.io.File
import java.io.IOException

class TestUtils {
    companion object {
        @Throws(IOException::class)
        fun readFile(fileName: String): String {
            return readFileToString(File(getFilePath(fileName)))
        }

        private fun getFilePath(fileName: String): String {
            return "src/sharedTest/resources/$fileName"
        }
    }
}
