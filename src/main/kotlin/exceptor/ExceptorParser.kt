package exceptor

import java.io.File

object ExceptorParser {
    fun parse(file: File): ExceptorFile {
        val lines = file.readLines()
        val filteredLines = lines.filterNot { it.startsWith("#") }
        val classEntries = mutableMapOf<String, ClassEntry>()
        val exceptorFile = ExceptorFile(classEntries)
        filteredLines.forEach {
            val className = it.substringBefore(".")
            val methodDescriptor = it.substringAfter(".").substringBefore("=")
            val exceptions = it.substringAfter("=").substringBefore("|")
            val params = it.substringAfter("|").split(",").toMutableList()
            if (it.substringAfter("|").isBlank())
                params.clear()

            val methodEntry = MethodEntry(className, methodDescriptor, exceptions, params)
            classEntries.getOrPut(className) {
                ClassEntry(
                    className,
                    mutableMapOf()
                )
            }.methods[methodEntry.methodDescriptor] = methodEntry
        }

        return exceptorFile
    }

}