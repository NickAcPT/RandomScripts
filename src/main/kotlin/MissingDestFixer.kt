
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor
import net.fabricmc.mappingio.adapter.MissingDescFilter
import net.fabricmc.mappingio.tree.MappingTree

class MissingDestFixer(
    val tree: MappingTree,
    val targetSrgNs: Int,
    val targetNamedNs: Int,
    next: MappingVisitor
) : ForwardingMappingVisitor(MissingDescFilter(next)) {
    var clazz: MappingTree.ClassMapping? = null
    override fun visitClass(srcName: String?): Boolean {
        clazz = tree.getClass(srcName)
        val srgName = clazz!!.getDstName(targetSrgNs)
        if (clazz != null && clazz!!.getDstName(targetNamedNs) == null && srgName != null) {
            clazz!!.setDstName(srgName, targetNamedNs)
        }
        return super.visitClass(srcName)
    }

    override fun visitField(srcName: String?, srcDesc: String?): Boolean {
        if (srcName != null && clazz != null) {
            val field = clazz!!.getField(srcName, srcDesc)
            val srgName = field.getDstName(targetSrgNs)
            if (field != null && field.getDstName(targetNamedNs) == null) {
                field.setDstName(srgName, targetNamedNs)
            }
        }
        return super.visitField(srcName, srcDesc)
    }

    override fun visitMethod(srcName: String?, srcDesc: String?): Boolean {
        if (srcName != null && clazz != null) {
            val method = clazz!!.getMethod(srcName, srcDesc)
            if (method != null && method.getDstName(targetNamedNs) == null) {
                val srgName = method.getDstName(targetSrgNs)
                method.setDstName(srgName, targetNamedNs)
            }
        }
        return super.visitMethod(srcName, srcDesc)
    }
}