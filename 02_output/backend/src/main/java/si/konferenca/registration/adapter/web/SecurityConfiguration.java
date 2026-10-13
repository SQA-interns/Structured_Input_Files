package si.konferenca.registration.adapter.web;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Access rules, headers and organizer authentication (spec §6, §7; SB-02, SB-03, SB-10). */
@Configuration
public class SecurityConfiguration {

  static final int MIN_PASSWORD_LENGTH = 16;

  @Bean
  public SecurityFilterChain apiSecurity(HttpSecurity http) {
    AuthenticationEntryPoint unauthorized =
        (request, response, e) -> {
          response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
          JsonErrors.write(response, 401, "Authentication required.");
        };
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .httpBasic(b -> b.realmName("organizer").authenticationEntryPoint(unauthorized))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            JsonErrors.write(response, 403, "Access denied.")))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny())
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/registrations/export")
                    .hasRole("ORGANIZER")
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** The one organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  public UserDetailsService organizerAccount(
      @Value("${app.organizer.username}") String username,
      @Value("${app.organizer.password}") String password,
      PasswordEncoder encoder) {
    if (username.isBlank()) {
      throw new IllegalStateException("ORGANIZER_USERNAME must not be empty");
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "ORGANIZER_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("ORGANIZER").build());
  }

  /** CORS only for the origins in CORS_ALLOWED_ORIGINS (local development). */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String allowedOrigins) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::strip)
            .filter(o -> !o.isEmpty())
            .toList();
    if (!origins.isEmpty()) {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(origins);
      config.setAllowedMethods(List.of("GET", "POST"));
      config.setAllowedHeaders(List.of("Content-Type"));
      config.setAllowCredentials(false);
      source.registerCorsConfiguration("/api/**", config);
    }
    return source;
  }
}
