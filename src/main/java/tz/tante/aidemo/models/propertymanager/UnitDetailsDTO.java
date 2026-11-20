package tz.tante.aidemo.models.propertymanager;

import java.math.BigDecimal;
import java.util.List;

public record UnitDetailsDTO(
  Long id,
  String unitNumber,
  Long rentalProfileId,
  Integer numberOfBedrooms,
  Integer numberOfBathrooms,
  Integer numberParkingSpots,
  BigDecimal rentAmount,
  String type,
  String status,
  Long roomSize,
  String sizeUnit,
  Boolean isListed,
  Integer floorNumber,
  BuildingSummaryDTO building,
  List<Object> images,
  List<Long> agents
)
{
}
