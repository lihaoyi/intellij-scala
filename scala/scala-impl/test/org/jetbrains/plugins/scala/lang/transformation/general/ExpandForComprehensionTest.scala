package org.jetbrains.plugins.scala
package lang
package transformation
package general

/**
  * @author Pavel Fatin
  */
class ExpandForComprehensionTest extends TransformerTest(new ExpandForComprehension()) {

  def testForeach(): Unit = check(
    before = "for(v <- Seq(A)) { v.a() }",
    after = "Seq(A).foreach(v => v.a())"
  )()

  def testMap(): Unit = check(
    before = "for(v <- Seq(A)) yield v.a()",
    after = "Seq(A).map(v => v.a())"
  )()

  def testFilter(): Unit = check(
    before = "for(v <- Seq(A) if p(v)) yield v.a()",
    after = "Seq(A).withFilter(v => p(v)).map(v => v.a())"
  )()

  def testFlatMap(): Unit = check(
    before = "for(v1 <- Seq(A); v2 <- Seq(B)) yield (v1, v2)",
    after = "Seq(A).flatMap(v1 => Seq(B).map(v2 => (v1, v2)))"
  )()

  def testEmptyBlock(): Unit = check(
    before = "for(v <- Seq(A)) {}",
    after = "Seq(A).foreach(v => {})"
  )()

  def testCompoundBlock(): Unit = check(
    before = "for(v <- Seq(A)) { ???; ??? }",
    after = "Seq(A).foreach(v => { ???; ??? })"
  )()

  def testTypedPattern(): Unit = check(
    before = "for(v: A <- Seq(A)) { ??? }",
    after = "Seq(A).foreach((v: A) => ???)"
  )()

  def testTuplePattern(): Unit = check(
    before = "for((v1, v2) <- Seq(A)) { ??? }",
    after = "Seq(A).withFilter { case (v1, v2) => true; case _ => false }.foreach { case (v1, v2) => ??? }"
  )()

  def testStringInterpolationConsistency(): Unit = check(
    before = "for(v <- Seq(A)) yield s\"${3 + v}\"",
    after = "Seq(A).map(v => s\"${3 + v}\")"
  )()

  def testPatternMatching(): Unit = check(
    before = "for((a: A, b) <- Seq((A, B), 4)) a.a()",
    after = "Seq((A, B), 4).withFilter { case (a: A, b) => true; case _ => false }.foreach { case (a: A, b) => a.a() }"
  )()

  def testPatternMatchingWithYield(): Unit = check(
    before = "for((a: A, b) <- Seq((A, B), 4)) yield a.a()",
    after = "Seq((A, B), 4).withFilter { case (a: A, b) => true; case _ => false }.map { case (a: A, b) => a.a() }"
  )()

  def testPatternMatchingWithCorrectType(): Unit = check(
    before = "for((v1, v2) <- Seq((A, B))) yield v1.a()",
    after = "Seq((A, B)).map { case (v1, v2) => v1.a() }"
  )()

  def testNotDesugaringInnerForComprehensionInEnumerators(): Unit = check(
    before = "for (a <- for (b <- List(A)) yield b) yield a",
    after = "(for (b <- List(A)) yield b).map(a => a)"
  )()

  def testNotDesugaringInnerForComprehensionsInBody(): Unit = check(
    before = "for (a <- List(A)) yield for (b <- List(a)) yield b",
    after = "List(A).map(a => for (b <- List(a)) yield b)"
  )()

  def testMultipleForBindings(): Unit = check(
    before = "for (a <- List(A); (b, c) = a; d = c) yield d",
    //after = "List(A).map(a => (a, a)).map { case (a, (b, c)) => (a, (b, c), c) }.map { case (a, (b, c), d) => d }",
    after = "List(A).map { a => val v$1@(b, c) = a; val d = c; d }"
  )()

  def testMultipleGuards(): Unit = check(
    before = "for (a <- List(A); b = a if p(b); if p(a); c = b) c.v()",
    after = "List(A).map { a => val b = a; (a, b) }.withFilter { case (a, b) => p(b) }.withFilter { case (a, b) => p(a) }.foreach { case (a, b) => val c = b; c.v() }"
  )()

  def testForWithMultipleUnderscores(): Unit = check(
    before = "for (_ <- _; _ = _ if _) yield _",
    after = "(forAnonParam$0, forAnonParam$1, forAnonParam$2, forAnonParam$3) => forAnonParam$0.map { _ => val _ = forAnonParam$1; () }.withFilter(_ => forAnonParam$2).map(_ => forAnonParam$3)"
  )()

  def testSimpleWildcardPattern(): Unit = check(
    before = "for (_ <- Seq(A)) yield 1",
    after = "Seq(A).map(_ => 1)"
  )()

  def testWildcardPatternWithForBindings(): Unit = check(
    before = "for (_ <- Seq(A); a = 1; _ = 2; if true; _ = 3) yield 4",
    //after = "Seq(A).map(_ => 1).map(a => (a, 2)).withFilter { case (a, _) => true }.map { case (a, _) => (a, 3) }.map { case (a, _) => 4 }",
    after = "Seq(A).map { _ => val a = 1; val _ = 2; a }.withFilter(a => true).map { a => val _ = 3; 4 }"
  )()

  def testForWithUnrelatedUnderscores(): Unit = check(
    before = "for (a <- Seq(A)) yield a + _",
    after = "Seq(A).map(a => a + _)"
  )()

  def testWithoutFilterWith(): Unit = check(
    before = "for (v <- new W if p(v)) v.p()",
    after = "(new W).filter(v => p(v)).foreach(v => v.p())"
  )(header = "class W { def filter(f: (A) => Boolean): W\n def foreach(f: (A) => Unit }")
}
