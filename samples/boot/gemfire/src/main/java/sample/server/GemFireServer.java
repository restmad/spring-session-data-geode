/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;

/**
 * A Spring Boot application bootstrapping a GemFire Cache Server JVM process.
 *
 * @author John Blum
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see org.apache.geode.cache.Cache
 * @since 1.2.1
 */
// tag::class[]
@SpringBootApplication
@EnableGemFireHttpSession(maxInactiveIntervalInSeconds = 20) // <1>
@SuppressWarnings("unused")
public class GemFireServer {

	static final String DEFAULT_GEMFIRE_LOG_LEVEL = "config";

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(GemFireServer.class);
		springApplication.setWebApplicationType(WebApplicationType.NONE);
		springApplication.run(args);
	}

	@Bean
	static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	Properties gemfireProperties() { // <2>

		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", applicationName());
		//gemfireProperties.setProperty("log-file", "gemfire-server.log");
		gemfireProperties.setProperty("log-level", logLevel());
		gemfireProperties.setProperty("jmx-manager", "true");
		gemfireProperties.setProperty("jmx-manager-start", "true");

		return gemfireProperties;
	}

	String applicationName() {
		return "spring-session-data-gemfire-boot-sample:".concat(getClass().getSimpleName());
	}

	String logLevel() {
		return System.getProperty("spring-session-data-gemfire.log.level", DEFAULT_GEMFIRE_LOG_LEVEL);
	}

	@Bean
	CacheFactoryBean gemfireCache() { // <3>

		CacheFactoryBean gemfireCache = new CacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setProperties(gemfireProperties());

		return gemfireCache;
	}

	@Bean
	CacheServerFactoryBean gemfireCacheServer(GemFireCache gemfireCache,
			@Value("${spring-session-data-gemfire.cache.server.bind-address:localhost}") String bindAddress,
			@Value("${spring-session-data-gemfire.cache.server.hostname-for-clients:localhost}") String hostnameForClients,
			@Value("${spring-session-data-gemfire.cache.server.port:40404}") int port) { // <4>

		CacheServerFactoryBean gemfireCacheServer = new CacheServerFactoryBean();

		gemfireCacheServer.setAutoStartup(true);
		gemfireCacheServer.setBindAddress(bindAddress);
		gemfireCacheServer.setCache((Cache) gemfireCache);
		gemfireCacheServer.setHostNameForClients(hostnameForClients);
		gemfireCacheServer.setMaxTimeBetweenPings(Long.valueOf(TimeUnit.SECONDS.toMillis(60)).intValue());
		gemfireCacheServer.setPort(port);

		return gemfireCacheServer;
	}

	@Bean(name = "Example")
	@Profile("debug")
	ReplicatedRegionFactoryBean<String, Object> exampleRegion(GemFireCache gemfireCache) {

		ReplicatedRegionFactoryBean<String, Object> exampleRegionFactory = new ReplicatedRegionFactoryBean<>();

		exampleRegionFactory.setCache(gemfireCache);
		exampleRegionFactory.setClose(false);

		return exampleRegionFactory;
	}

	@Autowired
	private GemFireCache gemfireCache;

	@Bean
	@Profile("debug")
	CommandLineRunner exampleInitializer() {

		return args -> {

			Region<String, Object> example = this.gemfireCache.getRegion("/Example");

			String key = "time";

			example.put(key, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:ss a")));

			System.err.printf("Example.get(%s) is [%s]%n", key, example.get(key));
		};
	}
}
// end::class[]
