package tz.tante.aidemo.models.propertymanager;

import java.util.List;

public record PropertySummaryDTO(
  Long id,
  String name,
  String code,
  String description,
  Double landSize,
  String landSizeUnit,
  LocationDetailsDTO location,
  String type,
  String developmentStatus,
  List<Object> images
)
{
}
