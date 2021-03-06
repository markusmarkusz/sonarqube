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
package org.sonar.db.rule;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class RuleDaoTest {

  private static final String ORGANIZATION_UUID = "org-1";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private RuleDao underTest = dbTester.getDbClient().ruleDao();

  @Test
  public void selectByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKey(dbTester.getSession(), ORGANIZATION_UUID, RuleKey.of("NOT", "FOUND")).isPresent()).isFalse();

    Optional<RuleDto> rule = underTest.selectByKey(dbTester.getSession(), ORGANIZATION_UUID, RuleKey.of("java", "S001"));
    assertThat(rule.isPresent()).isTrue();
    assertThat(rule.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectByKey_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKey(dbTester.getSession(), ORGANIZATION_UUID, RuleKey.of("java", "S001")).get().getOrganizationUuid())
      .isEqualTo(ORGANIZATION_UUID);
  }

  @Test
  public void selectDefinitionByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND")).isPresent()).isFalse();

    Optional<RuleDefinitionDto> rule = underTest.selectDefinitionByKey(dbTester.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.isPresent()).isTrue();
    assertThat(rule.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectById() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectById(55l, organizationUuid, dbTester.getSession())).isAbsent();
    Optional<RuleDto> ruleDtoOptional = underTest.selectById(1l, organizationUuid, dbTester.getSession());
    assertThat(ruleDtoOptional).isPresent();
    assertThat(ruleDtoOptional.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectById_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectById(1l, organizationUuid, dbTester.getSession()).get().getOrganizationUuid())
      .isEqualTo(organizationUuid);
  }

  @Test
  public void selectDefinitionById() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionById(55l, dbTester.getSession())).isAbsent();
    Optional<RuleDefinitionDto> ruleDtoOptional = underTest.selectDefinitionById(1l, dbTester.getSession());
    assertThat(ruleDtoOptional).isPresent();
    assertThat(ruleDtoOptional.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectByIds() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByIds(dbTester.getSession(), organizationUuid, asList(1))).hasSize(1);
    assertThat(underTest.selectByIds(dbTester.getSession(), organizationUuid, asList(1, 2))).hasSize(2);
    assertThat(underTest.selectByIds(dbTester.getSession(), organizationUuid, asList(1, 2, 3))).hasSize(2);

    assertThat(underTest.selectByIds(dbTester.getSession(), organizationUuid, asList(123))).isEmpty();
  }

  @Test
  public void selectByIds_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByIds(dbTester.getSession(), organizationUuid, asList(1, 2)))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid, organizationUuid);
  }

  @Test
  public void selectDefinitionByIds() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByIds(dbTester.getSession(), asList(1))).hasSize(1);
    assertThat(underTest.selectDefinitionByIds(dbTester.getSession(), asList(1, 2))).hasSize(2);
    assertThat(underTest.selectDefinitionByIds(dbTester.getSession(), asList(1, 2, 3))).hasSize(2);

    assertThat(underTest.selectDefinitionByIds(dbTester.getSession(), asList(123))).isEmpty();
  }

  @Test
  public void selectOrFailByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RuleDto rule = underTest.selectOrFailByKey(dbTester.getSession(), "org-1", RuleKey.of("java", "S001"));
    assertThat(rule.getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey_fails_if_rule_not_found() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    underTest.selectOrFailByKey(dbTester.getSession(), "org-1", RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectOrFailByKey_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectOrFailByKey(dbTester.getSession(), organizationUuid, RuleKey.of("java", "S001")).getOrganizationUuid())
      .isEqualTo(organizationUuid);
  }

  @Test
  public void selectOrFailDefinitionByKey_fails_if_rule_not_found() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    underTest.selectOrFailDefinitionByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectByKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByKeys(dbTester.getSession(), organizationUuid, Collections.emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(dbTester.getSession(), organizationUuid, asList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDto> rules = underTest.selectByKeys(dbTester.getSession(), organizationUuid, asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }

  @Test
  public void selectByKeys_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByKeys(dbTester.getSession(), organizationUuid, asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER"))))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid);
  }

  @Test
  public void selectDefinitionByKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByKeys(dbTester.getSession(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectDefinitionByKeys(dbTester.getSession(), asList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDefinitionDto> rules = underTest.selectDefinitionByKeys(dbTester.getSession(), asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }

  @Test
  public void selectAll() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectAll(dbTester.getSession(), "org-1"))
      .extracting(RuleDto::getId)
      .containsOnly(1, 2, 10);
  }

  @Test
  public void selectAll_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectAll(dbTester.getSession(), organizationUuid))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid, organizationUuid, organizationUuid);
  }

  @Test
  public void selectAllDefinitions() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RuleDefinitionDto> ruleDtos = underTest.selectAllDefinitions(dbTester.getSession());

    assertThat(ruleDtos).extracting("id").containsOnly(1, 2, 10);
  }

  @Test
  public void selectEnabled_with_ResultHandler() {
    dbTester.prepareDbUnit(getClass(), "selectEnabled.xml");

    final List<RuleDefinitionDto> rules = new ArrayList<>();
    ResultHandler resultHandler = resultContext -> rules.add((RuleDefinitionDto) resultContext.getResultObject());
    underTest.selectEnabled(dbTester.getSession(), resultHandler);

    assertThat(rules.size()).isEqualTo(1);
    RuleDefinitionDto ruleDto = rules.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.HTML);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void select_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String organizationUuid = "org-1";
    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid, RuleQuery.create())).hasSize(2);
    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid, RuleQuery.create().withKey("S001"))).hasSize(1);
    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid, RuleQuery.create().withConfigKey("S1"))).hasSize(1);
    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid, RuleQuery.create().withRepositoryKey("java"))).hasSize(2);
    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid,
      RuleQuery.create().withKey("S001").withConfigKey("S1").withRepositoryKey("java"))).hasSize(1);
  }

  @Test
  public void select_by_query_populates_organizationUuid_even_when_organization_has_no_metadata() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByQuery(dbTester.getSession(), organizationUuid, RuleQuery.create()))
        .extracting(RuleDto::getOrganizationUuid)
        .containsExactly(organizationUuid, organizationUuid);
  }

  @Test
  public void insert() throws Exception {
    RuleDefinitionDto newRule = new RuleDefinitionDto()
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(2_000_000_000_000L);
    underTest.insert(dbTester.getSession(), newRule);
    dbTester.getSession().commit();

    RuleDefinitionDto ruleDto = underTest.selectOrFailDefinitionByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getId()).isNotNull();
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
  }

  @Test
  public void update_RuleDefinitionDto() {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    RuleDefinitionDto ruleToUpdate = new RuleDefinitionDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setUpdatedAt(2_000_000_000_000L);

    underTest.update(dbTester.getSession(), ruleToUpdate);
    dbTester.getSession().commit();

    RuleDefinitionDto ruleDto = underTest.selectOrFailDefinitionByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
  }

  @Test
  public void update_RuleMetadataDto_inserts_row_in_RULE_METADATA_if_not_exists_yet() {
    dbTester.prepareDbUnit(getClass(), "update.xml");
    String organizationUuid = "org-1";

    RuleMetadataDto ruleToUpdate = new RuleMetadataDto()
      .setRuleId(1)
      .setOrganizationUuid(organizationUuid)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime())
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime())
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setRemediationGapMultiplier("1h")
      .setRemediationBaseEffort("5min")
      .setTags(newHashSet("tag1", "tag2"))
      .setCreatedAt(3_500_000_000_000L)
      .setUpdatedAt(4_000_000_000_000L);

    underTest.update(dbTester.getSession(), ruleToUpdate);
    dbTester.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(dbTester.getSession(), organizationUuid, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(4_000_000_000_000L);
  }

  @Test
  public void update_RuleMetadataDto_updates_row_in_RULE_METADATA_if_already_exists() {
    dbTester.prepareDbUnit(getClass(), "update.xml");
    String organizationUuid = "org-1";
    RuleMetadataDto metadataV1 = new RuleMetadataDto()
        .setRuleId(1)
        .setOrganizationUuid(organizationUuid)
        .setCreatedAt(3_500_000_000_000L)
        .setUpdatedAt(4_000_000_000_000L);
    RuleMetadataDto metadataV2 = new RuleMetadataDto()
        .setRuleId(1)
        .setOrganizationUuid(organizationUuid)
        .setNoteData("My note")
        .setNoteUserLogin("admin")
        .setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime())
        .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime())
        .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
        .setRemediationGapMultiplier("1h")
        .setRemediationBaseEffort("5min")
        .setTags(newHashSet("tag1", "tag2"))
        .setCreatedAt(6_500_000_000_000L)
        .setUpdatedAt(7_000_000_000_000L);

    underTest.update(dbTester.getSession(), metadataV1);
    dbTester.commit();

    assertThat(dbTester.countRowsOfTable("RULES_METADATA")).isEqualTo(1);
    RuleDto ruleDto = underTest.selectOrFailByKey(dbTester.getSession(), organizationUuid, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isNull();
    assertThat(ruleDto.getNoteUserLogin()).isNull();
    assertThat(ruleDto.getNoteCreatedAt()).isNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNull();
    assertThat(ruleDto.getRemediationFunction()).isNull();
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).isEmpty();
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(4_000_000_000_000L);

    underTest.update(dbTester.getSession(), metadataV2);
    dbTester.commit();

    ruleDto = underTest.selectOrFailByKey(dbTester.getSession(), organizationUuid, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(7_000_000_000_000L);
  }

  @Test
  public void select_parameters_by_rule_key() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    List<RuleParamDto> ruleDtos = underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getRuleId()).isEqualTo(1);
  }

  @Test
  public void select_parameters_by_rule_keys() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");

    assertThat(underTest.selectRuleParamsByRuleKeys(dbTester.getSession(),
      Arrays.asList(RuleKey.of("checkstyle", "AvoidNull"), RuleKey.of("unused", "Unused")))).hasSize(2);

    assertThat(underTest.selectRuleParamsByRuleKeys(dbTester.getSession(),
      singletonList(RuleKey.of("unknown", "Unknown")))).isEmpty();
  }

  @Test
  public void insert_parameter() {
    dbTester.prepareDbUnit(getClass(), "insert_parameter.xml");
    RuleDefinitionDto rule1 = underTest.selectOrFailDefinitionByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));

    RuleParamDto param = RuleParamDto.createFor(rule1)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    underTest.insertRuleParam(dbTester.getSession(), rule1, param);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insert_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void update_parameter() {
    dbTester.prepareDbUnit(getClass(), "update_parameter.xml");

    RuleDefinitionDto rule1 = underTest.selectOrFailDefinitionByKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    List<RuleParamDto> params = underTest.selectRuleParamsByRuleKey(dbTester.getSession(), rule1.getKey());
    assertThat(params).hasSize(1);

    RuleParamDto param = Iterables.getFirst(params, null);
    param
      .setName("format")
      .setType("STRING")
      .setDefaultValue("^[a-z]+(\\.[a-z][a-z0-9]*)*$")
      .setDescription("Regular expression used to check the package names against.");

    underTest.updateRuleParam(dbTester.getSession(), rule1, param);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "update_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void delete_parameter() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    assertThat(underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).hasSize(1);

    underTest.deleteRuleParam(dbTester.getSession(), 1);
    dbTester.getSession().commit();

    assertThat(underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).isEmpty();
  }
}
