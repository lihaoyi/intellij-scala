package org.jetbrains.plugins.scala.lang.lexer

import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaTokenType(debugName: String) extends IElementType(debugName, ScalaLanguage.INSTANCE) {

  override def isLeftBound: Boolean = true
}

case class ScalaModifierTokenType(modifier: ScalaModifier) extends ScalaTokenType(modifier.text()) {
  ScalaModifier.registerModifierTokenType(modifier, this)
}