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

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OtelExporterConfigurationTests {

	@Test
	void should_pick_jaeger_exporter_when_present_on_the_classpath() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withClassLoader(new FilteredClassLoader("io.opentelemetry.exporter.otlp"))
				.withUserConfiguration(OtelExporterConfiguration.class);

		contextRunner.run(context -> BDDAssertions.then(context).hasSingleBean(JaegerGrpcSpanExporter.class));
	}

	@Test
	void should_pick_otlp_exporter_when_present_on_the_classpath() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withClassLoader(new FilteredClassLoader("io.opentelemetry.exporter.jaeger"))
				.withUserConfiguration(OtelExporterConfiguration.class);

		contextRunner.run(context -> BDDAssertions.then(context).hasSingleBean(OtlpGrpcSpanExporter.class));
	}

	@Test
	void should_pick_multiple_exporters_when_present_on_the_classpath() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(OtelExporterConfiguration.class);

		contextRunner.run(context -> BDDAssertions.then(context).hasSingleBean(OtlpGrpcSpanExporter.class)
				.hasSingleBean(JaegerGrpcSpanExporter.class));
	}

}
