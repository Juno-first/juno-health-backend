package com.juno.healthapp.httpclient;

import com.juno.healthapp.dto.RouteInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeoapifyRoutingClient {

    private final RestClient restClient;

    @Value("${geoapify.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.geoapify.com/v1/routing";

    public GeoapifyRoutingClient() {
        this.restClient = RestClient.create();
    }

    public RouteInfo getRoute(BigDecimal fromLat, BigDecimal fromLon,
                              BigDecimal toLat, BigDecimal toLon) {
        try {
            String waypoints = fromLat + "," + fromLon + "|" + toLat + "," + toLon;

            String response = restClient.get()
                    .uri(BASE_URL + "?waypoints={wp}&mode=drive&apiKey={key}", waypoints, apiKey)
                    .retrieve()
                    .body(String.class);

            return parseRoute(response);
        } catch (Exception e) {
            return null;   // don't fail the whole request if routing is unavailable
        }
    }

    @SuppressWarnings("unchecked")
    private RouteInfo parseRoute(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode feature  = root.path("features").get(0);
            JsonNode props    = feature.path("properties");
            JsonNode legs     = props.path("legs").get(0);
            JsonNode stepsNode = legs.path("steps");

            double distanceMeters  = props.path("distance").asDouble();
            double durationSeconds = props.path("time").asDouble();
            double durationMinutes = durationSeconds / 60.0;

            // Extract step instructions
            List<String> steps = new ArrayList<>();
            for (JsonNode step : stepsNode) {
                String text = step.path("instruction").path("text").asText();
                if (!text.isBlank()) steps.add(text);
            }

            // Extract MultiLineString geometry
            JsonNode coordsNode = feature.path("geometry").path("coordinates");
            List<List<double[]>> geometry = new ArrayList<>();
            for (JsonNode lineString : coordsNode) {
                List<double[]> line = new ArrayList<>();
                for (JsonNode point : lineString) {
                    line.add(new double[]{ point.get(0).asDouble(), point.get(1).asDouble() });
                }
                geometry.add(line);
            }

            return new RouteInfo(distanceMeters, durationSeconds,
                    Math.round(durationMinutes * 10.0) / 10.0, steps, geometry);

        } catch (Exception e) {
            return null;
        }
    }
}