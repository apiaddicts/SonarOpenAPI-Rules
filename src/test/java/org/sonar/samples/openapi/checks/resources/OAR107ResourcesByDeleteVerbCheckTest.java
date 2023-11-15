package org.sonar.samples.openapi.checks.resources;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.samples.openapi.BaseCheckTest;

import apiquality.sonar.openapi.checks.resources.OAR107ResourcesByDeleteVerbCheck;

public class OAR107ResourcesByDeleteVerbCheckTest extends BaseCheckTest {

    @Before
    public void init() {
        ruleName = "OAR107";
        check = new OAR107ResourcesByDeleteVerbCheck();
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
        assertRuleProperties("OAR107 - ResourcesByDeleteVerb - Operation not recommended for resource path", RuleType.BUG, Severity.MAJOR, tags("resources"));
    }

    @Override
    public void verifyParameters() {
        assertNumberOfParameters(1);
        assertParameterProperties("reserved-words", "get,delete", RuleParamType.STRING);
    }
}