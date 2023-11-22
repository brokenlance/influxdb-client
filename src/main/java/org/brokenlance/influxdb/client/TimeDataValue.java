package org.brokenlance.influxdb.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeDataValue
{
   private Long   epoch;
   private Double value;
}
