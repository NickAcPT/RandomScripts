import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.adapter.MappingNsCompleter
import net.fabricmc.mappingio.adapter.MappingNsRenamer
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File

fun main() {

    println("Reading original mappings...")
    var oneDotFive = readOriginalMapping("1.5.2")
    oneDotFive = renameSingleNs(oneDotFive, "official", "official_1.5.2")
    oneDotFive = renameSingleNs(oneDotFive, "srg", "srg_1.5.2")
    oneDotFive = renameSingleNs(oneDotFive, "named", "named_1.5.2")

    var oneDotEight = readOriginalMapping("1.8.9")
    oneDotEight = renameSingleNs(oneDotEight, "official", "official_1.8.9")
    oneDotEight = renameSingleNs(oneDotEight, "srg", "srg_1.8.9")
    oneDotEight = renameSingleNs(oneDotEight, "named", "named_1.8.9")

    println("Reading intermediary mapping files...")
    var oneDotFiveIntermediary = readIntermediaryMapping("1.5.2")
    oneDotFiveIntermediary = renameSingleNs(oneDotFiveIntermediary, "official", "official_1.5.2")
    oneDotFiveIntermediary = mergeMapping(oneDotFiveIntermediary, oneDotFive)

    println("Merged 1.5.2 intermediary mapping file with 1.5.2 original mapping file")
    oneDotFiveIntermediary = switchIntermediaryToSource(oneDotFiveIntermediary)

    var oneDotEightIntermediary = readIntermediaryMapping("1.8.9")
    oneDotEightIntermediary = renameSingleNs(oneDotEightIntermediary, "official", "official_1.8.9")
    oneDotEightIntermediary = mergeMapping(oneDotEightIntermediary, oneDotEight)

    println("Merged 1.8.9 intermediary mapping file with 1.8.9 original mapping file")
    oneDotEightIntermediary = switchIntermediaryToSource(oneDotEightIntermediary)

    println("Merging mappings")
    var merged = mergeMapping(oneDotFiveIntermediary, oneDotEightIntermediary)
    merged = renameSingleNs(merged, "official_1.5.2", "official")
    merged = keepDestNs(merged, "official", "srg_1.5.2", "srg_1.8.9", "named_1.5.2", "named_1.8.9")
    merged = renameSingleNs(merged, "srg_1.5.2", "srg")

    merged = switchSourceNs(merged, "official")
    removeUnneededMembers(merged)
    merged = completeSingleNs(merged, "named_1.5.2", "named_1.8.9")
    merged = completeSingleNs(merged, "srg", "srg_1.8.9")

    merged = renameSingleNs(merged, "named_1.5.2", "named")

    merged = keepDestNs(merged, "srg", "named")
    merged = completeSingleNs(merged, "srg", "official")
    merged = completeSingleNs(merged, "named", "srg")

    val writer = MappingWriter.create(File("one_dot_five_merged_one_dot_eight.tiny").toPath(), MappingFormat.TINY_2)
    println("Writing merged mappings")
    var writerVisitor: MappingVisitor = writer

    merged.accept(writerVisitor)
    writer.close()
}

private fun removeUnneededMembers(merged: MemoryMappingTree) {
    val mappingTree = merged as MappingTree
    val clonedClasses = mappingTree.classes.toList()
    clonedClasses.forEach { clazz ->
        if (clazz.getDstName(merged.getNamespaceId("srg")) == null) {
            mappingTree.removeClass(clazz.srcName)
        } else {
            val clonedMethods = clazz.methods.toList()
            clonedMethods.forEach { method ->
                if (method.srcName.contains("method_")) {
                    clazz.removeMethod(method.srcName, method.srcDesc)
                } else {
                    if (method.getDstName(merged.getNamespaceId("named_1.8.9")) != null) {
                        method.setDstName(null, merged.getNamespaceId("named_1.5.2"))
                    }
                }
            }
            val clonedFields = clazz.fields.toList()
            clonedFields.forEach { field ->
                if (field.srcName.contains("field_")) {
                    clazz.removeField(field.srcName, field.srcDesc)
                }
            }
        }
    }
}

fun completeSingleNs(tree: MemoryMappingTree, src: String, dest: String) = MemoryMappingTree().apply {
    tree.accept(MappingNsCompleter(this, mapOf(src to dest), true))
}

fun completeDestNs(tree: MemoryMappingTree, src: String, dest: String) = MemoryMappingTree().apply {
    tree.accept(MissingDestFixer(tree, tree.getNamespaceId(src), tree.getNamespaceId(dest), this))
}

fun keepDestNs(tree: MemoryMappingTree, vararg destNs: String): MemoryMappingTree = MemoryMappingTree().apply {
    tree.accept(MappingDstNsReorder(this, destNs.toList()))
}

fun mergeMapping(first: MemoryMappingTree, second: MemoryMappingTree): MemoryMappingTree = MemoryMappingTree().apply {
    first.accept(this)
    second.accept(this)
}

fun renameSingleNs(tree: MemoryMappingTree, original: String, dest: String) = MemoryMappingTree().apply {
    tree.accept(MappingNsRenamer(this, mapOf(original to dest)))
}


private fun switchIntermediaryToSource(intermediary: MemoryMappingTree) = switchSourceNs(intermediary, "intermediary")

private fun switchSourceNs(intermediary: MemoryMappingTree, ns: String) = MemoryMappingTree().apply {
    intermediary.accept(MappingSourceNsSwitch(this, ns))
}


fun readIntermediaryMapping(version: String): MemoryMappingTree {
    return readMappingFromFile(File("""L:\Legacy-Intermediaries\mappings\$version.tiny"""))
}


fun readOriginalMapping(version: String): MemoryMappingTree {
    return readMappingFromFile(File("""L:\LightCraftMappings\$version\mappings-official-srg-named.tiny2"""))
}

fun readMappingFromFile(file: File): MemoryMappingTree {
    val mappingTree = MemoryMappingTree()
    return file.reader().use {
        MappingReader.read(it, MappingReader.detectFormat(file.toPath()), mappingTree)
        mappingTree
    }
}