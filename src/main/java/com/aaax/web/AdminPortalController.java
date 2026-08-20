package com.aaax.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPortalController {

    @GetMapping({"/admin", "/admin/"})
    public String admin() {
        return "forward:/admin/index.html";
    }
}
