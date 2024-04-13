package apiquality.sonar.openapi.utils;

import org.apiaddicts.apitools.dosonarapi.api.v2.OpenApi2Grammar;
import org.apiaddicts.apitools.dosonarapi.api.v3.OpenApi3Grammar;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.sonar.sslr.api.AstNodeType;

public class JsonNodeUtils {

    private JsonNodeUtils() {
        // Intentional blank
    }

    public static final String PROPERTIES = "properties";
    public static final String TYPE = "type";
    public static final String REQUIRED = "required";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_ANY = "*";

    public static JsonNode resolve(JsonNode original) {
        if (original.isRef()) {
            String ref = original.get("$ref").getTokenValue();
            if (ref.startsWith("#")) {
                return original.resolve();
            } else {
                // Gestión de referencias externas
                return resolveExternalRef(ref);
            }
        }
        JsonNode allOf = original.get("allOf");
        if (allOf != null && !allOf.isMissing()) {
            Collection<JsonNode> refs = allOf.elements();
            if (refs.size() == 1) {
                JsonNode refNode = refs.iterator().next();
                if (refNode.isRef()) {
                    String ref = refNode.get("$ref").getTokenValue();
                    if (ref.startsWith("#")) {
                        return refNode.resolve();
                    } else {
                        return resolveExternalRef(ref);
                    }
                }
            }
        }
        return original;
    }

    private static JsonNode resolveExternalRef(String ref) {
        try {
            URL url = new URL(ref);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
    
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = conn.getInputStream()) {
                    String jsonResponse = readInputStreamToString(is);
                    System.out.println("External JSON Response: " + jsonResponse);  // Muestra el contenido del JSON obtenido
                    return parseJsonFromString(jsonResponse);
                }
            } else {
                System.out.println("Failed to fetch external JSON: HTTP error code " + conn.getResponseCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while fetching external JSON: " + e.getMessage());
        }
        return null; // o algún nodo de error
    }

    private static String readInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private static JsonNode parseJsonFromString(String json) {
        // Implementa tu lógica de deserialización aquí
        // Esta parte depende de cómo deserializas una cadena JSON a JsonNode en tu infraestructura actual
        return null;  // Este es solo un placeholder.
    }
    


    public static JsonNode getType(JsonNode schema) {
        return schema.get(TYPE);
    }

    public static JsonNode getProperties(JsonNode schema) {
        return schema.get(PROPERTIES);
    }

    public static JsonNode getRequired(JsonNode schema) {
        return schema.get(REQUIRED);
    }

    public static Set<String> getRequiredValues(JsonNode required) {
        return required.elements().stream().map(JsonNode::getTokenValue).collect(Collectors.toSet());
    }

    public static boolean isObjectType(JsonNode schemaNode) {
        return isType(schemaNode, TYPE_OBJECT);
    }

    public static boolean isArrayType(JsonNode schemaNode) {
        return isType(schemaNode, TYPE_ARRAY);
    }

    public static boolean isStringType(JsonNode schemaNode) {
        return isType(schemaNode, TYPE_STRING);
    }

    public static boolean isIntegerType(JsonNode schemaNode) {
        return isType(schemaNode, TYPE_INTEGER);
    }

    public static boolean isBooleanType(JsonNode schemaNode) {
        return isType(schemaNode, TYPE_BOOLEAN);
    }

    public static boolean isType(JsonNode type, String name) {
        return TYPE_ANY.equals(name) || name.equals(type.getTokenValue());
    }

    public static boolean isOperation(JsonNode node) {
        AstNodeType type = node.getType();
        return type.equals(OpenApi2Grammar.OPERATION) || type.equals(OpenApi3Grammar.OPERATION);
    }
}
