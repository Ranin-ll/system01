package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RongguApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RongguApplication.class, args);
        System.out.println("\n"
                + "  ██████╗  ██████╗ ███╗   ██╗ ██████╗  ██████╗ ██╗   ██╗\n"
                + "  ██╔══██╗██╔═══██╗████╗  ██║██╔════╝ ██╔════╝ ██║   ██║\n"
                + "  ██████╔╝██║   ██║██╔██╗ ██║██║  ███╗██║  ███╗██║   ██║\n"
                + "  ██╔══██╗██║   ██║██║╚██╗██║██║   ██║██║   ██║██║   ██║\n"
                + "  ██║  ██║╚██████╔╝██║ ╚████║╚██████╔╝╚██████╔╝╚██████╔╝\n"
                + "  ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝  ╚═════╝  ╚═════╝ \n"
                + "  融谷实习生学习考核系统 启动成功");
    }
}
