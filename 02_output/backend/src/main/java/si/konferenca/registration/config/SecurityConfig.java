package si.konferenca.registration.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.api.ErrorWriter;
import si.konferenca.registration.api.FixedWindowRateLimiter;
import si.konferenca.registration.api.OrganizerTransportGuard;
import si.konferenca.registration.api.RequestLimitFilter;

/**
 * Access rules (SB-02, SB-03, SR-06): public form endpoints and health, organizer-only export with
 * HTTP Basic, everything else denied; security headers on every response (SB-10).
 */
@Configuration
public class SecurityConfig {

  static final int MIN_PASSWORD_LENGTH = 16;
  static final String ORGANIZER_ROLE = "ORGANIZER";

  @Bean
  SecurityFilterChain api(HttpSecurity http, AppProperties props) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(
            basic ->
                basic.authenticationEntryPoint(
                    (request, response, e) -> {
                      response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
                      ErrorWriter.write(
                          response,
                          HttpServletResponse.SC_UNAUTHORIZED,
                          "Authentication required.");
                    }))
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                    (request, response, e) ->
                        ErrorWriter.write(
                            response, HttpServletResponse.SC_FORBIDDEN, "Access denied.")))
        .cors(c -> c.configurationSource(cors(props.cors().allowedOrigin())))
        .addFilterBefore(requestLimits(props), BasicAuthenticationFilter.class)
        .addFilterBefore(
            new OrganizerTransportGuard(props.organizer().httpsOnly()),
            BasicAuthenticationFilter.class)
        .headers(
            h ->
                h.contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny())
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/export")
                    .hasRole(ORGANIZER_ROLE)
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  private static RequestLimitFilter requestLimits(AppProperties props) {
    AppProperties.RateLimit r = props.rateLimit();
    return new RequestLimitFilter(
        new FixedWindowRateLimiter(Clock.systemUTC()),
        new RequestLimitFilter.Limits(
            r.registrationPerMinute(),
            r.formConfigPerMinute(),
            r.exportPerMinute(),
            props.maxRequestBytes()));
  }

  /** CORS only when an origin is configured (local only, checked at startup). */
  private static UrlBasedCorsConfigurationSource cors(String origin) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (origin != null && !origin.isBlank()) {
      CorsConfiguration c = new CorsConfiguration();
      c.setAllowedOrigins(List.of(origin.strip()));
      c.setAllowedMethods(List.of("GET", "POST"));
      c.setAllowedHeaders(List.of("Content-Type", "Authorization"));
      source.registerCorsConfiguration("/api/**", c);
    }
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** The single organizer; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  UserDetailsService organizer(AppProperties props, PasswordEncoder encoder) {
    String username = props.organizer().username();
    String password = props.organizer().password();
    if (username == null || username.isBlank()) {
      throw new IllegalStateException("ORGANIZER_USERNAME must be set");
    }
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "ORGANIZER_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(username.strip())
            .password(encoder.encode(password))
            .roles(ORGANIZER_ROLE)
            .build());
  }
}
