package com.securebank.controller;

import com.securebank.service.BankService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final BankService bankService;

    public AuthController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin";
        }
        if (session.getAttribute("userEmail") != null) {
            return "redirect:/bank";
        }
        if (!model.containsAttribute("loginStatus")) model.addAttribute("loginStatus", "");
        if (!model.containsAttribute("regStatus")) model.addAttribute("regStatus", "");
        if (!model.containsAttribute("adminStatus")) model.addAttribute("adminStatus", "");
        return "auth";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email,
                            @RequestParam String password, RedirectAttributes redirect) {
        BankService.ActionResult result = bankService.register(name, email, password);
        redirect.addFlashAttribute("regStatus", result.message());
        return "redirect:/";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                         HttpSession session, RedirectAttributes redirect) {
        BankService.ActionResult result = bankService.login(email, password);
        if (result.success()) {
            session.setAttribute("userEmail", email);
            session.setAttribute("isAdmin", false);
            return "redirect:/bank";
        }
        redirect.addFlashAttribute("loginStatus", result.message());
        return "redirect:/";
    }

    @PostMapping("/admin-login")
    public String adminLogin(@RequestParam String username, @RequestParam String password,
                              HttpSession session, RedirectAttributes redirect) {
        if (bankService.adminLogin(username, password)) {
            session.setAttribute("isAdmin", true);
            session.removeAttribute("userEmail");
            return "redirect:/admin";
        }
        redirect.addFlashAttribute("adminStatus", "Invalid admin credentials.");
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
