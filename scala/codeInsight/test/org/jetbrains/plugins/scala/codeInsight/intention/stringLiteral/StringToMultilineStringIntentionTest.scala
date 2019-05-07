package org.jetbrains.plugins.scala.codeInsight.intention.stringLiteral

import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.plugins.scala.codeInsight.intentions

class StringToMultilineStringIntentionTest extends intentions.ScalaIntentionTestBase {
  override def familyName: String = StringToMultilineStringIntention.FAMILY_NAME

  import EditorTestUtil.{CARET_TAG => CARET}

  // ATTENTION:
  //   We shouldn't do .stripMargin for before/after strings because it is for some reason .stripMargin is called inside
  //    ScalaLightCodeInsightFixtureTestAdapter.normalize, that is used in ScalaIntentionTestBase.doTest
  // TODO: cleanup, ideally we should do stripMargin in tests, not somewhere under the hood

  def testConvertMultiline1() {
    val before =
      s"""object A {
         |  "one ${CARET}two"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"one ${CARET}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertMultiline2() {
    val before =
      s"""object A {
         |  $CARET"one two"
         |}
      """

    val after =
      s"""object A {
         |  $CARET\"\"\"one two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertEmptyToMultiline() {
    val before =
      s"""object A {
         |  "$CARET"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"$CARET\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertInterpolatedMultiline1() {
    val before =
      s"""object A {
         |  ${CARET}interp"one$${iterpVal}two"
         |}
      """

    val after =
      s"""object A {
         |  ${CARET}interp\"\"\"one$${iterpVal}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertInterpolatedMultiline2() {
    val before =
      s"""object A {
         |  interp$CARET"one$${iterpVal}two"
         |}
      """

    val after =
      s"""object A {
         |  interp$CARET\"\"\"one$${iterpVal}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertInterpolatedMultiline3() {
    val before =
      s"""object A {
         |  s"one$${iterpVal}${CARET}two"
         |}
      """

    val after =
      s"""object A {
         |  s\"\"\"one$${iterpVal}${CARET}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertInterpolatedMultiline4() {
    val before =
      s"""object A {
         |  interp"one$${iterpVal}${CARET}two"
         |}
      """

    val after =
      s"""object A {
         |  interp\"\"\"one$${iterpVal}${CARET}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertInterpolatedMultiline5() {
    val before =
      s"""object A {
         |  interp"one$${iterp${CARET}Val}two"
         |}
      """

    val after =
      s"""object A {
         |  interp\"\"\"one$${iterp${CARET}Val}two\"\"\"
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertMultilineWithNewLines1() {
    val before =
      s"""object A {
         |  "${CARET}one\\ntwo\\nthree\\nfour"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"${CARET}one
         |    |two
         |    |three
         |    |four\"\"\".stripMargin
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }


  def testConvertMultilineWithNewLines2() {
    val before =
      s"""object A {
         |  "one\\n${CARET}two\\nthree\\nfour"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"one
         |    |${CARET}two
         |    |three
         |    |four\"\"\".stripMargin
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertMultilineWithNewLines3() {
    val before =
      s"""object A {
         |  "one\\ntwo\\nthree\\n${CARET}four"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"one
         |    |two
         |    |three
         |    |${CARET}four\"\"\".stripMargin
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertMultilineWithNewLines4() {
    val before =
      s"""object A {
         |  "one\\ntwo\\nthree\\nfour\\n${CARET}five"
         |}
      """

    val after =
      s"""object A {
         |  \"\"\"one
         |    |two
         |    |three
         |    |four
         |    |${CARET}five\"\"\".stripMargin
         |}
      """

    doTest(before, after)
    doTest(after, before)
  }

  def testConvertFromMultilineWithNewLinesWithoutStripMargin() {
    val before =
      s"""object A {
         |  \"\"\"one
         |    |${CARET}two\"\"\"
         |}
      """
    val after =
      s"""object A {
         |  "one\\n    |${CARET}two"
         |}
      """
    doTest(before, after)
  }

  def testConvertFromMultilineWithNewLinesAndShiftedMargins() {
    val before =
      s"""object A {
         |  \"\"\"one
         | |two
         |           |${CARET}three\"\"\".stripMargin
         |}
      """
    val after =
      s"""object A {
         |  "one\\ntwo\\n${CARET}three"
         |}
      """
    doTest(before, after)
  }

  def testConvertFromMultilineWithCaretBeforeMarginChar() {
    val before =
      s"""object A {
         |  \"\"\"one
         |    |two
         | $CARET   |three\"\"\".stripMargin
         |}
      """
    val after =
      s"""object A {
         |  "one\\ntwo\\n${CARET}three"
         |}
      """
    doTest(before, after)
    doTest(before, after)
  }

  def testConvertMultilineWithNewLinesAndAssigment1() {
    val before =
      s"""object A {
         |  val value = $CARET"one\\ntwo\\nthree\\nfour"
         |}
      """

    val after =
      s"""object A {
         |  val value =
         |    $CARET\"\"\"one
         |      |two
         |      |three
         |      |four\"\"\".stripMargin
         |}
      """

    val afterAfter =
      s"""object A {
         |  val value =
         |    $CARET"one\\ntwo\\nthree\\nfour"
         |}
      """

    doTest(before, after)
    doTest(after, afterAfter)
  }

  def testConvertMultilineWithNewLinesAndAssigment2() {
    val before =
      s"""object A {
         |  val value = "${CARET}one\\ntwo\\nthree\\nfour"
         |}
      """

    val after =
      s"""object A {
         |  val value =
         |    \"\"\"${CARET}one
         |      |two
         |      |three
         |      |four\"\"\".stripMargin
         |}
      """

    val afterAfter =
      s"""object A {
         |  val value =
         |    "${CARET}one\\ntwo\\nthree\\nfour"
         |}
      """

    doTest(before, after)
    doTest(after, afterAfter)
  }

  def testConvertMultilineWithNewLinesAndAssigment3() {
    val before =
      s"""object A {
         |  val value = "one\\n${CARET}two\\nthree\\nfour"
         |}
      """

    val after =
      s"""object A {
         |  val value =
         |    \"\"\"one
         |      |${CARET}two
         |      |three
         |      |four\"\"\".stripMargin
         |}
      """

    val afterAfter =
      s"""object A {
         |  val value =
         |    "one\\n${CARET}two\\nthree\\nfour"
         |}
      """

    doTest(before, after)
    doTest(after, afterAfter)
  }

  def testConvertMultilineWithNewLinesAndAssigment4() {
    val before =
      s"""object A {
         |  val value = "one\\ntwo\\nthree\\n${CARET}four"
         |}
      """

    val after =
      s"""object A {
         |  val value =
         |    \"\"\"one
         |      |two
         |      |three
         |      |${CARET}four\"\"\".stripMargin
         |}
      """

    val afterAfter =
      s"""object A {
         |  val value =
         |    "one\\ntwo\\nthree\\n${CARET}four"
         |}
      """

    doTest(before, after)
    doTest(after, afterAfter)
  }

  def testConvertInterpolatedMultilineWithNewLinesAndAssigment() {
    val before =
      s"""object A {
         |  val value = interp"one\\ntwo\\nthree\\n${CARET}four"
         |}
      """

    val after =
      s"""object A {
         |  val value =
         |    interp\"\"\"one
         |            |two
         |            |three
         |            |${CARET}four\"\"\".stripMargin
         |}
      """

    val afterAfter =
      s"""object A {
         |  val value =
         |    interp"one\\ntwo\\nthree\\n${CARET}four"
         |}
      """

    doTest(before, after)
    doTest(after, afterAfter)
  }
}
