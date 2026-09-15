package apiaddicts.sonar.openapi.checks.security;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static apiaddicts.sonar.openapi.utils.JsonNodeUtils.isStringType;

@Rule(key = OAR075StringParameterIntegrityCheck.KEY)
public class OAR075StringParameterIntegrityCheck extends AbstractTypedParameterIntegrityCheck {

    public static final String KEY = "OAR075";
    private static final String MESSAGE = "OAR075.error";
    private static final String DEFAULT = "minLength,maxLength,pattern,enum";

    private static final Set<String> SELF_CONSTRAINED_FORMATS = new HashSet<>(Arrays.asList(
            "date", "date-time", "uuid", "ipv4", "ipv6"));

    @RuleProperty(
            key = "parameter_integrity",
            description = "String integrity checks",
            defaultValue = DEFAULT
    )
    private String integrityStr = DEFAULT;

    public OAR075StringParameterIntegrityCheck() {
        super(KEY, MESSAGE);
    }

    @Override
    protected boolean isTargetType(JsonNode typeNode) {
        return isStringType(typeNode);
    }

    @Override
    protected void validateTypedNode(JsonNode node,JsonNode typeNode) {
        if (hasSelfConstrainedFormat(node.get("format"))) return;

        Set<String> checks = Arrays.stream(integrityStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        boolean ok = checks.stream().anyMatch(k->{
            JsonNode n = node.get(k);
            return n != null && !n.isMissing();
        });

        if(!ok) addIssue(ruleKey,translate(messageKey),typeNode);
    }

    private static boolean hasSelfConstrainedFormat(JsonNode formatNode) {
        if (formatNode == null || formatNode.isMissing() || formatNode.isNull()) return false;
        if (formatNode.isArray() || formatNode.isObject()) return false;
        String value = formatNode.getTokenValue();
        return value != null && SELF_CONSTRAINED_FORMATS.contains(value.trim().toLowerCase(Locale.ROOT));
    }
}