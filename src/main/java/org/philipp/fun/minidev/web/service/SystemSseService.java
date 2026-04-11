package org.philipp.fun.minidev.web.service;

import org.springframework.stereotype.Service;

@Service
public class SystemSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "SYSTEM";
    }
}
