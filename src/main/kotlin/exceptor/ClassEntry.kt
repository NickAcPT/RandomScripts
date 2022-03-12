package exceptor

data class ClassEntry(val className: String, val methods: MutableMap<String, MethodEntry>) {
    override fun toString(): String {
        return methods.values.joinToString("\n")
    }
}