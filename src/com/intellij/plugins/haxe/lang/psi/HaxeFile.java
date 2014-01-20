/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeFile extends PsiFileBase {
  public HaxeFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return HaxeFileType.HAXE_FILE_TYPE;
  }

  @Override
  public String toString() {
    return "Haxe File";
  }

  @Override
  public Icon getIcon(int flags) {
    return super.getIcon(flags);
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return super.findReferenceAt(offset);
  }

  @Override
  public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
    final String oldName = FileUtil.getNameWithoutExtension(getName());
    final PsiElement result = super.setName(newName);
    final HaxeClass haxeClass = HaxeResolveUtil.findComponentDeclaration(this, oldName);
    if (haxeClass != null) {
      haxeClass.setName(FileUtil.getNameWithoutExtension(newName));
    }
    return result;
  }
}
