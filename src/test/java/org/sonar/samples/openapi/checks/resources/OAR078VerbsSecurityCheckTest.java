package org.sonar.samples.openapi.checks.resources;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.samples.openapi.BaseCheckTest;

public class OAR078VerbsSecurityCheckTest extends BaseCheckTest {
    
    @Before
    public void init() {
        ruleName = "OAR078";
        check = new OAR078VerbsSecurityCheck();
        v2Path = getV2Path("resources");
        v3Path = getV3Path("resources");
    }

    @Test
    public void verifyInV2WithSecurity() {
        verifyV2("with-security");
    }

    @Test
    public void verifyInV2NoSecurity() {
        verifyV2("no-security");
    }

    @Test
    public void verifyInV3WithSecurity() {
        verifyV3("with-security");
    }

    @Test
    public void verifyInV3NoSecurity() {
        verifyV3("no-security");
    }

    @Override
    public void verifyRule() {
        assertRuleProperties("OAR078 - VerbsSecurity - All API methods must have security", RuleType.BUG, Severity.MAJOR, tags("resources"));
    }
}