package org.jetbrains.plugins.scala.lang.dfa.utils

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.dataFlow.TypeConstraints
import com.intellij.codeInspection.dataFlow.jvm.SpecialField
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet
import com.intellij.codeInspection.dataFlow.types.{DfType, DfTypes}
import com.intellij.psi.PsiNamedElement
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeConstants._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.literals._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.Any

object ScalaDfaTypeUtils {

  def dfTypeCollectionOfSize(size: Int): DfType = SpecialField.COLLECTION_SIZE.asDfType(DfTypes.intValue(size))

  def literalToDfType(literal: ScLiteral): DfType = literal match {
    case _: ScNullLiteral => DfTypes.NULL
    case int: ScIntegerLiteral => DfTypes.intValue(int.getValue)
    case long: ScLongLiteral => DfTypes.longValue(long.getValue)
    case float: ScFloatLiteral => DfTypes.floatValue(float.getValue)
    case double: ScDoubleLiteral => DfTypes.doubleValue(double.getValue)
    case boolean: ScBooleanLiteral => DfTypes.booleanValue(boolean.getValue)
    case char: ScCharLiteral => DfTypes.intValue(char.getValue.toInt)
    case _ => DfType.TOP
  }

  def dfTypeToReportedConstant(dfType: DfType): DfaConstantValue = dfType match {
    case DfTypes.TRUE => DfaConstantValue.True
    case DfTypes.FALSE => DfaConstantValue.False
    case _ => DfaConstantValue.Unknown
  }

  @Nls
  def constantValueToProblemMessage(value: DfaConstantValue, warningType: ProblemHighlightType): String = value match {
    case DfaConstantValue.True => ScalaInspectionBundle.message("condition.always.true", warningType)
    case DfaConstantValue.False => ScalaInspectionBundle.message("condition.always.false", warningType)
    case _ => throw new IllegalStateException(s"Trying to report an unexpected DFA constant value: $value")
  }

  @Nls
  def exceptionNameToProblemMessage(exceptionName: String): String = {
    val warningType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    exceptionName match {
      case IndexOutOfBoundsExceptionName => ScalaInspectionBundle.message("invocation.index.out.of.bounds", warningType)
      case NoSuchElementExceptionName => ScalaInspectionBundle.message("invocation.no.such.element", warningType)
      case _ => throw new IllegalStateException(s"Trying to report an unexpected DFA exception: $exceptionName")
    }
  }

  //noinspection UnstableApiUsage
  def scTypeToDfType(scType: ScType): DfType = scType.extractClass match {
    case Some(psiClass) if psiClass.qualifiedName.startsWith(s"$ScalaCollectionImmutable.Nil") =>
      dfTypeCollectionOfSize(0)
    case Some(psiClass) if scType == Any(psiClass.getProject) => DfType.TOP
    case Some(psiClass) => psiClass.qualifiedName match {
      case "scala.Int" => DfTypes.INT
      case "scala.Long" => DfTypes.LONG
      case "scala.Float" => DfTypes.FLOAT
      case "scala.Double" => DfTypes.DOUBLE
      case "scala.Boolean" => DfTypes.BOOLEAN
      case "scala.Char" => DfTypes.intRange(LongRangeSet.range(Char.MinValue.toLong, Character.MAX_VALUE.toLong))
      case "scala.Short" => DfTypes.intRange(LongRangeSet.range(Short.MinValue.toLong, Short.MaxValue.toLong))
      case "scala.Byte" => DfTypes.intRange(LongRangeSet.range(Byte.MinValue.toLong, Byte.MaxValue.toLong))
      case _ => TypeConstraints.exactClass(psiClass).asDfType().meet(DfTypes.OBJECT_OR_NULL)
    }
    case _ => DfType.TOP
  }

  def isStableElement(element: PsiNamedElement): Boolean = element match {
    case typedDefinition: ScTypedDefinition => typedDefinition.isStable
    case _ => false
  }
}