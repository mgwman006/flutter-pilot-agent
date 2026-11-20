package tz.tante.ai.models.propertymanager;

public record LocationDetailsDTO(
  Double latitude,
  Double longitude,
  AddressDTO address
)
{
}
