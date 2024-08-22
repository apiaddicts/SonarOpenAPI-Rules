package org.sonar.samples.openapi.checks.resources;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.samples.openapi.BaseCheckTest;

public class OAR015ResourceLevelMaxAllowedCheckTest extends BaseCheckTest {

    @Before
    public void init() {
        ruleName = "OAR015";
        check = new OAR015ResourceLevelMaxAllowedCheck();
        v2Path = getV2Path("resources");
        v3Path = getV3Path("resources");
    }

    @Test
    public void verifyInV2() {
        verifyV2("plain");
    }

    @Test
    public void verifyInV3() {
        verifyV3("plain");
    }

    @Override
    public void verifyRule() {
        assertRuleProperties("OAR015 - ResourceLevelMax - Resources depth level should be smaller", RuleType.BUG, Severity.CRITICAL, tags("resources"));
    }

    @Override
    public void verifyParameters() {
        assertNumberOfParameters(1);
        assertParameterProperties("max-level-allowed", "5", RuleParamType.INTEGER);
    }
}
