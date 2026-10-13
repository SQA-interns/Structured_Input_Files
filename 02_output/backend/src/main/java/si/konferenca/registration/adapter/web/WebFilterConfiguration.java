package si.konferenca.registration.adapter.web;

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

  static final int HTTPS_ONLY_ORDER = -200;

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
