import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

fun main() {
    File("L:\\LightCraftMappings").toPath().fileSystem.also { fs ->
        Files.walkFileTree(fs.getPath("L:\\LightCraftMappings"), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (file.toAbsolutePath().toString().contains(".git")) return FileVisitResult.CONTINUE
                val tree = MemoryMappingTree()
                MappingReader.read(file, MappingFormat.TINY_2, tree)
                Files.delete(file)
                val writer = MappingWriter.create(file, MappingFormat.TINY_2)
                val value = object : ForwardingMappingVisitor(writer) {
                    override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String?) {
                        if (targetKind == MappedElementKind.CLASS && name != null && !name.contains('/')) {
                            //This is a root package class
                            return super.visitDstName(targetKind, namespace, "net/minecraft/client/misc/${name}")
                        }
                        super.visitDstName(targetKind, namespace, name)
                    }
                }

                tree.accept(value)
                writer.close()
                return FileVisitResult.CONTINUE
            }
        })
    }
}