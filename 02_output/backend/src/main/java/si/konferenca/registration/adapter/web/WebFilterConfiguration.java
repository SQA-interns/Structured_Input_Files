package si.konferenca.registration.adapter.web;

import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet filters that run before Spring Security (order -100), in the order of spec §6 and §7:
 * rate limit, body size, organizer HTTPS-only.
 */
@Configuration
public class WebFilterConfiguration {

  static final int RATE_LIMIT_ORDER = -300;
  static final int BODY_LIMIT_ORDER = -250;
  static final int HTTPS_ONLY_ORDER = -200;

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      @Value("${app.rate-limit.form-config-per-minute}") int formConfig,
      @Value("${app.rate-limit.registrations-per-minute}") int registrations,
      @Value("${app.rate-limit.export-per-minute}") int export,
      Clock clock) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(
            new RateLimitFilter(
                Map.of(
                    RateLimitFilter.Group.FORM_CONFIG, formConfig,
                    RateLimitFilter.Group.REGISTRATIONS, registrations,
                    RateLimitFilter.Group.EXPORT, export),
                clock));
    registration.setOrder(RATE_LIMIT_ORDER);
    registration.addUrlPatterns("/api/*");
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilter(
      @Value("${app.request.max-body-bytes}") long maxBodyBytes) {
    FilterRegistrationBean<RequestBodyLimitFilter> registration =
        new FilterRegistrationBean<>(new RequestBodyLimitFilter(maxBodyBytes));
    registration.setOrder(BODY_LIMIT_ORDER);
    registration.addUrlPatterns("/api/*");
    return registration;
  }

  @Bean
  public FilterRegistrationBean<OrganizerHttpsOnlyFilter> organizerHttpsOnlyFilter(
      @Value("${app.organizer.https-only}") boolean httpsOnly) {
    FilterRegistrationBean<OrganizerHttpsOnlyFilter> registration =
        new FilterRegistrationBean<>(new OrganizerHttpsOnlyFilter(httpsOnly));
    registration.setOrder(HTTPS_ONLY_ORDER);
    registration.addUrlPatterns(OrganizerHttpsOnlyFilter.EXPORT_PATH);
    return registration;
  }
}
