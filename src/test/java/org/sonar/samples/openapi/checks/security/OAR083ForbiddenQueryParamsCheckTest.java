package org.sonar.samples.openapi.checks.security;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.samples.openapi.BaseCheckTest;

public class OAR083ForbiddenQueryParamsCheckTest extends BaseCheckTest {

    @Before
    public void init() {
        ruleName = "OAR083";
        check = new OAR083ForbiddenQueryParamsCheck();
        v2Path = getV2Path("security");
        v3Path = getV3Path("security");
    }

    @Test
    public void verifyInV2() {
        verifyV2("valid-query-params");
    }

    @Test
    public void verifyInV2ForbiddenQueryParams() {
        verifyV2("forbidden-query-params");
    }

    @Test
    public void verifyInV3() {
        verifyV3("valid-query-params");
    }

    @Test
    public void verifyInV3ForbiddenQueryParams() {
        verifyV3("forbidden-query-params");
    }

    @Override
    public void verifyRule() {
        assertRuleProperties("OAR083 - ForbiddenQueryParams - Some parameters should not pass through this querystring", RuleType.VULNERABILITY, Severity.MAJOR, tags("vulnerability"));
    }

    @Override
    public void verifyParameters() {
        assertNumberOfParameters(3);
        assertParameterProperties("forbidden-query-params", "forbiddenParam1, forbiddenParam2", RuleParamType.STRING);
        assertParameterProperties("paths", "/examples", RuleParamType.STRING);
        assertParameterProperties("pathValidationStrategy", "/include", RuleParamType.STRING);
    }
}