package org.jetbrains.plugins.scala.lang.dfa.controlFlow

import com.intellij.codeInspection.dataFlow.types.DfType
import com.intellij.codeInspection.dataFlow.value.{DfaVariableValue, VariableDescriptor}
import com.intellij.psi.PsiNamedElement
import org.jetbrains.plugins.scala.extensions.PsiNamedElementExt

case class ScalaVariableDescriptor(variable: PsiNamedElement, override val isStable: Boolean) extends VariableDescriptor {

  // TODO implement mapping from ScType to DfType, then use variable.`type`()
  override def getDfType(qualifier: DfaVariableValue): DfType = DfType.TOP

  override def toString: String = variable.name
}
