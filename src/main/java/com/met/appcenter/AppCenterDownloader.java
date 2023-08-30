package com.met.appcenter;

import com.met.appcenter.interfaces.IProgressDisplay;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AppCenterDownloader {
    private AppCenterDownloader(){}

    static IProgressDisplay progressBarDisplay = new ProgressBarDisplay();


    private static final Logger logger = LoggerFactory.getLogger(AppCenterDownloader.class);

    // Download app via app center api
    private static final String BASE_URI = "https://api.appcenter.ms/v0.1/apps";
    private static final int TIMEOUT_SECONDS = 120;
    private static final int BUFFER_SIZE = 8192;

    public static List<String> downloadApp(String appTokenValue, String appOwner, String appName, String downloadFilePath, String appVersion, String appBuild) {
        List<String> appDetails = getAppDownloadUrl(appTokenValue, appOwner, appName, appBuild, appVersion);
        appDetails.add(appName);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                .setConnectionRequestTimeout(TIMEOUT_SECONDS * 1000)
                .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                .build();

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {

            HttpGet getRequest = new HttpGet(appDetails.get(3));
            File app = new File(downloadFilePath + File.separator + appName);

            app.getParentFile().mkdirs();
            app.createNewFile();

            try (CloseableHttpResponse response = client.execute(getRequest);
                 InputStream in = response.getEntity().getContent();
                 OutputStream outstream = new BufferedOutputStream(new FileOutputStream(app))) {

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    long contentLength = entity.getContentLength();
                    byte[] buffer = new byte[BUFFER_SIZE]; // Use a larger buffer for improved performance
                    int bytesRead;
                    long totalBytesRead = 0;
                    int prevProgress = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        outstream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        int progress = (int) ((totalBytesRead * 100) / contentLength);
                        if (progress > prevProgress) {
                            progressBarDisplay.displayProgress(progress);
                            prevProgress = progress;
                        }
                    }
                    logger.info("Download Complete");
                }
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            logger.error("Error occurred during app download.", e);
        }

        return appDetails;
    }

    public static JSONTokener appCenterApiGet(String urlToRead, String accessToken) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                .setConnectionRequestTimeout(TIMEOUT_SECONDS * 1000)
                .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                .build();

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build()) {

            HttpGet getRequest = new HttpGet(urlToRead);
            getRequest.addHeader("content-type", "application/json");
            getRequest.addHeader("X-API-Token", accessToken);

            HttpResponse response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (InputStream inputStream = entity.getContent()) {
                    return new JSONTokener(convertStreamToString(inputStream));
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred during API request.", e);
        }

        return null;
    }

    private static String convertStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }

    public static List<String> getAppReleaseDetails(String appTokenValue, String appOwner, String appName,
                                                    String version, String shortVersion) {
        List<String> appDetails = new ArrayList<>();

        try {
            JSONTokener result = appCenterApiGet(String.format("%s/%s/%s/releases", BASE_URI, appOwner, appName), appTokenValue);
            JSONObject selectedRelease = getJsonObject(version, shortVersion, result);

            if (selectedRelease != null) {
                addReleaseDetails(appDetails, selectedRelease);
                logger.info("App release id: {}", appDetails.get(0));
            } else {
                logger.error("No matching app release found.");
            }
        } catch (ConnectTimeoutException | NoHttpResponseException e) {
            logger.error("Error timed out while waiting for app release id Stacktrace:", e);
        } catch (Exception e) {
            logger.error("Error occurred while fetching app release id for app download.", e);
        }

        return appDetails;
    }

    private static JSONObject getJsonObject(String version, String shortVersion, JSONTokener result) {
        JSONArray body = new JSONArray(result);

        JSONObject selectedRelease = null;

        if (version == null && shortVersion == null) {
            selectedRelease = body.getJSONObject(0); // Assuming the first release is the latest
        } else {
            for (int i = 0; i < body.length(); i++) {
                JSONObject release = body.getJSONObject(i);
                String releaseVersion = release.getString("version");
                String releaseShortVersion = release.getString("short_version");

                if ((version == null || version.equals(releaseVersion)) &&
                        (shortVersion == null || shortVersion.equals(releaseShortVersion))) {
                    selectedRelease = release;
                    break; // Break after finding the first matching release
                }
            }
        }
        return selectedRelease;
    }

    public static List<String> getAppDownloadUrl(String appTokenValue, String appOwner, String appName,
                                                 String version, String shortVersion) {
        List<String> appDetails = getAppReleaseDetails(appTokenValue, appOwner, appName, version, shortVersion); // Fetch the latest release

        try {
            String releaseId = appDetails.get(0);
            JSONTokener result = appCenterApiGet(String.format("%s/%s/%s/releases/%s", BASE_URI, appOwner, appName, releaseId), appTokenValue);
            JSONObject body = new JSONObject(result);
            String downloadUrl = body.getString("download_url");
            appDetails.add(downloadUrl);
            logger.info("App download url: {}", downloadUrl);
        } catch (ConnectTimeoutException | NoHttpResponseException e) {
            logger.error("Error timed out while waiting for app download url. Stacktrace:", e);
        } catch (Exception e) {
            logger.error("Error occurred while fetching app download url for app download.", e);
        }

        return appDetails;
    }


    private static void addReleaseDetails(List<String> appDetails, JSONObject release) {
        String id = release.optString("id");
        String version = release.optString("version");
        String shortVersion = release.optString("short_version");

        appDetails.add(id);
        appDetails.add(version);
        appDetails.add(shortVersion);
    }




}
