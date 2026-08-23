package com.mindq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {

    private static final List<String> STATIC_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
    );

    private static final int DEFAULT_PORT = 5173;

    @Value("#{'${app.cors.allowed-origins:}'.split(',')}")
    private List<String> configuredOrigins;

    /**
     * Detect all non-loopback IPv4 addresses on this machine.
     * This allows CORS from any LAN IP without hardcoding.
     */
    private List<String> detectMachineIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to detect machine IPs: {}", e.getMessage());
        }
        return ips;
    }

    /**
     * Build the full list of allowed origins:
     * 1. Explicitly configured origins (from env/app config)
     * 2. Static defaults (localhost variants)
     * 3. All machine LAN IPs with port 5173
     */
    private List<String> buildOrigins() {
        List<String> origins = new ArrayList<>();

        // 1. Explicitly configured origins
        List<String> explicit = configuredOrigins.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        origins.addAll(explicit);

        // 2. Static defaults
        origins.addAll(STATIC_ORIGINS);

        // 3. Dynamic machine IPs
        for (String ip : detectMachineIps()) {
            origins.add("http://" + ip + ":" + DEFAULT_PORT);
        }

        // Deduplicate
        List<String> unique = origins.stream().distinct().toList();
        log.info("CORS allowed origins ({}): {}", unique.size(), unique);
        return unique;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        List<String> origins = buildOrigins();
        String[] originArray = origins.toArray(new String[0]);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(originArray)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "X-Request-Id")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = buildOrigins();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
