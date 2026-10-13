package com.example.springai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工具调用（Tool / Function Calling）示例。
 *
 * 学习要点：
 * - @Tool 把普通 Java 方法描述为模型可调用的工具，模型拿到的是 JSON Schema；
 * - 调用闭环由框架自动完成：模型发起 tool_call -> 框架反射执行 -> 结果回传模型 -> 模型继续生成；
 * - Spring AI 中工具对象通过 .tools(...) 传给 ChatClient（对比 LangChain4j：工具对象传给 AiServices.builder）。
 */
@Component
public class AgentTools {

    private static final Logger log = LoggerFactory.getLogger(AgentTools.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = "查询当前日期和时间，当用户询问时间或日期时使用")
    public String getCurrentTime() {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(FMT);
        log.info("[Tool 被调用] getCurrentTime -> {}", now);
        return now;
    }

    @Tool(description = "对两个数做加减乘除运算。a、b 为操作数，operator 取 + - * / 之一")
    public String calculate(
            @ToolParam(description = "第一个操作数") double a,
            @ToolParam(description = "第二个操作数") double b,
            @ToolParam(description = "运算符，只能是 + - * / 之一") String operator) {
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
