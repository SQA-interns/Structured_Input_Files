package si.konferenca.registration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the {@code app.*} settings. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class PropertiesConfiguration {}
