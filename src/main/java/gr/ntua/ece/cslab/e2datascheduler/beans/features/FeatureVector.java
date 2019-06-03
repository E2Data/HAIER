package gr.ntua.ece.cslab.e2datascheduler.beans.features;

import java.util.ArrayList;
import java.util.List;

public class FeatureVector {

      private double AggOps;
      private double BinOps;
      private double BitBinOps;
      private double CallOps;
      private double GlobalMemAcc;
      private double LoadOps;
      private double LocalMemAcc;
      private double OtherOps;
      private double PrivateMemAcc;
      private double StoreOps;
      private double VecOps;

      public List<Double> getVector(){
          List<Double> vector = new ArrayList<>();
          vector.add(AggOps);
          vector.add(BinOps);
          vector.add(BitBinOps);
          vector.add(CallOps);
          vector.add(GlobalMemAcc);
          vector.add(LoadOps);
          vector.add(LocalMemAcc);
          vector.add(OtherOps);
          vector.add(PrivateMemAcc);
          vector.add(StoreOps);
          vector.add(VecOps);
          return vector;
      }
}
