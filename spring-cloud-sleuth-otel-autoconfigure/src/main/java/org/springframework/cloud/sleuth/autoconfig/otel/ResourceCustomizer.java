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

import io.opentelemetry.sdk.resources.Resource;

/**
 * Allows to customize the {@link Resource}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@FunctionalInterface
public interface ResourceCustomizer {

	/**
	 * Customizes the resource or returns itself if there's nothing to customize.
	 * @param resource - resource to customize
	 * @return resource to be merged with the customized resource
	 */
	Resource customize(Resource resource);

}
