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
package org.sonar.java.checks.methods;

import com.google.common.collect.Lists;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;

public class MethodInvocationMatcherCollection {

  private List<MethodInvocationMatcher> matchers = Lists.newLinkedList();

  private MethodInvocationMatcherCollection() {
  }

  public static MethodInvocationMatcherCollection create() {
    return new MethodInvocationMatcherCollection();
  }

  public static MethodInvocationMatcherCollection create(MethodInvocationMatcher... matchers) {
    MethodInvocationMatcherCollection collection = new MethodInvocationMatcherCollection();
    for (MethodInvocationMatcher matcher : matchers) {
      collection.matchers.add(matcher);
    }
    return collection;
  }

  public MethodInvocationMatcherCollection add(MethodInvocationMatcher matcher) {
    this.matchers.add(matcher);
    return this;
  }

  public boolean anyMatch(MethodInvocationTree mit) {
    for (MethodInvocationMatcher matcher : matchers) {
      if (matcher.matches(mit)) {
        return true;
      }
    }
    return false;
  }
}
