package com.blackducksoftware.integration.hub.detect.workflow.diagnostic.report;

import java.util.List;

import com.blackducksoftware.integration.hub.detect.testutils.ObjectPrinter;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.BomToolEvaluation;
import com.blackducksoftware.integration.hub.detect.workflow.diagnostic.DiagnosticReportWriter;

public class ExtractionStateReporter {

    public void writeReport(final DiagnosticReportWriter writer, final List<BomToolEvaluation> bomToolEvaluations) {
        for (final BomToolEvaluation evaluation : bomToolEvaluations) {
            if (evaluation.isExtractable()) {
                writer.writeSeperator();
                writer.writeLine("Bom Tool Name : " + evaluation.getBomTool().getDescriptiveName());
                if (evaluation.getExtractionId() != null) {
                    writer.writeLine("Extraction Id : " + evaluation.getExtractionId().toUniqueString());
                } else {
                    writer.writeLine("No extraction id.");
                }
                ObjectPrinter.printObjectPrivate(writer, null, evaluation.getBomTool());
                writer.writeSeperator();
            }
        }
    }

}