package tz.tante.ai.models;

public record PropertyManagerApiResponse<T>(
  boolean success,
  String message,
  T data,
  int statusCode
)
{
}
