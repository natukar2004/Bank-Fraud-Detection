package com.securebank.controller;

import com.securebank.model.User;
import com.securebank.service.BankService;
import com.securebank.store.DataStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "transfer") String tab,
                             HttpSession session, Model model) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) return "redirect:/";

        Optional<User> userOpt = bankService.findUser(userEmail);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/";
        }
        User user = userOpt.get();

        model.addAttribute("welcomeName", user.getName());
        model.addAttribute("balance", String.format("%,d", user.getBalance()));
        model.addAttribute("tab", tab);
        model.addAttribute("deviceOptions", DataStore.DEVICE_OPTIONS);
        model.addAttribute("locationOptions", DataStore.LOCATION_OPTIONS);

        if (!model.containsAttribute("transferResult")) model.addAttribute("transferResult", "");
        if (!model.containsAttribute("transferOk")) model.addAttribute("transferOk", true);

        if ("history".equals(tab)) {
            model.addAttribute("history", bankService.getHistory(userEmail));
        } else if ("alerts".equals(tab)) {
            model.addAttribute("alerts", bankService.getAlerts(userEmail));
        }

        return "bank";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam String receiver, @RequestParam long amount,
                            @RequestParam String device, @RequestParam String location,
                            HttpSession session, RedirectAttributes redirect) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) return "redirect:/";

        BankService.ActionResult result = bankService.transfer(userEmail, receiver, amount, device, location);
        redirect.addFlashAttribute("transferResult", result.message());
        redirect.addFlashAttribute("transferOk", result.success());
        return "redirect:/bank?tab=transfer";
    }
}
