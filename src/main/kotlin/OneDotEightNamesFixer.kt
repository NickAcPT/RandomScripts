@file:Suppress("DuplicatedCode")

import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
import net.fabricmc.mappingio.tree.MappingTree

class OneDotEightNamesFixer(
    val tree: MappingTree,
    val targetSrgNs: Int,
    val targetNamedNs: Int,
    next: MappingVisitor,
    val treeOneDotEight: MappingTree?
) : ForwardingMappingVisitor(next) {

    val duplicateNames = mutableListOf<String>()
    private val oneDotEightNames = treeOneDotEight?.classes?.mapNotNull { it.getDstName(targetSrgNs) }
    override fun visitDstName(targetKind: MappedElementKind?, namespace: Int, name: String?) {
        var finalName = name
        if (name != null && oneDotEightNames != null && targetKind == MappedElementKind.CLASS && namespace == targetNamedNs && finalName != null) {
            val className = finalName.removePrefix("net/minecraft/src/")
            finalName = oneDotEightNames.firstOrNull { it.substringAfterLast('/') == className } ?: name

            if (name.startsWith("net/minecraft/client/") || name.startsWith("net/minecraft/server/"))
                finalName = name

            if (!finalName.contains("/")) {
                finalName = "net/minecraft/$finalName"
            }
            if (className.startsWith("Packet")) {
                finalName = "net/minecraft/network/$className"
            }
            if (className.startsWith("Gui")) {
                finalName = "net/minecraft/client/gui/$className"
            }
            if (className.startsWith("Block")) {
                finalName = "net/minecraft/block/$className"
            }
            if (className.startsWith("Item")) {
                finalName = "net/minecraft/item/$className"
            }
            if (className.startsWith("Render")) {
                finalName = "net/minecraft/client/render/$className"
            }

            if (finalName.contains("net/minecraft/src/")) {
                finalName = "net/minecraft/$className"
            }

            val duplicateCount = duplicateNames.count { it == finalName }
            duplicateNames.add(finalName)
            if (duplicateCount > 0)
                finalName += duplicateCount
        }

        super.visitDstName(targetKind, namespace, finalName)
    }
}