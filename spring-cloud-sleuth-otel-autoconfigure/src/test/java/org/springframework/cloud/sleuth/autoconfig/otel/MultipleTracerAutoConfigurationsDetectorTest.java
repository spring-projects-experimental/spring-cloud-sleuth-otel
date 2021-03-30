/*
 * Copyright 2021-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MultipleTracerAutoConfigurationsDetectorTest {

	@Test
	void should_fail_when_brave_and_otel_autoconfig_is_on_the_classpath() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MultipleTracerAutoConfigurationsDetector.class))
				.run(context -> assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(
						MultipleTracerAutoConfigurationsDetector.MultipleTracersFoundException.class));
	}

	@Test
	void should_not_fail_when_brave_autoconfig_is_not_on_the_classpath() {
		new ApplicationContextRunner()
				.withClassLoader(new FilteredClassLoader(
						"org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration"))
				.withConfiguration(AutoConfigurations.of(MultipleTracerAutoConfigurationsDetector.class))
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void should_not_fail_when_sleuth_is_disabled() {
		new ApplicationContextRunner().withPropertyValues("spring.sleuth.enabled=false")
				.withConfiguration(AutoConfigurations.of(MultipleTracerAutoConfigurationsDetector.class))
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void should_not_fail_when_detector_is_disabled() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.sleuth.otel.multiple-tracer-configs-detector-enabled=false")
				.withConfiguration(AutoConfigurations.of(MultipleTracerAutoConfigurationsDetector.class))
				.run(context -> assertThat(context).hasNotFailed());
	}

}
