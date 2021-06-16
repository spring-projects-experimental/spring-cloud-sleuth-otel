/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.sleuth.otel.autoconfig.actuate;

import java.util.stream.Collectors;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.otel.OtelTestSpanHandler;
import org.springframework.cloud.sleuth.otel.bridge.ArrayListSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@ContextConfiguration(classes = TraceSleuthActuatorAutoConfigurationIntegrationTests.Config.class)
public class TraceSleuthActuatorAutoConfigurationIntegrationTests extends
		org.springframework.cloud.sleuth.autoconfig.actuate.TraceSleuthActuatorAutoConfigurationIntegrationTests {

	@AfterEach
	void cleanup() {
		bufferingSpanReporter.drainFinishedSpans();
	}

	@Test
	void tracesOtlpSnapshot() throws Exception {
		tracesSnapshot(protobuf(), otlpBody());
	}

	@Test
	void tracesOtlp() throws Exception {
		traces(protobuf(), otlpBody());
	}

	private ResultMatcher otlpBody() {
		return result -> {
			byte[] contentAsByteArray = result.getResponse().getContentAsByteArray();
			ResourceSpans resourceSpans = ResourceSpans.parseFrom(contentAsByteArray);
			then(resourceSpans.getInstrumentationLibrarySpansCount()).isEqualTo(1);
			then(resourceSpans.getInstrumentationLibrarySpans(0).getSpansList().stream().map(Span::getName)
					.collect(Collectors.toList())).containsExactlyInAnyOrder("first", "second", "third");
		};
	}

	private MediaType protobuf() {
		return MediaType.parseMediaType("application/x-protobuf");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		OtelTestSpanHandler testSpanHandlerSupplier() {
			return new OtelTestSpanHandler(new ArrayListSpanProcessor());
		}

		@Bean
		io.opentelemetry.sdk.trace.samplers.Sampler alwaysSampler() {
			return Sampler.alwaysOn();
		}

	}

}
