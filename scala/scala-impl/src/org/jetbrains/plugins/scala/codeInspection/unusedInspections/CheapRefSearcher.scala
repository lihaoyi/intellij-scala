package org.jetbrains.plugins.scala.codeInspection.unusedInspections

import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.{LocalSearchScope, PsiSearchHelper, TextOccurenceProcessor, UsageSearchContext}
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiClass, PsiElement, PsiIdentifier}
import org.jetbrains.plugins.scala.annotator.usageTracker.ScalaRefCountHolder
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, PsiFileExt}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.{isImplicit, isOnlyVisibleInLocalFile}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScEnumCase, ScFunctionDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScEnum, ScTypeDefinition}
import org.jetbrains.plugins.scala.util.ScalaUsageNamesUtil
import org.jetbrains.plugins.scala.util.ImplicitUtil.ImplicitTargetExt

import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsScala}

sealed trait ElementUsage {
  def targetCanBePrivate(): Boolean = {
    this match {
      case UnknownElementUsage => false
      case KnownElementUsage(reference, target) =>

        var result = false
        val targetContainingClass = PsiTreeUtil.getParentOfType(target, classOf[PsiClass])
        var refContainingClass = PsiTreeUtil.getParentOfType(reference, classOf[PsiClass])

        val MaxSearchDepth = 10
        var counter = 0

        if (targetContainingClass != null) {
          while (counter < MaxSearchDepth && refContainingClass != null && refContainingClass != targetContainingClass) {
            refContainingClass = PsiTreeUtil.getParentOfType(refContainingClass, classOf[PsiClass])
            counter += 1
          }

          if (refContainingClass == targetContainingClass) result = true
        }

        result
    }
  }
}
case object UnknownElementUsage extends ElementUsage
case class KnownElementUsage(reference: PsiElement, target: ScNamedElement) extends ElementUsage

object CheapRefSearcher {

  /**
   * Return certain as well as maybe references.
   *
   * Contract:
   * 1. It promises not to miss references, but there may be false positives,
   * such as references to a declaration with the same name but in a different package.
   *
   * 2. For any search approach that can be stopped, like text-search, the search will indeed stop
   * after some reference outside the element's private scope has been found.
   *
   * 3. Some references are only registered to exist, with no further information about them.
   */
  def search(element: ScNamedElement, isOnTheFly: Boolean, reportPublicDeclarations: Boolean): Seq[ElementUsage] = {

    lazy val refSearch = referencesSearch(element)
    lazy val enumSearch = getForeignEnumUsages(element)

    if (isOnlyVisibleInLocalFile(element)) {
      if (isImplicit(element)) {
        isImplicitUsed(element)
      } else if (isOnTheFly) {
        val refCounter = ScalaRefCountHolder(element)

        var used = false
        val success = refCounter.runIfUnusedReferencesInfoIsAlreadyRetrievedOrSkip { () =>
          used = refCounter.isValueReadUsed(element) || refCounter.isValueWriteUsed(element)
        }

        if (!success || used) Seq(UnknownElementUsage) else Seq.empty
      } else {
        refSearch
      }
    } else if (refSearch.nonEmpty) {
      refSearch
    } else if (!reportPublicDeclarations) {
      Seq(UnknownElementUsage)
    } else if (enumSearch.nonEmpty) {
      enumSearch
    } else if (ScalaPsiUtil.isImplicit(element)) {
      Seq(UnknownElementUsage)
    } else {
      textSearch(element)
    }
  }

  private def referencesSearch(element: ScNamedElement): Seq[ElementUsage] = {
    val elementsForSearch = element match {
      case enumCase: ScEnumCase =>
        val syntheticMembers = ScalaPsiUtil.getCompanionModule(enumCase.enumParent).toSeq.flatMap(_.membersWithSynthetic).collect {
          case n: ScNamedElement if ScalaUsageNamesUtil.enumSyntheticMethodNames.contains(n.name) => n
        }
        enumCase.getSyntheticCounterpart +: syntheticMembers
      case e: ScNamedElement => Seq(e)
    }

    val scope = new LocalSearchScope(element.getContainingFile)

    elementsForSearch.flatMap(ReferencesSearch.search(_, scope).findAll().asScala).map { ref =>
      KnownElementUsage(ref.getElement, element)
    }
  }

  private def getForeignEnumUsages(element: ScNamedElement): Seq[ElementUsage] = {

    val result = new mutable.ListBuffer[ElementUsage]

    val scEnum = element match {
      case el: ScEnumCase => Some(el.enumParent)
      case el: ScEnum => Some(el)
      case _ => None
    }

    val processor = new TextOccurenceProcessor {
      override def execute(e2: PsiElement, offsetInElement: Int): Boolean =
        if (e2.getContainingFile.isScala2File || e2.getContainingFile.isScala3File) {
          true
        } else {
          result.addOne(KnownElementUsage(e2, element))
          result.last.targetCanBePrivate()
        }
    }

    val caseSensitive = true
    val searchContext = (UsageSearchContext.IN_CODE | UsageSearchContext.IN_FOREIGN_LANGUAGES).toShort
    val scope = element.getUseScope

    scEnum.foreach { e =>

      val text = e.getName

      PsiSearchHelper.getInstance(element.getProject).processElementsWithWord(processor, scope, text, searchContext, caseSensitive)

      if (result.isEmpty) {
        PsiSearchHelper.getInstance(element.getProject)
          .processElementsWithWord(processor, scope, s"$text$$.MODULE$$", searchContext, caseSensitive)
      }
    }

    result.toSeq
  }

  private def textSearch(element: ScNamedElement): Seq[ElementUsage] = {
    val result = new mutable.ListBuffer[ElementUsage]
    val helper = PsiSearchHelper.getInstance(element.getProject)
    val processor = new TextOccurenceProcessor {
      override def execute(e2: PsiElement, offsetInElement: Int): Boolean =
        if (element.getContainingFile == e2.getContainingFile) {
          true
        } else {
          (e2, Option(e2.getParent)) match {
            case (_, Some(_: ScReferencePattern)) =>
            case (_, Some(_: ScTypeDefinition)) =>
            case (_: PsiIdentifier, _) => result.addOne(KnownElementUsage(e2, element))
            case (l: LeafPsiElement, _) if l.isIdentifier => result.addOne(KnownElementUsage(e2, element))
            case (_: ScStableCodeReference, _) => result.addOne(KnownElementUsage(e2, element))
            case _ =>
          }

          result.isEmpty || result.last.targetCanBePrivate()
        }
    }

    ScalaUsageNamesUtil.getStringsToSearch(element).asScala.foreach { name =>
      if (result.isEmpty) {
        helper.processElementsWithWord(processor, element.getUseScope, name,
          (UsageSearchContext.IN_CODE | UsageSearchContext.IN_FOREIGN_LANGUAGES).toShort, true)
      }
    }

    result.toSeq
  }

  private def isImplicitUsed(target: PsiElement): Seq[ElementUsage] =
    target.getContainingFile.depthFirst().filter(target.refOrImplicitRefIn(_).nonEmpty).toSeq.map(_ => UnknownElementUsage)
}