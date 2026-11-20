package tz.tante.aidemo.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
  name = "property-manager",
  url = "${property.manager.base.url}"
)
public interface PropertyManagerFeignApi
{
  @GetMapping("/v1/units/{unitId}")
  String getUnitDetails(@PathVariable("unitId") Long unitId);
}
