package org.campagnelab.dl.genotype.learning.domains;

import org.campagnelab.dl.framework.domains.prediction.PredictionInterpreter;

import org.campagnelab.dl.genotype.predictions.GenotypePrediction;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Interprets the outout of NumDistinctAllelesLabelMapper.
 * Created by fac2003 on 12/20/16.
 */
public class NumDistinctAllelesInterpreter implements PredictionInterpreter<BaseInformationRecords.BaseInformation, NumDistinctAlleles> {

    int ploidy;

    public NumDistinctAllelesInterpreter(int ploidy) {
        this.ploidy = ploidy;
    }

    @Override
    public NumDistinctAlleles interpret(INDArray trueLabels, INDArray[] outputs, int predictionIndex) {
        throw new RuntimeException("Not implemented.");
    }

    @Override
    public NumDistinctAlleles interpret(BaseInformationRecords.BaseInformation record, INDArray output) {
        NumDistinctAlleles result = new NumDistinctAlleles();
        final String trueGenotype = record.getTrueGenotype();
        result.trueValue = GenotypePrediction.alleles(trueGenotype).size();
        result.inspectRecord(record);
        // interpret the prediction
        double maxProbability = -1;
        int maxIndex = -1;
        int predictionIndex = 0;
        for (int i = 0; i < ploidy; i++) {
            double outputDouble = output.getDouble(predictionIndex, i);
            if (maxProbability < outputDouble) {
                maxIndex = i;
                maxProbability = outputDouble;
            }
        }
        result.predictedValue = maxIndex + 1;
        return result;
    }
}