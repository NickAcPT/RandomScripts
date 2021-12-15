
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    arrayOf("1.5.2").forEach { version ->
        MappingWriter.create(File("$version.tiny").toPath(), MappingFormat.TINY_2).use {
            val tree = MemoryMappingTree()
            MappingReader.read(
                File("""L:\LightCraftMappings\$version\mappings-official-srg-named.tiny2""").toPath(),
                tree
            )

            tree.accept(MappingDstNsReorder(it, "named"))
        }
    }
}