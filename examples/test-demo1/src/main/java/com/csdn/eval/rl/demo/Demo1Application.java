package com.csdn.eval.rl.demo;

import com.csdn.eval.rl.core.EnableEvalRateLimiter;
import com.csdn.eval.rl.core.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableEvalRateLimiter
@RestController
@RequestMapping("/demo1")
public class Demo1Application {

    @Autowired
    private RateLimiter rateLimiter;

    public static void main(String[] args) {
        SpringApplication.run(Demo1Application.class, args);
    }

    @GetMapping("/test")
    public String test(@RequestParam(value = "count", defaultValue = "105") int count) {
        for (int i = 0; i < count; i++) {
            doTest(i);
        }
        return "hello";
    }

    private void doTest(int count) {
        System.out.println("当前请求次数：" + count + "，节点限流剩余次数：" + rateLimiter.getRemainingRequests());
        if (!rateLimiter.tryAcquire()) {
            // 5秒后再发送
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            doTest(count);
        }
    }

}