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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.ClassUtils;

/**
 * Prints a warning when multiple {@link Tracer} implementations are present on the
 * classpath.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class OtelMultipleTracersAnalyzer {

	private static final Log log = LogFactory.getLog(OtelMultipleTracersAnalyzer.class);

	@PostConstruct
	void verify() {
		if (isBraveOnClasspath()) {
			log.warn(
					"You have both Spring Cloud Sleuth OpenTelemetry (OTel) and Spring Cloud Sleuth Brave tracers on the classpath. We will pick OTel over Brave however please consider excluding <org.springframework.cloud:spring-cloud-sleuth-brave> dependency from your <org.springframework.cloud:spring-cloud-starter-sleuth> dependency.");
		}
	}

	boolean isBraveOnClasspath() {
		return ClassUtils.isPresent("org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration", null);
	}

	static final class MultipleTracersFoundException extends RuntimeException {

	}

}
