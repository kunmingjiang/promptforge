package com.chengrongjieyue.assistant.audit;

import com.chengrongjieyue.assistant.entity.AuditLog;
import com.chengrongjieyue.assistant.mapper.AuditLogMapper;
import com.chengrongjieyue.assistant.security.LoginUser;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 审计切面：方法成功返回后落一条审计记录。
 * 只记录动作与对象类别，不记录任何请求参数，避免敏感内容进入日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @AfterReturning("@annotation(com.chengrongjieyue.assistant.audit.Audit)")
    public void record(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Audit audit = signature.getMethod().getAnnotation(Audit.class);
            LoginUser user = SecurityUtils.currentOrNull();
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(user == null ? null : user.getId());
            auditLog.setAction(audit.action());
            auditLog.setTarget(audit.target().isEmpty() ? null : audit.target());
            auditLog.setIp(SecurityUtils.clientIp());
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 审计失败不影响业务主流程，但必须留痕便于排查
            log.error("审计日志写入失败: {}", e.toString());
        }
    }
}
