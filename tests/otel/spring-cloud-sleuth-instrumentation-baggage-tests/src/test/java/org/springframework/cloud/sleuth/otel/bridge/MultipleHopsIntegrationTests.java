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

package org.springframework.cloud.sleuth.otel.bridge;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.testing.context.SettableContextStorageProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.baggage.multiple.DemoApplication;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.cloud.sleuth.otel.OtelTestSpanHandler;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = MultipleHopsIntegrationTests.Config.class)
@TestPropertySource(properties = "spring.sleuth.otel.propagation.sleuth-baggage.enabled=true")
public class MultipleHopsIntegrationTests
		extends org.springframework.cloud.sleuth.baggage.multiple.MultipleHopsIntegrationTests {

	@Autowired
	MyBaggageChangedListener myBaggageChangedListener;

	@Autowired
	DemoApplication demoApplication;

	@Autowired
	ApplicationEventPublisher publisher;

	// TODO: Why do we have empty names here
	@Override
	protected void assertSpanNames() {
		then(this.spans).extracting(FinishedSpan::getName).containsAll(asList("HTTP GET", "handle", "send"));
	}

	@BeforeEach
	void setUp() {
		// manually set a context storage that doesn't use the standard default wrappers,
		// so we can test that baggage changes get emitted as events properly.
		SettableContextStorageProvider
				.setContextStorage(new EventPublishingContextWrapper(publisher).apply(ContextStorage.defaultStorage()));
	}

	@Override
	protected void assertBaggage(Span initialSpan) {
		then(this.myBaggageChangedListener.baggageChanged).as("All have request ID")
				.filteredOn(b -> b.getBaggage() != null)
				.filteredOn(b -> b.getBaggage().getEntryValue(REQUEST_ID) != null).isNotEmpty()
				.allMatch(event -> "f4308d05-2228-4468-80f6-92a8377ba193"
						.equals(event.getBaggage().getEntryValue(REQUEST_ID)));
		then(this.demoApplication.getBaggageValue()).isEqualTo("FO");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration(
			exclude = { MongoAutoConfiguration.class, QuartzAutoConfiguration.class, JmxAutoConfiguration.class })
	static class Config {

		@Bean
		OtelTestSpanHandler testSpanHandlerSupplier() {
			return new OtelTestSpanHandler(new ArrayListSpanProcessor());
		}

		@Bean
		Sampler alwaysSampler() {
			return Sampler.alwaysOn();
		}

		@Bean
		MyBaggageChangedListener myBaggageChangedListener() {
			return new MyBaggageChangedListener();
		}

	}

	static class MyBaggageChangedListener implements ApplicationListener<ApplicationEvent> {

		Queue<EventPublishingContextWrapper.ScopeAttachedEvent> baggageChanged = new LinkedBlockingQueue<>();

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof EventPublishingContextWrapper.ScopeAttachedEvent) {
				this.baggageChanged.add((EventPublishingContextWrapper.ScopeAttachedEvent) event);
			}
		}

	}

}
