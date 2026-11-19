package tz.tante.aidemo.configs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig
{
  @Bean
  public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel)
  {
    return ChatClient.builder(openAiChatModel)
      .defaultSystem("""
        You are a helpful assistant that provides information about properties.
        For questions about availability or pricing, use internal tools to get the figures.
        For now you are limited to tanzania context, and you should not provide information about other countries.
        If you don't know the answer to a question, please respond with "I don't know."
        """);

  }

  @Bean
  public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools)
  {
    return chatClientBuilder
      .defaultTools(tools)
      .build();
  }

}
