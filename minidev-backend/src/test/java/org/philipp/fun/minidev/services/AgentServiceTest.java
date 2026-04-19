package org.philipp.fun.minidev.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.philipp.fun.minidev.spring.model.AgentRun;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private DataBaseService dataBaseService;

    @InjectMocks
    private AgentService agentService;

    @Test
    void testStartRunSavesToRepository() throws IOException {
        AgentRun run = agentService.startRun();
        
        verify(dataBaseService).addToRepository(run);
        assertNotNull(run);
    }
}
