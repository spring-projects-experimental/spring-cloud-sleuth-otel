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

import java.util.Objects;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OtelResourceConfigurationTest {

	@Test
	void should_have_custom_attributes() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withPropertyValues("spring.sleuth.otel.resource.attributes.foo=bar")
				.withConfiguration(AutoConfigurations.of(OtelAutoConfiguration.class));

		runner.run(context -> assertThat(context).hasNotFailed().hasBean("customResourceProvider")
				.hasSingleBean(Resource.class).getBean(Resource.class).matches(resource -> Objects
						.equals(resource.getAttributes().get(AttributeKey.stringKey("foo")), "bar")));
	}

	@Test
	void should_not_require_custom_resource_attributes_to_start() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(OtelAutoConfiguration.class));

		runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("customResourceProvider"));
	}

}
