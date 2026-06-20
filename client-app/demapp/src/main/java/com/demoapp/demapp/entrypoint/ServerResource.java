package com.demoapp.demapp.entrypoint;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class ServerResource {

    @Value("${instance-id}")
    private String instanceId;

    private int count;

    @GetMapping("/request")
    public String getApi()
    {
        count++;
        log.info("Request catered by this server.");
        return String.format("Request catered by get method, instance-id: %s", instanceId);
    }
    @PostMapping("/request")
    public String postApi(@RequestBody Body body)
    {
        log.info("Post method Body is {}", body);
        return String.format("Request catered by post method with body %s,  instance-id: %s", body, instanceId);
    }

    @GetMapping("/health")
    public void health()
    {

    }

    @GetMapping("/slow")
    public String slowApi(@RequestParam(defaultValue = "30000") long delay) throws InterruptedException
    {
        log.info("Slow request started, holding for {}ms, instance-id: {}", delay, instanceId);
        Thread.sleep(delay);
        log.info("Slow request finished after {}ms, instance-id: {}", delay, instanceId);
        return String.format("Slow request completed after %dms, instance-id: %s", delay, instanceId);
    }

    @GetMapping("/some-delay")
    public String someDelay(@RequestParam(defaultValue = "5000") long delay) throws InterruptedException
    {
        log.info("Slow request started, holding for {}ms, instance-id: {}", delay, instanceId);
        Thread.sleep(delay);
        log.info("Slow request finished after {}ms, instance-id: {}", delay, instanceId);
        return String.format("Slow request completed after %dms, instance-id: %s", delay, instanceId);
    }

}