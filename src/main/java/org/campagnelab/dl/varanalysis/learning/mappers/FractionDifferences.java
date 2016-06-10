package org.campagnelab.dl.varanalysis.learning.mappers;

import org.campagnelab.dl.varanalysis.learning.genotypes.BaseGenotypeCountFactory;
import org.campagnelab.dl.varanalysis.learning.genotypes.GenotypeCountFactory;
import org.campagnelab.dl.varanalysis.learning.iterators.AbstractFeatureMapper;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.dl.varanalysis.storage.RecordReader;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;

/**
 * This is a fraction difference mapper, producing (germline proportion of total counts at base) - (somatic proportion of total counts at this base).
 * Not finished and (neural network should be able to learn this linear combination on its own).
 * @author Remi Torracinta, rct66
 */

public class FractionDifferences extends AbstractFeatureMapper implements FeatureMapper {


    //only implemented for records with 2 samples exactly
    public static final int FRACTION_NORM = 1;
    public int totalCountsGerm;
    public int totalCountsSom;


    public int numberOfFeatures() {
        // we need features for the normal sample and for the tumor sample:

        return MAX_GENOTYPES;
    }

    public void prepareToNormalize(BaseInformationRecords.BaseInformationOrBuilder record, int indexOfRecord) {
        totalCountsGerm = 0;
        totalCountsSom = 0;
        for (int i = 0; i < MAX_GENOTYPES; i++){
            BaseInformationRecords.CountInfo germline = record.getSamples(0).getCounts(i);
            BaseInformationRecords.CountInfo somatic = record.getSamples(1).getCounts(i);
            totalCountsGerm += (germline.getGenotypeCountForwardStrand() + somatic.getGenotypeCountReverseStrand());
            totalCountsSom += (somatic.getGenotypeCountForwardStrand() + germline.getGenotypeCountReverseStrand());
        }
    }


    int[] indices = new int[]{0, 0};

    public void mapFeatures(BaseInformationRecords.BaseInformationOrBuilder record, INDArray inputs, int indexOfRecord) {
        indices[0] = indexOfRecord;
        for (int featureIndex = 0; featureIndex < numberOfFeatures(); featureIndex++) {
            indices[1] = featureIndex;
            inputs.putScalar(indices, produceFeature(record, featureIndex));
        }
    }

    public float produceFeature(BaseInformationRecords.BaseInformationOrBuilder record, int featureIndex) {
        return normalize(produceFeatureInternal(record, featureIndex), FRACTION_NORM);
    }


    private float normalize(float value, int normalizationFactor) {
        if (normalizationFactor == 0) {
            return 0;
        }
        float normalized = value / normalizationFactor;
        assert normalized >= 0 && normalized <= 1 : "value must be normalized: " + normalized;
        return normalized;
    }


    public float produceFeatureInternal(BaseInformationRecords.BaseInformationOrBuilder record, int featureIndex) {
        assert featureIndex >= 0 && featureIndex < MAX_GENOTYPES: "Only MAX_GENOTYPES features";
        BaseInformationRecords.CountInfo somatic = record.getSamples(0).getCounts(featureIndex);
        BaseInformationRecords.CountInfo germline = record.getSamples(1).getCounts(featureIndex);
        int germCounts = (somatic.getGenotypeCountForwardStrand() + somatic.getGenotypeCountReverseStrand());
        int somCounts = (germline.getGenotypeCountForwardStrand() + germline.getGenotypeCountReverseStrand());
        return normalize(germCounts,totalCountsGerm) - normalize(somCounts,totalCountsSom);
    }

    @Override
    protected GenotypeCountFactory getGenotypeCountFactory() {
        return new BaseGenotypeCountFactory() {

            @Override
            public GenotypeCount create() {
                return new FractionGenotypeCount();
            }
        };
    }


    @Override
    protected void initializeCount(BaseInformationRecords.CountInfo sampleCounts, GenotypeCount count) {
//        int bothCounts = (sampleCounts.getGenotypeCountForwardStrand() + sampleCounts.getGenotypeCountReverseStrand());
//        FractionGenotypeCount myCount = (FractionGenotypeCount) count;
//        myCount.set(avgQuality(RecordReader.expandFreq(sampleCounts.getQualityScoresForwardStrandList())));
    }
}
