/*
 * Copyright 2021-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Detects if both OTel (this project) and Brave are on the classpath.
 *
 * @author Jonatan Ivanov
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "brave.Tracer")
@ConditionalOnProperty(
		value = { "spring.sleuth.enabled", "spring.sleuth.otel.multiple-tracer-configs-detector-enabled" },
		matchIfMissing = true)
class MultipleTracerAutoConfigurationsDetector {

	@PostConstruct
	void signalMultipleTracerAutoConfigurationsPresent() {
		throw new MultipleTracersFoundException();
	}

	static final class MultipleTracersFoundException extends RuntimeException {

		MultipleTracersFoundException() {
			super("You have both OpenTelemetry (OTel) and Brave (probably from Spring Cloud Sleuth Brave) tracers on the "
					+ "classpath. Please exclude <org.springframework.cloud:spring-cloud-sleuth-brave> dependency from "
					+ "<org.springframework.cloud:spring-cloud-starter-sleuth>. If the issue persist, please check the "
					+ "classpath, something brings in Brave that you need to exclude");
		}

	}

}
