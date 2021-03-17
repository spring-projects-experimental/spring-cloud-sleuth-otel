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

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(OutputCaptureExtension.class)
class OtelMultipleTracersAnalyzerTests {

	@Test
	void should_not_print_warning_when_brave_not_on_classpath(CapturedOutput capturedOutput) {
		new OtelMultipleTracersAnalyzer() {
			@Override
			boolean isBraveOnClasspath() {
				return false;
			}
		}.verify();

		then(capturedOutput).doesNotContain("You have both Spring Cloud Sleuth OpenTelemetry");
	}

	@Test
	void should_print_warning_when_brave_not_on_classpath(CapturedOutput capturedOutput) {
		new OtelMultipleTracersAnalyzer() {
			@Override
			boolean isBraveOnClasspath() {
				return true;
			}
		}.verify();

		then(capturedOutput).contains("You have both Spring Cloud Sleuth OpenTelemetry");
	}
}