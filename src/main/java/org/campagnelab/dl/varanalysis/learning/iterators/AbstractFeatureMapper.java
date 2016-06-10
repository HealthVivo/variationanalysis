package org.campagnelab.dl.varanalysis.learning.iterators;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.dl.varanalysis.learning.genotypes.GenotypeCountFactory;
import org.campagnelab.dl.varanalysis.learning.mappers.FeatureMapper;
import org.campagnelab.dl.varanalysis.learning.mappers.GenotypeCount;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;

import java.util.Collections;

/**
 * AbstractFeatureMapper encapsulates behavior common to many feature mappers.
 * Created by fac2003 on 6/3/16.
 *
 * @author Fabien Campagne
 */
public abstract class AbstractFeatureMapper implements FeatureMapper {
    public static final int MAX_GENOTYPES = 5;
    private static final int N_GENOTYPE_INDEX = 6;

    private boolean oneSampleHasTumor(java.util.List<org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords.SampleInfo> samples) {
        for (BaseInformationRecords.SampleInfo sample : samples) {
            if (sample.getIsTumor()) return true;
        }
        return false;

    }

    protected ObjectArrayList<? extends GenotypeCount> getAllCounts(BaseInformationRecords.SampleInfo sample, GenotypeCountFactory factory) {
        return getAllCounts(sample, factory, true);
    }

    protected ObjectArrayList<? extends GenotypeCount> getAllCounts(BaseInformationRecords.SampleInfo sample, GenotypeCountFactory factory, boolean sort) {
        ObjectArrayList<GenotypeCount> list = new ObjectArrayList<>();
        int genotypeIndex = 0;
        for (BaseInformationRecords.CountInfo sampleCounts : sample.getCountsList()) {
            GenotypeCount count = factory.create();
            count.set(sampleCounts.getGenotypeCountForwardStrand(), sampleCounts.getGenotypeCountReverseStrand(),
                    sampleCounts.getToSequence(), genotypeIndex);
            initializeCount(sampleCounts, count);
            list.add(count);
            genotypeIndex++;
        }
        // DO not increment genotypeIndex. It must remain constant for all N bases
        int genotypeIndexFor_Ns = N_GENOTYPE_INDEX;
        // pad with zero until we have 10 elements:
        while (list.size() < MAX_GENOTYPES) {
            final GenotypeCount genotypeCount = getGenotypeCountFactory().create();
            genotypeCount.set(0, 0, "N", genotypeIndexFor_Ns);
            list.add(genotypeCount);

        }
        // trim the list at 5 elements because we will consider only the 5 genotypes with largest total counts:
        list.trim(MAX_GENOTYPES);
        //sort in decreasing order of counts:
        if (sort) Collections.sort(list);
        return list;
    }


    protected abstract void initializeCount(BaseInformationRecords.CountInfo sampleCounts, GenotypeCount count);

    protected ObjectArrayList<? extends GenotypeCount> getAllCounts(BaseInformationRecords.BaseInformationOrBuilder record, boolean isTumor, boolean sort) {
        assert oneSampleHasTumor(record.getSamplesList()) : "at least one sample must have hasTumor=true.";
        for (BaseInformationRecords.SampleInfo sampleInfo : record.getSamplesList()) {
            if (isTumor != sampleInfo.getIsTumor()) continue;
            // a subclass is expected to override getGenotypeCountFactory to provide its own type for Genotype counts:
            return getAllCounts(sampleInfo, getGenotypeCountFactory(), sort);
        }
        throw new InternalError("At least one sample matching isTumor, and one matching not isTumor must be found.");
    }

    protected ObjectArrayList<? extends GenotypeCount> getAllCounts(BaseInformationRecords.BaseInformationOrBuilder record, boolean isTumor) {
        return getAllCounts(record, isTumor, true);
    }

    protected abstract GenotypeCountFactory getGenotypeCountFactory();
}
