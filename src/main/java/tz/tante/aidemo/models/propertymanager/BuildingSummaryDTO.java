package tz.tante.aidemo.models.propertymanager;

public record BuildingSummaryDTO(
  Long id,
  String name,
  Integer number,
  String code,
  String description,
  Integer numberOfFloors,
  PropertySummaryDTO property
)
{
}
