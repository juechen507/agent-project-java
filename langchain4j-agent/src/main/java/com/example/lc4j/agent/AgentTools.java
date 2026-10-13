package com.example.lc4j.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工具调用（Tool Calling）示例 —— LangChain4j 版。
 *
 * 学习要点（对比 Spring AI 的 @Tool）：
 * - dev.langchain4j.agent.tool.Tool 注解方法，value() 即给模型看的工具描述；
 * - @P 注解参数并给出说明，框架据此生成 JSON Schema；
 * - 工具对象在装配 AiServices 时通过 .tools(...) 注册，模型自主决定调用；
 * - 若某工具方法需要当前会话 id，可声明一个 @ToolMemoryId Object 参数，框架自动注入。
 */
@Component
public class AgentTools {

    private static final Logger log = LoggerFactory.getLogger(AgentTools.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool("查询当前日期和时间，当用户询问时间或日期时使用")
    public String getCurrentTime() {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(FMT);
        log.info("[Tool 被调用] getCurrentTime -> {}", now);
        return now;
    }

    @Tool("对两个数做加减乘除运算。operator 取 + - * / 之一")
    public String calculate(
            @P("第一个操作数") double a,
            @P("第二个操作数") double b,
            @P("运算符，只能是 + - * / 之一") String operator) {
        double r = switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b == 0 ? Double.NaN : a / b;
            default -> throw new IllegalArgumentException("不支持的运算符: " + operator);
        };
        String result = "%s %s %s = %s".formatted(a, operator, b, r);
        log.info("[Tool 被调用] calculate -> {}", result);
        return result;
    }
}
