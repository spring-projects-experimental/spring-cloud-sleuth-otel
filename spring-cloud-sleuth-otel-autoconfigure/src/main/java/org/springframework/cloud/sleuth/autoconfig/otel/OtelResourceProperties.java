/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.autoconfig.otel;

import java.util.Collections;
import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for setting OTel resource providers.
 *
 * @author Knut Schleßelmann
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@ConfigurationProperties("spring.sleuth.otel.resource")
public class OtelResourceProperties {

	/**
	 * Enables default {@link Resource} implementations.
	 */
	private boolean enabled = true;

	/**
	 * Map of custom resource attributes (e.g. service.version)
	 */
	private Map<String, String> attributes = Collections.emptyMap();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

}
