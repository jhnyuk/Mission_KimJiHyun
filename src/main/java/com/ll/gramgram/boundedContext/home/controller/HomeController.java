package com.ll.gramgram.boundedContext.home.controller;

import com.ll.gramgram.base.rq.Rq;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final Rq rq;

    @GetMapping("/")
    public String showMain() {
        if (rq.isLogout()) return "redirect:/usr/member/login";

        return "redirect:/usr/member/me";
    }

    @GetMapping("/usr/home/about")
    public String showAbout() {
        return "usr/home/about";
    }

    @GetMapping("/usr/debugSession")
    @ResponseBody
    public String showDebugSession(HttpSession session) {
        StringBuilder sb = new StringBuilder("Session content:\n");

        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            sb.append(String.format("%s: %s\n", attributeName, attributeValue));
        }

        return sb.toString().replaceAll("\n", "<br>");
    }

    @GetMapping("/usr/historyBackTest")
    @PreAuthorize("hasAuthority('admin')")
    public String showHistoryBackTest(HttpSession session) {
        return rq.historyBack("관리자 권한이 없습니다.");
    }
}