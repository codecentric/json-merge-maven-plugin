package de.codecentric.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import com.github.hemantsonu20.json.JsonMerge;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mojo(name = "merge", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JsonMergeMojo extends AbstractMojo {

    public enum Format {JSON, YAML}

    @Parameter(name = "filenames", required = true)
    List<String> filenames;

    @Parameter(name = "outputPath", property = "json-merge.maven.plugin.outputPath",
            defaultValue = "${project.build.directory}/json-merge")
    String outputPath;

    @Parameter(name = "outputFilename", property = "json-merge.maven.plugin.outputFilename", defaultValue = "json-merge")
    String outputFilename = "json-merge";

    @Parameter(name = "outputFormat", property = "openapi.validator.maven.plugin.outputFormat", defaultValue = "JSON")
    Format outputFormat = Format.JSON;

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(name = "encoding", property = "json-merge.maven.plugin.encoding")
    String encoding;

    String projectEncoding = "UTF-8";

    @Parameter(name = "prettyPrint", property = "json-merge.maven.plugin.prettyPrint")
    boolean prettyPrint = true;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<JsonNode> jsonNodes = readNodes(filenames);
            JsonNode mergedNodes = jsonNodes.get(0);
            for (int i = 1; i < jsonNodes.size(); i++) {
                mergedNodes = JsonMerge.merge(mergedNodes, jsonNodes.get(i));
            }

            determineEncoding();
            boolean yaml = Format.YAML == outputFormat;
            String mergedFileContent = writeJsonNode(mergedNodes, yaml);

            Path path = Paths.get(outputPath, "temp");
            File parentFile = path.toFile().getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }

            Path mergedFilePath = Paths.get(outputPath, outputFilename + (yaml ? ".yaml" : ".json"));
            getLog().info(String.format("Writing: %s", mergedFilePath));
            Files.write(mergedFilePath, mergedFileContent.getBytes(Charset.forName(encoding)));
        } catch (Exception e) {
            getLog().error("Error while trying to merge Json", e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<JsonNode> readNodes(List<String> filenames) throws IOException {
        Objects.requireNonNull(filenames, "Filenames are required for merging");

        List<JsonNode> jsonNodes = new ArrayList<>(filenames.size());
        for (String filename : filenames) {
            jsonNodes.add(readNode(filename));
        }
        return jsonNodes;
    }

    private JsonNode readNode(String filename) throws IOException {
        Objects.requireNonNull(filename);

        getLog().info(String.format("Reading : %s", filename));
        Path path = Paths.get(filename);

        ObjectMapper mapper = new ObjectMapper();
        if (isYamlFile(path)) {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        mapper.registerModule(new JSONPModule());
        InputStream stream = new BufferedInputStream(Files.newInputStream(path));
        return mapper.readTree(stream);
    }

    private static boolean isYamlFile(Path path) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:^.*\\.[Yy][Aa]?[Mm][Ll]$");
        // Assume json file unless .yaml or .yml extension.
        return matcher.matches(path);
    }

    private void determineEncoding() {
        if (project != null) {
            String pEnc = project.getProperties().getProperty("project.build.sourceEncoding");
            if (StringUtils.isNotBlank(pEnc)) {
                projectEncoding = pEnc;
            }
        }
        if (StringUtils.isBlank(encoding)) {
            encoding = projectEncoding;
        }
        getLog().info(String.format("Using '%s' encoding to write output files", encoding));
    }

    private String writeJsonNode(JsonNode jsonObject, boolean yaml) throws JsonProcessingException {
        ObjectMapper mapper;
        if (yaml) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }
        mapper.registerModule(new JSONPModule());
        if (prettyPrint) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } else {
            return mapper.writeValueAsString(jsonObject);
        }
    }
}
