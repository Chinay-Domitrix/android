// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.android.inspections;

import com.android.tools.idea.res.IdeResourcesUtil;
import com.intellij.codeInsight.daemon.impl.quickfix.ConvertSwitchToIfIntention;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.IntentionPowerPackBundle;
import com.siyeh.ipp.switchtoif.ReplaceSwitchWithIfIntention;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class AndroidNonConstantResIdsInSwitchInspection extends LocalInspectionTool {
  private final ReplaceSwitchWithIfIntention myBaseIntention = new ReplaceSwitchWithIfIntention();

  @Nls
  @NotNull
  @Override
  public String getGroupDisplayName() {
    return AndroidBundle.message("android.inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return AndroidBundle.message("android.inspections.non.constant.res.ids.in.switch.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return "AndroidNonConstantResIdsInSwitch";
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitSwitchLabelStatement(@NotNull PsiSwitchLabelStatement statement) {
        final AndroidFacet facet = AndroidFacet.getInstance(statement);
        if (facet == null || facet.getConfiguration().isAppProject()) {
          return;
        }

        final PsiExpression caseValue = statement.getCaseValue();
        if (!(caseValue instanceof PsiReferenceExpression)) {
          return;
        }

        final PsiSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(statement, PsiSwitchStatement.class);
        if (switchStatement == null || !ReplaceSwitchWithIfIntention.canProcess(switchStatement)) {
          return;
        }

        final PsiElement resolvedElement = ((PsiReferenceExpression)caseValue).resolve();
        if (resolvedElement == null || !(resolvedElement instanceof PsiField)) {
          return;
        }

        final PsiField resolvedField = (PsiField)resolvedElement;
        if (!IdeResourcesUtil.isResourceField(resolvedField)) {
          return;
        }

        final PsiModifierList modifierList = resolvedField.getModifierList();

        if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.FINAL)) {
          holder.registerProblem(caseValue, AndroidBundle.message("android.inspections.non.constant.res.ids.in.switch.message"),
                                 new MyQuickFix());
        }
      }
    };
  }

  @IntentionFamilyName
  public String getQuickFixName() {
    return IntentionPowerPackBundle.message("replace.switch.with.if.intention.name");
  }

  private class MyQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getFamilyName() {
      return getQuickFixName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final PsiElement element = descriptor.getPsiElement();
      if (element == null) {
        return;
      }

      final PsiSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(element, PsiSwitchStatement.class);
      if (switchStatement == null) {
        return;
      }

      ConvertSwitchToIfIntention.doProcessIntention(switchStatement);
    }
  }
}
