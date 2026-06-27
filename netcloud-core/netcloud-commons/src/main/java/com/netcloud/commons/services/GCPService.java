package com.netcloud.commons.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;

@Service
public class GCPService {
    @Value("${gcp:meta-zone}")
    private static String METADATA_ZONE_URL;
    public String fetchGcpZone() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METADATA_ZONE_URL))
                    .header("Metadata-Flavor", "Google") // Mandatory header for GCP metadata
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String fullZonePath = response.body();
                // Extracts "us-central1-a" from "projects/12345/zones/us-central1-a"
                return fullZonePath.substring(fullZonePath.lastIndexOf('/') + 1);
            }
        } catch (Exception e) {
            System.out.println("[Registration] Metadata server unreachable. Falling back to default region.");
        }
        return "LOCAL-DEV";
    }
    public String getLocalIpAddress() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr.getAddress().length == 4) {
                    return addr.getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }
}
