/*
 * Copyright 2013-2023 the original author or authors.
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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.internal.SleuthContextListener;
import org.springframework.cloud.sleuth.otel.bridge.OtelTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OtelAutoConfiguration}.
 */
class OtelAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OtelAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasSingleBean(SpanProcessorProvider.class);
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).hasSingleBean(SpanLimits.class);
			assertThat(context).hasSingleBean(OtelTracer.class);
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasSingleBean(SleuthContextListener.class);
		});
	}

	@Test
	void samplerIsParentBased() {
		this.contextRunner.run((context) -> {
			Sampler sampler = context.getBean(Sampler.class);
			assertThat(sampler).isNotNull();
			assertThat(sampler.getDescription()).startsWith("ParentBased{");
		});
	}

	@Test
	void shouldNotSupplyBeansIfDependencyIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.api")).run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetry.class);
			assertThat(context).doesNotHaveBean(SdkTracerProvider.class);
			assertThat(context).doesNotHaveBean(SpanProcessorProvider.class);
			assertThat(context).doesNotHaveBean(Resource.class);
			assertThat(context).doesNotHaveBean(SpanLimits.class);
			assertThat(context).doesNotHaveBean(OtelTracer.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
			assertThat(context).doesNotHaveBean(SleuthContextListener.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customOpenTelemetry");
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasBean("customSdkTracerProvider");
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasBean("customSpanProcessorProvider");
			assertThat(context).hasSingleBean(SpanProcessorProvider.class);
			assertThat(context).hasBean("customResource");
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).hasBean("customSpanLimits");
			assertThat(context).hasSingleBean(SpanLimits.class);
			assertThat(context).hasBean("customOtelTracer");
			assertThat(context).hasSingleBean(OtelTracer.class);
			assertThat(context).hasBean("customSampler");
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasBean("customSleuthContextListener");
			assertThat(context).hasSingleBean(SleuthContextListener.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		OpenTelemetry customOpenTelemetry() {
			return mock(OpenTelemetry.class);
		}

		@Bean
		SdkTracerProvider customSdkTracerProvider() {
			return SdkTracerProvider.builder().build();
		}

		@Bean
		SpanProcessorProvider customSpanProcessorProvider() {
			return mock(SpanProcessorProvider.class);
		}

		@Bean
		Resource customResource() {
			return mock(Resource.class);
		}

		@Bean
		SpanLimits customSpanLimits() {
			return mock(SpanLimits.class);
		}

		@Bean
		OtelTracer customOtelTracer() {
			return mock(OtelTracer.class);
		}

		@Bean
		Sampler customSampler() {
			return mock(Sampler.class);
		}

		@Bean
		SleuthContextListener customSleuthContextListener() {
			return mock(SleuthContextListener.class);
		}

	}

}
