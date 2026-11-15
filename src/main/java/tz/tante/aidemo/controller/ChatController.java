package tz.tante.aidemo.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

@RestController
@RequestMapping("/v1/api")
public class ChatController
{
  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Pattern repositoriesForUserPattern = Pattern.compile(
    "(?:repos|repositories|github\\s+repos|github\\s+repositories).{0,40}?(?:for\\s+(?:user|owner)|user|owner)\\s+([A-Za-z0-9_.-]+)",
    Pattern.CASE_INSENSITIVE
  );

  private final ChatClient chatClient;
  private final ToolCallbackProvider toolCallbackProvider;

  public ChatController(ChatClient chatClient, ToolCallbackProvider toolCallbackProvider)
  {
    this.chatClient = chatClient;
    this.toolCallbackProvider = toolCallbackProvider;
  }

  @Value("classpath:/promptTemplates/userPromptTemplate.st")
  Resource userPromptTemplate;

  @Value("classpath:/promptTemplates/systemPromptTemplate.st")
  Resource systemPromptTemplate;

  @GetMapping("/chat")
  public String chat(@RequestParam String message, @RequestParam String customerName)
  {
    String directRepositoryListing = handleRepositoryListingRequest(message);
    if (directRepositoryListing != null)
    {
      return directRepositoryListing;
    }

    try
    {
      String response = chatClient.prompt()
        .system(systemPromptTemplate)
        .user(message)
        .options(OpenAiChatOptions.builder().parallelToolCalls(false))
        .call()
        .content();
      if (looksLikeRawToolCall(response))
      {
        logger.warn("Raw tool call returned to client for '{}': {}", message, response);
        return fallbackWithoutTools(message);
      }
      return response;
    }
    catch (Exception e)
    {
      if (isUnexpectedToolFailure(e))
      {
        logger.warn("Tool failure for '{}', falling back to no-tool response: {}", message, e.getMessage());
        return fallbackWithoutTools(message);
      }
      logger.error("Chat model error for message '{}': {}", message, e.getMessage());
      return "Sorry, I was unable to complete that request. Please try again.";
    }
  }

  private String handleRepositoryListingRequest(String message)
  {
    String owner = extractRepositoryOwner(message);
    if (owner == null)
    {
      return null;
    }

    ToolCallback callback = findRepositoryListingToolCallback();
    if (callback == null)
    {
      return "I couldn't list repositories because the GitHub repository tool is unavailable.";
    }

    try
    {
      return formatRepositoryList(owner, callback.call("{\"query\":\"user:" + owner + "\"}"));
    }
    catch (Exception e)
    {
      logger.error("Direct repository listing failed for '{}': {} — {}", owner, e.getClass().getSimpleName(), e.getMessage(), e);
      return "I couldn't list repositories for " + owner + ": " + e.getMessage();
    }
  }

  private ToolCallback findRepositoryListingToolCallback()
  {
    ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
    for (ToolCallback callback : callbacks)
    {
      if (callback.getToolDefinition().name().equals("search_repositories"))
      {
        return callback;
      }
    }

    for (ToolCallback callback : callbacks)
    {
      String normalizedName = normalizeToolName(callback.getToolDefinition().name());
      if (normalizedName.contains("searchrepositories") || normalizedName.contains("listrepositor"))
      {
        logger.info("Using repository listing tool callback '{}'", callback.getToolDefinition().name());
        return callback;
      }
    }

    logger.warn("Available tool callbacks: {}", Arrays.stream(callbacks)
      .map(tool -> tool.getToolDefinition().name())
      .toList());
    return null;
  }

  private String formatRepositoryList(String owner, String result)
  {
    if (result == null || result.isBlank())
    {
      return "No results were returned for " + owner + ".";
    }

    Set<String> names = extractRepositoryNames(result);
    if (!names.isEmpty())
    {
      return String.join("\n", names);
    }

    if (result.contains("No results were returned"))
    {
      return "No results were returned for " + owner + ".";
    }
    return normalizeWhitespace(result);
  }

  private Set<String> extractRepositoryNames(String result)
  {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    try
    {
      JsonNode root = mapper.readTree(result);
      // MCP wraps results in [{text: "...json..."}] — unwrap first
      if (root.isArray() && root.size() > 0 && root.get(0).has("text"))
      {
        root = mapper.readTree(root.get(0).get("text").asText());
      }
      // search_repositories returns {total_count, items:[...]}
      JsonNode items = root.get("items");
      collectRepositoryNames(items != null ? items : root, names);
    }
    catch (Exception ignored)
    {
      Matcher fullNameMatcher = Pattern.compile("\"full_name\"\\s*:\\s*\"([^\"]+)\"").matcher(result);
      while (fullNameMatcher.find())
      {
        names.add(fullNameMatcher.group(1));
      }
    }
    return names;
  }

  private void collectRepositoryNames(JsonNode node, Set<String> names)
  {
    if (node == null) return;
    if (node.isArray())
    {
      for (JsonNode item : node) collectRepositoryNames(item, names);
      return;
    }
    if (node.isObject())
    {
      JsonNode fullName = node.get("full_name");
      if (fullName != null && fullName.isTextual()) names.add(fullName.asText());
    }
  }

  private String extractRepositoryOwner(String message)
  {
    String normalized = message.toLowerCase(Locale.ROOT);
    if (!normalized.contains("repo"))
    {
      return null;
    }

    Matcher matcher = repositoriesForUserPattern.matcher(message);
    if (matcher.find())
    {
      return matcher.group(1);
    }
    return null;
  }

  private String fallbackWithoutTools(String originalMessage)
  {
    try
    {
      return chatClient.prompt()
        .system(systemPromptTemplate)
        .user("Answer in plain text without calling any tools. Explain that a tool call failed or returned an " +
          "unexpected response, summarize the problem clearly, and stop. Ask for explicit missing details only if " +
          "they are necessary.\n\nOriginal request: " + originalMessage)
        .options(OpenAiChatOptions.builder().toolChoice("none").parallelToolCalls(false))
        .call()
        .content();
    }
    catch (Exception e)
    {
      logger.error("Fallback failed for '{}': {}", originalMessage, e.getMessage());
      return "The model could not complete this request. Please rephrase with explicit details, for example: " +
        "owner=mgwman006, repo name, branch name, etc.";
    }
  }

  private boolean isPegNativeFormatError(Exception exception)
  {
    String message = exception.getMessage();
    return message != null && message.contains("expected peg-native format");
  }

  private boolean isUnexpectedToolFailure(Exception exception)
  {
    String message = exception.getMessage();
    if (message == null)
    {
      return false;
    }
    return isPegNativeFormatError(exception)
      || message.contains("Error calling tool")
      || message.contains("Input validation error")
      || message.contains("missing required parameter")
      || message.contains("parameter ")
      || message.contains("tool call");
  }

  private boolean looksLikeRawToolCall(String response)
  {
    if (response == null)
    {
      return false;
    }
    String trimmed = response.trim();
    return trimmed.startsWith("{")
      && trimmed.contains("\"name\"")
      && trimmed.contains("\"parameters\"");
  }

  private String normalizeWhitespace(String value)
  {
    return value.replaceAll("\\s+", " ").trim();
  }

  private String normalizeToolName(String value)
  {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }
}
