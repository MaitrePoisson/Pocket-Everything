package fr.pocketeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class VersionConsistencyTest {

    private static final Path ROOT = Path.of(System.getProperty("project.dir"));
    private static final String SEMVER = "\\d+\\.\\d+\\.\\d+";
    private static final Pattern CHANGELOG_HEADING = Pattern.compile("(?m)^## (" + SEMVER + ") ");

    private static String read(String file) throws IOException {
        return Files.readString(ROOT.resolve(file), StandardCharsets.UTF_8);
    }

    private static String modVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(ROOT.resolve("gradle.properties"), StandardCharsets.UTF_8));
        String version = properties.getProperty("mod_version");
        assertTrue(version.matches(SEMVER), "mod_version must be MAJOR.MINOR.PATCH: " + version);
        return version;
    }

    @Test
    void fabricModJsonCarriesTheModVersion() throws IOException {
        Matcher m = Pattern.compile("\"version\"\s*:\s*\"([^\"]+)\"")
                .matcher(read("build/resources/main/fabric.mod.json"));
        assertTrue(m.find());
        assertEquals(modVersion(), m.group(1));
    }

    @Test
    void changelogStartsWithTheModVersion() throws IOException {
        Matcher m = CHANGELOG_HEADING.matcher(read("CHANGELOG.md"));
        assertTrue(m.find());
        assertEquals(modVersion(), m.group(1));
    }

    @Test
    void changelogHasNoDuplicateVersion() throws IOException {
        Matcher m = CHANGELOG_HEADING.matcher(read("CHANGELOG.md"));
        Set<String> seen = new HashSet<>();
        while (m.find()) {
            assertTrue(seen.add(m.group(1)), "duplicate changelog version " + m.group(1));
        }
    }

    @Test
    void docsCiteTheCurrentJar() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(ROOT.resolve("gradle.properties"), StandardCharsets.UTF_8));
        String jar = "pocket-everything-" + properties.getProperty("mod_loader") + "-" + properties.getProperty("minecraft_version") + ".jar";
        assertTrue(read("README.md").contains(jar));
        assertTrue(read("TEST_GUIDE.md").contains(jar));
    }
}
