package com.demoapp.demapp.entrypoint;


import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class ServerResource {

    private int count;

    @GetMapping("/request")
    public String getApi()
    {
        count++;
        return "The number of requests by this api are " + count;
    }
    @PostMapping("/request")
    public String postApi(@RequestBody Body body)
    {
        count += body.getCount();
        return "The number of requests by this api are " + count;
    }

    @GetMapping("/health")
    public void health()
    {

    }

}