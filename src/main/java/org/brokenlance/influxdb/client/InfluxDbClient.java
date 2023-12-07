package org.brokenlance.influxdb.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.joda.time.MutableDateTime;

import static java.util.Arrays.stream;

@Slf4j
public class InfluxDbClient
{
   private static Pattern      pattern      = Pattern.compile( "^.*?values[^\\[]+\\[\\[(.*)\\]\\].*$", Pattern.DOTALL );
   private        String       url          = "http://localhost:8086";
   private        String       host         = "localhost";
   private        String       port         = "8086";
   private        String       queryBase    = "SELECT value FROM http_req_duration WHERE time > ";
   private        String       query        = "SELECT value FROM http_req_duration WHERE time > ";
   private        String       timestamp    = "'2023-11-20T15:38:31.366818Z'";
   private        String       order        = " ORDER BY time";
   private        String       measurement  = "response-time-data";
   private        String       token        = "Token _QUrkqZvVPM2X3OnuQJ7dfWT62QaOhC3072OdD-ER_Z7Vxcuw9aTN7dSo9tAaOeEVpNfAszU41CRUl3T5nWlUA==";
   private        String       sutTag       = null;
   private        OkHttpClient client       = new OkHttpClient();
   private        Request      request      = new Request.Builder()
                                                         .url( url + "/query?db=" + measurement + "&q=" + query + timestamp + order )
                                                         .header( "Authorization", token )
                                                         .build();

   /**
    * Default constructor.
    */
   public InfluxDbClient()
   {
   }

   /**
    * @param String -- the URL if different from http://localhost:8086
    */
   public void setUrl( String url )
   {
      this.url = url;
   }

   /**
    * @param String -- the host if different from localhost
    */
   public void setHost( String host )
   {
      this.host = host;
      this.url = "http://" + host + ":" + port;
   }

   /**
    * @param String -- the port if different from 8086
    */
   public void setPort( String port )
   {
      this.port = port;
      this.url = "http://" + host + ":" + port;
   }

   /**
    * @param String -- the query in string format
    */
   public void setQueryString( String query )
   {
      this.query = query;
   }

   /**
    * @return String -- builds the query string from the data values.
    */
   public String getQueryString()
   {
      StringBuilder q = new StringBuilder( query );

      q.append( timestamp                        );
      q.append( ( sutTag == null ) ? "" : sutTag );
      q.append( order                            );

      return q.toString();
   }

   /**
    * @param String -- the measurement or database
    */
   public void setMeasurement( String db )
   {
      this.measurement = db;
   }

   /**
    * @param String -- sets the order by clause to a particular value, null if not required.
    */
   public void setOrderBy( String order )
   {
      this.order = order;
   }

   /**
    * @param String -- the authorization token string
    */
   public void setAuthToken( String token )
   {
      this.token = "Token " + token;
   }

   /**
    * @param String -- the timestamp from which all data values after will be retrieved
    * Note that this value (along with all values in influx) should be single-quoted.
    */
   public void setTimestamp( String timestamp )
   {
      if( timestamp != null && !timestamp.strip().isEmpty() )
      {
         this.timestamp = "'" + timestamp.replaceAll( "\"", "" ) + "'";
      }
      else
      {
         this.timestamp = "'2023-11-20T15:38:31.366818Z'";
      }
   }

   /**
    * @param String -- the System Under Test (SUT) tag name.
    * In the event that there are multiple systems being tested, we only want to pull out time data points
    * for the SUT.
    */
   public void setSutTag( String tag )
   {
      this.sutTag = " AND sut = '" + tag + "'";
   }

   /**
    * Rebuilds the OkHttp request object when any data field changes.
    */
   private void buildRequest()
   {
      StringBuilder u = new StringBuilder( url );

      u.append( "/query?db="     );
      u.append( measurement      );
      u.append( "&q="            );
      u.append( getQueryString() );

      request = new Request.Builder()
                           .url( u.toString() )
                           .header( "Authorization", token )
                           .build();
   }

   /**
    * Retrieve all data since the beginning timestamp to retrieve data points.
    * @return List< TimeDataValue > -- all the time data values after the timestamp or all if timestamp is null.
    */
   public List< TimeDataValue > retrieve()
   {
      List< TimeDataValue > values = null;

      buildRequest();

      log.info( "the request is: {}", request );

      try( Response response = client.newCall( request ).execute() )
      {
         Matcher                   mat    = pattern.matcher( response.body().string() );
         AtomicReference< String > last   = new AtomicReference<>();

         if( mat.matches() )
         {
            values = stream( mat.group( 1 ).split( "\\],\\[" ) ).map( i -> { String[] a = i.split( "," );
                                                                             last.set( a[ 0 ] );
                                                                             return new TimeDataValue( MutableDateTime.parse( a[ 0 ].replaceAll( "\"", "" ) ).getMillis(),
                                                                                                       Double.valueOf( a[ 1 ].toString() ) ); } )
                                                                .toList();
            setTimestamp( last.get() );
         }

         log.info( "last timestamp: {}", timestamp );
      }
      catch( IOException e )
      {
         log.error( "Error calling influxdb: " + e );
      }

      return values;
   }
}
