package com.location.testapp.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.logging.Logger;

@RestController
public class Location {

    Logger logger = Logger.getLogger(getClass().getName());

    @GetMapping("/listHeaders")
    public ResponseEntity<String> listAllHeaders(
            @RequestHeader Map<String, String> headers) {
        headers.forEach((key, value) -> {
            logger.info(String.format("Header '%s' = %s", key, value));
        });

        return new ResponseEntity<String>(
                String.format("Listed %s headers", headers), HttpStatus.OK);
    }
}
