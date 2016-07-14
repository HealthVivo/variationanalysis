package org.campagnelab.dl.varanalysis.learning;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.dl.model.utils.mappers.*;
import org.campagnelab.dl.varanalysis.learning.architecture.*;
import org.campagnelab.dl.varanalysis.learning.iterators.*;
import org.campagnelab.dl.varanalysis.storage.RecordWriter;
import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.ScoreImprovementEpochTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Train a neural network to predict mutations. Implement early stopping, using a validation set to measure performance
 * after each training epoch completes.
 * <p>
 *
 */
public class TrainSomaticModelEarlyStopping extends SomaticTrainer {
    public static final int MIN_ITERATION_BETWEEN_BEST_MODEL = 1000;
    static private Logger LOG = LoggerFactory.getLogger(TrainSomaticModel.class);

    protected  String validationFile;

    public TrainSomaticModelEarlyStopping(String validationFilePath) {
            this.validationFile=RecordWriter.addParqExtension(validationFilePath);
    }

    public static void main(String[] args) throws IOException  {
        if (args.length < 2) {
            System.err.println("usage: DetectMutations <input-validation-file> <input-training-directory>");
            System.exit(1);
        }
        String otherArgs[]=new String[args.length-1];
        System.arraycopy(args,0, otherArgs,0, otherArgs.length);
        TrainSomaticModelEarlyStopping trainer=new TrainSomaticModelEarlyStopping(args[0]);
        System.out.println("Early stopping using validation="+args[0]);
        trainer.execute(new FeatureMapperV15(), otherArgs);
    }

    public  void execute(FeatureMapper featureCalculator, String[] trainingFiles) throws IOException {


        List<String> trainIterList = new ObjectArrayList<String>(trainingFiles.length - 1);
        for (int i = 1; i < trainingFiles.length; i++) {
            trainIterList.add(RecordWriter.addParqExtension(trainingFiles[i]));
        }

        super.execute(featureCalculator, trainIterList.toArray(new String[trainIterList.size()]),32);

        ModelPropertiesHelper mpHelper=new ModelPropertiesHelper();
        mpHelper.setFeatureCalculator(featureCalculator);
        mpHelper.setLearningRate(learningRate);
        mpHelper.setNumHiddenNodes(numHiddenNodes);
        mpHelper.setMiniBatchSize(miniBatchSize);
        mpHelper.setBestScore(bestScore);
        mpHelper.setNumEpochs(numEpochs);
        mpHelper.setNumTrainingSets(trainingFiles.length);
        mpHelper.setTime(time);
        mpHelper.setSeed(seed);
        mpHelper.writeProperties(directory);


        System.out.println("Model completed, saved at time: " + attempt);

    }
    protected EarlyStoppingResult<MultiLayerNetwork> train(MultiLayerConfiguration conf, DataSetIterator async) throws IOException {
        net.setListeners(new ScoreIterationListener(100));
        EarlyStoppingConfiguration<MultiLayerNetwork> esConf = new EarlyStoppingConfiguration.Builder()
                .epochTerminationConditions(new MaxEpochsTerminationCondition(numEpochs),new ScoreImprovementEpochTerminationCondition(earlyStopCondition))
                .scoreCalculator(new DataSetLossCalculator(new BaseInformationIterator(validationFile, miniBatchSize,
                        featureCalculator, labelMapper), true))
                .evaluateEveryNEpochs(1)
                .modelSaver(new LocalFileModelSaver(directory))
                .build();

        EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,conf,async);
        trainer.setListener(new EStatusListener());
        EarlyStoppingResult<MultiLayerNetwork> result = trainer.fit();

        //Print out the results:
        System.out.println("Termination reason: " + result.getTerminationReason());
        System.out.println("Termination details: " + result.getTerminationDetails());
        System.out.println("Total epochs: " + result.getTotalEpochs());
        System.out.println("Best epoch number: " + result.getBestModelEpoch());
        System.out.println("Score at best epoch: " + result.getBestModelScore());



        MultiLayerNetwork bestModel = result.getBestModel();
        double bestScore = bestModel.score();

        FileWriter scoreWriter = new FileWriter(directory + "/bestScore");
        scoreWriter.append(Double.toString(bestScore));
        scoreWriter.close();
        return result;
    }


}