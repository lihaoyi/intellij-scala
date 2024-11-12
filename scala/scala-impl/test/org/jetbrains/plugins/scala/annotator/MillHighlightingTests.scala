package org.jetbrains.plugins.scala.annotator

/**
 * Check customized behavior for Mill build files
 */
class MillHighlightingTests extends ScalaHighlightingTestBase {
  //SCL-23198
  def testAllowDirectPackageReferences(): Unit = {
    val code = """package foo{
                 |  object `package`
                 |}
                 |
                 |package bar{
                 |  object qux{
                 |    val reference = foo
                 |  }
                 |}
                 |""".stripMargin
    scala.Predef.assert(errorsFromScalaCode(code, "build.mill").isEmpty)
    scala.Predef.assert(errorsFromScalaCode(code, "package.mill").isEmpty)
    scala.Predef.assert(errorsFromScalaCode(code, "build.mill.scala").isEmpty)
    scala.Predef.assert(errorsFromScalaCode(code, "build.scala").nonEmpty)
  }
}
