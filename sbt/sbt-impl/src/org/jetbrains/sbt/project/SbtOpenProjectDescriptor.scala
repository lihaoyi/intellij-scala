package org.jetbrains.sbt
package project

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile

import scala.annotation.nowarn

class SbtOpenProjectDescriptor extends OpenProjectFileChooserDescriptor(true) {

  @nowarn("cat=deprecation")
  override def isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean =
    super.isFileVisible(file, showHiddenFiles) &&
      (file.isDirectory || language.SbtFileType.isMyFileType(file))

  override def isFileSelectable(file: VirtualFile): Boolean =
    super.isFileSelectable(file) &&
      SbtProjectImportProvider.canImport(file)
}
