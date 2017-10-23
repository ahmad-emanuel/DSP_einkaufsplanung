/*
Version 4: das subtour problem ist behoben
           die objektivfunktion Problem im Fall der Infisibilitat ist behoben
*/

import gurobi.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jxl.*;
import jxl.read.biff.BiffException;



public class DSPProject extends GRBCallback {
    private final GRBVar[][] route;
    private final GRBVar[] node;

    public DSPProject(GRBVar[][] xvars , GRBVar[] xnode) {
        route = xvars;
        node = xnode;
    }
    
    // Subtour elimination callback.  Whenever a feasible solution is found,
    // find the subtour that contains node 0, and add a subtour elimination
    // constraint if the tour doesn't visit every node.

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                // Found an integer feasible solution - does it visit every node?
                
                double [] a = getSolution(node);
                int n = 0;
                for (int i = 0; i < a.length; i++) {
                    if (Math.round(a[i]) == 1.0)
                        n++;
                }
                int[] tour = findsubtour(getSolution(route));
                System.out.println(Arrays.toString(tour)+"\nn= " + n +"\n");

                if (tour.length < n) {
                    // Add subtour elimination constraint
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int i = 0; i < tour.length; i++)
                      for (int j = i+1; j < tour.length; j++)
                        expr.addTerm(1.0, route[tour[i]][tour[j]]);
                    addLazy(expr, GRB.LESS_EQUAL, tour.length-1);
                }
            }
        } catch (GRBException e) {
          System.out.println("Error code: " + e.getErrorCode() + ". " +
              e.getMessage());
          e.printStackTrace();
        }
    }
  
    public static void main(String[] args) {
        
        float [][] c ;
        float [][] d ;
        String [] artikeln = {"Äpfel 1kg","Zitrone 2 Stück","Rispentomaten 500g","Paprika 500g",
                              "Speisemöhren 1kg","Bananen 1kg","Limetten 4 Stück","Milch 4L","Margarine 500g",
                              "Käse 150g","Salami","Rapsöl 750ml","Marmelade","Frischkäse 125g",
                              "Joghurt 450g","Nutella 380g","Toast","Brot","Funny-Frisch Chipsfrisch",
                              "Kinder Schokolade 125g","Haribo 200g","Prinzenrolle","Coca Cola 1L","Carolinen Medium 12L",
                              "Carolinen Apfelschorle","Krombacher 4L"};
        
        String [] Filialen = {"Haus","REWE, 1. Filiale","REWE, 2. Filiale","REWE, 3. Filiale","REWE, 4. Filiale","Aldi, 1. Filiale",
                             "Aldi, 2. Filiale","Aldi, 3. Filiale","Aldi, 4. Filiale","Lidl, 1. Filiale","Lidl, 2. Filiale",
                             "Lidl, 3. Filiale","Netto, 1. Filiale","Netto, 2. Filiale","Netto, 3. Filiale","Edeka","Marktkauf",
                             "Kaufland","Real"};
        //Einkaufzettel
        int [] e = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        //int [] e = {1,1,1,1,1,1,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
        //hochste Zahl der bettrofenen Supermaerkte
        int markts = 6;
        int [] start = {0,0};
        int [] end = {25,18};
        d = ConverttoFloat ("Einkaufsliste.xls", 0 , start , end);
        d = Transpose(d);
        end [0] = 18;
        end [1] = 18;
        c = ConverttoFloat ("Entfernungsmatrix.xls", 0 , start , end);
        c = Transpose (c);
        //Entrernung dreifach multipeyieren
        for (int i = 0; i < c[0].length; i++) {
            for (int j = 0; j < c.length; j++) {
                c[i][j]=3*c[i][j];
            }
        }
        int n;
        int m;

        if (c.length == c[0].length && e.length == d[0].length && c.length == d.length) {
            n = c.length;
            m = d[0].length;
        }
        else {
            m = n = 0;
            System.out.println("Error: The matrix of distance must be symetry, or the dimmension of d don't match to shopping list " + "m="+ m + "n="+ n);
            System.exit(1);                      
        }
            
        try {    
            GRBEnv   env   = new GRBEnv("DSP projct.log");
            GRBModel model = new GRBModel(env);
            
            // Must set LazyConstraints parameter when using lazy constraints
            model.getEnv().set(GRB.IntParam.LazyConstraints, 1);
            
            // create variables
            
            GRBVar [][] x = new GRBVar [n][n];
            GRBVar [][] y = new GRBVar [n][m];
            GRBVar [] a = new GRBVar [n];
            
            for (int i = 0; i < n; i++)
                for (int j = 0; j <= i; j++) {
                    x [i][j] = model.addVar(0.0, 1.0, c[i][j],
                                            GRB.BINARY,
                                            "x"+String.valueOf(i)+"_"+String.valueOf(j));
                    x [j][i] = x [i][j];
                }
            
            for (int i = 0; i < n; i++)
                for (int j = 0; j < m; j++) {
                    y [i][j] = model.addVar(0.0, 1.0, d[i][j],
                                            GRB.INTEGER,
                                            "y"+String.valueOf(i)+"_"+String.valueOf(j));
                }
            
            for (int i = 0; i < n; i++) {
                a [i] = model.addVar(0.0, 1.0, 0.0,
                                            GRB.BINARY,
                                            "a"+String.valueOf(i));
            }
            
            // The objective is to minimize the total pay costs
            model.set(GRB.IntAttr.ModelSense, 1);
            
            // Update the model to integrate variables
            model.update();
            
            // (HOME) define constraint 1: ∑(i=0)(j∈V) xij=2  
            
            GRBLinExpr expr = new GRBLinExpr();
            for (int j = 0; j < n; j++)
                expr.addTerm(1.0, x [0][j]);
            model.addConstr(expr, GRB.EQUAL, 2.0, "deg2_Home");
            
            
            // define constraint 2: ∑(j∈V) xij= 2*ai  ∀ i∈V.
            
            for (int i = 0; i < n; i++) {
                expr = new GRBLinExpr();
                GRBLinExpr temp = new GRBLinExpr();
                temp.addTerm(2.0, a[i]);
                for (int j = 0; j < n; j++)
                    expr.addTerm(1.0, x[i][j]);
                model.addConstr(expr, GRB.EQUAL, temp , "deg2_node "+String.valueOf(i));
            }
            
            
            // define constraint 3: ∑(k) yjk <= M*a  ∀ j∈V.
            
            for (int i = 1; i < n; i++) {
                expr = new GRBLinExpr();
                for (int j = 0; j < m; j++)
                    expr.addTerm(1.0, y[i][j]);
                GRBLinExpr temp = new GRBLinExpr();
                temp.addTerm(100000000, a[i]);
                model.addConstr(expr, GRB.LESS_EQUAL , temp ,
                                            "Y"+String.valueOf(i) + "<= M*a"+String.valueOf(i));
            }
            
            // define constraint 3: ∑(k) yjk >= a  ∀ j∈V.
            
            for (int i = 1; i < n; i++) {
                expr = new GRBLinExpr();
                for (int j = 0; j < m; j++)
                    expr.addTerm(1.0, y[i][j]);
                model.addConstr(expr, GRB.GREATER_EQUAL , a[i] ,
                                            "Y"+String.valueOf(i) + ">= a"+String.valueOf(i));
            }
            
            // define constraint 4: ∑(k) yjk = e  ∀ j∈V.
            
            for (int i = 0; i < m; i++) {
                expr = new GRBLinExpr();
                for (int j = 1; j < n; j++)
                    expr.addTerm(1.0, y[j][i]);
                model.addConstr(expr, GRB.EQUAL , e[i] , "Einkaufzettel"+String.valueOf(i));
            }
            
            // define constraint 5: wie viele supermaerkte hochstens soll besucht werden
            
            expr = new GRBLinExpr ();
            for (int i = 0; i < n; i++)
                expr.addTerm(1.0, a[i]);
            model.addConstr(expr, GRB.LESS_EQUAL , markts , "hochste zahl der supermarkte");
            
            //Forbid edge from node back to itself

            for (int i = 0; i < n; i++)
              x[i][i].set(GRB.DoubleAttr.UB, 0.0);
            
            //Optimization
            model.setCallback(new DSPProject(x,a));
            model.optimize();
            
            //show the Antwort
            
            //Status of model
            int status = model.get(GRB.IntAttr.Status);
            
            //Losung mogkichkeiten
            //wenn ubergebene Losung ist optimal
            if (status == GRB.Status.OPTIMAL) {
                
                System.out.println("<---------------------------------->"+"\n der Prozess ist erfolgreich abgeschlossen\n"
                                  +"<---------------------------------->");
                
                double [] AntwortA;
                AntwortA = model.get(GRB.DoubleAttr.X, a);
                
                double [][] AntwortX = model.get(GRB.DoubleAttr.X, x);
                
                int route [] = findsubtour(AntwortX);
                System.out.print("\ndie ausgewaelte Supermaerkte: ");
                for (int i = 0; i < route.length; i++) {
                    System.out.print("[" + Filialen[route[i]] + "] ");
                    System.out.print("-->");
                }
                System.out.print("[Haus]");
                
                double [][] Antworty;
                Antworty = model.get(GRB.DoubleAttr.X, y);
                System.out.print("\n\nim jeweiligen Supermarkt wird folgender/werden folgende Artikel gekauft:");
                for (int i = 1; i < AntwortA.length; i++) {
                    if (AntwortA[i] == 1){
                        System.out.format("\nim %s : ", Filialen[i]);
                        for (int j = 0; j < m; j++) {
                            if (Antworty [i][j] == 1){
                                System.out.print(artikeln[j]+" , ");
                            }
                        }
                    }
                }
                double obj;
                obj = model.get(GRB.DoubleAttr.ObjVal);
                System.out.format("\n\nder preis betraegt = %2.2f €\n", obj);
            }
            
            //wenn uebergebene Antwort ist unzulaessig
            if ( status == GRB.Status.INFEASIBLE ) {
                //Anpassung der objektivfunktion min obj = ∑(i,j) 2*c(i,j)*x(i,j) + ∑(i,j) d(i,j)*y(i,j)
                expr = new GRBLinExpr();
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        expr.addTerm(2*c[i][j], x[i][j]);
                    }
                    for (int j = 0; j < m; j++) {
                        expr.addTerm(d[i][j], y[i][j]);
                    }
                }
                model.setObjective(expr);
                
                
                //Remove degree 2 constraints
                model.remove(model.getConstrByName("deg2_Home"));
                for (int i = 0; i < n; i++)
                    model.remove(model.getConstrByName("deg2_node "+String.valueOf(i)));
                
                
                // (HOME) define constraint 1: ∑(i=0)(j∈V) xij=1
                expr = new GRBLinExpr();
                for (int j = 0; j < n; j++)
                    expr.addTerm(1.0, x [0][j]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "deg1_Home");
            
            
                // define constraint 2: ∑(j∈V) xij= ai  ∀ i∈V.
                for (int i = 0; i < n; i++) {
                    expr = new GRBLinExpr();                    
                    for (int j = 0; j < n; j++)
                        expr.addTerm(1.0, x[i][j]);
                    model.addConstr(expr, GRB.EQUAL, a[i] , "deg2_node "+String.valueOf(i));
                }
                
                //Optimization
                model.optimize();
                
                System.out.println("<---------------------------------->"+"\n der Prozess ist erfolgreich abgeschlossen\n"
                                    + "Das Modell ist an Unzulaessigkeit angepasst worden\n"
                                    +"<---------------------------------->");
                
                double [] AntwortA;
                AntwortA = model.get(GRB.DoubleAttr.X, a);
                
                double [][] AntwortX = model.get(GRB.DoubleAttr.X, x);
                
                int route [] = findsubtour(AntwortX);
                System.out.print("\ndie ausgewaelte Supermaerkte: ");
                for (int i = 0; i < route.length; i++) {
                    System.out.print("[" + Filialen[route[i]] + "] ");
                    System.out.print("-->");
                }
                System.out.print("[Haus]");
                
                double [][] Antworty;
                Antworty = model.get(GRB.DoubleAttr.X, y);
                System.out.print("\n\nim jeweiligen Supermarkt wird folgender/werden folgende Artikel gekauft:");
                for (int i = 1; i < AntwortA.length; i++) {
                    if (AntwortA[i] == 1){
                        System.out.format("\nim %s : ", Filialen[i]);
                        for (int j = 0; j < m; j++) {
                            if (Antworty [i][j] == 1){
                                System.out.print(artikeln[j]+" , ");
                            }
                        }
                    }
                }
                double obj;
                obj = model.get(GRB.DoubleAttr.ObjVal);
                System.out.format("\n\nder preis betraegt = %2.2f €\n", obj);                
            }
            
            // Dispose of model and environment
                model.dispose();
                env.dispose();    
              
        } catch (GRBException ex) {
            Logger.getLogger(DSPProject.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static float [][] ConverttoFloat (String FileName , int Sheet , int [] StartCell , int [] EndCell){
        
        if (StartCell.length != 2 && EndCell.length != 2 )
            System.out.println("Please give zero base number for start cell and end cell");
        
        Cell [][] a = new Cell [1 + EndCell [0] - StartCell [0]][1 + EndCell [1] - StartCell [1]];
        String [][] b = new String [1 + EndCell [0] - StartCell [0]][1 + EndCell [1] - StartCell [1]];
        float [][] c = new float [1 + EndCell [0] - StartCell [0]][1 + EndCell [1] - StartCell [1]];
        
        try { 
            Workbook workbook;
            workbook = Workbook.getWorkbook(new File(FileName));
            Sheet sheet = workbook.getSheet(Sheet);          
            
                for (int i = StartCell [0]; i <= EndCell [0]; i++) 
                    for (int j = StartCell [1]; j <= EndCell [1]; j++) {
                        a[i][j]= sheet.getCell(j, i);
                        b [i][j]= a[i][j].getContents();
                        b [i][j]= ("".equals(b [i][j]) ) ? "0" : b [i][j] ; 
                        c [i][j] = Float.parseFloat(b[i][j].replace(",", "."));
                    }
                                        
        } catch (IOException | BiffException ex) {
            Logger.getLogger(DSPProject.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return c;
    }
    
    public static void ShowArray (double [][] a ){
        for (int i = 0; i < a.length; i++){ 
                    for (int j = 0; j < a[0].length; j++)
                        System.out.print("[" + (int) a[i][j] + "]" + " ");
                    System.out.println("");
                }
    }
    
    public static float [][] Transpose (float [][] array) {
        //empty or unset array, nothing do to here
        if (array == null || array.length == 0)
        return array;

        int width = array.length;
        int height = array[0].length;

        float [][] array_new = new float [height][width];

        for (int i = 0; i < width; i++)
          for (int j = 0; j < height; j++) {
            array_new[j][i] = array[i][j];
          }
  
    return array_new;
    }
    
    public static int[] findsubtour(double[][] sol){
        if (sol.length != sol[0].length)
            System.out.println("Error: the Route matrix must be symmetry.");
        int n = sol.length;
        int [] tour = new int [n];
        int rowold = 0;
        int rownew = 0;
        int count = 0;
        while(true){
            for (int j = 0; j < n; j++) {
                if (Math.round(sol[rownew][j]) == 1 && j != rowold) {
                tour [count+1] = j;
                rowold = rownew;
                rownew = j;
                break;
                }
            }
            
            count++;
            if (count+1 == n || tour[count] == tour[0] )                
                break;
        }
        for (int i = 1; i < tour.length; i++) {
            if (tour[i] == 0){
                count = i;
                break;
            }
            else
                count = tour.length;
        }
        int [] result = new int [count];
        System.arraycopy(tour, 0, result, 0, count);
        return result;
    }
}