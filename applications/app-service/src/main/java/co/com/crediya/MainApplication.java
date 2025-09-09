package co.com.crediya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MainApplication {
    public static void main(String[] args) throws IOException {
        Properties envProps = new Properties();
        try (FileInputStream fis = new FileInputStream(".env")) {
            envProps.load(fis);
        }
        envProps.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
        SpringApplication.run(MainApplication.class, args);
    }
}
