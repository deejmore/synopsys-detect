package com.synopsys.integration.detectable.detectables.go.unit;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.detectable.Extraction;
import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModCliExtractor;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModGraphParser;

public class GoModCliExtractorTest {

    @Test
    public void handleMultipleReplacementsForOneComponentTest() throws ExecutableRunnerException {
        ExecutableRunner executableRunner = Mockito.mock(ExecutableRunner.class);
        File directory = new File("");
        File goExe = new File("");

        String[] goListArgs = {"list", "-m"};
        Mockito.when(executableRunner.execute(directory, goExe, goListArgs)).thenReturn(goListOutput());

        String[] goListJsonArgs = {"list", "-m", "-u", "-json", "all"};
        Mockito.when(executableRunner.execute(directory, goExe, goListJsonArgs)).thenReturn(goListJsonOutput());

        String[] goModGraphArgs = {"mod", "graph"};
        Mockito.when(executableRunner.execute(directory, goExe, goModGraphArgs)).thenReturn(goModGraphOutput());

        GoModGraphParser goModGraphParser = new GoModGraphParser(new ExternalIdFactory());
        GoModCliExtractor goModCliExtractor = new GoModCliExtractor(executableRunner, goModGraphParser);

        boolean wasSuccessful = true;
        Extraction extraction = goModCliExtractor.extract(directory, goExe);
        if (extraction.getError() instanceof ArrayIndexOutOfBoundsException) {
            wasSuccessful = false;
        }

        Assertions.assertTrue(wasSuccessful);
    }

    private ExecutableOutput goListOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "git.daimler.com/c445/t1"
        ));
        return new ExecutableOutput("", 0, standardOutput, "");
    }

    private ExecutableOutput goListJsonOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "{",
            "\t\"Path\": \"github.com/codegangsta/negroni\",",
            "\t\"Version\": \"v1.0.0\",",
            "\t\"Replace\": {",
            "\t\t\"Path\": \"github.com/codegangsta/negroni\",",
            "\t\t\"Version\": \"v2.0.0\"",
            "\t}",
            "}",
            "",
            "{",
            "\t\"Path\": \"github.com/sirupsen/logrus\",",
            "\t\"Version\": \"v1.1.1\",",
            "\t\"Replace\": {",
            "\t\t\"Path\": \"github.com/sirupsen/logrus\",",
            "\t\t\"Version\": \"v2.0.0\"",
            "\t}",
            "}"
        ));
        return new ExecutableOutput("", 0, standardOutput, "");
    }

    private ExecutableOutput goModGraphOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "github.com/codegangsta/negroni@v1.0.0 github.com/sirupsen/logrus@v1.1.1"
        ));
        return new ExecutableOutput("", 0, standardOutput, "");
    }
}
