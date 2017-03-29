/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.index;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.server.es.BaseDoc;

public class RuleExtensionDoc extends BaseDoc {

  public RuleExtensionDoc(Map<String, Object> fields) {
    super(fields);
  }

  public RuleExtensionDoc() {
    super(Maps.newHashMapWithExpectedSize(4));
  }

  @Override
  public String getId() {
    return getKey().toString() + ":" + getScope();
  }

  @Override
  public String getRouting() {
    return null;
  }

  @Override
  public String getParent() {
    return getKey().toString();
  }

  public RuleKey getKey() {
    return getField(RuleIndexDefinition.FIELD_RULE_EXTENSION_KEY);
  }

  public RuleExtensionDoc setKey(@Nullable RuleKey key) {
    setField(RuleIndexDefinition.FIELD_RULE_EXTENSION_KEY, key);
    return this;
  }

  public RuleExtensionScope getScope() {
    return RuleExtensionScope.parse(getField(RuleIndexDefinition.FIELD_RULE_EXTENSION_SCOPE));
  }

  public RuleExtensionDoc setScope(@Nullable RuleExtensionScope scope) {
    setField(RuleIndexDefinition.FIELD_RULE_EXTENSION_SCOPE, scope.getScope());
    return this;
  }

  public Set<String> getTags() {
    return getField(RuleIndexDefinition.FIELD_RULE_EXTENSION_RULE_TAGS);
  }

  public RuleExtensionDoc setTags(Set<String> tags) {
    setField(RuleIndexDefinition.FIELD_RULE_EXTENSION_RULE_TAGS, tags);
    return this;
  }

  public static RuleExtensionDoc of(RuleKey key, RuleExtensionScope scope, RuleMetadataDto ruleExtension) {
    return new RuleExtensionDoc()
      .setKey(key)
      .setScope(scope)
      .setTags(ruleExtension.getTags());
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
