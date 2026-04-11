package org.philipp.fun.minidev.web;

import org.springframework.stereotype.Service;

@Service
public class TerminalSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "TERMINAL";
    }
}
