package ProjectDraft;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.util.Arrays;
import java.lang.Math;
import java.util.ArrayList;

import static HAL.Util.*;

class Cell1 extends SphericalAgent2D<Cell1, FinalModelTumor>{
    int type;
    double forceSum;
    double resistance;
    public void Init(int color) {
        this.type = color;
        if (type == FirstModelTumor.PACC) {
            this.radius = 0.5;
        } else if (type == FirstModelTumor.ANEUPLOID) {
            this.radius = 0.25;
        }
    }

    double ForceCalc(double overlap, Cell1 other){
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(G.RADIUS*2, this::ForceCalc);
    }
    public boolean CanDivide(double div_bias,double inhib_weight){
        return G.rn.Double()<Math.tanh(div_bias-forceSum*inhib_weight);
    }

    public void Move() {
        ForceMove();
        ApplyFriction(G.FRICTION);
    }

    public void Div() {
        if ((type == FinalModelTumor.PACC && CanDivide(G.PACC_DIV_BIAS, G.PACC_INHIB_WEIGHT)) || (type == FinalModelTumor.ANEUPLOID && CanDivide(G.ANEU_DIV_BIAS, G.ANEU_INHIB_WEIGHT))) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(type);
        }
    }

    public void Die() {
        Dispose();
    }

    public void Mutation() {
        boolean mutated = G.rn.Bool();
        if(!mutated) {
            return;
        } else if(mutated) {
            double favorability = G.gaussian.Sample(0.5, 1, new Rand());
            if(favorability < 0.7) {
                mutated = false;
            } else if(favorability > 0.7) {
                mutated = true;
                double resistanceAdded = G.rn.Double(1);
                G.totalResistance = G.totalResistance + resistanceAdded;
            }
        }
    }
}

public class FinalModelTumor extends AgentGrid2D<Cell1> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int PACC = RGB256(60, 179, 113);
    static final int ANEUPLOID = RGB256(65, 105, 225);
    static int CYTOPLASM = RGB256(255,228,225);
    double RADIUS = 0.5;
    double FORCE_SCALER = 0.25;
    double FRICTION = 0.5;
    double PACC_DIV_BIAS = 0.01;
    double ANEU_DIV_BIAS = 0.02;
    double PACC_INHIB_WEIGHT = 0.02;
    double ANEU_INHIB_WEIGHT = 0.05;
    public static int PACCPop = 0;
    public static int aneuPop = 0;
    ArrayList<Cell1> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand rn = new Rand(0);
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;
    FileIO out;

    public FinalModelTumor(int x, int y, String outFileName) {
        super(x, y, Cell1.class, true, true);

        out = new FileIO(outFileName, "w");
    }


    public static double logisticGrowth(double aneuPop, double PACCPop) {
        double logistic = 0.6*aneuPop*(10000 - aneuPop - 2*PACCPop)/10000;
        if(logistic < 0){
            return 0;
        } else {
            return logistic;
        }
    }

    public static double obligateToPACC(double aneuPop) {
        double obligate = 0.02 * aneuPop;
        return obligate;
    }

    public static double facultativeToPACC(double aneuPop, double drugResistance) {
        double facultative = 0.7 * aneuPop * (1/(1+drugResistance));
        return facultative;
    }

    public static double fromPACC(double PACCPop) {
        double fromPACC = 0.4 * PACCPop;
        return fromPACC;
    }

    public static double deathDueToDrug(double drugDose, double aneuPop, double totalResistance) {
        double death = aneuPop * (drugDose/(1 + totalResistance));
        return death;
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        FinalModelTumor model = new FinalModelTumor(x, y, "PopOut.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("Model Tumor", 750, 750, x, y);
        model.Setup( 500, 5, 0.5);
        while (!vis.IsClosed()) {
            vis.TickPause(0);
            model.StepCells();
            model.Draw(vis);
        }
        if (model.out != null) {
            model.out.Close();
        }
        vis.Close();
    }

    public void Setup(double initPop, double initRadius, double propRed) {
        for (int i = 0; i < initPop; i++) {
            rn.RandomPointInCircle(initRadius, divCoordStorage);
            NewAgentPT(divCoordStorage[0] + xDim / 2.0, divCoordStorage[1] + yDim / 2.0).Init(rn.Double() < propRed ? PACC : ANEUPLOID);
        }
    }

    public void applyDrug(OpenGL2DWindow vis) {
        for(Cell1 cell : this){
            CYTOPLASM = RGB256(255,150,150);;
        }
    }
    public void Draw(OpenGL2DWindow vis) {
        vis.Clear(WHITE);
        for (Cell1 cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius,CYTOPLASM);
        }
        for (Cell1 cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }

    public void StepCells() {
        PACCPop = 0;
        aneuPop = 0;
        for (Cell1 cell : this) {
            if (cell.type == PACC) {
                PACCPop++;
            } else {
                aneuPop++;
            }
        }

        for (Cell1 cell : this) {
            cell.CalcMove();
            cell.Mutation();
        }

        for (Cell1 cell : this) {
            cell.Move();
            double[] eventProbabilities = {logisticGrowth(aneuPop, PACCPop), obligateToPACC(aneuPop), facultativeToPACC(aneuPop, 5), fromPACC(PACCPop)};
            double sum = 0;
            for(int i = 0; i < eventProbabilities.length; i++){
                sum = sum + eventProbabilities[i];
            }
            double[] eventPercentages = new double[eventProbabilities.length];
            for(int w = 0; w < eventPercentages.length; w++) {
                eventPercentages[w] = (eventProbabilities[w]/sum);
            }
            double[] eventPercentagesSorted = new double[eventPercentages.length];
            for(int r = 0; r < eventPercentagesSorted.length; r++) {
                eventPercentagesSorted[r] = eventPercentages[r];
            }
            Arrays.sort(eventPercentagesSorted);
            double event = rn.Double(1);
            System.out.println("Percentages: " + Arrays.toString(eventPercentages));
            System.out.println("Random: " + event);
            double event2 = 0;
            for(int f = 0; f < eventPercentagesSorted.length; f++) {
                if(event < eventPercentagesSorted[f]) {
                    event2 = eventPercentagesSorted[f];
                    break;
                } else if(event2 > eventPercentagesSorted[3]){
                    event2 = 0;
                }
            }
            for(int e = 0; e < eventPercentages.length; e++) {
                if(event2 == eventPercentages[e]) {
                    event2 = e;
                }
            }
            boolean logistic = event2 == 0;
            boolean obligate = event2  == 1;
            boolean facultative = event2 == 2;
            boolean depolyploidize = event2 == 3;
            boolean deathDueToDrug = event2 == 4;
            System.out.println(event2);
            if ((cell.type == ANEUPLOID) && (cell.CanDivide(ANEU_DIV_BIAS, ANEU_INHIB_WEIGHT))) {
                if(logistic){
                    cell.Div();
                } else if((obligate)||(facultative)){
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(PACC);
                } else if(deathDueToDrug) {
                    cell.Die();
                }
            } else if ((cell.type == PACC) && (cell.CanDivide(PACC_DIV_BIAS, PACC_INHIB_WEIGHT))) {
                if(depolyploidize){
                    cell.Die();
                    NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ANEUPLOID);
                    NewAgentPT(cell.Xpt()+0.25, cell.Ypt()).Init(ANEUPLOID);
                }
            }
        }
        if(out!=null){
            //if an output file has been generated, write to it
            RecordOut(out);
        }
    }

    public void RecordOut (FileIO writeHere){
        int ctPACC = 0, ctAneu = 0;
        for (Cell1 cell : this) {
            if (cell.type == PACC) {
                ctPACC++;
            } else {
                ctAneu++;
            }
        }
        writeHere.Write(ctAneu + "," + ctPACC + "\n");
    }
}