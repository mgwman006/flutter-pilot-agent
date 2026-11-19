package tz.tante.aidemo.models;

public record PropertyManagerApiResponse<T>(
  boolean success,
  String message,
  T data,
  int statusCode
)
{
}
