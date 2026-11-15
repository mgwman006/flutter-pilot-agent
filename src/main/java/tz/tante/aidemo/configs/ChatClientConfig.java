package tz.tante.aidemo.configs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import tz.tante.aidemo.tools.TimeTools;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class ChatClientConfig
{
  @Bean
  public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
    ToolCallbackProvider toolCallbackProvider)
  {
    ToolCallback[] raw = toolCallbackProvider.getToolCallbacks();
    ToolCallback[] fixed = Arrays.stream(raw).map(TypeFixingCallback::new).toArray(ToolCallback[]::new);
    return chatClientBuilder
      .defaultTools(ToolCallbackProvider.from(fixed))
      .build();
  }

  private static final class TypeFixingCallback implements ToolCallback
  {
    private static final Logger log = LoggerFactory.getLogger(TypeFixingCallback.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ToolCallback delegate;

    TypeFixingCallback(ToolCallback delegate)
    {
      this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition()
    {
      return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata()
    {
      return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput)
    {
      return callWithGuard(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext)
    {
      return callWithGuard(toolInput, toolContext);
    }

    private String callWithGuard(String toolInput, ToolContext toolContext)
    {
      String name = delegate.getToolDefinition().name();
      String fixedInput = fixTypes(name, toolInput);

      if (isBlockedCall(name, fixedInput))
      {
        return "Tool outcome: UNEXPECTED_FAILURE — blocked invalid call to " + name;
      }

      String result;
      try
      {
        result = toolContext != null ? delegate.call(fixedInput, toolContext) : delegate.call(fixedInput);
      }
      catch (Exception e)
      {
        log.error("MCP tool '{}' threw exception: {}", name, e.getMessage());
        return "Tool outcome: UNEXPECTED_FAILURE — " + e.getMessage();
      }

      return classifyResult(name, result);
    }

    private boolean isBlockedCall(String name, String input)
    {
      if ("issue_read".equals(name))
      {
        try
        {
          JsonNode node = mapper.readTree(input);
          JsonNode issueNum = node.get("issue_number");
          if (issueNum != null && issueNum.isTextual())
          {
            String val = issueNum.asText();
            try { Integer.parseInt(val); } catch (NumberFormatException e) { return true; }
          }
        }
        catch (Exception ignored) {}
      }
      if (input != null && input.contains("EISDIR"))
      {
        return true;
      }
      return false;
    }

    private String classifyResult(String toolName, String result)
    {
      if (result == null || result.isBlank())
      {
        return "Tool outcome: UNEXPECTED_FAILURE — empty result from " + toolName;
      }
      String lower = result.toLowerCase(Locale.ROOT);
      if (lower.contains("name already exists on this account"))
      {
        return "Tool outcome: TERMINAL_SUCCESS_EQUIVALENT — repository already exists";
      }
      if (lower.contains("you need admin access") || lower.contains("403") || lower.contains("token"))
      {
        return "Tool outcome: TERMINAL_USER_ACTION_NEEDED — " + result;
      }
      if (lower.contains("eisdir") || lower.contains("enoent") || lower.contains("illegal operation on a directory")
        || lower.contains("orgs/null") || lower.contains("404 not found") || lower.contains("failed to create"))
      {
        return "Tool outcome: UNEXPECTED_FAILURE — " + result;
      }
      return result;
    }

    private String fixTypes(String toolName, String toolInput)
    {
      if (toolInput == null || toolInput.isBlank())
      {
        return toolInput;
      }
      try
      {
        JsonNode raw = mapper.readTree(toolInput);
        if (!raw.isObject())
        {
          return toolInput;
        }
        ObjectNode node = (ObjectNode) raw;
        fixBooleans(node, "minimal_output", "dryRun", "private", "is_private", "auto_init", "autoInit");
        fixIntegers(node, "issue_number", "pull_number", "page", "per_page", "perPage", "head", "tail");
        fixArrays(node, "fields", "excludePatterns");
        fixEdits(node);
        stripNullStrings(node, "org", "owner", "team_id");
        return mapper.writeValueAsString(node);
      }
      catch (Exception e)
      {
        log.warn("Type-fixing failed for tool '{}', using original input: {}", toolName, e.getMessage());
        return toolInput;
      }
    }

    private void stripNullStrings(ObjectNode node, String... fields)
    {
      for (String field : fields)
      {
        JsonNode value = node.get(field);
        if (value != null && (value.isNull() || (value.isTextual() && (value.asText().equalsIgnoreCase("null") || value.asText().isBlank()))))
        {
          node.remove(field);
        }
      }
    }

    private void fixBooleans(ObjectNode node, String... fields)
    {
      for (String field : fields)
      {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual())
        {
          node.put(field, Boolean.parseBoolean(value.asText()));
        }
      }
    }

    private void fixIntegers(ObjectNode node, String... fields)
    {
      for (String field : fields)
      {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual())
        {
          try { node.put(field, Integer.parseInt(value.asText())); } catch (NumberFormatException ignored) {}
        }
      }
    }

    private void fixArrays(ObjectNode node, String... fields)
    {
      for (String field : fields)
      {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual())
        {
          ArrayNode arr = mapper.createArrayNode();
          arr.add(value.asText());
          node.set(field, arr);
        }
      }
    }

    private void fixEdits(ObjectNode node)
    {
      JsonNode edits = node.get("edits");
      if (edits != null && edits.isTextual())
      {
        try
        {
          JsonNode parsed = mapper.readTree(edits.asText());
          node.set("edits", parsed);
        }
        catch (Exception ignored) {}
      }
    }
  }
}

