import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    arrayOf("1.8.9").forEach { version ->
        arrayOf("mappings-official-srg-named.tiny2", "mappings-official-named.tiny2").forEach { mappingFileName ->
            val inputFile = File("""L:\LightCraftMappings\$version\$mappingFileName""")
            val tree: MappingTree = MemoryMappingTree()
            MappingReader.read(inputFile.toPath(), tree as MappingVisitor)
            tree.classes.forEach { clazz ->
                clazz.methods.forEach sus@{ method ->
                    val comment = method.comment ?: return@sus
                    if (comment.isEmpty() || comment.isBlank()) {
                        method.comment = null
                    }
                }
                clazz.fields.forEach sus@{ f ->
                    val comment = f.comment ?: return@sus
                    if (comment.isEmpty() || comment.isBlank()) {
                        f.comment = null
                    }
                }
            }

            MappingWriter.create(inputFile.toPath(), MappingFormat.TINY_2).use {
                tree.accept(it)
            }

        }
    }
}