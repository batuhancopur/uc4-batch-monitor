package com.example.uc4monitor;

import com.example.uc4monitor.config.Uc4AnomalyProperties;
import com.example.uc4monitor.config.Uc4Properties;
import com.example.uc4monitor.config.Uc4ReportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({Uc4Properties.class, Uc4AnomalyProperties.class, Uc4ReportProperties.class})
public class Uc4BatchMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(Uc4BatchMonitorApplication.class, args);
    }
}
