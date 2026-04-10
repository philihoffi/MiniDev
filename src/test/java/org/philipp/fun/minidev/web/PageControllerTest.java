package org.philipp.fun.minidev.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageControllerTest {

    @Test
    void indexPageUsesExpectedTemplateAndModel() {
        PageController controller = new PageController("MiniDev");
        Model model = new ConcurrentModel();

        String viewName = controller.index(model);

        assertEquals("index", viewName);
        assertEquals("MiniDev", model.getAttribute("appName"));
        assertEquals("Initial View", model.getAttribute("viewTitle"));
        assertEquals("Tiny autonomous game developer", model.getAttribute("tagline"));
        assertTrue(new ClassPathResource("templates/index.html").exists());
        assertTrue(new ClassPathResource("static/css/main.css").exists());
    }

}
