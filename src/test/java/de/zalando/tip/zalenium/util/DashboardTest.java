package de.zalando.tip.zalenium.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DashboardTest {

    private TestInformation ti = new TestInformation("seleniumSessionId", "testName", "proxyName", "browser",
            "browserVersion", "platform");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void restoreCommonProxyUtilities() {
        Dashboard.restoreCommonProxyUtilities();
    }

    @Before
    public void initDashboard() throws IOException {
        Dashboard.setExecutedTests(0);
        DashboardTestingSupport.ensureRequiredInputFilesExist(temporaryFolder);
        DashboardTestingSupport.mockCommonProxyUtilitiesForDashboardTesting(temporaryFolder);

        Dashboard.updateDashboard(ti);
    }

    @Test
    public void testCountOne() throws IOException {
        Assert.assertEquals(1, Dashboard.getExecutedTests());
    }

    @Test
    public void testCountTwo() throws IOException {
        Dashboard.updateDashboard(ti);
        Assert.assertEquals(2, Dashboard.getExecutedTests());
    }

    @Test
    public void missingExecutedTestsFile() throws IOException {
        cleanTempVideosFolder();
        DashboardTestingSupport.ensureRequiredInputFilesExist(temporaryFolder);
        Dashboard.updateDashboard(ti);
        Assert.assertEquals(1, Dashboard.getExecutedTests());
    }

    @Test
    public void nonNumberContentsIgnored() throws IOException {
        File testCountFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/" + Dashboard.VIDEOS_FOLDER_NAME
                + "/" + Dashboard.AMOUNT_OF_RUN_TESTS_FILE_NAME);
        FileUtils.writeStringToFile(testCountFile, "Not-A-Number", UTF_8);
        Dashboard.setExecutedTests(0);
        Dashboard.updateDashboard(ti);
        Assert.assertEquals("1", FileUtils.readFileToString(testCountFile, UTF_8));
    }

    @Test
    public void clearRecordedVideosAndLogs() throws IOException {
        fileInTempVideosFolder("fake_video.mp4").createNewFile();
        fileInTempVideosFolder("logs").mkdir();

        Dashboard.clearRecordedVideosAndLogs();

        Assert.assertFalse(fileInTempVideosFolder("fake_video.mp4").exists());
        Assert.assertFalse(fileInTempVideosFolder("logs").exists());
        Assert.assertFalse(fileInTempVideosFolder(Dashboard.LIST_FILE_NAME).exists());
    }

    private File fileInTempVideosFolder(String fileName) {
        String tempVideoFolder = temporaryFolder.getRoot().getAbsolutePath() + "/" + Dashboard.VIDEOS_FOLDER_NAME;
        return new File(tempVideoFolder + "/" + fileName);
    }

    private void cleanTempVideosFolder() throws IOException {
        FileUtils.cleanDirectory(new File(temporaryFolder.getRoot().getAbsolutePath()));
    }
}
