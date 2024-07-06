package DACNPM.asset_management.controller;

import DACNPM.asset_management.model.Account;
import DACNPM.asset_management.model.DetailAccount;
import DACNPM.asset_management.service.DetailAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    DetailAccountService detailAccountService;

    @GetMapping("/userManagement")
    public String userManagement(@SessionAttribute(name = "loggedInAccount", required = false)Account loggedInAccount, Model model, @Param("keyword") String keyword, HttpSession session) {
        if (loggedInAccount == null) {
            return "redirect:/login";
        }
        List<DetailAccount> listUser= detailAccountService.getAllUser(keyword);
        model.addAttribute("listUser", listUser);
        model.addAttribute("loggedInAccount", loggedInAccount);
        model.addAttribute("keyword", keyword);



        return "user-management"; // Trả về trang index.html

    }
    @ResponseBody
    @GetMapping("/listUser")
    public List<DetailAccount> getAll(@Param("keyword") String keyword){
        return detailAccountService.getAllUser(keyword);
    }
    @PostMapping("/updateUser/{id}")
    public String updateUser(@PathVariable("id") int id, @ModelAttribute("detailAccount") DetailAccount detailAccount) throws Exception {
        detailAccountService.updateUser(id, detailAccount);
        return "redirect:/userManagement";
    }



    @PostMapping("/deleteUser/{id}")
    public String deleteUser(@PathVariable("id")String id){
        detailAccountService.deleteUser(Integer.parseInt(id));
        return "redirect:/userManagement";
    }
}