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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.exporter.SpanFilter;
import org.springframework.cloud.sleuth.exporter.SpanReporter;
import org.springframework.cloud.sleuth.otel.bridge.CompositeSpanExporter;
import org.springframework.cloud.sleuth.otel.bridge.SpanExporterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable OpenTelemetry exporters.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SpanExporter.class)
@EnableConfigurationProperties(OtelExporterProperties.class)
class OtelExporterConfiguration {

	@Bean
	@ConditionalOnProperty(value = "spring.sleuth.otel.exporter.sleuth-span-filter.enabled", matchIfMissing = true)
	SpanExporterCustomizer sleuthSpanFilterConverter(ObjectProvider<List<SpanFilter>> spanFilters,
			ObjectProvider<List<SpanReporter>> reporters) {
		return new SpanExporterCustomizer() {
			@Override
			public SpanExporter customize(SpanExporter spanExporter) {
				return new CompositeSpanExporter(spanExporter, spanFilters.getIfAvailable(ArrayList::new),
						reporters.getIfAvailable(ArrayList::new));
			}
		};
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JaegerGrpcSpanExporter.class)
	static class JaegerExporterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JaegerGrpcSpanExporter otelJaegerGrpcSpanExporter(OtelExporterProperties properties) {
			JaegerGrpcSpanExporterBuilder builder = JaegerGrpcSpanExporter.builder();
			String endpoint = properties.getJaeger().getEndpoint();
			if (StringUtils.hasText(endpoint)) {
				builder.setEndpoint(endpoint);
			}
			Long timeout = properties.getJaeger().getTimeout();
			if (timeout != null) {
				builder.setTimeout(timeout, TimeUnit.MILLISECONDS);
			}
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OtlpGrpcSpanExporter.class)
	static class OtlpExporterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		OtlpGrpcSpanExporter otelOtlpGrpcSpanExporter(OtelExporterProperties properties) {
			OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder();
			String endpoint = properties.getOtlp().getEndpoint();
			if (StringUtils.hasText(endpoint)) {
				builder.setEndpoint(endpoint);
			}
			Long timeout = properties.getOtlp().getTimeout();
			if (timeout != null) {
				builder.setTimeout(timeout, TimeUnit.MILLISECONDS);
			}
			Map<String, String> headers = properties.getOtlp().getHeaders();
			if (!headers.isEmpty()) {
				headers.forEach(builder::addHeader);
			}
			return builder.build();
		}

	}

}
