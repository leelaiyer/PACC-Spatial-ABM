package Project.ProjectFirstDraft;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;
import java.lang.Math;
import java.util.ArrayList;
import static HAL.Util.RGB256;

class Cell2 extends SphericalAgent2D<Cell2, FirstModelTumor>{
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

    double ForceCalc(double overlap, Cell2 other){
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
        if ((type == FirstModelTumor.PACC && CanDivide(G.PACC_DIV_BIAS, G.PACC_INHIB_WEIGHT)) || (type == FirstModelTumor.ANEUPLOID && CanDivide(G.ANEU_DIV_BIAS, G.ANEU_INHIB_WEIGHT))) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(type);
        }
    }

    public void Die() {
        Dispose();
    }
}

public class FirstModelTumor extends AgentGrid2D<Cell2> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int PACC = RGB256(60, 179, 113);
    static final int ANEUPLOID = RGB256(65, 105, 225);
    static final int CYTOPLASM = RGB256(255,228,225);
    double RADIUS = 0.5;
    double FORCE_SCALER = 0.25;
    double FRICTION = 0.5;
    double PACC_DIV_BIAS = 0.01;
    double ANEU_DIV_BIAS = 0.02;
    double PACC_INHIB_WEIGHT = 0.02;
    double ANEU_INHIB_WEIGHT = 0.05;
    public static int PACCPop = 0;
    public static int aneuPop = 0;
    public static int PACCPopFinal = 0;
    public static int aneuPopFinal = 0;
    ArrayList<Cell2> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand rn = new Rand(0);
    Gaussian gn = new Gaussian();
    FileIO out;

    public FirstModelTumor(int x, int y) {

        super(x, y, Cell2.class, true, true);
    }

    public FirstModelTumor(int x, int y, String outFileName) {
        super(x, y, Cell2.class, true, true);
        out = new FileIO(outFileName, "w");
    }

    public static int changeInPACCPop(double aneuploidPop, double PACCPop, double drugResistance) {
        double changeInPop2 = 0;
        double obligateFrom2N = 0.014*aneuploidPop;
        double facultativeFrom2N = (0.49*aneuploidPop)/(1+drugResistance);
        double to2N = 0.2*PACCPop;
        changeInPop2 = obligateFrom2N + facultativeFrom2N - to2N;
        if (changeInPop2 < 0){
            return 0;
        } else {
            return (int) changeInPop2;
        }
    }

    public static int changeIn2NPop(double aneuploidPop, double PACCPop, double drugResistance) {
        double changeinPop1 = 0;
        double logisticGrowth = 0.6*aneuploidPop*(10000 - aneuploidPop - 2*PACCPop)/10000;
        double obligateToPACC = 0.02 * aneuploidPop;
        double drugInducedDeath = aneuploidPop*(1/(1+drugResistance));
        double facultativeToPACC = 0.7 * aneuploidPop * (1/(1+drugResistance));
        double fromPACC = 0.4 * PACCPop;

        changeinPop1 = logisticGrowth - obligateToPACC - facultativeToPACC + fromPACC;
        if (changeinPop1 < 0){
            return 0;
        } else {
            return (int) changeinPop1;
        }
    }


    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        //to record output, call the constructor with an output filename
        FirstModelTumor model = new FirstModelTumor(x, y, "PopOut.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("First Model Tumor", 750, 750, x, y);
        model.Setup( 100, 5, 0.5);
        int i = 0;
        while (i < 10000 && !vis.IsClosed()) {
            vis.TickPause(0);
            model.StepCells();
            model.DrawCells(vis);
            i++;
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

    public void DrawCells(OpenGL2DWindow vis) {
        vis.Clear(WHITE);
        for (Cell2 cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius,CYTOPLASM);
        }
        for (Cell2 cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }

    public void StepCells() {
        PACCPop = 0;
        aneuPop = 0;
        for (Cell2 cell : this) {
            if (cell.type == PACC) {
                PACCPop++;
            } else {
                aneuPop++;
            }
        }

        PACCPopFinal = (PACCPop * changeInPACCPop(aneuPop, PACCPop, 5)) + PACCPop;
        aneuPopFinal = aneuPop * changeIn2NPop(aneuPop, PACCPop, 5) + aneuPop;

        for (Cell2 cell : this) {
            cell.CalcMove();
        }

        for (Cell2 cell : this) {
            cell.Move();
            boolean morePACC = PACCPop > PACCPopFinal;
            boolean lessPACC = PACCPop < PACCPopFinal;
            boolean moreAneu = aneuPop > aneuPopFinal;
            boolean lessAneu = aneuPop < aneuPopFinal;
            if ((cell.type == FirstModelTumor.PACC) && (PACCPop != PACCPopFinal)) {
                if (morePACC) {
                    cell.Die();
                    PACCPop--;
                }
                if (lessPACC) {
                    cell.Div();
                    PACCPop++;
                }

            } else if ((cell.type == FirstModelTumor.ANEUPLOID) && (aneuPop != aneuPopFinal)) {
                if (moreAneu) {
                    cell.Die();
                    aneuPop--;

                }
                if (lessAneu) {
                    cell.Div();
                    aneuPop++;
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
        for (Cell2 cell : this) {
            if (cell.type == PACC) {
                ctPACC++;
            } else {
                ctAneu++;
            }
        }
        writeHere.Write(ctAneu + "," + ctPACC + "\n");
    }
}