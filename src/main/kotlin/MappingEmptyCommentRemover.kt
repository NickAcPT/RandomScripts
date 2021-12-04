import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    arrayOf("1.5.2", "1.6.4", "1.7.10", "1.8.9").forEach { version ->
        val inputFile = File("""L:\LightCraftMappings\$version\mappings-official-srg-named.tiny2""")
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