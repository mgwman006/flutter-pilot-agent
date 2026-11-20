package tz.tante.ai.models.propertymanager;

public record AddressDTO(
  Long postalCode,
  Long houseNumber,
  String streetName,
  String ward,
  String city,
  String region,
  String country
)
{
}
