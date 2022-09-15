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

package org.springframework.cloud.sleuth.autoconfig.zipkin2;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Disabled;

import org.springframework.cloud.sleuth.autoconfig.otel.OtelAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.otel.zipkin2.ZipkinOtelAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class OtelZipkinAutoConfigurationTests
		extends org.springframework.cloud.sleuth.autoconfig.zipkin2.ZipkinAutoConfigurationTests {

	@Override
	protected Class tracerZipkinConfiguration() {
		return ZipkinOtelAutoConfiguration.class;
	}

	@Override
	protected Class tracerConfiguration() {
		return OtelAutoConfiguration.class;
	}

	@Override
	protected Class configurationClass() {
		return Config.class;
	}

	@Override
	@Disabled
	void should_apply_micrometer_reporter_metrics_when_meter_registry_bean_present() {

	}

	@Configuration(proxyBeanMethods = false)
	protected static class Config {

		@Bean
		Sampler sampler() {
			return Sampler.alwaysOn();
		}

	}

}
