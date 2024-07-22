package Project;

import HAL.GridsAndAgents.SphericalAgent2D;
import HAL.GridsAndAgents.AgentGrid2D;
import HAL.Gui.OpenGL2DWindow;
import HAL.Tools.FileIO;
import HAL.Tools.Internal.Gaussian;
import HAL.Rand;

import java.lang.Math;
import java.util.ArrayList;

import static HAL.Util.*;

class Cell7 extends SphericalAgent2D<Cell7, NonspatialTumor>{
    int type;
    double resistance;
    double forceSum;
    public void Init(int color, double resistance) {
        this.type = color;
        this.resistance = resistance + G.totalResistance;
        if (type == NonspatialTumor.SGM_PACC) {
            this.radius = 0.15;
        } else if (type == NonspatialTumor.SGM_ANEU) {
            this.radius = 0.1;
        } else if (type == NonspatialTumor.ET_PACC) {
            this.radius = 0.15;
        } else if (type == NonspatialTumor.ET_ANEU) {
            this.radius = 0.1;
        }
    }

    double ForceCalc(double overlap, Cell7 other) {
        if(overlap < 0) {
            return 0;
        }
        return G.FORCE_SCALER*overlap;
    }

    public void CalcMove(){
        forceSum = SumForces(.3, this::ForceCalc);
    }

    public boolean CanDivide(double div_bias,double inhib_weight){
        return G.rn.Double()<Math.tanh(div_bias-forceSum*inhib_weight);
    }

    public void Move() {
        ForceMove();
        ApplyFriction(G.FRICTION);
    }

    public void Div() {
        if (type == NonspatialTumor.SGM_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(NonspatialTumor.SGM_PACC, resistance);
        } else if(type == NonspatialTumor.SGM_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(NonspatialTumor.SGM_ANEU, resistance);
        } else if(type == NonspatialTumor.ET_PACC) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(NonspatialTumor.ET_PACC, resistance);
        } else if(type == NonspatialTumor.ET_ANEU) {
            Divide(radius*2.0/3.0, G.divCoordStorage, G.rn).Init(NonspatialTumor.ET_ANEU, resistance);
        }
    }

    public void Die() {
        Dispose();
    }

    public void Mutation() {
        int[] neighborhood = CircleHood(false, radius);
        int options = MapEmptyHood(neighborhood);
        double mutationChance = G.rn.Double(1);
        boolean mutated;
        if ((options < 0) || (NonspatialTumor.deathDueToDrug(NonspatialTumor.drugDose, resistance) > NonspatialTumor.fitnessThreshold)) {
            if (mutationChance < 0.7) {
                mutated = true;
            } else {
                mutated = false;
            }
        } else {
            if (mutationChance < 0.3) {
                mutated = true;
            } else {
                mutated = false;
            }
        }
        if (mutated) {
            double favorability = G.rn.Double(1);
            if (favorability > 0.9) {
                double resistanceAdded = G.rn.Double(100);
                resistance += resistanceAdded;
            }
        }
    }
}

public class NonspatialTumor extends AgentGrid2D<Cell7> {

    static final int WHITE = RGB256(248, 255, 252);
    static final int ET_PACC = RGB256(155, 155, 235);
    static final int ET_ANEU = RGB256(18, 148, 144);
    static final int SGM_PACC = RGB256(5, 47, 95);
    static final int SGM_ANEU = RGB256(170,68,101);
    static int drugCYTOPLASM = RGB256(240,177,177);
    static int CYTOPLASM = RGB256(255,227,217);

    Rand rn = new Rand(System.nanoTime());
    double FORCE_SCALER = .25;
    double FRICTION = 0.5;
    double PACC_DIV_BIAS = 0.02;
    double ANEU_DIV_BIAS = 0.02;
    double PACC_INHIB_WEIGHT = 0.05;
    double ANEU_INHIB_WEIGHT = 0.05;
    public static int time = 0;
    public static double drugDose = 0;
    public static int fitnessThreshold = 50;
    ArrayList<Cell7> neighborList = new ArrayList<>();
    ArrayList<double[]> neighborInfo = new ArrayList<>();
    double[] divCoordStorage = new double[2];
    Rand r3 = new Rand(0);
    Gaussian gaussian = new Gaussian();
    static double totalResistance = 0;
    FileIO out;


    public NonspatialTumor(int x, int y, String outFileName) {
        super(x, y, Cell7.class, true, true);
        out = new FileIO(outFileName, "w");
    }

    public double facultativeToPACC(double drugDose, double totalResistance) {
        return 0.7*(drugDose/(100+totalResistance));
    }

    public static double deathDueToDrug(double drugDose,  double totalResistance) {
        return drugDose/(100 + totalResistance);
    }

    public static void main(String[] args) {
        OpenGL2DWindow.MakeMacCompatible(args);
        int x = 30, y = 30;
        NonspatialTumor model = new NonspatialTumor(x, y, "Non-spatialTumor.csv");
        OpenGL2DWindow vis = new OpenGL2DWindow("Non-spatial Tumor", 700, 700, x, y);
        model.Setup( 2, 2);
        while ((time < 100000000)&&(!vis.IsClosed())) {
            while(time < 700) {
                drugDose = 0;
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                time++;
                System.out.println("time: " + time);

            }
            while(time < 1400) {
                drugDose = 50;
                CYTOPLASM = drugCYTOPLASM;
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                time++;
                System.out.println("time: " + time);
            } while(time < 2500) {
                drugDose = 0;
                CYTOPLASM = RGB256(255,228,225);
                vis.TickPause(0);
                model.Draw(vis);
                model.StepCells();
                time++;
                System.out.println("time: " + time);
            }
        }
        if (model.out != null) {
            model.out.Close();
        }
        vis.Close();
    }

    public void Setup(double initPop, double initRadius) {
        for (int i = 0; i < initPop; i++) {
            double cellType = rn.Double(1);
            double pointx = rn.Double(xDim);
            double pointy = rn.Double(yDim);
            if(cellType < 0.05) {
                NewAgentPT(pointx,pointy).Init(ET_PACC, totalResistance);
            } else if(cellType < 0.1) {
                NewAgentPT(pointx,pointy).Init(SGM_PACC, totalResistance);
            } else if(cellType < 0.55) {
                NewAgentPT(pointx,pointy).Init(SGM_ANEU, totalResistance);
            } else {
                NewAgentPT(pointx,pointy).Init(ET_ANEU, totalResistance);
            }
        }
    }

    public void Draw(OpenGL2DWindow vis) {
        vis.Clear(WHITE);
        for (Cell7 cell : this) {
            vis.Circle(cell.Xpt(),cell.Ypt(),cell.radius,CYTOPLASM);
        }
        for (Cell7 cell : this) {
            vis.Circle(cell.Xpt(), cell.Ypt(), cell.radius / 3, cell.type);
        }
        vis.Update();
    }

    public void StepCells() {

        for(Cell7 cell : this){
            cell.CalcMove();
        } for(Cell7 cell : this) {
            cell.Move();
        }
        for (Cell7 cell : this) {
            double logistic = 0.6;
            double obligate = 0.02;
            double facultative = facultativeToPACC(drugDose, cell.resistance);
            double death = deathDueToDrug(drugDose, cell.resistance);
            double depoly = 0.6;
            double nothing = rn.Double(0.5);

            if(((cell.type == ET_ANEU)||(cell.type == SGM_ANEU))&&(cell.CanDivide(ANEU_DIV_BIAS,ANEU_INHIB_WEIGHT))) {
                if(cell.type == ET_ANEU) {
                    System.out.println("et aneu resistance = " + cell.resistance);
                } else {
                    System.out.println("sgm aneu resistance = " + cell.resistance);
                }
                double[] eventsAneu = {logistic, death, obligate, facultative, nothing};
                double[] eventPercentagesAneu = new double[eventsAneu.length];
                double sum = logistic + obligate + facultative + death + nothing;
                for(int i = 0; i < eventsAneu.length; i++) {
                    eventPercentagesAneu[i] = (eventsAneu[i]/sum);
                }
                double[] eventProbabilitiesAneu = new double[eventsAneu.length];
                eventProbabilitiesAneu[0] = eventPercentagesAneu[0];
                for(int i = 1; i < eventsAneu.length; i++) {
                    eventProbabilitiesAneu[i] = eventProbabilitiesAneu[i-1] + eventPercentagesAneu[i];
                }
                double r = rn.Double(1);

                if (r < eventProbabilitiesAneu[0]) {
                    cell.Mutation();
                    cell.Div();
                } else if((r < eventProbabilitiesAneu[1])&&(eventsAneu[1] != 0)) {
                    cell.Die();
                } else if((r < eventProbabilitiesAneu[2])||(r < eventProbabilitiesAneu[3])) {
                    cell.Die();
                    if(cell.type == ET_ANEU) {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(ET_PACC, cell.resistance);
                    } else {
                        NewAgentPT(cell.Xpt(),cell.Ypt()).Init(SGM_PACC, cell.resistance);
                    }
                } else if(r < eventProbabilitiesAneu[4]) {
                }
            } else if(((cell.type == ET_PACC)||(cell.type == SGM_PACC))&&(cell.CanDivide(PACC_DIV_BIAS,PACC_INHIB_WEIGHT))) {
                double[] eventsPACC = {depoly, nothing};
                double[] eventPercentagesPACC = new double[eventsPACC.length];
                double sum = depoly + nothing;
                for(int i = 0; i < eventsPACC.length; i++) {
                    eventPercentagesPACC[i] = (eventsPACC[i]/sum);
                }
                double[] eventProbabilitiesPACC = new double[eventsPACC.length];
                eventProbabilitiesPACC[0] = eventPercentagesPACC[0];
                for(int i = 1; i < eventsPACC.length; i++) {
                    eventProbabilitiesPACC[i] = eventProbabilitiesPACC[i-1] + eventPercentagesPACC[i];
                }
                double r = rn.Double(1);
                if(r < eventProbabilitiesPACC[0]) {
                    if (cell.type == ET_PACC) {
                        cell.Mutation();
                        cell.Die();
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        if (cell.Xpt() + 0.5 < xDim - 0.5) {
                            NewAgentPT(cell.Xpt() + 0.5, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        } else if (cell.Ypt() + 0.5 < yDim - 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.5).Init(ET_ANEU, cell.resistance);
                        } else if (cell.Xpt() - 0.5 > xDim + 0.5) {
                            NewAgentPT(cell.Xpt() - 0.5, cell.Ypt()).Init(ET_ANEU, cell.resistance);
                        } else if (cell.Ypt() - 0.5 > yDim + 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.5).Init(ET_ANEU, cell.resistance);
                        }
                    } else if (cell.type == SGM_PACC) {
                        cell.Mutation();
                        double resistanceThreshold = 10;
                        if (cell.resistance > resistanceThreshold) {
                            cell.Die();
                            NewAgentPT(cell.Xpt(), cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                            if (cell.Xpt() + 0.5 < xDim - 0.5) {
                                NewAgentPT(cell.Xpt() + 0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                            } else if (cell.Ypt() + 0.5 < yDim - 0.5) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() + 0.5).Init(SGM_ANEU, cell.resistance);
                            } else if (cell.Xpt() - 0.5 > xDim + 0.5) {
                                NewAgentPT(cell.Xpt() - 0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                            } else if (cell.Ypt() - 0.5 > yDim + 0.5) {
                                NewAgentPT(cell.Xpt(), cell.Ypt() - 0.5).Init(SGM_ANEU, cell.resistance);
                            }
                        }
                    } else {
                        cell.Die();
                        NewAgentPT(cell.Xpt(), cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        if (cell.Xpt() + 0.5 < xDim - 0.5) {
                            NewAgentPT(cell.Xpt() + 0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        } else if (cell.Ypt() + 0.5 < yDim - 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt() + 0.5).Init(SGM_ANEU, cell.resistance);
                        } else if (cell.Xpt() - 0.5 > xDim + 0.5) {
                            NewAgentPT(cell.Xpt() - 0.5, cell.Ypt()).Init(SGM_ANEU, cell.resistance);
                        } else if (cell.Ypt() - 0.5 > yDim + 0.5) {
                            NewAgentPT(cell.Xpt(), cell.Ypt() - 0.5).Init(SGM_ANEU, cell.resistance);
                        }
                    }

                } else if(r < eventProbabilitiesPACC[1]) {

                }
            }
        }
        if(out!=null){
            //if an output file has been generated, write to it
            RecordOutSize(out);
        }
    }

    public void RecordOutSize (FileIO writeHere){
        int ctPACCSGM = 0, ctPACCET = 0, ctAneuSGM = 0, ctAneuET = 0;

        for (Cell7 cell : this) {
            if (cell.type == ET_PACC) {
                ctPACCET++;
            } else if(cell.type == SGM_PACC){
                ctPACCSGM++;
            } else if(cell.type == ET_ANEU) {
                ctAneuET++;
            } else if(cell.type == SGM_ANEU){
                ctAneuSGM++;
            }
        }
        writeHere.Write(time + " , " + ctPACCET + ", " + ctPACCSGM + ", " + ctAneuET + ", " + ctAneuSGM + "\n");
    }
}