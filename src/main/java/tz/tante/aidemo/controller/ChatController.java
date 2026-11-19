package tz.tante.aidemo.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat/property")
public class ChatController
{
  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
  private final ChatClient chatClient;

  public ChatController(ChatClient chatClient)
  {
    this.chatClient = chatClient;
  }

  @Value("classpath:/promptTemplates/systemPromptTemplate.st")
  Resource systemPromptTemplate;

  @GetMapping("/find")
  public String findProperty(@RequestParam String message, @RequestParam String customerName)
  {
    try
    {
      String response = chatClient.prompt()
        .system(systemPromptTemplate)
        .user(message)
        .call()
        .content();

      return response;
    }
    catch (Exception e)
    {
      logger.error("Chat model error for message '{}': {}", message, e.getMessage());
      return "Sorry, I was unable to complete that request. Please try again.";
    }
  }
}
