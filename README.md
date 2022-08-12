# json-merge-maven-plugin

Maven plugin to merge multiple JSON files within a maven goal.

## Usage

Place the plugin in your `pom.xml`. The default phase where it gets exeuted is `generate-sources`. Possible values of the
configuration parameter `outputFormat` are `JSON` and `YAML`.

```xml

<plugin>
    <groupId>de.codecentric</groupId>
    <artifactId>json-merge-maven-plugin</artifactId>
    <version>${json-merge.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <filenames>
                    <filename>file1</filename>
                    <filename>file2</filename>
                    ...
                </filenames>
                <outputPath>${project.build.directory}/merged</outputPath>
                <outputFilename>my-merged-file</outputFilename>
                <outputFormat>JSON</outputFormat>
            </configuration>
        </execution>
    </executions>
</plugin>
```
