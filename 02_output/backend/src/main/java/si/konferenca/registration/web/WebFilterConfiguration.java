package si.konferenca.registration.web;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Rate and size limits run before Spring Security, so failed logins count too (SR-03). */
@Configuration
public class WebFilterConfiguration {

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      Clock clock,
      @Value("${app.rate-limit.form-config-per-minute}") int formConfig,
      @Value("${app.rate-limit.registration-per-minute}") int registration,
      @Value("${app.rate-limit.export-per-minute}") int export) {
    FilterRegistrationBean<RateLimitFilter> bean =
        new FilterRegistrationBean<>(new RateLimitFilter(clock, formConfig, registration, export));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }

  @Bean
  FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilter(
      @Value("${app.max-request-bytes}") long maxBytes) {
    FilterRegistrationBean<RequestSizeLimitFilter> bean =
        new FilterRegistrationBean<>(new RequestSizeLimitFilter(maxBytes));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return bean;
  }
}
