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

}