# influxdb-client
This library can be used to pull data from an InfluxDb database as data is being written to it.

## Usage
The usage should be simple:
```java
      InfluxDbClient client = new InfluxDbClient();
      List< TimeDataValue > data = client.retrieve();
      data.stream().forEach( System.out::println );
```
Simply call the setter for the field to change:
```java
      InfluxDbClient client = new InfluxDbClient();
      client.setHost( "192.168.1.104" );
      client.setAuthToken( "XXXXX" );
      client.setTimestamp( "'2023-11-20T15:38:31.366818Z'" );
      List< TimeDataValue > data = client.retrieve();
      data.stream().forEach( System.out::println );
```
