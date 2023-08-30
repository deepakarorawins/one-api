package com.met.appcenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.met.appcenter.enums.AppName.*;

/**
 * This class demonstrates how to download an app from App Center based on provided parameters.
 * It uses the AppCenterDownloader class to facilitate downloading the app and extracting its details.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * The main method that initiates the app download process.
     *
     * @param args The command-line arguments (not used in this context).
     */
    public static void main(String[] args) {

        // Set up the required parameters for app download
        String appTokenValue = "a00aa8a84447d6135be5711bcf80f023ffc8c14c";
        String appOwner = "onekeyadmin-781x";

        System.setProperty("productName", "OneKey"); // Heated-Gear, OneKey
        System.setProperty("env", "stage");
//        System.setProperty("region", "na");
//        System.setProperty("tfd", "y");
//        System.setProperty("appVersion", "8.64.0");
//        System.setProperty("appBuild", "5878");


        // Retrieve optional parameters using Optional class
        Optional<String> productName = Optional.ofNullable(System.getProperty("productName"));
        Optional<String> appVersion = Optional.ofNullable(System.getProperty("appVersion"));
        Optional<String> appBuild = Optional.ofNullable(System.getProperty("appBuild"));
        Optional<String> env = Optional.ofNullable(System.getProperty("env"));
        Optional<String> region = Optional.ofNullable(System.getProperty("region"));
        Optional<String> tfd = Optional.ofNullable(System.getProperty("tfd"));


        // Set up the app download path
        String appDownloadPath = System.getProperty("user.dir") + "/apps/ios/" + env.orElse("test") + "_" + region.orElse("na");

        // Retrieve the app name based on provided parameters
        String appName = getAppName(productName.orElse("OneKey"), env.orElse("test"), region.orElse("na"), tfd.orElse("n"));

        // Download the app using the AppCenterDownloader class
        List<String> appDetails = AppCenterDownloader.downloadApp(appTokenValue, appOwner, appName, appDownloadPath, appVersion.orElse(null), appBuild.orElse(null));


        // Display downloaded app details
        logger.info("App Details: - {} version: {}_{}", appDetails.get(4), appDetails.get(2), appDetails.get(1));

    }

    /**
     * Retrieves the app name based on provided parameters.
     *
     * @param productName The product name of the app.
     * @param environment The environment of the app (e.g., "test" or "stage").
     * @param region      The region of the app (e.g., "na", "emea", "anz").
     * @param isTfd       A flag indicating whether the app is TFD ("y" or "n").
     * @return The app name corresponding to the provided parameters.
     */
    public static String getAppName(String productName, String environment, String region, String isTfd) {
        try {
            String environmentToCheck = environment.toLowerCase();
            if (productName.equalsIgnoreCase("Heated-Gear")) {
                return HEATED_GEAR.getName();
            } else if (environmentToCheck.equals("test")) {
                return isTfd.equalsIgnoreCase("y") ? ONE_KEY_TFD_NA.getName() : ONE_KEY_TEST_NA.getName();
            } else {
                // Define the region-to-name mapping
                Map<String, String> regionMapping = new HashMap<>();
                regionMapping.put("na", ONE_KEY_STAGE_NA.getName());
                regionMapping.put("emea", ONE_KEY_STAGE_EMEA.getName());
                regionMapping.put("anz", ONE_KEY_STAGE_ANZ.getName());

                String regionToCheck = region.toLowerCase();
                return regionMapping.getOrDefault(regionToCheck, ONE_KEY_STAGE_NA.getName());
            }
        } catch (Exception e) {
            logger.error("Error occurred while getting app name", e);
            return null;
        }
    }
}
