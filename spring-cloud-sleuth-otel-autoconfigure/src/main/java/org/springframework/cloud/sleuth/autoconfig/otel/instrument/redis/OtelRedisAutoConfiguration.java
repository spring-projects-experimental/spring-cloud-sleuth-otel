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

package org.springframework.cloud.sleuth.autoconfig.otel.instrument.redis;

import java.net.SocketAddress;

import brave.Tracing;
import brave.sampler.Sampler;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.BraveTracing;
import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.TracerProvider;
import io.lettuce.core.tracing.Tracing;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.brave.instrument.redis.TraceRedisProperties;
import org.springframework.cloud.sleuth.autoconfig.instrument.redis.TraceRedisAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.otel.OtelAutoConfiguration;
import org.springframework.cloud.sleuth.brave.instrument.redis.ClientResourcesBuilderCustomizer;
import org.springframework.cloud.sleuth.brave.instrument.redis.TraceLettuceClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables Redis span information propagation.
 *
 * @author Chao Chang
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.sleuth.redis.enabled", matchIfMissing = true)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(OtelAutoConfiguration.class)
@AutoConfigureBefore({ RedisAutoConfiguration.class, TraceRedisAutoConfiguration.class })
@EnableConfigurationProperties(TraceRedisProperties.class)
@ConditionalOnClass(Tracing.class)
public class OtelRedisAutoConfiguration {

	@Bean
	Tracing lettuceBraveTracing(Tracer tracer, TraceRedisProperties traceRedisProperties) {
		return new Tracing() {
			@Override
			public TracerProvider getTracerProvider() {
				return new TracerProvider() {
					@Override
					public io.lettuce.core.tracing.Tracer getTracer() {
						return new io.lettuce.core.tracing.Tracer() {
							@Override
							public Span nextSpan() {
								org.springframework.cloud.sleuth.Span nextSpan = tracer.nextSpan();
								return new Span() {
									@Override
									public Span start(RedisCommand<?, ?, ?> redisCommand) {
										return null;
									}

									@Override
									public Span name(String s) {
										return null;
									}

									@Override
									public Span annotate(String s) {
										return null;
									}

									@Override
									public Span tag(String s, String s1) {
										return null;
									}

									@Override
									public Span error(Throwable throwable) {
										return null;
									}

									@Override
									public Span remoteEndpoint(Endpoint endpoint) {
										return null;
									}

									@Override
									public void finish() {

									}
								};
							}

							@Override
							public Span nextSpan(TraceContext traceContext) {
								return null;
							}
						};
					}
				};
			}

			@Override
			public TraceContextProvider initialTraceContextProvider() {
				return null;
			}

			@Override
			public boolean isEnabled() {
				return false;
			}

			@Override
			public boolean includeCommandArgsInSpanTags() {
				return false;
			}

			@Override
			public Endpoint createEndpoint(SocketAddress socketAddress) {
				return null;
			}
		};
	}

}
