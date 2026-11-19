package tz.tante.aidemo.clients;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import tz.tante.aidemo.models.PropertyManagerApiResponse;

@FeignClient(
  name = "property-manager",
  url = "${property.manager.base.url}"
)
public interface PropertyManagerFeignApi
{
  @RequestLine("GET /v1/units/{unitId}")
  PropertyManagerApiResponse<JsonNode> getUnitDetails(@Param("unitId") Long unitId);
}
