package com.securebank.controller;

import com.securebank.service.BankService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BankService bankService;

    public AdminController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/";
        }
        model.addAttribute("pending", bankService.getPending());
        model.addAttribute("resolved", bankService.getResolved());
        if (!model.containsAttribute("adminActionStatus")) model.addAttribute("adminActionStatus", "");
        return "admin";
    }

    @PostMapping("/decide")
    public String decide(@RequestParam(required = false) Integer txnId,
                          @RequestParam String decision,
                          HttpSession session, RedirectAttributes redirect) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/";
        }
        BankService.ActionResult result = bankService.decide(txnId, "approve".equals(decision));
        redirect.addFlashAttribute("adminActionStatus", result.message());
        return "redirect:/admin";
    }
}
