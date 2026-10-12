package si.konferenca.registration.security;

import java.nio.charset.StandardCharsets;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.config.AppProperties;

/**
 * Access rules (SB-02, BR-08): options and registration are public, the export needs the organizer
 * role via HTTP Basic; everything else is denied. Stateless, security headers (SB-10).
 */
@Configuration
public class SecurityConfiguration {

  static final String ORGANIZER_ROLE = "ORGANIZER";
  private static final String UNAUTHORIZED_BODY =
      "{\"message\":\"Organizer credentials are required.\"}";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .securityContext(context -> context.requireExplicitSave(true))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/export")
                    .hasRole(ORGANIZER_ROLE)
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized()))
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized()))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .formLogin(form -> form.disable())
        .logout(logout -> logout.disable());
    String origin = properties.cors().allowedOrigin();
    if (origin != null && !origin.isBlank()) {
      http.cors(cors -> cors.configurationSource(corsSource(origin)));
    } else {
      http.cors(cors -> cors.disable());
    }
    return http.build();
  }

  /** The organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  UserDetailsService organizerAccount(AppProperties properties, PasswordEncoder encoder) {
    AppProperties.Organizer organizer = properties.organizer();
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER_ROLE)
            .build());
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private static AuthenticationEntryPoint unauthorized() {
    return (request, response, exception) -> {
      response.setStatus(401);
      response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\", charset=\"UTF-8\"");
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write(UNAUTHORIZED_BODY);
    };
  }

  private static UrlBasedCorsConfigurationSource corsSource(String origin) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(origin.trim()));
    configuration.setAllowedMethods(List.of("GET", "POST"));
    configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
