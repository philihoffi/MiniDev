package org.philipp.fun.minidev.web;

import org.springframework.stereotype.Service;

@Service
public class SystemSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "SYSTEM";
    }
}
