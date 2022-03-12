import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import exceptor.ExceptorFile
import exceptor.ExceptorParser
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.format.SrgReader
import net.fabricmc.mappingio.format.Tiny2Writer
import net.fabricmc.mappingio.tree.ClassAnalysisDescCompleter
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files

val officialNamespace = "official"
val srgNamespace = "srg"
val namedNamespace = "named"

fun main() {
    arrayOf("1.8.9").forEach { version ->
        val configDir = File("""L:\Work-MCP\$version\conf""")
        val oneDotEightConfigDir = File("""L:\Work-MCP\1.8.9\conf""")
        var tree = getSrgTree(configDir)

        val treeOneDotEight =
            version.takeIf { it == "1.5.2" || it == "1.6.4" }?.let { getSrgTree(oneDotEightConfigDir) }

        val jarPath = File("M:\\client ($version).jar").toPath()
        ClassAnalysisDescCompleter.process(jarPath, officialNamespace, tree)

        val fs = FileSystems.newFileSystem(jarPath)

        val methodsCsv = csvReader().readAllWithHeader(File(configDir, "methods.csv"))
        val fieldsCsv = csvReader().readAllWithHeader(File(configDir, "fields.csv"))
        val exceptor = getMcpFileWithExtension(configDir, "exc")?.let { ExceptorParser.parse(it) }

        tree.visitNamespaces(srgNamespace, mutableListOf(srgNamespace, namedNamespace))

        val srgNamespaceId = tree.getNamespaceId(srgNamespace)
        val namedNamespaceId = tree.getNamespaceId(namedNamespace)

        if (true) {
            //com/google/common/base/Objects	com/google/common/base/MoreObjects
            tree.visitNamespaces(officialNamespace, listOf(srgNamespace))
            tree.visitClass("com/google/common/base/Objects")
            tree.visitDstName(MappedElementKind.CLASS, srgNamespaceId, "com/google/common/base/MoreObjects")
        }

        tree = completeSingleNs(tree, "srg", "official")
        tree = completeSingleNs(tree, "named", "srg")

        (tree as MappingTree).classes.forEach { clazz ->
            applySrg(tree, methodsCsv, clazz, srgNamespaceId, namedNamespaceId)
            applySrg(tree, fieldsCsv, clazz, srgNamespaceId, namedNamespaceId, isMethod = false)
            if (exceptor != null) {
                applyExceptor(tree, exceptor, clazz, srgNamespaceId, namedNamespaceId, fs)
            }
        }

        val mappingsDir =
            File("L:\\LightCraftMappings\\$version")

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

fun applyExceptor(
    tree: MemoryMappingTree,
    exceptor: ExceptorFile,
    clazz: MappingTree.ClassMapping,
    srgNamespaceId: Int,
    namedNamespaceId: Int,
    fs: FileSystem
) {
    val clazzPath = fs.getPath("/${clazz.srcName}.class")
    if (Files.notExists(clazzPath)) {
        println("No class file for ${clazz.srcName}(${clazz.getDstName(namedNamespaceId)})")
        return
    }

    val exceptorClass = exceptor.classEntries[clazz.getDstName(namedNamespaceId)]
    if (exceptorClass == null) {
       // println("No exceptor class for ${clazz.getDstName(namedNamespaceId)}")
        return
    }
    val classNode = ClassNode(Opcodes.ASM9)
    ClassReader(Files.readAllBytes(clazzPath)).accept(classNode, 0)

    //println("Applying exceptor for ${clazz.getDstName(namedNamespaceId)}")
    exceptorClass.methods.forEach { (name, exceptorMethod) ->
        val mappingMethod = clazz.methods.firstOrNull { (it.getName(srgNamespaceId) + it.getDstDesc(srgNamespaceId)) == name }

        if (mappingMethod != null) {

            val args = exceptorMethod.params
            val methodNode =
                classNode.methods.firstOrNull { it.name == mappingMethod.srcName && it.desc == mappingMethod.srcDesc }
                    ?: return

            val access = methodNode.access
            val isInterface = (classNode.access and Opcodes.ACC_INTERFACE) != 0
            val isMethodStatic = (access and Opcodes.ACC_STATIC) != 0 && !isInterface

            if (!isMethodStatic) {
                mappingMethod.removeArg(0, 0, null)
            }

            args.forEachIndexed { index, argName ->
                tree.visitClass(clazz.srcName)
                tree.visitMethod(mappingMethod.srcName, mappingMethod.srcDesc)
                tree.visitMethodArg(index, index + (if (isMethodStatic) 0 else 1), "arg${index}")
                tree.dstNamespaces.forEach { destNs ->
                    val nsId = tree.getNamespaceId(destNs)
                    tree.visitDstName(MappedElementKind.METHOD_ARG, nsId, argName)
                }
            }
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
        getMcpFileWithExtension(configDir, "srg")
            ?: throw Exception("Unable to find official -> srg file")

    val tree = MemoryMappingTree()
    SrgReader.read(obfToSrgFile.reader(), officialNamespace, srgNamespace, tree)
    return tree
}

private fun getMcpFileWithExtension(configDir: File, extension: String) =
    File(configDir, "joined.$extension").takeIf { it.exists() } ?: File(
        configDir,
        "client.$extension"
    ).takeIf { it.exists() }

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
