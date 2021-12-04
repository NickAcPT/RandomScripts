import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
import net.fabricmc.mappingio.format.Tiny2Writer
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    val oneEightTree = getMappingTree(File("""R:\Work\1.8.9\client-latest.tinyv2"""))

    val mcpFile = File("""R:\Work\LegacyCraftMappings\1.5.2\client-old.tinyv2""").toPath()
    val detectFormat = MappingReader.detectFormat(mcpFile)
    val fileWriter = File("""R:\Work\LegacyCraftMappings\1.5.2\client.tinyv2""").writer()
    val writer = Tiny2Writer(fileWriter, false)


    val value = object : ForwardingMappingVisitor(writer) {
        val duplicateNames = mutableListOf<String>()
        val newMcpTree = oneEightTree as MappingTree
        val names = newMcpTree.classes.mapNotNull { kotlin.runCatching { it.getDstName(0) }.getOrNull() }

        override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String) {
            if (targetKind == MappedElementKind.CLASS) {

                val className = name.removePrefix("net/minecraft/src/")
                var finalName = names.firstOrNull { it.substringAfterLast('/') == className } ?: name

                if (name.startsWith("net/minecraft/client/") || name.startsWith("net/minecraft/server/"))
                    finalName = name

                if (!finalName.contains("/")) {
                    finalName = "net/minecraft/$finalName"
                }
                if (className.startsWith("Packet")) {
                    finalName = "net/minecraft/network/$className"
                }
                if (className.startsWith("Gui")) {
                    finalName = "net/minecraft/client/gui/$className"
                }
                if (className.startsWith("Block")) {
                    finalName = "net/minecraft/block/$className"
                }
                if (className.startsWith("Item")) {
                    finalName = "net/minecraft/item/$className"
                }
                if (className.startsWith("Render")) {
                    finalName = "net/minecraft/client/render/$className"
                }

                if (finalName.contains("net/minecraft/src/")) {
                    finalName = "net/minecraft/$className"
                }

                val duplicateCount = duplicateNames.count { it == finalName }
                duplicateNames.add(finalName)
                if (duplicateCount > 0)
                    finalName += duplicateCount

                println(finalName)
                super.visitDstName(targetKind, namespace, finalName)
                return
            }
            super.visitDstName(targetKind, namespace, name)
        }
    }

    MappingReader.read(mcpFile, detectFormat, value)
    fileWriter.close()
}


private fun getMappingTree(file: File): MemoryMappingTree {
    val mcpFile = file.toPath()
    val tree = MemoryMappingTree()
    MappingReader.read(mcpFile, MappingReader.detectFormat(mcpFile), tree)

    return tree
}