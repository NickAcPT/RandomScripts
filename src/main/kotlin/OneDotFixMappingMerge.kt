import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.*
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {
    val oneDotFiveTree = readOriginalIntermediaryMerged("1.5.2")
    val oneDotEightTree = readOriginalIntermediaryMerged("1.8.9")

    val intermediaryTree = MemoryMappingTree()
    println("Merging intermediary mappings...")
    oneDotFiveTree.accept(intermediaryTree)
    oneDotEightTree.accept(intermediaryTree)

    val tree = MemoryMappingTree()
    intermediaryTree.accept(
        MappingSourceNsSwitch(
            MappingDstNsReorder(
            MappingNsRenamer(
                MissingDescFilter(tree), mapOf(
                    "intermediary" to "srg",
                    "named_1.5.2" to "named",
                    "named_1.8.9" to "named_one_dot_eight",
                )
            ),
                "intermediary",
                "named_1.5.2",
                "named_1.8.9"
            ), "official_1.5.2"
        )
    )

    println("Removing non-existent mappings...")
    val mappingTree = tree as MappingTree
    val clonedClasses = mappingTree.classes.toList()
    clonedClasses.forEach { clazz ->
        if (clazz.getDstName(tree.getNamespaceId("srg")) == null) {
            mappingTree.removeClass(clazz.srcName)
        } else {
            val clonedMethods = clazz.methods.toList()
            clonedMethods.forEach { method ->
                if (method.srcName.contains("method_") || method.getDstName(tree.getNamespaceId("srg"))
                        .isNullOrEmpty() || method.getDstDesc(tree.getNamespaceId("named")).isNullOrEmpty()
                ) {
                    clazz.removeMethod(method.srcName, method.srcDesc)
                } else {
                    if (method.getDstName(tree.getNamespaceId("named_one_dot_eight"))?.isMethodYeet() == true) {
                        println("Removing mcp mapping for ${clazz.srcName}.${method.srcName}")
                        method.setDstName(null, tree.getNamespaceId("named_one_dot_eight"))
                    }
                }
            }
            val clonedFields = clazz.fields.toList()
            clonedFields.forEach { field ->
                if (field.srcName.contains("field_") || field.getDstName(tree.getNamespaceId("srg"))
                        .isNullOrEmpty() || field.getDstName(tree.getNamespaceId("named")).isNullOrEmpty()
                ) {
                    clazz.removeField(field.srcName, field.srcDesc)
                } else {
                    if (field.getDstName(tree.getNamespaceId("named_one_dot_eight"))?.startsWith("field_") == true) {
                        field.setDstName(null, tree.getNamespaceId("named_one_dot_eight"))
                    }
                }
            }
        }
    }

    println("Writing merged mappings...")
    val writer = MappingWriter.create(File("one_dot_five_merged_one_dot_eight.tiny").toPath(), MappingFormat.TINY_2)
    writer.use {
        tree.accept(
            mappingsExporter(
                tree,
                tree.getNamespaceId("named"),
                tree.getNamespaceId("named_one_dot_eight"),
                it
            )
        )
    }
    println("Done!")
}

private fun CharSequence?.isMethodYeet(): Boolean =
    this?.startsWith("func_") == true || this?.startsWith("method_") == true


private fun mappingsExporter(
    tree: MemoryMappingTree,
    srgNamespaceId: Int,
    namedNamespaceId: Int,
    writer: MappingVisitor,
) = MissingDestFixer(
    tree,
    srgNamespaceId,
    namedNamespaceId,
    MappingNsCompleter(
        MappingNsRenamer(
            MappingDstNsReorder(
                MappingNsRenamer(
                    MappingNsCompleter(writer, mapOf("named" to "srg")),
                    mapOf("named_one_dot_eight" to "named")
                ),
                listOf("srg", "named_one_dot_eight")
            ), mapOf("official_1.5.2" to "official")
        ),
        mapOf(
            "named" to "named_one_dot_eight",
            "srg" to "named_one_dot_eight"
        )
    )
)

fun readOriginalIntermediaryMerged(version: String): MemoryMappingTree {
    val resultTree = MemoryMappingTree()
        .also { result ->
            val intermediaryMapping = readIntermediaryMapping(version)
            intermediaryMapping.accept(MappingSourceNsSwitch(result, "intermediary"))
            val originalMapping = readOriginalMapping(version)
            originalMapping.accept(
                MappingNsRenamer(
                    MappingSourceNsSwitch(result, "intermediary"),
                    mapOf("srg_1.5.2" to "intermediary")
                )
            )
        }

    return resultTree
}

fun readIntermediaryMapping(version: String): MemoryMappingTree {
    return readMappingFromFile(File("""L:\Legacy-Intermediaries\mappings\$version.tiny"""), version)
}

fun readOriginalMapping(version: String): MemoryMappingTree {
    return readMappingFromFile(File("""L:\LightCraftMappings\$version\mappings-official-srg-named.tiny2"""), version)
}

private fun readMappingFromFile(file: File, version: String): MemoryMappingTree {
    val mappingTree = MemoryMappingTree()
    val visitor = MappingNsRenamer(
        mappingTree,
        mapOf("official" to "official_$version", "srg" to "intermediary", "named" to "named_$version")
    )
    return file.reader().use {
        MappingReader.read(it, MappingReader.detectFormat(file.toPath()), visitor)
        mappingTree
    }
}