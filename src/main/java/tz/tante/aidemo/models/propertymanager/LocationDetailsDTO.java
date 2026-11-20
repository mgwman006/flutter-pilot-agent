package tz.tante.aidemo.models.propertymanager;

public record LocationDetailsDTO(
  Double latitude,
  Double longitude,
  AddressDTO address
)
{
}
