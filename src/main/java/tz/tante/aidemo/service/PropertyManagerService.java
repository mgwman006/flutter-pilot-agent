package tz.tante.aidemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.stereotype.Service;
import tz.tante.aidemo.clients.PropertyManagerFeignApi;
import tz.tante.aidemo.models.PropertyManagerApiResponse;
import tz.tante.aidemo.models.propertymanager.UnitDetailsDTO;

@Service
public class PropertyManagerService
{
  private final PropertyManagerFeignApi propertyManagerFeignApi;
  private final ObjectMapper objectMapper;

  public PropertyManagerService(PropertyManagerFeignApi propertyManagerFeignApi)
  {
    this.propertyManagerFeignApi = propertyManagerFeignApi;
    this.objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public UnitDetailsDTO getUnitDetails(Long unitId)
  {
    try
    {
      String rawResponse = propertyManagerFeignApi.getUnitDetails(unitId);
      if (rawResponse == null || rawResponse.isBlank())
      {
        throw new IllegalStateException("Property Manager returned an empty response body");
      }

      PropertyManagerApiResponse<UnitDetailsDTO> response = objectMapper.readValue(
        rawResponse,
        new TypeReference<PropertyManagerApiResponse<UnitDetailsDTO>>() {}
      );

      if (!response.success())
      {
        throw new IllegalStateException("Property Manager request failed: " + response.message());
      }

      if (response.data() == null)
      {
        throw new IllegalStateException("Property Manager returned success but no unit data");
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
    catch (JsonProcessingException ex)
    {
      throw new IllegalStateException("Property Manager response parsing failed: " + ex.getMessage(), ex);
    }
  }

  public String getUnitDetailsJson(Long unitId)
  {
    try
    {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(getUnitDetails(unitId));
    }
    catch (JsonProcessingException ex)
    {
      throw new IllegalStateException("Property Manager response serialization failed: " + ex.getMessage(), ex);
    }
  }
}
