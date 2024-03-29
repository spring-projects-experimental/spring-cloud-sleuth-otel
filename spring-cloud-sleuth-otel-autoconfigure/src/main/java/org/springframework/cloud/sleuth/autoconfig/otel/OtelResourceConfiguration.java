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

import java.util.function.Supplier;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.extension.resources.OsResource;
import io.opentelemetry.sdk.extension.resources.ProcessResource;
import io.opentelemetry.sdk.extension.resources.ProcessRuntimeResource;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable OpenTelemetry exporters.
 *
 * @author Knut Schleßelmann
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtelResourceProperties.class)
@ConditionalOnProperty(value = "spring.sleuth.otel.resource.enabled", matchIfMissing = true)
class OtelResourceConfiguration {

	@Bean
	@Conditional(OtelResourceAttributesCondition.class)
	Supplier<Resource> customResourceProvider(OtelResourceProperties properties) {
		AttributesBuilder attributesBuilder = Attributes.builder();
		properties.getAttributes().forEach(attributesBuilder::put);

		return () -> Resource.create(attributesBuilder.build());
	}

	@ConditionalOnClass(ProcessResource.class)
	static class OtelProcessResourceConfiguration {

		@Bean
		Supplier<Resource> otelOsResourceProvider() {
			return OsResource::get;
		}

		@Bean
		Supplier<Resource> otelProcessResourceProvider() {
			return ProcessResource::get;
		}

		@Bean
		Supplier<Resource> otelProcessRuntimeResourceProvider() {
			return ProcessRuntimeResource::get;
		}

	}

	static class OtelResourceAttributesCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			OtelResourceProperties config = Binder.get(context.getEnvironment())
					.bind("spring.sleuth.otel.resource", OtelResourceProperties.class).orElse(null);

			return config != null && !config.getAttributes().isEmpty();
		}

	}

}
