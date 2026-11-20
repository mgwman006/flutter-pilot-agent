package tz.tante.aidemo.models;

import tz.tante.aidemo.models.propertymanager.UnitDetailsDTO;

public record ChatFindResponseData(
  String answer,
  String source,
  Long unitId,
  UnitDetailsDTO unitDetails
)
{
}
