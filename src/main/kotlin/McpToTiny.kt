import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.format.SrgReader
import net.fabricmc.mappingio.format.Tiny2Writer
import net.fabricmc.mappingio.tree.ClassAnalysisDescCompleter
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

val officialNamespace = "official"
val srgNamespace = "srg"
val namedNamespace = "named"

fun main() {
    arrayOf("1.6.4").forEach { version ->
        val configDir = File("""L:\Work-MCP\MCP\$version\conf""")
        val oneDotEightConfigDir = File("""L:\Work-MCP\MCP\1.8.9\conf""")
        val tree = getSrgTree(configDir)

        val treeOneDotEight = version.takeIf { it == "1.5.2" || it == "1.6.4" }?.let { getSrgTree(oneDotEightConfigDir) }

        ClassAnalysisDescCompleter.process(File("M:\\client ($version).jar").toPath(), officialNamespace, tree)

        val methodsCsv = csvReader().readAllWithHeader(File(configDir, "methods.csv"))
        val fieldsCsv = csvReader().readAllWithHeader(File(configDir, "fields.csv"))

        tree.visitNamespaces(srgNamespace, mutableListOf(srgNamespace, namedNamespace))

        val srgNamespaceId = tree.getNamespaceId(srgNamespace)
        val namedNamespaceId = tree.getNamespaceId(namedNamespace)

        if (true) {
            //com/google/common/base/Objects	com/google/common/base/MoreObjects
            tree.visitNamespaces(officialNamespace, listOf(srgNamespace))
            tree.visitClass("com/google/common/base/Objects")
            tree.visitDstName(MappedElementKind.CLASS, srgNamespaceId, "com/google/common/base/MoreObjects")
        }

        (tree as MappingTree).classes.forEach { clazz ->
            applySrg(tree, methodsCsv, clazz, srgNamespaceId, namedNamespaceId)
            applySrg(tree, fieldsCsv, clazz, srgNamespaceId, namedNamespaceId, isMethod = false)
        }

        val mappingsDir =
            File("M:${File.separatorChar}LightCraft${File.separatorChar}LightCraftMappings${File.separatorChar}${version}")

        Tiny2Writer(File(mappingsDir, "mappings-official-srg-named.tiny2").writer(), false).use {
            tree.accept(mappingsExporter(tree, srgNamespaceId, namedNamespaceId, it, treeOneDotEight))
        }

        if (true)
            Tiny2Writer(File(mappingsDir, "mappings-official-named.tiny2").writer(), false).use {
                tree.accept(
                    mappingsExporter(
                        tree,
                        srgNamespaceId,
                        namedNamespaceId,
                        MappingDstNsReorder(it, namedNamespace),
                        treeOneDotEight
                    )
                )
            }
    }
}

private fun mappingsExporter(
    tree: MemoryMappingTree,
    srgNamespaceId: Int,
    namedNamespaceId: Int,
    writer: MappingVisitor,
    treeOneDotEight: MemoryMappingTree?
) = MissingDestFixer(
    tree,
    srgNamespaceId,
    namedNamespaceId,
    OneDotEightNamesFixer(tree, srgNamespaceId, namedNamespaceId, writer, treeOneDotEight)
)

private fun getSrgTree(configDir: File): MemoryMappingTree {
    val obfToSrgFile =
        File(configDir, "joined.srg").takeIf { it.exists() } ?: File(configDir, "client.srg").takeIf { it.exists() }
        ?: throw Exception("Unable to find official -> srg file")

    val tree = MemoryMappingTree()
    SrgReader.read(obfToSrgFile.reader(), officialNamespace, srgNamespace, tree)
    return tree
}

private fun applySrg(
    tree: MemoryMappingTree,
    csv: List<Map<String, String>>,
    clazz: MappingTree.ClassMapping,
    srgNamespaceId: Int,
    namedNamespaceId: Int,
    isMethod: Boolean = true
) {
    val knownNames: Map<String, String> =
        csv.map { it["searge"] to it["name"] }.filter { it.first != null && it.second != null }
            .toMap() as Map<String, String>

    csv.forEach { csvValue ->
        val name = csvValue["searge"]
        val namedName = csvValue["name"]
        val javadocComment = csvValue["desc"]

//        if (isMethod && clazz.methods.firstOrNull() { it.getDstName(srgNamespaceId) == name } != null) {
//            println("method")
//        }
        val members: List<MappingTree.MemberMapping> =
            (if (isMethod) clazz.methods else clazz.fields).filter { it.getDstName(srgNamespaceId) == name }
        members.forEach { member ->
            member.setDstName(namedName, namedNamespaceId)
            javadocComment?.also {
                var finalComment = it
                knownNames.forEach { (original, named) ->
                    finalComment = finalComment.replace(original, named)
                }

                member.comment = finalComment
            }
        }
    }
}
