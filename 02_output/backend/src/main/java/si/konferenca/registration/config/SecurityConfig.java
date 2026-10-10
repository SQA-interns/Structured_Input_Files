package si.konferenca.registration.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

/** Organizer-only export, public form API, headers, filters (spec section 8). */
@Configuration
public class SecurityConfig {

  static final String ORGANIZER_ROLE = "ORGANIZER";
  private static final String REALM = "organizer";

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /** The single organizer; the password is kept only as a BCrypt hash (SB-03). */
  @Bean
  public UserDetailsService organizers(
      AppProperties properties, PasswordEncoder encoder, StartupGuard guard) {
    AppProperties.Organizer organizer = properties.organizer();
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER_ROLE)
            .build());
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties)
      throws Exception {
    JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(REALM);
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny())
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/export/**")
                    .hasRole(ORGANIZER_ROLE)
                    .requestMatchers("/api/**", "/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(b -> b.realmName(REALM).authenticationEntryPoint(entryPoint))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(new JsonAccessDeniedHandler()));
    for (jakarta.servlet.Filter filter : RequestFilters.create(properties)) {
      http.addFilterBefore(filter, BasicAuthenticationFilter.class);
    }
    String origin = properties.cors().allowedOrigin();
    if (origin != null && !origin.isBlank()) {
      CorsConfiguration cors = new CorsConfiguration();
      cors.setAllowedOrigins(List.of(origin));
      cors.setAllowedMethods(List.of("GET", "POST"));
      cors.setAllowedHeaders(List.of("Content-Type"));
      cors.setAllowCredentials(false);
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/api/**", cors);
      http.cors(c -> c.configurationSource(source));
    }
    return http.build();
  }
}
