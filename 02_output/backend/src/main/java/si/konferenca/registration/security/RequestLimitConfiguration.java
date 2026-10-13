package si.konferenca.registration.security;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.RegistrationProperties;

/** Rate limit and body-size filters on /api, ahead of Spring Security (SR-03). */
@Configuration
public class RequestLimitConfiguration {

  /** Before the Spring Security filter chain (order -100). */
  static final int RATE_LIMIT_ORDER = -120;

  static final int BODY_LIMIT_ORDER = -110;

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      RegistrationProperties properties, Clock clock) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(new RateLimitFilter(properties.rateLimit(), clock));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(RATE_LIMIT_ORDER);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<BodySizeLimitFilter> bodySizeLimitFilter(
      RegistrationProperties properties) {
    FilterRegistrationBean<BodySizeLimitFilter> registration =
        new FilterRegistrationBean<>(new BodySizeLimitFilter(properties.maxBodyBytes()));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(BODY_LIMIT_ORDER);
    return registration;
  }
}
