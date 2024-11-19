package pl.piomin.ch6;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "custom")
public class DemoProperties {

    private String property;
    private String secureProperty;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getSecureProperty() {
        return secureProperty;
    }

    public void setSecureProperty(String secureProperty) {
        this.secureProperty = secureProperty;
    }
}
