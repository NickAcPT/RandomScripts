
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    val mappingUrls = listOf(
        "https://raw.githubusercontent.com/CLModding/intermediary/main/intermediary/60d7e254e0b9202649beea30ec43a40f.tiny",
        "https://raw.githubusercontent.com/CLModding/mappings-new/master/legacy/client.tiny",
        "https://raw.githubusercontent.com/CLModding/mappings-new/master/legacy/craftlandia.tiny"
    )
    val tree = MemoryMappingTree()

    mappingUrls.forEachIndexed() { i, it ->
        val file = File("mapping-$i")
        //file.writeBytes(URL(it).readBytes())

        MappingReader.read(file.toPath(), MappingReader.detectFormat(file.toPath()), tree)
        println()
    }

    val writer = MappingWriter.create(File("mapping-out.tinyv2").toPath(), MappingFormat.TINY_2)

    tree.accept(writer)
}