import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.format.MappingFormat
import java.io.File

fun main() {
    arrayOf("1.7.10", "1.5.2").forEach { version ->
        MappingWriter.create(File("$version.tiny").toPath(), MappingFormat.TINY_2).use {
            MappingReader.read(
                File("""C:\Users\NickAc\.gradle\caches\light-craft\$version\mappings\mappings-final.tinyv2""").toPath(),
                MappingDstNsReorder(it, "named")
            )
        }
    }
}