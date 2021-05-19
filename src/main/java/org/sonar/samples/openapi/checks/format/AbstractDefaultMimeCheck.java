package org.sonar.samples.openapi.checks.format;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.samples.openapi.checks.BaseCheck;
import org.sonar.sslr.yaml.grammar.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sonar.samples.openapi.utils.JsonNodeUtils.*;

public abstract class AbstractDefaultMimeCheck extends BaseCheck {

	private static final String DEFAULT_MEDIA_TYPE_VALUE = "application/json";
	private static final String MEDIA_TYPE_EXCEPTIONS_VALUE = "-";

	private String key;
	private String section;
	private String message;

	@RuleProperty(
			key = "media-type-exceptions",
			description = "Media type exceptions.",
			defaultValue = MEDIA_TYPE_EXCEPTIONS_VALUE)
	public String mediaTypeExceptionsStr = MEDIA_TYPE_EXCEPTIONS_VALUE;

    private Set<String> mediaTypeExceptions;

	@RuleProperty(
			key = "default-mime-type",
			description = "Default media type.",
			defaultValue = DEFAULT_MEDIA_TYPE_VALUE)
	public String defaultMediaType = DEFAULT_MEDIA_TYPE_VALUE;

	private boolean globalSupportsDefaultMimeType = false;

	public AbstractDefaultMimeCheck(String key, String section, String message) {
		this.key = key;
		this.section = section;
		this.message = translate(message);
	}

	@Override
	public Set<AstNodeType> subscribedKinds() {
		return ImmutableSet.of(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION, OpenApi3Grammar.RESPONSE);
	}

	@Override
	protected void visitFile(JsonNode root) {
		mediaTypeExceptions = Stream.of(mediaTypeExceptionsStr.split(","))
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
		globalSupportsDefaultMimeType = (root.getType() instanceof OpenApi2Grammar) ? supportsDefaultMimeTypeV2(root) : false;
	}

	@Override
	public void visitNode(JsonNode node) {
		if (node.getType() instanceof OpenApi2Grammar) {
			visitV2Node(node);
		} else {
			visitV3Node(node);
		}
	}

	private void visitV2Node(JsonNode node) {
		JsonNode sectionNode = node.get(section);
		boolean definesMimeTypes = !sectionNode.isMissing();

		if (definesMimeTypes) {
			if (!supportsDefaultMimeTypeV2(node)) {
				addIssue(key, message, sectionNode.key());
			}
		} else {
			if (!globalSupportsDefaultMimeType) {
				addIssue(key, message, node.key());
			}
		}
	}

	private void visitV3Node(JsonNode node) {
		if ( node.getType() == OpenApi3Grammar.OPERATION && section.equals("consumes") ) {
			String operation = node.key().getTokenValue().toLowerCase();
			if ( operation.equals("post") || operation.equals("put") || operation.equals("patch") ) {
				visitContentNode(node);
			} else {
				JsonNode requestBodyNode = node.at("/requestBody");
				if (!requestBodyNode.isMissing()) {
					addIssue(key, translate("OAR010.error-request-body-not-allowed", operation.toUpperCase()), node.key());
				}
			}
		}

		if ( node.getType() == OpenApi3Grammar.RESPONSE && section.equals("produces") ) {
			visitContentNode(node);
		}
	}

	private void visitContentNode(JsonNode node) {
		JsonNode contentNode;
		if (section.equals("consumes")) {
			JsonNode requestBodyNode = node.at("/requestBody");
			contentNode = resolve(requestBodyNode).at("/content");
		} else {
			contentNode = node.at("/content");
		}
		boolean definesMimeTypes = !contentNode.isMissing();
		if (definesMimeTypes) {
			if( !supportsDefaultMimeTypeV3(contentNode) ) {
				addIssue(key, message, contentNode.key());
			}
		} else {
			addIssue(key, message, node.key());
		}
	}

	private boolean supportsDefaultMimeTypeV2(JsonNode node) {
		JsonNode consumes = node.get(section);
		if (consumes.isMissing() || consumes.isNull()) return false;
		List<JsonNode> mimeTypeNodes = consumes.elements();
		if (mimeTypeNodes.stream().map(AstNode::getTokenValue).anyMatch(mediaTypeExceptions::contains)) return true;
		return mimeTypeNodes.stream().map(AstNode::getTokenValue).anyMatch(defaultMediaType::equals);
	}

	private boolean supportsDefaultMimeTypeV3(JsonNode content) {
		if (content.isMissing() || content.isNull()) return false;
		Map<String, JsonNode> properties = content.propertyMap();
		if (properties.entrySet().stream().map(entry -> entry.getKey()).anyMatch(mediaTypeExceptions::contains)) return true;
		return properties.entrySet().stream().map(entry -> entry.getKey()).anyMatch(defaultMediaType::equals);
	}
}
