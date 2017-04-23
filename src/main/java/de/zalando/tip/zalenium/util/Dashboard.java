package de.zalando.tip.zalenium.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.*;

/**
 * Class in charge of building the dashboard, using templates and coordinating video downloads.
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Dashboard {

    public static final String ZALANDO_ICO_FILE_NAME = "zalando.ico";
    private static final String DASHBOARD_TEMPLATE_FILE_NAME = "dashboard_template.html";
    private static final String DASHBOARD_FILE_NAME = "dashboard.html";
    public static final String AMOUNT_OF_RUN_TESTS_FILE_NAME = "amount_of_run_tests.txt";
    public static final String LIST_FILE_NAME = "list.html";
    @SuppressWarnings("WeakerAccess")
    public static final String VIDEOS_FOLDER_NAME = "videos";
    public static final String LOGS_FOLDER_NAME = "logs";
    public static final String LIST_TEMPLATE_FILE_NAME = "list_template.html";
    private static final Logger LOGGER = Logger.getLogger(Dashboard.class.getName());
    private static CommonProxyUtilities commonProxyUtilities = new CommonProxyUtilities();
    private static int executedTests = 0;

    @VisibleForTesting
    public static int getExecutedTests() {
        return executedTests;
    }

    @VisibleForTesting
    static void setExecutedTests(int executedTests) {
        Dashboard.executedTests = executedTests;
    }

    public static synchronized void clearRecordedVideosAndLogs() throws IOException {
        String currentLocalPath = commonProxyUtilities.currentLocalPath();
        String localVideosPath = currentLocalPath + "/" + VIDEOS_FOLDER_NAME;
        deleteGeneratedFiles(localVideosPath);
        deleteLogsAndVideos(localVideosPath);
        setExecutedTests(0);
    }

    private static void deleteLogsAndVideos(String localVideosPath) throws IOException {
        FileUtils.deleteDirectory(new File(localVideosPath + "/" + LOGS_FOLDER_NAME));
        FileFilter fileFilter = new WildcardFileFilter("*.mp4");
        File[] mp4Files = new File(localVideosPath).listFiles(fileFilter);
        if (mp4Files != null) {
            for (File file : mp4Files) {
                file.delete();
            }
        }
    }

    private static void deleteGeneratedFiles(String localVideosPath) {
        FileUtils.deleteQuietly(new File(localVideosPath + "/" + LIST_FILE_NAME));
        FileUtils.deleteQuietly(new File(localVideosPath + "/" + DASHBOARD_FILE_NAME));
        FileUtils.deleteQuietly(new File(localVideosPath + "/" + AMOUNT_OF_RUN_TESTS_FILE_NAME));
    }

    public static synchronized void updateDashboard(TestInformation testInformation) throws IOException {
        String currentLocalPath = commonProxyUtilities.currentLocalPath();
        String localVideosPath = currentLocalPath + "/" + VIDEOS_FOLDER_NAME;

        String testEntry = FileUtils.readFileToString(new File(currentLocalPath, LIST_TEMPLATE_FILE_NAME), UTF_8);
        testEntry = testEntry.replace("{fileName}", testInformation.getFileName())
                .replace("{testName}", testInformation.getTestName())
                .replace("{dateAndTime}", commonProxyUtilities.getShortDateAndTime())
                .replace("{browserAndPlatform}", testInformation.getBrowserAndPlatform())
                .replace("{proxyName}", testInformation.getProxyName())
                .replace("{seleniumLogFileName}", testInformation.getSeleniumLogFileName())
                .replace("{browserDriverLogFileName}", testInformation.getBrowserDriverLogFileName())
                .replace("{browserConsoleLogFileName}", testInformation.getBrowserConsoleLogFileName());

        File testList = new File(localVideosPath, LIST_FILE_NAME);
        // Putting the new entry at the top
        if (testList.exists()) {
            if (isFileOlderThanOneDay(testList.lastModified())) {
                LOGGER.log(Level.FINE, "Deleting file older than one day: " + testList.getAbsolutePath());
                testList.delete();
            } else {
                String testListContents = FileUtils.readFileToString(testList, UTF_8);
                testEntry = testEntry.concat("\n").concat(testListContents);
            }
        }
        FileUtils.writeStringToFile(testList, testEntry, UTF_8);

        executedTests++;
        File testCountFile = new File(localVideosPath, AMOUNT_OF_RUN_TESTS_FILE_NAME);
        synchronizeTestsCountWithFile(testCountFile);
        LOGGER.log(Level.FINE, "Test count: " + executedTests);
        FileUtils.writeStringToFile(testCountFile, String.valueOf(executedTests), UTF_8);

        generateDashboardHtml(executedTests, testEntry, localVideosPath, currentLocalPath);
        copyResources(localVideosPath, currentLocalPath);
    }

    private static void generateDashboardHtml(int numTests, String testEntry, String localVideosPath,
            String currentLocalPath) throws IOException {
        File dashboardHtml = new File(localVideosPath, DASHBOARD_FILE_NAME);
        String dashboard = FileUtils.readFileToString(new File(currentLocalPath, DASHBOARD_TEMPLATE_FILE_NAME), UTF_8);
        dashboard = dashboard.replace("{testList}", testEntry).replace("{executedTests}",
                String.valueOf(executedTests));
        FileUtils.writeStringToFile(dashboardHtml, dashboard, UTF_8);
    }

    private static void copyResources(String localVideosPath, String currentLocalPath) throws IOException {
        File zalandoIco = new File(localVideosPath, ZALANDO_ICO_FILE_NAME);
        if (!zalandoIco.exists()) {
            FileUtils.copyFile(new File(currentLocalPath, ZALANDO_ICO_FILE_NAME), zalandoIco);
        }
        copyFolderIfAbsent("css", localVideosPath, currentLocalPath);
        copyFolderIfAbsent("js", localVideosPath, currentLocalPath);
    }

    private static void copyFolderIfAbsent(String folderName, String targetPath, String sourcePath) throws IOException {
        File targetFolder = new File(targetPath + "/" + folderName);
        if (!targetFolder.exists()) {
            FileUtils.copyDirectory(new File(sourcePath + "/" + folderName), targetFolder);
        }
    }

    @VisibleForTesting
    static void synchronizeTestsCountWithFile(File testCountFile) throws IOException {
        if (testCountFile.exists()) {
            if (isFileOlderThanOneDay(testCountFile.lastModified())) {
                LOGGER.log(Level.FINE, "Deleting file older than one day: " + testCountFile.getAbsolutePath());
                testCountFile.delete();
            } else {
                String executedTestsFromFile = FileUtils.readFileToString(testCountFile, UTF_8);
                try {
                    executedTests = executedTests == 1 ? Integer.parseInt(executedTestsFromFile) + 1 : executedTests;
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, e.toString(), e);
                }
            }
        } else {
            // reset executedTests if testCountFile is missing
            executedTests = 1;
        }
    }

    @VisibleForTesting
    public static void restoreCommonProxyUtilities() {
        commonProxyUtilities = new CommonProxyUtilities();
    }

    public static void setCommonProxyUtilities(CommonProxyUtilities commonProxyUtilities) {
        Dashboard.commonProxyUtilities = commonProxyUtilities;
    }

    @VisibleForTesting
    public static boolean isFileOlderThanOneDay(long lastModified) {
        long timeSinceLastModification = new Date().getTime() - lastModified;
        return timeSinceLastModification > (24 * 60 * 60 * 1000);
    }

}
