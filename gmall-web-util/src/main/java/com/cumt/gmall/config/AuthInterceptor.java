package com.cumt.gmall.config;

import com.alibaba.fastjson.JSON;
import com.cumt.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

// 拦截器  可以继承也可以实现  implements HandlerInterceptor
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    //将 token 放入cookie 中，登录之后，显示用户昵称！
    /**
     * 调用Handler之前执行，称为前置方法  顺序执行
     * 返回值：true表示放行，后续业务逻辑继续执行
     * false表示被拦截，后续业务逻辑不再执行，但之前返回true的拦截器的完成方法会倒叙执行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //当用户点击登陆以后
        String token = request.getParameter("newToken");
        if(token != null){
            //将token放入cookie中
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }

        //当用户登陆以后访问其他业务模块的时候,访问不到newToken时,但是token已经存在cookie中
        if (token == null){
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        //当token中有数据时获取nickName
        if (token != null){
            // 获取token中的数据
            Map map = getUserMapByToken(token);

            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName",nickName);
        }



        //获取用户访问的控制器上是否有注解，并且判断其属性值
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取方法上的注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        if(methodAnnotation != null){ //表示有注解
            //获取salt
            String salt = request.getHeader("X-forwarded-for");
            System.out.println("salt= " + salt);
            // http://passport.cumt.com/verify
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){ //认证成功，处于登陆状态

                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId",userId);

                return true;
            } else { //认证失败，并且访问需要登陆的页面需跳转登陆

                if(methodAnnotation.autoRedirect()){

                    String requestURL  = request.getRequestURL().toString();
                    //System.out.println("requestURL----" + requestURL);
                    String encodeURL = URLEncoder.encode(requestURL, "utf-8"); // 编码
                    //重定向到登陆页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+ "?originUrl=" + encodeURL);
                    return false;
                }

            }

        }

        return true;
    }

    private Map getUserMapByToken(String token) {

        String tokenUserInfo  = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes  = base64UrlCodec.decode(tokenUserInfo);

        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;

    }

    /**
     * 调用Handler之后执行，称为后置方法  倒序执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    /**
     * afterCompletion视图渲染完成之后执行  倒序执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
