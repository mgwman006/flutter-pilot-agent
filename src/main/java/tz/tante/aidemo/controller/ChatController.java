package tz.tante.aidemo.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.tante.aidemo.models.ChatFindResponseData;
import tz.tante.aidemo.models.PropertyManagerApiResponse;
import tz.tante.aidemo.models.propertymanager.UnitDetailsDTO;
import tz.tante.aidemo.service.PropertyManagerService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/v1/chat/property")
public class ChatController
{
  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
  private static final Pattern UNIT_ID_PATTERN = Pattern.compile("(?i)(?:unit\\s*(?:id|#)?\\s*|id\\s+)(\\d+)");
  private final ChatClient chatClient;
  private final PropertyManagerService propertyManagerService;

  public ChatController(ChatClient chatClient, PropertyManagerService propertyManagerService)
  {
    this.chatClient = chatClient;
    this.propertyManagerService = propertyManagerService;
  }

  @Value("classpath:/promptTemplates/systemPromptTemplate.st")
  Resource systemPromptTemplate;

  @GetMapping("/find")
  public ResponseEntity<PropertyManagerApiResponse<ChatFindResponseData>> findProperty(
    @RequestParam String message,
    @RequestParam String customerName,
    @RequestParam Long userAccountId
  )
  {
    Long unitId = extractUnitId(message);
    UnitDetailsDTO unitDetails = null;
    String verifiedUnitContext = "";

    if (unitId != null)
    {
      try
      {
        unitDetails = propertyManagerService.getUnitDetails(unitId);
        verifiedUnitContext = """
          Verified unit data from Tante Property Manager:
          %s
          Use only this verified data for unit facts. Do not add missing numerical values.
          """.formatted(unitDetails);
      }
      catch (IllegalStateException e)
      {
        logger.error("Unit lookup failed for ID {} and message '{}'", unitId, message, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new PropertyManagerApiResponse<>(
            false,
            "Failed to retrieve unit " + unitId + " from the property database.",
            null,
            HttpStatus.SERVICE_UNAVAILABLE.value()
          ));
      }
    }

    try
    {
      String answer = chatClient
        .prompt()
        .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID,userAccountId))
        .system(systemPromptTemplate)
        .user("""
          Customer: %s
          User question: %s
          %s
          """.formatted(customerName, message, verifiedUnitContext))
        .call()
        .content();
      ChatFindResponseData responseData = new ChatFindResponseData(answer, "llm", unitId, unitDetails);
      return ResponseEntity.ok(new PropertyManagerApiResponse<>(true, "Request successful", responseData, HttpStatus.OK.value()));
    }
    catch (RuntimeException e)
    {
      logger.error("Chat model error for message '{}'", message, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new PropertyManagerApiResponse<>(
          false,
          "Sorry, I was unable to complete that request. Please try again.",
          null,
          HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }
  }

  private Long extractUnitId(String message)
  {
    Matcher matcher = UNIT_ID_PATTERN.matcher(message);
    if (!matcher.find())
    {
      return null;
    }
    return Long.parseLong(matcher.group(1));
  }
}
