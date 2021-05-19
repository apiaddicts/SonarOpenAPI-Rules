package org.sonar.samples.openapi.checks.resources;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNodeType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sonar.samples.openapi.utils.JsonNodeUtils.*;

@Rule(key = OAR029StandardResponseCheck.KEY)
public class OAR029StandardResponseCheck extends AbstractSchemaCheck {

    public static final String KEY = "OAR029";

    //private static final String RESPONSE_SCHEMA = "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"object\",\"properties\":{\"http_status\":{\"type\":\"string\"},\"code\":{\"type\":\"integer\"},\"description\":{\"type\":\"string\"},\"internal_code\":{\"type\":\"string\"},\"errors\":{\"type\":[\"array\",\"null\"],\"items\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"value\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"name\",\"value\"]}}},\"required\":[\"http_status\",\"code\",\"description\"]},\"data\":{\"type\":[\"object\",\"array\",\"null\"]}},\"required\":[\"status\",\"data\"]}";
    private static final String RESPONSE_SCHEMA = "{\"type\":\"object\",\"properties\":{\"result\":{\"type\":\"object\",\"properties\":{\"http_code\":{\"type\":\"integer\"},\"status\":{\"type\":\"boolean\"},\"trace_id\":{\"type\":\"string\"},\"errors\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"value\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"name\",\"value\"]}}},\"required\":[\"status\",\"http_code\",\"trace_id\"]},\"data\":{\"type\":\"any\"}},\"requiredOnError\":[],\"requiredOnSuccess\":[\"data\"],\"requiredAlways\":[\"result\"],\"dataProperty\":\"data\"}";

    @RuleProperty(
            key = "response-schema",
            description = "Response Schema as String",
            defaultValue = RESPONSE_SCHEMA
    )
    private String responseSchemaStr = RESPONSE_SCHEMA;
    private JSONObject responseSchema;

    private JSONArray requiredOnSuccess = null;
    private JSONArray requiredOnError = null;
    private JSONArray requiredAlways = null;
    private JSONObject responseSchemaProperties = null;
    private String dataProperty = null;

    private static final String DEFAULT_EXCLUSION = "/status";
    @RuleProperty(
            key = "path-exclusions",
            description = "List of explicit paths to exclude from this rule.",
            defaultValue = DEFAULT_EXCLUSION
    )
    private String exclusionStr = DEFAULT_EXCLUSION;
    private Set<String> exclusion;

    public OAR029StandardResponseCheck() {
        super(KEY);
    }

    @Override
    protected void visitFile(JsonNode root) {
        exclusion = Arrays.stream(exclusionStr.split(",")).map(String::trim).collect(Collectors.toSet());

        try {
            responseSchema = new JSONObject(responseSchemaStr);
            requiredOnSuccess = (responseSchema.has("requiredOnSuccess")) ? responseSchema.getJSONArray("requiredOnSuccess") : null;
            requiredOnError = (responseSchema.has("requiredOnError")) ? responseSchema.getJSONArray("requiredOnError") : null;
            requiredAlways = (responseSchema.has("requiredAlways")) ? responseSchema.getJSONArray("requiredAlways") : null;
            responseSchemaProperties = (responseSchema.has("properties")) ? responseSchema.getJSONObject("properties") : null;
            dataProperty = (responseSchema.has("dataProperty")) ? responseSchema.getString("dataProperty") : null;
        } catch (JSONException err) {
			addIssue(KEY, "Error parsing Standard Response Schemas", root.key());
        }
    }

    @Override
    public Set<AstNodeType> subscribedKinds() {
        return ImmutableSet.of(OpenApi2Grammar.PATH);
    }

    @Override
    public void visitNode(JsonNode node) {
        visitV2Node(node);
    }

    private void visitV2Node(JsonNode node) {
        String path = node.key().getTokenValue();
        if (exclusion.contains(path)) return;
        List<JsonNode> allResponses = node.properties().stream() // operations
                .map(JsonNode::value)
                .flatMap(n -> n.properties().stream()) // responses
                .map(JsonNode::value)
                .flatMap(n -> n.properties().stream()) // response
                .collect(Collectors.toList());
        for (JsonNode responseNode : allResponses) {
            String statusCode = responseNode.key().getTokenValue();
            boolean successCode = false;
            int code = 0;
            if (!statusCode.equalsIgnoreCase("default")) {
                code = Integer.parseInt(statusCode);
                successCode = 200 <= code && 300 > code && code != 204;
            }

            JsonNode schemaNode = responseNode.value().get("schema");
            if (schemaNode.isMissing()) continue;
            schemaNode = resolve(schemaNode);

            Map<String, JsonNode> properties = getAllProperties(schemaNode);

            if (successCode) {
                validateRootProperties(requiredOnSuccess, properties, schemaNode);
            } else {
                /*if (requiredOnError != null && requiredOnError.length() > 0 && responseSchemaProperties != null) {
                    JsonNode parentNode = schemaNode.key();
                    requiredOnError.toList().forEach(property -> {
                        String propertyName = (String) property;
                        JSONObject propertySchema = (responseSchemaProperties != null && responseSchemaProperties.has(propertyName)) ? responseSchemaProperties.getJSONObject(propertyName) : null;
                        String propertyType = (propertySchema != null && propertySchema.has("type")) ? propertySchema.getString("type") : TYPE_ANY;
                        if (propertyType == null || propertyType.isBlank() || propertyType.equals("any")) propertyType = TYPE_ANY;
                        validateProperty(properties, propertyName, propertyType, parentNode);
                    });
                }*/
                validateRootProperties(requiredOnError, properties, schemaNode);
            }

            if (code != 204) {
                //validateProperty(properties, "result", TYPE_OBJECT, schemaNode.key())
                //        .ifPresent(this::validateResult);
                validateRootProperties(requiredAlways, properties, schemaNode);
            }
        }
    }

    private void validateRootProperties(JSONArray requiredPropertiesJSONArray, Map<String, JsonNode> properties, JsonNode parentNode) {
        if (requiredPropertiesJSONArray != null && requiredPropertiesJSONArray.length() > 0) {
            Set<String> requiredProperties = requiredPropertiesJSONArray.toList().stream().map(element -> (String) element).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
            //System.out.println(requiredProperties);
            //validateRequiredProperties(parentNode, requiredProperties, String.join(", ", requiredProperties));
            requiredProperties.forEach(propertyName -> {
                JSONObject propertySchema = (responseSchemaProperties != null && responseSchemaProperties.has(propertyName)) ? responseSchemaProperties.getJSONObject(propertyName) : null;
                String propertyType = (propertySchema != null && propertySchema.has("type")) ? propertySchema.getString("type") : TYPE_ANY;
                if (propertyType == null || propertyType.isBlank() || propertyType.equals("any")) propertyType = TYPE_ANY;
                validateProperty(properties, propertyName, propertyType, parentNode.key()).ifPresent(node -> {
                    validateProperties(propertyName, propertySchema, node);
                });
            });
        }
    }

    private void validateProperties(String propertyName, JSONObject propertySchema, JsonNode parentNode) {
        if (propertyName.equals(dataProperty)) {
            Map<String, JsonNode> allProp = getAllProperties(parentNode);
            if (allProp.isEmpty() && !parentNode.get("type").getTokenValue().equals("array")) {
                addIssue(KEY, translate("OARE001.error-required-one-property", propertyName), parentNode.key());
            }
        } else {
            JsonNode properties = getProperties(parentNode);
            JSONArray requiredPropertiesJSONArray = (propertySchema != null && propertySchema.has("required")) ? propertySchema.getJSONArray("required") : null;
            if (requiredPropertiesJSONArray != null && requiredPropertiesJSONArray.length() > 0 ) {
                Set<String> requiredProperties = requiredPropertiesJSONArray.toList().stream().map(element -> (String) element).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
                System.out.println(requiredProperties);
                validateRequiredProperties(parentNode, requiredProperties, String.join(", ", requiredProperties));
            }
            Map<String, JsonNode> propertyMap = properties.propertyMap();
            JSONObject propertySchemaProperties = (propertySchema != null && propertySchema.has("properties")) ? propertySchema.getJSONObject("properties") : null;
            List<String> sortedList = new ArrayList<>(propertySchemaProperties.keySet());
            Collections.sort(sortedList);
            sortedList.forEach(subpropertyName -> {
                JSONObject subpropertySchema = (propertySchemaProperties != null && propertySchemaProperties.has(subpropertyName)) ? propertySchemaProperties.getJSONObject(subpropertyName) : null;
                String propertyType = (subpropertySchema != null && subpropertySchema.has("type")) ? subpropertySchema.getString("type") : TYPE_ANY;
                validateProperty(propertyMap, subpropertyName, propertyType, properties.key());
            });
        }
    }
}
