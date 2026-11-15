//package tz.tante.aidemo.tools;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//
//@Component
//public class TimeTools
//{
//  private static final Logger logger = LoggerFactory.getLogger(TimeTools.class);
//
//  @Tool(name = "getCurrentTime", description = "Get the current time in the user's local timezone", returnDirect = false)
//  public String getCurrentTime(@ToolParam(description = "The timezone to get the current time for") String timezone)
//  {
//    String now = LocalDateTime.now(ZoneId.of(timezone)).toString();
//    logger.info("Current time in a given timezone: {}", now);
//    return now;
//  }
//
//  @Tool(name = "getCurrentLocalTime", description = "Get the current time in the user's local timezone", returnDirect = true)
//  public String getCurrentLocalTime()
//  {
//    String now = LocalDateTime.now().toString();
//    logger.info("Current time: {}", now);
//    return now;
//  }
//
//}
