// CORRECT_ERROR_TYPES
// NON_EXISTENT_CLASS
// NO_VALIDATION

@Suppress("UNRESOLVED_REFERENCE")
@Suppress("CANNOT_INFER_PARAMETER_TYPE")
object NonExistentType {
    val a: ABCDEF? = null
    val b: List<ABCDEF>? = null
    val c: (ABCDEF) -> Unit = { f -> }
    val d: ABCDEF<String, (List<ABCDEF>) -> Unit>? = null
    
    val foo: Foo get() = Foo()

    fun a(a: ABCDEF, s: String): ABCDEF {}
    fun b(s: String): ABCDEF {}
}
