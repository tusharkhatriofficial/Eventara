package com.eventara.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API, non-static-resource requests to index.html
 * so that React Router can handle client-side routing.
 *
 * Without this, navigating directly to /settings or /rules in the browser
 * would return a Spring Boot 404 instead of loading the SPA.
 */
@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/",
            "/{path:^(?!api|ws|swagger|v3|webjars|static|assets|favicon\\.ico).*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
