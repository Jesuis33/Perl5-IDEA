/*
 * Copyright 2015-2021 Alexandr Evstigneev
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

package com.perl5.lang.tt2.idea.liveTemplates;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.perl5.lang.tt2.TemplateToolkitLanguage;
import com.perl5.lang.tt2.elementTypes.TemplateToolkitElementTypes;
import com.perl5.lang.tt2.lexer.TemplateToolkitSyntaxElements;
import com.perl5.lang.tt2.psi.PsiElsifBranch;
import com.perl5.lang.tt2.psi.impl.*;
import com.perl5.lang.tt2.utils.TemplateToolkitPsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class TemplateToolkitTemplateContextType extends TemplateContextType implements TemplateToolkitElementTypes {
  public TemplateToolkitTemplateContextType(@NotNull @NonNls String id,
                                            @NotNull String presentableName,
                                            @Nullable Class<? extends TemplateContextType> baseContextType) {
    super(id, presentableName, baseContextType);
  }

  protected abstract boolean isInContext(@NotNull PsiElement element);

  @Override
  public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
    var file = templateActionContext.getFile();
    var offset = templateActionContext.getStartOffset();
    FileViewProvider viewProvider = file.getViewProvider();
    PsiFile ttFile = viewProvider.getPsi(TemplateToolkitLanguage.INSTANCE);

    if (ttFile == null) {
      return false;
    }
    PsiElement element = viewProvider.findElementAt(offset, TemplateToolkitLanguage.INSTANCE);
    if (element == null && offset > 0) {
      element = viewProvider.findElementAt(offset - 1, TemplateToolkitLanguage.INSTANCE);
    }

    return element != null && element.getLanguage() == TemplateToolkitLanguage.INSTANCE && isInContext(element);
  }

  public static class Generic extends TemplateToolkitTemplateContextType {
    public Generic() {
      super(TemplateToolkitLanguage.NAME, TemplateToolkitLanguage.NAME, EverywhereContextType.class);
    }

    public Generic(String id, String presentableName) {
      super(id, presentableName, TemplateToolkitTemplateContextType.Generic.class);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      return false;
    }
  }

  public static class Postfix extends TemplateToolkitTemplateContextType.Generic {
    public Postfix() {
      this(TemplateToolkitLanguage.NAME + ".postfix", "Postfix");
    }

    public Postfix(String id, String presentableName) {
      super(id, presentableName);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      IElementType tokenType = element.getNode().getElementType();
      if (tokenType != TT2_IDENTIFIER) {
        return false;
      }

      PsiElement parent = element.getParent();
      if (!(parent instanceof PsiErrorElement)) {
        return false;
      }

      PsiElement prevElement = TemplateToolkitPsiUtil.getPrevSignificantSibling(parent);
      return prevElement != null && TemplateToolkitSyntaxElements.ATOMIC_EXPRESSIONS.contains(prevElement.getNode().getElementType());
    }
  }

  public static class CommandPosition extends TemplateToolkitTemplateContextType.Generic {
    public CommandPosition() {
      this(TemplateToolkitLanguage.NAME + ".command", "Directive");
    }

    public CommandPosition(String id, String presentableName) {
      super(id, presentableName);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      return PsiUtilCore.getElementType(element) == TT2_HTML;
    }
  }

  public static class CommandPositionElsif extends TemplateToolkitTemplateContextType.CommandPosition {
    public CommandPositionElsif() {
      this(TemplateToolkitLanguage.NAME + ".command.elsif", "ELSIF/ELSE branch");
    }

    public CommandPositionElsif(String id, String presentableName) {
      super(id, presentableName);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      return super.isInContext(element) &&
             (PsiTreeUtil.getParentOfType(element, PsiIfBranchImpl.class) != null ||
              PsiTreeUtil.getParentOfType(element, PsiUnlessBranchImpl.class) != null ||
              PsiTreeUtil.getParentOfType(element, PsiElsifBranch.class) != null);
    }
  }

  public static class CommandPositionCase extends TemplateToolkitTemplateContextType.CommandPosition {
    public CommandPositionCase() {
      this(TemplateToolkitLanguage.NAME + ".command.case", "CASE branch");
    }

    public CommandPositionCase(String id, String presentableName) {
      super(id, presentableName);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      return super.isInContext(element) && PsiTreeUtil.getParentOfType(element, PsiSwitchBlockImpl.class) != null;
    }
  }

  public static class CommandPositionCatch extends TemplateToolkitTemplateContextType.CommandPosition {
    public CommandPositionCatch() {
      this(TemplateToolkitLanguage.NAME + ".command.catch", "CATCH/FINAL branch");
    }

    public CommandPositionCatch(String id, String presentableName) {
      super(id, presentableName);
    }

    @Override
    protected boolean isInContext(@NotNull PsiElement element) {
      return super.isInContext(element) &&
             (PsiTreeUtil.getParentOfType(element, PsiTryBranchImpl.class) != null ||
              PsiTreeUtil.getParentOfType(element, PsiCatchBranchImpl.class) != null);
    }
  }
}
