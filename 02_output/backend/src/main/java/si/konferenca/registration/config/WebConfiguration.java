package si.konferenca.registration.config;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import si.konferenca.registration.application.FormSettings;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.web.OrganizerHttpsFilter;
import si.konferenca.registration.web.RateLimitFilter;
import si.konferenca.registration.web.RequestSizeFilter;

/** Web settings and the filters that run before Spring Security (S10). */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration {

  @Bean
  FormSettings formSettings(AppProperties properties, OptionCatalog catalog) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    String siteKey = recaptcha.testMode() || recaptcha.siteKey() == null ? "" : recaptcha.siteKey();
    return new FormSettings(
        properties.conferenceName(), catalog.consent(), recaptcha.testMode(), siteKey);
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(AppProperties properties, Clock clock) {
    AppProperties.RateLimit limits = properties.rateLimit();
    RateLimitFilter filter =
        new RateLimitFilter(
            new RateLimitFilter.Limits(
                limits.registrationsPerMinute(),
                limits.readsPerMinute(),
                limits.organizerPerMinute()),
            clock);
    return register(filter, Ordered.HIGHEST_PRECEDENCE + 10);
  }

  @Bean
  FilterRegistrationBean<RequestSizeFilter> requestSizeFilter(AppProperties properties) {
    return register(
        new RequestSizeFilter(properties.maxRequestBytes()), Ordered.HIGHEST_PRECEDENCE + 20);
  }

  @Bean
  FilterRegistrationBean<OrganizerHttpsFilter> organizerHttpsFilter(AppProperties properties) {
    return register(
        new OrganizerHttpsFilter(properties.organizer().requireHttps()),
        Ordered.HIGHEST_PRECEDENCE + 30);
  }

  private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> register(
      T filter, int order) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(order);
    registration.addUrlPatterns("/api/*");
    return registration;
  }
}
