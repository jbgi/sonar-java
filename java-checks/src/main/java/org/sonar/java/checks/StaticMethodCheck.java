/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;

@Rule(
  key = "S2325",
  name = "\"private\" methods that don't access instance data should be \"static\"",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class StaticMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Symbol> outerClasses = new LinkedList<>();
  private Deque<Boolean> atLeastOneReference = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    outerClasses.push(tree.symbol());
    super.visitClass(tree);
    outerClasses.pop();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      return;
    }
    Symbol.MethodSymbol symbol = tree.symbol();
    if (symbol == null || (outerClasses.size() > 1 && !outerClasses.peek().isStatic())) {
      return;
    }
    atLeastOneReference.push(Boolean.FALSE);
    scan(tree.block());
    Boolean oneReference = atLeastOneReference.pop();
    if (symbol.isPrivate() && !symbol.isStatic() && !oneReference) {
      context.addIssue(tree, this, "Make \"" + symbol.name() + "\" a \"static\" method.");
    }
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    super.visitIdentifier(tree);
    if (!atLeastOneReference.isEmpty() && !atLeastOneReference.peek() && referenceInstance(tree.symbol())) {
      atLeastOneReference.pop();
      atLeastOneReference.push(Boolean.TRUE);
    }
  }

  private boolean referenceInstance(Symbol symbol) {
    return symbol.isUnknown() || (!symbol.isStatic() && fromInstance(symbol.owner()));
  }

  private boolean fromInstance(Symbol owner) {
    for (Symbol outerClass : outerClasses) {
      Type ownerType = owner.type();
      if (owner.equals(outerClass) || (ownerType != null && outerClass.type().isSubtypeOf(ownerType))) {
        return true;
      }
    }
    return false;
  }
}
