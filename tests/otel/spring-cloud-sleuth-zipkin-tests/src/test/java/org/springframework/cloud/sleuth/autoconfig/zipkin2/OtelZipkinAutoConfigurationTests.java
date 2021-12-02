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

import java.util.concurrent.TimeUnit;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import okhttp3.mockwebserver.RecordedRequest;
import org.awaitility.Awaitility;
import zipkin2.reporter.AsyncReporter;

import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.otel.OtelAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.otel.zipkin2.ZipkinOtelAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

public class OtelZipkinAutoConfigurationTests
		extends org.springframework.cloud.sleuth.autoconfig.zipkin2.ZipkinAutoConfigurationTests {

	@Override
	void defaultsToV2Endpoint() throws Exception {
		zipkinRunner().withPropertyValues("spring.zipkin.base-url=" + this.server.url("/").toString()).run(context -> {
			context.getBean(Tracer.class).nextSpan().name("foo").tag("foo", "bar").start().end();

			context.getBean(ZipkinAutoConfiguration.REPORTER_BEAN_NAME, AsyncReporter.class).flush();
			Awaitility.await().atMost(5, TimeUnit.SECONDS)
					.untilAsserted(() -> then(this.server.getRequestCount()).isGreaterThan(1));

			Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
				RecordedRequest request = this.server.takeRequest(1, TimeUnit.SECONDS);
				then(request).isNotNull();
				then(request.getPath()).isEqualTo("/api/v2/spans");
				then(request.getBody().readUtf8()).contains("localEndpoint");
			});
		});
	}

	@Override
	public void encoderDirectsEndpoint() throws Exception {
		zipkinRunner().withPropertyValues("spring.zipkin.base-url=" + this.server.url("/").toString(),
				"spring.zipkin.encoder=JSON_V1").run(context -> {
					context.getBean(Tracer.class).nextSpan().name("foo").tag("foo", "bar").start().end();

					Awaitility.await().atMost(5, TimeUnit.SECONDS)
							.untilAsserted(() -> then(this.server.getRequestCount()).isGreaterThan(0));

					Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
						RecordedRequest request = this.server.takeRequest(1, TimeUnit.SECONDS);
						then(request).isNotNull();
						then(request.getPath()).isEqualTo("/api/v1/spans");
						then(request.getBody().readUtf8()).contains("binaryAnnotations");
					});
				});
	}

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

	@Configuration(proxyBeanMethods = false)
	protected static class Config {

		@Bean
		Sampler sampler() {
			return Sampler.alwaysOn();
		}

	}

}
