package exceptor

data class MethodEntry(
    val className: String,
    val methodDescriptor: String,
    val exceptions: String,
    val params: List<String>
) {
    override fun toString(): String {
        return "$className.$methodDescriptor=$exceptions|${params.joinToString(",")}"
    }
}