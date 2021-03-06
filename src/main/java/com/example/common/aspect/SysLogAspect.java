/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.example.common.aspect;

import com.example.common.annotation.SysLog;
import com.example.shiro.common.utils.HttpContextUtils;
import com.example.shiro.common.utils.IPUtils;
import com.example.shiro.sys.entity.SysUserEntity;
import com.example.shiro.web.entity.WebUserEntity;
import com.example.web.entity.SysLogEntity;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * 系统日志，切面处理类
 *
 * @author Mark sunlightcs@gmail.com
 */
@Aspect
@Component
@Slf4j
public class SysLogAspect {

    @Pointcut("@annotation(com.example.common.annotation.SysLog)")
    public void logPointCut() {

    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        //执行方法
        Object result = point.proceed();
        //执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;

        //保存日志
        saveSysLog(point, time);

        return result;
    }

    private void saveSysLog(ProceedingJoinPoint joinPoint, long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        SysLogEntity sysLog = new SysLogEntity();
        SysLog syslog = method.getAnnotation(SysLog.class);
        if (syslog != null) {
            //注解上的描述
            sysLog.setOperation(syslog.value());
            sysLog.setType(syslog.type());
        }

        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLog.setMethod(className + "." + methodName + "()");

        //请求的参数
        Object[] args = joinPoint.getArgs();
        try {
            String params = new Gson().toJson(args);
            sysLog.setParams(params);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //获取request
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        //设置IP地址
        sysLog.setIp(IPUtils.getIpAddr(request));

        //用户名
        Object obj = SecurityUtils.getSubject().getPrincipal();
        if (obj != null) {
            String className1 = obj.getClass().getName();
            String username = null;
            if (className1.indexOf("SysUserEntity") > 0) {
                username = ((SysUserEntity) obj).getUsername();
            } else {
                username = ((WebUserEntity) obj).getUsername();
            }
            sysLog.setUsername(username);
        }

        sysLog.setTime(time);
        sysLog.setCreateDate(new Date());

        //保存系统日志
        log.info(new StringBuilder().append("\n \t\ttype:").append(sysLog.getType())
                .append("\toperation:").append(sysLog.getOperation())
                .append("\tuserName:").append(sysLog.getUsername())
                .append("\n \t\tclassName:").append(className)
                .append("\tmethod:").append(methodName)
                .append("\n \t\tparams:").append(sysLog.getParams())
                .toString());
    }
}
