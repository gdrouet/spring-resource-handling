package org.springframework.samples.resources;

import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.samples.resources.groovy.GroovyHelperViewResolver;
import org.springframework.samples.resources.handlebars.ProfileHelper;
import org.springframework.samples.resources.handlebars.ResourceUrlHelper;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.*;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;

import java.util.HashMap;
import java.util.Map;


@EnableWebMvc
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private Environment env;

	@Value("${resources.projectroot:}")
	private String projectRoot;

	@Value("${app.version:}")
	private String appVersion;


	private String getProjectRootRequired() {
		Assert.state(this.projectRoot != null, "Please set \"resources.projectRoot\" in application.yml");
		return this.projectRoot;
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("index");
		registry.addViewController("/groovy").setViewName("hello");
		registry.addViewController("/app").setViewName("app");
		registry.addViewController("/less").setViewName("less");
	}

	@Bean
	public HandlebarsViewResolver handlebarsViewResolver(ResourceUrlProvider urlProvider) {
		HandlebarsViewResolver resolver = new HandlebarsViewResolver();
		resolver.setPrefix("classpath:/handlebars/");
		resolver.registerHelper("src", new ResourceUrlHelper(urlProvider));
		resolver.registerHelper(ProfileHelper.NAME, new ProfileHelper(this.env.getActiveProfiles()));
		resolver.setCache(!this.env.acceptsProfiles("development"));
		return resolver;
	}

	@Bean
	public GroovyHelperViewResolver groovyViewResolver(ResourceUrlProvider urlProvider) {
		GroovyHelperViewResolver resolver = new GroovyHelperViewResolver();
		resolver.setSuffix(".tpl");
		resolver.setOrder(1);
		resolver.addTemplateHelper("linkTo", s -> urlProvider.getForLookupPath((String) s));
		return resolver;
	}

	@Bean
	public GroovyMarkupConfigurer groovyTemplateConfigurer() {

		GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
		configurer.setResourceLoaderPath("classpath:groovy/");
		configurer.setAutoIndent(true);
		configurer.setAutoNewLine(true);
		configurer.setCacheTemplates(false);
		return configurer;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		AppCacheResourceTransformer appCacheTransformer = new AppCacheResourceTransformer();

		if (this.env.acceptsProfiles("development")) {
			String location = "file:///" + getProjectRootRequired() + "/client/src/";

			registry.addResourceHandler("/**")
					.addResourceLocations(location)
					.setCachePeriod(0)
					.enableDevMode()
					.addVersion("dev", "/**/*.js")
					.addVersionHash("/**")
					.addTransformer(appCacheTransformer);
		}
		else {
			String location = "classpath:static/";

			registry.addResourceHandler("/**")
					.addResourceLocations(location)
					.addVersion(this.appVersion, "/**/*.js")
					.addVersionHash("/**")
					.addTransformer(appCacheTransformer);
		}
	}

}
