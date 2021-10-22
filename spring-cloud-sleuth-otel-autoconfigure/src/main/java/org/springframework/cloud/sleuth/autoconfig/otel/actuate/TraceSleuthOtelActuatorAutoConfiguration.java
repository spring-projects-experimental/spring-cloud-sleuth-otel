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

package org.springframework.cloud.sleuth.autoconfig.otel.actuate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.actuate.FinishedSpanWriter;
import org.springframework.cloud.sleuth.autoconfig.actuate.TextOutputFormat;
import org.springframework.cloud.sleuth.autoconfig.actuate.TracesScrapeEndpoint;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.otel.ConditionalOnOtelEnabled;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Sleuth OTel actuator endpoint.
 *
 * @author Marcin Grzejszczak
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@ConditionalOnAvailableEndpoint(endpoint = TracesScrapeEndpoint.class)
@AutoConfigureBefore(BraveAutoConfiguration.class)
@ConditionalOnOtelEnabled
@ConditionalOnClass(Producible.class)
public class TraceSleuthOtelActuatorAutoConfiguration {

	@Bean
	@ConditionalOnClass(name = "io.opentelemetry.exporter.zipkin.ZipkinSpanExporter")
	Supplier<FinishedSpanWriter> sleuthZipkinOtelFinishedSpanWriter() {
		return OtelZipkinFinishedSpanWriter::new;
	}

	@Bean
	@ConditionalOnClass(name = "io.opentelemetry.exporter.otlp.internal.traces.ResourceSpansMarshaler")
	Supplier<FinishedSpanWriter> sleuthOtlpOtelFinishedSpanWriter() {
		return OtelOtlpFinishedSpanWriter::new;
	}

	@Bean
	@ConditionalOnMissingBean
	FinishedSpanWriter<Object> compositeFinishedSpanWriter(ObjectProvider<List<Supplier<FinishedSpanWriter>>> writers) {
		return new CompositeFinishedSpanWriter(writers.getIfAvailable(ArrayList::new));
	}

	static class CompositeFinishedSpanWriter implements FinishedSpanWriter<Object> {

		private final List<FinishedSpanWriter> writers;

		CompositeFinishedSpanWriter(List<Supplier<FinishedSpanWriter>> writers) {
			this.writers = writers.stream().map(Supplier::get).collect(Collectors.toList());
		}

		@Override
		public Object write(TextOutputFormat format, List<FinishedSpan> spans) {
			for (FinishedSpanWriter<?> writer : this.writers) {
				Object writtenSpans = writer.write(format, spans);
				if (writtenSpans != null) {
					return writtenSpans;
				}
			}
			return null;
		}

	}

}
