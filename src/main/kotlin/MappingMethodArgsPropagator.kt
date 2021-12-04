import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

fun main() {
    arrayOf("1.5.2", "1.7.10", "1.8.9").forEach ver@{ version ->
        val inputFile = File("""L:\LightCraftMappings\$version\mappings-official-srg-named.tiny2""")
        val tree: MappingTree = MemoryMappingTree()
        MappingReader.read(inputFile.toPath(), tree as MappingVisitor)
        val fs =
            FileSystems.newFileSystem(File("""C:\Users\NickAc\.gradle\caches\light-craft\$version\minecraft.jar""").toPath())
        val regex = Regex("(?:,\\s*|Arg(?:ument)?s\\s*:\\s*)([a-zA-Z0-9]+)")

        tree.classes.forEach { clazz ->
            val clazzPath = fs.getPath("/${clazz.srcName}.class")
            if (Files.notExists(clazzPath)) {
                println("Skipped ${clazz.srcName}")
                return@forEach
            }
            val classNode = ClassNode(Opcodes.ASM9)
            ClassReader(Files.readAllBytes(clazzPath)).accept(classNode, 0)
            clazz.methods.forEach sus@{ method ->
                val methodNode =
                    classNode.methods.firstOrNull { it.name == method.srcName && it.desc == method.srcDesc }
                        ?: return@sus
                val access = methodNode.access
                val isInterface = (classNode.access and Opcodes.ACC_INTERFACE) != 0
                val isMethodStatic = (access and Opcodes.ACC_STATIC) != 0 && !isInterface

                val comment = method.comment?.takeIf { it.matches(Regex(".+Arg(?:ument)?s\\s*:.+")) } ?: return@sus
                val commentToMatch = Regex("Arg(?:ument)?s\\s*:.+").find(comment)?.value ?: return@sus
                val args = regex.findAll(commentToMatch).toList().takeIf { it.isNotEmpty() } ?: return@sus

                if (!isMethodStatic) {
                    method.removeArg(0, 0, null)
                }

                args.forEachIndexed { index, matchResult ->
                    tree.visitClass(clazz.srcName)
                    tree.visitMethod(method.srcName, method.srcDesc)
                    tree.visitMethodArg(index, index + (if (isMethodStatic) 0 else 1), "arg${index}")
                    tree.dstNamespaces.forEach { destNs ->
                        val nsId = tree.getNamespaceId(destNs)
                        tree.visitDstName(MappedElementKind.METHOD_ARG, nsId, matchResult.groupValues[1])
                    }
                }
            }
        }

        fs.close()

        MappingWriter.create(inputFile.toPath(), MappingFormat.TINY_2).use {
            tree.accept(it)
        }
    }
}