package com.aaax.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPortalController {

    @GetMapping({"/admin", "/admin/"})
    public String admin() {
        return "forward:/admin/index.html";
    }

    @GetMapping({"/sign-in", "/sign-in/"})
    public String signIn() {
        return "forward:/sign-in/index.html";
    }

    @GetMapping({"/sign-up", "/sign-up/"})
    public String signUp() {
        return "forward:/sign-up/index.html";
    }

    @GetMapping({"/user", "/user/"})
    public String user() {
        return "forward:/user/index.html";
    }
}
