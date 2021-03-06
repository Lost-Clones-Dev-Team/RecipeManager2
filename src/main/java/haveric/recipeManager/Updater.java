package haveric.recipeManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater {
 // The project's unique ID
    private static int projectID;

    // An optional API key to use, will be null if not submitted
    private static String apiKey;

    // Keys for extracting file information from JSON response
    private static final String API_NAME_VALUE = "name";
    private static final String API_LINK_VALUE = "downloadUrl";

    // Static information for querying the API
    private static final String API_QUERY = "/servermods/files?projectIds=";
    private static final String API_HOST = "https://api.curseforge.com";

    // Only used to link the user to manually download files
    private static String urlFiles;
    private static RecipeManager plugin;
    private static String pluginName;

    private static String latestVersion;
    private static String latestLink;
    private static String currentBetaStatus = "";
    private static String latestBetaStatus = "";

    private static String versionRegex = "[^de0-9 ]?[v]?([0-9\\.]+[^\\.a-z -])(?:[ -])?([dev|alpha|beta]*[-]*[0-9]*)?";

    private static int taskId = -1;

    private Updater() { } // Private constructor for utility class

    /**
     * Check for updates using your Curse account (with key)
     *
     * @param projectID The BukkitDev Project ID, found in the "Facts" panel on the right-side of your project page.
     * @param apiKey Your ServerMods API key, found at https://dev.bukkit.org/home/servermods-apikey/
     */
    public static void init(RecipeManager newPlugin, int newProjectID, String newApiKey) {
        plugin = newPlugin;
        urlFiles = plugin.getDescription().getWebsite() + "files";
        pluginName = plugin.getDescription().getName();
        latestVersion = null;
        latestLink = null;
        projectID = newProjectID;
        apiKey = newApiKey;
        stop();

        query(null); // Do one initial check

        int time = Settings.getInstance().getUpdateCheckFrequency();

        if (time > 0) {
            time *= 60 * 60 * 20;
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                public void run() {
                    query(null);
                }

            }, time, time);
        }
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public static String getCurrentVersion() {
        Pattern pattern = Pattern.compile(versionRegex);
        String currentVersion = plugin.getDescription().getVersion();

        Matcher matcher = pattern.matcher(currentVersion);
        if (matcher.find()) {
            currentVersion = matcher.group(1).replaceAll(" v", "");

            currentBetaStatus = matcher.group(2);
            if (currentBetaStatus == null) {
                currentBetaStatus = "";
            } else {
                currentBetaStatus = currentBetaStatus.replaceAll(" -", "");
            }
        }

        return currentVersion;
    }

    public static String getLatestVersion() {
        Pattern pattern = Pattern.compile(versionRegex);
        String latest = latestVersion;

        if (latest != null) {
            Matcher matcher = pattern.matcher(latest);
            if (matcher.find()) {
                latest = matcher.group(1).replaceAll(" v", "");

                latestBetaStatus = matcher.group(2);
                if (latestBetaStatus == null) {
                    latestBetaStatus = "";
                } else {
                    latestBetaStatus = latestBetaStatus.replaceAll(" -", "");
                }
            }
        }

        return latest;
    }

    /**
     *
     * @return compare<br>
     *  3: Current and BukkitDev are running different beta versions
     *  2: BukkitDev has a new beta
     *  1: Current version is newer than the BukkitDev<br>
     *  0: Same version as BukkitDev<br>
     * -1: BukkitDev is newer than current version
     * -2: Error occurred
     */
    public static int compareVersions() {
        int compare = -2;

        String current = getCurrentVersion();
        String latest = getLatestVersion();

        if (latest != null) {
            if (current.equals(latest)) {
                if (currentBetaStatus.equals(latestBetaStatus)) {
                    compare = 0;
                } else if (!currentBetaStatus.equals("") && latestBetaStatus.equals("")) {
                    compare = -1;
                } else if (currentBetaStatus.equals("") && !latestBetaStatus.equals("")) {
                    compare = 2;
                } else {
                    compare = 3;
                }
            } else {
                String[] currentArray = current.split("\\.");
                String[] latestArray = latest.split("\\.");

                int shortest = currentArray.length;
                int latestLength = latestArray.length;
                if (latestLength < shortest) {
                    shortest = latestLength;
                }

                for (int i = 0; i < shortest; i++) {
                    int c = Integer.parseInt(currentArray[i]);
                    int l = Integer.parseInt(latestArray[i]);

                    if (c > l) {
                        compare = 1;
                        break;
                    } else if (l > c) {
                        compare = -1;
                        break;
                    }
                }

                // Same up to the shortest version
                if (compare == -2) {
                    if (currentArray.length > latestLength) {
                        compare = 1;
                    } else {
                        compare = -1;
                    }
                }
            }
        }

        return compare;
    }

    public static String getLatestBetaStatus() {
        return latestBetaStatus;
    }

    public static String getCurrentBetaStatus() {
        return currentBetaStatus;
    }

    public static String getLatestLink() {
        return latestLink;
    }

    /**
     * Query the API to find the latest approved file's details.
     */
    public static void query(CommandSender sender) {
        if (Settings.getInstance().getUpdateCheckEnabled()) {
            URL url = null;

            try {
                // Create the URL to query using the project's ID
                url = new URL(API_HOST + API_QUERY + projectID);

                // Open a connection and query the project
                URLConnection conn = url.openConnection();

                if (apiKey != null) {
                    // Add the API key to the request if present
                    conn.addRequestProperty("X-API-Key", apiKey);
                }

                // Add the user-agent to identify the program
                conn.addRequestProperty("User-Agent", pluginName);

                // Read the response of the query
                // The response will be in a JSON format, so only reading one line is necessary.
                final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();

                // Parse the array of files from the query's response
                JSONArray array = (JSONArray) JSONValue.parse(response);

                if (array.size() > 0) {
                    // Get the newest file's details
                    JSONObject latest = (JSONObject) array.get(array.size() - 1);

                    // Get the version's title
                    latestVersion = (String) latest.get(API_NAME_VALUE);

                    // Get the version's link
                    latestLink = (String) latest.get(API_LINK_VALUE);
                }

                if (latestVersion == null) {
                    if (sender != null) { // send this message only if it's a requested update check
                        Messages.sendAndLog(sender, "<red>Unable to check for updates, please check manually by visiting:<yellow> " + urlFiles);
                    } else {
                        return; // block the disable message
                    }
                } else {
                    String currentVersion = getCurrentVersion();
                    String latest = getLatestVersion();

                    if (latest != null) {
                        int compare = compareVersions();

                        if (compare == 0) {
                            if (sender != null) { // send this message only if it's a requested update check
                                Messages.sendAndLog(sender, "<gray>Using the latest version: " + latest);
                            } else {
                                return; // block the disable message
                            }
                        } else if (compare == -1) {
                            Messages.sendAndLog(sender, "New version: <green>" + latest + "<reset>! You're using <yellow>" + currentVersion);
                            Messages.sendAndLog(sender, "Grab it at: <green>" + latestLink);
                        } else if (compare == 1) {
                            Messages.sendAndLog(sender, "<gray>You are using a newer version: <green>" + currentVersion + "<reset>. Latest on BukkitDev: <yellow>" + latest);
                        } else if (compare == 2) {
                            Messages.send(sender, "New alpha/beta version: <green>" + latestVersion + " " + Updater.getLatestBetaStatus() + "<reset>! You're using <yellow>" + currentVersion + "<reset>, grab it at: <light_purple>" + Updater.getLatestLink());
                            Messages.sendAndLog(sender, "Grab it at: <green>" + latestLink);
                        } else if (compare == 3) {
                            Messages.send(sender, "BukkitDev has a different alpha/beta version: <green>" + latestVersion + " " + Updater.getLatestBetaStatus() + "<reset>! You're using <yellow>" + currentVersion + " " + Updater.getCurrentBetaStatus() + "<reset>, grab it at: <light_purple>" + Updater.getLatestLink());
                            Messages.sendAndLog(sender, "Grab it at: <green>" + latestLink);
                        }
                    }
                }

                if (sender == null) {
                    Messages.sendAndLog(null, "<gray>You can disable this check from config.yml.");
                }
            } catch (MalformedURLException e) {
                Messages.error(null, e, "Error while checking for updates");
                Messages.info("You can disable the update checker in config.yml, but please report the error.");
            } catch (IOException e) {
                // There was an error reading the query
                Messages.error(null, e, "Error while checking for updates");
                Messages.info("You can disable the update checker in config.yml, but please report the error.");
            }
        }
    }
}