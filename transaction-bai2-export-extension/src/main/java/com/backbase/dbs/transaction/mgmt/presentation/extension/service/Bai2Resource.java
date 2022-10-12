package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.AbstractResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.resource.HttpResource;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class Bai2Resource extends AbstractResource implements HttpResource {
    private final String bai2file;

    @Override
    public String getDescription() {
        return "BAI2 Transaction Output File";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(bai2file.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/x-bai2");
        headers.add("Content-Encoding", StandardCharsets.UTF_8.toString());
        return headers;
    }
    
}