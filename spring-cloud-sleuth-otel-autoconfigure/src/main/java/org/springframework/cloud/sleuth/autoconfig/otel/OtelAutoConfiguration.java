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
import java.util.stream.Collectors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.autoconfig.SleuthAnnotationConfiguration;
import org.springframework.cloud.sleuth.autoconfig.SleuthBaggageProperties;
import org.springframework.cloud.sleuth.autoconfig.SleuthSpanFilterProperties;
import org.springframework.cloud.sleuth.autoconfig.SleuthTracerProperties;
import org.springframework.cloud.sleuth.autoconfig.TraceConfiguration;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.cloud.sleuth.internal.SleuthContextListener;
import org.springframework.cloud.sleuth.otel.bridge.SpanExporterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable tracing via Spring Cloud Sleuth and OpenTelemetry SDK.
 *
 * @author Marcin Grzejszczak
 * @author John Watson
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ io.opentelemetry.api.trace.Tracer.class, OtelProperties.class })
@ConditionalOnOtelEnabled
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(org.springframework.cloud.sleuth.Tracer.class)
@EnableConfigurationProperties({ OtelProperties.class, SleuthSpanFilterProperties.class, SleuthBaggageProperties.class,
		SleuthTracerProperties.class })
@Import({ OtelBridgeConfiguration.class, OtelPropagationConfiguration.class, TraceConfiguration.class,
		SleuthAnnotationConfiguration.class, OtelResourceConfiguration.class })
@AutoConfigureBefore(BraveAutoConfiguration.class)
public class OtelAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetry otel(SdkTracerProvider tracerProvider, ContextPropagators contextPropagators) {
		return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).setPropagators(contextPropagators).build();
	}

	@Bean
	@ConditionalOnMissingBean
	SdkTracerProvider otelTracerProvider(SpanLimits spanLimits, ObjectProvider<List<SpanProcessor>> spanProcessors,
			SpanExporterCustomizer spanExporterCustomizer, ObjectProvider<List<SpanExporter>> spanExporters,
			Sampler sampler, Resource resource) {
		SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder().setResource(resource)
				.setSampler(sampler).setSpanLimits(spanLimits);
		List<SpanProcessor> processors = spanProcessors.getIfAvailable(ArrayList::new);
		processors.addAll(spanExporters.getIfAvailable(ArrayList::new).stream()
				.map(e -> SimpleSpanProcessor.create(spanExporterCustomizer.customize(e)))
				.collect(Collectors.toList()));
		processors.forEach(sdkTracerProviderBuilder::addSpanProcessor);
		return sdkTracerProviderBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	Resource otelResource(Environment env, ObjectProvider<List<ResourceProvider>> resourceProviders) {
		String applicationName = env.getProperty("spring.application.name");
		Resource resource = defaultResource(applicationName);
		List<ResourceProvider> resourceCustomizers = resourceProviders.getIfAvailable(ArrayList::new);
		for (ResourceProvider provider : resourceCustomizers) {
			Resource providedResource = provider.create();
			resource = resource.merge(providedResource);
		}
		return resource;
	}

	private Resource defaultResource(String applicationName) {
		if (applicationName == null) {
			return Resource.getDefault();
		}
		return Resource.getDefault()
				.merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
	}

	@Bean
	@ConditionalOnMissingBean
	SpanLimits otelSpanLimits(OtelProperties otelProperties) {
		return SpanLimits.getDefault().toBuilder().setMaxLengthOfAttributeValues(otelProperties.getMaxAttrLength())
				.setMaxNumberOfAttributes(otelProperties.getMaxAttrs())
				.setMaxNumberOfAttributesPerEvent(otelProperties.getMaxEventAttrs())
				.setMaxNumberOfAttributesPerLink(otelProperties.getMaxLinkAttrs())
				.setMaxNumberOfEvents(otelProperties.getMaxEvents()).setMaxNumberOfLinks(otelProperties.getMaxLinks())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	Tracer otelTracer(TracerProvider tracerProvider, OtelProperties otelProperties) {
		return tracerProvider.get(otelProperties.getInstrumentationName());
	}

	@Bean
	@ConditionalOnMissingBean
	Sampler otelSampler(OtelProperties otelProperties) {
		return Sampler.traceIdRatioBased(otelProperties.getTraceIdRatioBased());
	}

	@Bean
	SleuthContextListener sleuthContextListener() {
		return new SleuthContextListener();
	}

}
