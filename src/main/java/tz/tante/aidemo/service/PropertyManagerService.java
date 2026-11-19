package tz.tante.aidemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import org.springframework.stereotype.Service;
import tz.tante.aidemo.clients.PropertyManagerFeignApi;
import tz.tante.aidemo.models.PropertyManagerApiResponse;

@Service
public class PropertyManagerService
{
  private final PropertyManagerFeignApi propertyManagerFeignApi;

  public PropertyManagerService(PropertyManagerFeignApi propertyManagerFeignApi)
  {
    this.propertyManagerFeignApi = propertyManagerFeignApi;
  }

  public JsonNode getUnitDetails(Long unitId)
  {
    try
    {
      PropertyManagerApiResponse<JsonNode> response = propertyManagerFeignApi.getUnitDetails(unitId);

      if (response == null)
      {
        throw new IllegalStateException("Property Manager returned an empty response body");
      }

      if (!response.success())
      {
        throw new IllegalStateException("Property Manager request failed: " + response.message());
      }

      return response.data();
    }
    catch (FeignException ex)
    {
      throw new IllegalStateException(
        "Property Manager request failed with status " + ex.status() + ": " + ex.contentUTF8(),
        ex
      );
    }
  }
}
