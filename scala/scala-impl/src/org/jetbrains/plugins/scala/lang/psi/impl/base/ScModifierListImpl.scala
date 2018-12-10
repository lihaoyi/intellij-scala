package org.jetbrains.plugins.scala
package lang
package psi
package impl
package base

import com.intellij.lang.ASTNode
import com.intellij.psi._
import org.jetbrains.plugins.scala.extensions.{ElementType, StubBasedExt}
import org.jetbrains.plugins.scala.lang.lexer.{ScalaModifier, ScalaModifierTokenType, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory._
import org.jetbrains.plugins.scala.lang.psi.stubs.ScModifiersStub
import org.jetbrains.plugins.scala.util.EnumSet
import org.jetbrains.plugins.scala.util.EnumSet._

/**
* @author Alexander Podkhalyuzin
* Date: 22.02.2008
*/
class ScModifierListImpl private (stub: ScModifiersStub, node: ASTNode)
  extends ScalaStubBasedElementImpl(stub, ScalaElementType.MODIFIERS, node) with ScModifierList {

  def this(node: ASTNode) = this(null, node)

  def this(stub: ScModifiersStub) = this(stub, null)

  override def toString: String = "Modifiers"

  override def modifiers: EnumSet[ScalaModifier] = byStubOrPsi(_.modifiers) {
    var result = EnumSet.empty[ScalaModifier]

    //this method is optimized to avoid creation of unnecessary arrays

    var currentChild = getFirstChild

    while (currentChild != null) {
      currentChild match {
        case a: ScAccessModifier =>
          result ++= (if (a.isPrivate) ScalaModifier.Private else ScalaModifier.Protected)
        case ElementType(ScalaModifierTokenType(mod)) =>
          result ++= mod
        case _ =>
      }
      currentChild = currentChild.getNextSibling
    }

    result
  }

  def hasModifierProperty(name: String): Boolean = {
    if (name == PsiModifier.PUBLIC)
      !modifiers.contains(ScalaModifier.Private) && !modifiers.contains(ScalaModifier.Protected)
    else
      hasExplicitModifier(name)
  }

  override def accessModifier: Option[ScAccessModifier] = Option {
    getStubOrPsiChild(ScalaElementType.ACCESS_MODIFIER)
  }

  override def setModifierProperty(name: String, value: Boolean): Unit = {
    checkSetModifierProperty(name, value)

    if (name == PsiModifier.PUBLIC) {
      if (value) {
        setModifierProperty(PsiModifier.PRIVATE, value = false)
        setModifierProperty(PsiModifier.PROTECTED, value = false)
      }
      else {
        //not supported
      }
    }

    val mod = ScalaModifier.byText(name)

    if (mod == null || value == modifiers.contains(mod)) {
      return
    }

    if (value) {
      addModifierProperty(name)
    }

    if (!value) {
      val elemToRemove = mod match {
        case ScalaModifier.Private   if accessModifier.exists(_.isPrivate)   => accessModifier
        case ScalaModifier.Protected if accessModifier.exists(_.isProtected) => accessModifier
        case _ => Option(findChildByType(mod.getTokenType))
      }
      elemToRemove.foreach(e => getNode.removeChild(e.getNode))
    }
  }

  override def getAnnotations: Array[PsiAnnotation] = getParent match {
    case null => PsiAnnotation.EMPTY_ARRAY
    case parent =>
      parent.stubOrPsiChildren(
        ScalaElementType.ANNOTATIONS,
        JavaArrayFactoryUtil.ScAnnotationsFactory
      ) match {
        case Array() => PsiAnnotation.EMPTY_ARRAY
        case Array(head, _@_*) =>
          val scAnnotations = head.getAnnotations
          val result = PsiAnnotation.ARRAY_FACTORY.create(scAnnotations.length)
          scAnnotations.copyToArray(result)
          result
      }
  }

  override def findAnnotation(name: String): PsiAnnotation = getAnnotations.find {
    _.getQualifiedName == name
  }.getOrElse {
    name match {
      case "java.lang.Override" =>
        JavaPsiFacade.getInstance(getProject).getElementFactory
          .createAnnotationFromText("@" + name, this); // hack to disable AddOverrideAnnotationAction,
      case _ => null
    }
  }

  override def accept(visitor: ScalaElementVisitor): Unit =
    visitor.visitModifierList(this)

  override def accept(visitor: PsiElementVisitor): Unit = visitor match {
    case scalaVisitor: ScalaElementVisitor => accept(scalaVisitor)
    case _ => super.accept(visitor)
  }

  def hasExplicitModifier(name: String): Boolean = {
    val mod = ScalaModifier.byText(name)
    mod != null && modifiers.contains(mod)
  }

  def checkSetModifierProperty(name: String, value: Boolean): Unit = {}

  def getApplicableAnnotations: Array[PsiAnnotation] = PsiAnnotation.EMPTY_ARRAY

  def addAnnotation(qualifiedName: String): PsiAnnotation = null

  private def addModifierProperty(name: String): Unit = {
    val node = getNode
    val modifierNode = createModifierFromText(name).getNode

    val addAfter = name != ScalaModifier.CASE
    val spaceNode = createNewLineNode(" ")

    getFirstChild match {
      case null if addAfter =>
        val parentNode = getParent.getNode
        var nextSibling = getNextSibling
        while (ScalaTokenTypes.WHITES_SPACES_AND_COMMENTS_TOKEN_SET.contains(nextSibling.getNode.getElementType)) {
          val currentNode = nextSibling.getNode
          nextSibling = nextSibling.getNextSibling

          parentNode.removeChild(currentNode)
          parentNode.addChild(currentNode, node)
        }

        node.addChild(modifierNode)
        parentNode.addChild(spaceNode, nextSibling.getNode)
      case null =>
        node.addChild(modifierNode)
        node.addChild(spaceNode)
      case first if addAfter =>
        val firstNode = first.getNode
        node.addChild(modifierNode, firstNode)
        node.addChild(spaceNode, firstNode)
      case _ =>
        node.addChild(spaceNode)
        node.addChild(modifierNode)
    }
  }
}