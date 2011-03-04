package org.jetbrains.plugins.scala
package lang.completion

import com.intellij.codeInsight.lookup.{Lookup, CharFilter}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause

class ScalaCharFilter extends CharFilter {
  def acceptChar(c: Char, prefixLength: Int, lookup: Lookup) = {
    lookup.getPsiElement.getContext match {
      // avoids:
      //   "(va:" => "(var"
      //   "(va<SPACE>" => "(var "
      case x: ScParameterClause if Set(' ', ':').contains(c) => CharFilter.Result.HIDE_LOOKUP
      case _ => null
    }
  }
}