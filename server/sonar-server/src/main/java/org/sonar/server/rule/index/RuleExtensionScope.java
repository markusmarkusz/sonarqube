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

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.organization.OrganizationDto;

public class RuleExtensionScope {

  private static final String FAKE_UUID_FOR_SYSTEM = "system";

  private final Optional<String> organizationUuid;

  private RuleExtensionScope(@Nullable String organizationUuid) {
    this.organizationUuid = Optional.ofNullable(organizationUuid);
  }

  public static RuleExtensionScope system() {
    return new RuleExtensionScope(null);
  }

  public static RuleExtensionScope organization(OrganizationDto organization) {
    return organization(organization.getUuid());
  }

  public static RuleExtensionScope organization(String organizationUuid) {
    return new RuleExtensionScope(organizationUuid);
  }

  public RuleExtensionScope ifIsSystem(Runnable action) {
    if (!organizationUuid.isPresent()) {
      action.run();
    }
    return this;
  }

  public RuleExtensionScope ifIsOrganization(Consumer<String> action) {
    organizationUuid.ifPresent(action);
    return this;
  }

  public String getScope() {
    return organizationUuid.orElse(FAKE_UUID_FOR_SYSTEM);
  }

  public static RuleExtensionScope parse(String scope) {
    if (FAKE_UUID_FOR_SYSTEM.equals(scope))
      return system();
    return new RuleExtensionScope(scope);
  }
}