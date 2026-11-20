package tz.tante.ai.models;

import tz.tante.ai.models.propertymanager.UnitDetailsDTO;

public record ChatFindResponseData(
  String answer,
  String source,
  Long unitId,
  UnitDetailsDTO unitDetails
)
{
}
