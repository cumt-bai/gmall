package com.cumt.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.UserInfo;
import com.cumt.gmall.passport.util.JwtUtil;
import com.cumt.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${token.key}")
    private String key;

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){

        // 获取跳转登陆页面前的URL并存入域中，方便登陆后直接跳转回去
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);

        return "index";
    }


    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo , HttpServletRequest request){

        // 获取的是访问当前登陆页的IP地址
        String salt = request.getHeader("X-forwarded-for");

        UserInfo info = userInfoService.login(userInfo);

        if (info != null){
            // 制作token
            Map<String,Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());

            String token = JwtUtil.encode(key, map, salt);
            return token;
        } else {
            return "false";
        }

    }

    @RequestMapping("verify") //认证：用户访问每一个控制器的时候都会调用！
    @ResponseBody
    public String verify(HttpServletRequest request){

        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        if(map!=null){
            String userId = (String) map.get("userId");
            UserInfo userInfo = userInfoService.verify(userId);
            if (userInfo != null){
                return "success";
            }
        }

        return "false";
    }
}
