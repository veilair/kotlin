// SKIP_TXT

typealias A = CharSequence.(Int) -> Unit

var w: Int = 1

fun myPrint(x: Int) {}

fun <T> select(vararg x: T) = x[0]

val a1: A = select(
    { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>a: Int<!> -> myPrint(a + this.length + 1) },
    { a: Int -> myPrint(a + this.length + 2) }
)

val a2 = select(
    { a: Int -> myPrint(a + this.<!UNRESOLVED_REFERENCE!>length<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1) },
    fun CharSequence.(a: Int) { myPrint(a + this.length + 2) },
    { a: Int -> myPrint(a + this.<!UNRESOLVED_REFERENCE!>length<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 3) }
)
