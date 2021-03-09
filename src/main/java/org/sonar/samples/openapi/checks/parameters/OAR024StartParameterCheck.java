package org.sonar.samples.openapi.checks.parameters;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

import static org.sonar.samples.openapi.utils.VerbPathMatcher.GET_ALL;
import static org.sonar.samples.openapi.utils.VerbPathMatcher.POST_GET;
import static org.sonar.samples.openapi.utils.VerbPathMatcher.POST_SUB_RESOURCE_GET;

@Rule(key = OAR024StartParameterCheck.KEY)
public class OAR024StartParameterCheck extends AbstractParameterCheck {

	public static final String KEY = "OAR024";
	private static final String PARAM = "$start";
	private static final String DEFAULT_PATTERN = GET_ALL + POST_GET + POST_SUB_RESOURCE_GET;
	private static final String DEFAULT_EXCLUSION = "get:/status";

	@RuleProperty(
			key = "resources-paths",
			description = "List of resources paths to apply the rule. Format: <V>,<V>:<RX>,<RX>;<V>,<V>:<RX>,<RX>",
			defaultValue = DEFAULT_PATTERN
	)
	private String pattern = DEFAULT_PATTERN;

	@RuleProperty(
			key = "resources-exclusions",
			description = "List of explicit resources to exclude from this rule. Format: <V>,<V>:<R>,<R>;<V>,<V>:<R>,<R>",
			defaultValue = DEFAULT_EXCLUSION
	)
	private String exclusion = DEFAULT_EXCLUSION;

	public OAR024StartParameterCheck() {
		super(KEY, PARAM);
		super.verbPathPattern = pattern;
		super.verbExclusions = exclusion;
	}
}