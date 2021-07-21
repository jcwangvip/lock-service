package org.jcm.lock.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * boot启动类
 *
 * @author jiancheng
 */
@SpringBootApplication(scanBasePackages = "org.jcm.lock")
public class BootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }

}
