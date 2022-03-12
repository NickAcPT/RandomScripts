import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import java.io.File

fun main() {
    val srg = readMappingFromFile(File("""L:\1.8.9_srg.tiny"""))
    var named = readMappingFromFile(File("""L:\1.8.9_named.tiny"""))

    srg.accept(named)
    named = keepDestNs(named, "srg", "named")
    named = completeSingleNs(named, "srg", "official")
    named = completeSingleNs(named, "named", "srg")

    MappingWriter.create(
        File("""L:\LightCraftMappings\1.8.9\mappings-official-srg-named.tiny2""").toPath(),
        MappingFormat.TINY_2
    ).use {
        named.accept(it)
    }
}