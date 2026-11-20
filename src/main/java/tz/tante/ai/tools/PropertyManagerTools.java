package tz.tante.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import tz.tante.ai.service.PropertyManagerService;

@Component
public class PropertyManagerTools
{
  private static final Logger logger = LoggerFactory.getLogger(PropertyManagerTools.class);
  private final PropertyManagerService propertyManagerService;

  public PropertyManagerTools(PropertyManagerService propertyManagerService)
  {
    this.propertyManagerService = propertyManagerService;
  }

  @Tool(
    name = "getUnitDetails",
    description = "Get detailed information about a property unit by unit ID from Property Manager and return exact factual JSON data",
    returnDirect = true
  )
  public String getUnitDetails(@ToolParam(description = "The unique unit ID") Long unitId)
  {
    logger.info("Fetching details for unit ID: {}", unitId);
    return propertyManagerService.getUnitDetailsJson(unitId);
  }
}
