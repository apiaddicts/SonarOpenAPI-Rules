package apiquality.sonar.openapi.checks.resources;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.apiaddicts.apitools.dosonarapi.api.v2.OpenApi2Grammar;
import org.apiaddicts.apitools.dosonarapi.api.v3.OpenApi3Grammar;
import apiquality.sonar.openapi.checks.BaseCheck;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Rule(key = OAR106ResourcesByPatchVerbCheck.KEY)
public class OAR106ResourcesByPatchVerbCheck extends BaseCheck {

    public static final String KEY = "OAR106";
    private static final String MESSAGE = "OAR106.error";
    private static final String RESERVED_WORDS = "get,delete";
    @RuleProperty(
        key = "reserved-words",
        description = "Comma-separated list of reserved words that should not appear as consecutive static parts",
        defaultValue = RESERVED_WORDS
    )
    private String reservedWordsStr = RESERVED_WORDS;

    private Set<String> reservedWords;

    public OAR106ResourcesByPatchVerbCheck() {
        init();
    }

    private void init() {
        reservedWords = Arrays.stream(reservedWordsStr.split(","))
                              .map(String::toLowerCase)
                              .map(String::trim)
                              .collect(Collectors.toSet());
    }

    @Override
    public Set<AstNodeType> subscribedKinds() {
        return ImmutableSet.of(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
    }

    @Override
    public void visitNode(JsonNode node) {
        if ("patch".equalsIgnoreCase(node.key().getTokenValue())) {
            JsonNode pathNode = findParentPathNode(node);
            if (pathNode != null) {
                String path = pathNode.key().getTokenValue();
                if (!isCorrect(path)) {
                    String formattedMessage = formatMessage(path);
                    addIssue(KEY, formattedMessage, node.key());
                }
            }
        }
    }

    private JsonNode findParentPathNode(JsonNode node) {
        JsonNode parent = (JsonNode) node.getParent();
        while (parent != null && !(parent.getType() == OpenApi2Grammar.PATH || parent.getType() == OpenApi3Grammar.PATH)) {
            parent = (JsonNode) parent.getParent();
        }
        return parent;
    }

    private boolean isCorrect(String path) {
        String[] parts = Stream.of(path.split("/"))
                               .filter(p -> !p.trim().isEmpty())
                               .toArray(String[]::new);
        if (parts.length == 0) return true;
    
        // Marcar como no conforme si la ruta contiene solo un segmento y este no es una variable
        if (parts.length == 1 && !isVariable(parts[0])) {
            return false;
        }
    
        // Verificar palabras reservadas en segmentos estáticos
        for (String part : parts) {
            if (!isVariable(part) && reservedWords.contains(part.toLowerCase())) {
                return false;
            }
        }
    
        return true;
    }
    
    private boolean isVariable(String part) {
        return part.startsWith("{") && part.endsWith("}");
    }


    private String formatMessage(String path) {
        String[] parts = path.split("/");
        String resource = parts.length > 1 ? parts[1] : "";
        String nextPart = parts.length > 2 ? "/" + parts[2] : ""; 
        return translate(MESSAGE, resource + nextPart); 
    }
}