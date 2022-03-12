package exceptor

data class ExceptorFile(val classEntries: MutableMap<String, ClassEntry>) {
    override fun toString(): String {
        return classEntries.values.joinToString("\n")
    }
}