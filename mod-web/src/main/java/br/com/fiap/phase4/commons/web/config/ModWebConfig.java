package br.com.fiap.phase4.commons.web.config;

import br.com.fiap.phase4.commons.web.advice.CommonsExceptionAdvice;
import br.com.fiap.phase4.commons.web.interceptor.LogInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(CommonsExceptionAdvice.class)
public class ModWebConfig implements WebMvcConfigurer {

    @Bean
    public LogInterceptor logInterceptor() {
        return new LogInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**");
    }
}
