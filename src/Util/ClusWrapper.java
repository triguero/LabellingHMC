
/*
	ClusWrapper.java
	Isaac Triguero Velazquez.

	Created by Isaac Triguero Velazquez  25/11/2014
	Copyright (c) 2008 __MyCompanyName__. All rights reserved.

 */

package Util;

/*

import  keel.Algorithms.Semi_Supervised_Learning.Basic.Fichero;
import keel.Algorithms.Semi_Supervised_Learning.Basic.PrototypeSet;
import keel.Algorithms.Semi_Supervised_Learning.Basic.Prototype;
import keel.Algorithms.Semi_Supervised_Learning.Basic.Utilidades;*/

import Util.tree.GenericTreeNode;
import Util.Fichero;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/*import keel.Algorithms.Semi_Supervised_Learning.utilities.*;
import keel.Dataset.Attribute;
import keel.Dataset.Attributes;
*/




/*
import org.apache.commons.io.FileUtils;

import clus.*;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.tune.CDTTuneFTest;
import clus.ext.ensembles.ClusEnsembleClassifier;
import clus.main.*;
import clus.util.ClusException;
import addon.hmc.HMCAverageSingleClass.HMCAverageSingleClass;
import addon.hmc.HMCConvertToSC.HMCConvertToSC;

import java.util.StringTokenizer;

import jeans.util.cmdline.CMDLineArgs;
*/



/**
 * This class implements the ClusWrapper in a  semi-supervised context. It predicts the classes of unlabeled and test sets with the labeled data.
 * @author triguero
 *
 */

public class ClusWrapper {


	private double [][] probabilities;
	private double [][] actualClasses;
	private double []reliability;

	private double [] CD; // original class distribution of the training data.

	private static String typeData= "TREE";
	private int numberOfClass;
	private int trainSize,valSize, testSize; 
	private ArrayList<String> classes;


	private static double Ftest=1.0;
	private static int Trees=50;
	private static boolean bag=false;

	public static String trainingSTR;
	public static String testSTR;

	public static String currentdir = System.getProperty("user.dir")+"/";

	private String bagSelection ="0";
	private static String evalClasses="EvalClasses = evalclasses.txt\n";

	private long tiempo;

	private double MTO[];
	private boolean exactSearch=true;
	
	private TreeHMC arbolSTO; // tree for STO approaches... in ordre to avoid recomputation for every single threshold.
	
	private double actualLC[];
			
	private PrintStream realSystemOut = System.out;
	private PrintStream realSystemErr = System.err;

	private static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b){
			return;
		}
		@Override
		public void write(byte[] b){
			return;
		}
		@Override
		public void write(byte[] b, int off, int len){
			return;
		}
		public NullOutputStream(){
		}
	}


	public ClusWrapper(int classes){
		this.numberOfClass = classes;
	}

	public ClusWrapper(int classes,  String typeData){
		this.numberOfClass = classes;
		this.typeData = typeData;
	}

	// the classes have to be computed somehow.
	public ClusWrapper(String typeData){
		this.typeData = typeData;
	}

	public double [][] getProb(){
		return probabilities;
	}

	public double [][] getActualClasses(){
		return actualClasses;
	}

	public double [] getRealiability(){
		return reliability;
	}

	public void setFtest(double f){
		this.Ftest=f;
	}

	public void setTress(int t){
		this.Trees=t;
	}

	public void setCurrentDir(String dir){
		this.currentdir = dir;
	}

	public void setEvalClassesFile(String file){
		this.evalClasses=file;
	}
	public void setBag(boolean b){
		this.bag=b;
	}

	public void setBagSelection(String s){
		this.bagSelection=s;
	}

	public void setClassDistribution(double []cd){
		this.CD=cd;
	}
	
	public void setActualClasses(double [][]actual){
		this.actualClasses=actual;
	}
	
	public void setProb(double [][]prob){
		this.probabilities=prob;
	}
	
	
	public void setLabelCaridinality(double []lc){
		this.actualLC=lc;
	}
	
	
	public void setSearchMode(boolean mode){
		this.exactSearch=mode;
	}
	
	
	public int getNumberOfClass(){
		return this.numberOfClass;
	}
	
	public int getTrainSize(){
		return this.trainSize;
	}
	
	public int getTestSize(){
		return this.testSize;
	}
	
	public int getValSize(){
		return this.valSize;
	}
	
	public double [] getClassDistribution(){
		return this.CD;
	}
	
	public static void getSolicitaGarbageColector(){

		try{
			Runtime basurero = Runtime.getRuntime();
			basurero.gc(); 

		}
		catch( Exception e ){
			e.printStackTrace();
		}


	}
	

/*
 * 
 * The way the CDs are computed here corresponds to the results of the paper.
 * This function reads an output file and compute:
 * size.
 * distribution per class.
 */
	public void ObtainDataInfoFromOutFile(String dataName){
		
		String cadena,linea, token;
		StringTokenizer lineas,tokens;

		//System.out.println("Loading file: "+currentdir+dataName+".test.pred.arff"+valOrTest);
	    System.out.println(currentdir+dataName+".out.val");
		cadena = Fichero.leeFichero(currentdir+dataName+".out.val");
		
	    //System.out.println(cadena);
		
		lineas = new StringTokenizer (cadena,"\n\r");
		
		linea = "";
		int c=0;
		while(!linea.equalsIgnoreCase("Training error")){
			linea = lineas.nextToken();
		}
		
		linea = lineas.nextToken();linea = lineas.nextToken();
		//System.out.println("Linea: "+linea);
		String [] chunk=linea.split(": ");
		
		trainSize = Integer.parseInt(chunk[1]);
		
		System.out.println("Number of train examples: "+trainSize);
		
		linea = lineas.nextToken();linea = lineas.nextToken();
		linea = lineas.nextToken();
	
		int numClass=0;

		ArrayList<Double> def= new ArrayList<Double>();
		
		classes=new ArrayList<String>();
		while(!linea.contains("T(")){
			
//			System.out.println(linea);

			String [] chunks=linea.split(",");
			
			classes.add(chunks[0]);
			
			String split[]= chunks[1].split(": ");
			
	//		System.out.println(split[1]);
			
			def.add(Double.parseDouble(split[1]));
			linea = lineas.nextToken();
			numClass++;
		}
		
		System.out.println("NUmber of classes: "+numClass);
		
		this.numberOfClass=numClass;
		
		this.CD = new double[numberOfClass];

		// Save CD
		
		for(int i=0; i<this.numberOfClass; i++){
			
			this.CD[i]= def.get(i);
			System.out.print(CD[i]+ ",");
			//System.out.println(nameClasses.get(i));
		}
		
		
		while(!linea.equalsIgnoreCase("Testing error")){
			linea = lineas.nextToken();
		}
		
		linea = lineas.nextToken();linea = lineas.nextToken();
		chunk=linea.split(": ");
		
		valSize = Integer.parseInt(chunk[1]);
		System.out.println("Number of val examples: "+valSize);

		// Computing class distribution in the VALIDATION SET"!
		linea = lineas.nextToken();linea = lineas.nextToken();
		linea = lineas.nextToken();
		
		//int numClass=0;

		//ArrayList<Double> def= new ArrayList<Double>();
		
		classes=new ArrayList<String>();
		while(!linea.contains("T(")){
			
//			System.out.println(linea);

			String [] chunks=linea.split(",");
			
			classes.add(chunks[0]);
			
			String split[]= chunks[1].split(": ");
			
	//		System.out.println(split[1]);
			
			def.add(Double.parseDouble(split[1]));
			linea = lineas.nextToken();
			//numClass++;
		}
		
		//System.out.println("NUmber of classes: "+numClass);
		
		// Save CD
		
		for(int i=0; i<this.numberOfClass; i++){
			
		//	this.CD[i]= def.get(i);
			//System.out.println(nameClasses.get(i));
		//	System.out.println(CD[i]);
		}
		
		
		// Now read the test to know number of lines:
		
		cadena = Fichero.leeFichero(currentdir+dataName+".out.test");
		lineas = new StringTokenizer (cadena,"\n\r");
		
		linea = "";
		
		while(!linea.equalsIgnoreCase("Testing error")){
			linea = lineas.nextToken();
		}
		linea = lineas.nextToken();linea = lineas.nextToken();
		chunk=linea.split(": ");
		
		testSize = Integer.parseInt(chunk[1]);
			
		System.out.println("Number of test examples: "+testSize);
		
		
		//System.exit(1);
		
				
		
	}
	
	/**
	 * Process the output of Clus for HMC -base. It returns just the probabilities, for separate studies.
	 * @param idAlg
	 * @return predictions matrix
	 * @throws IOException 
	 * 
	 */

	public double[][] obtainProbabilitiesHMCbase(int testSize, String valOrTest, String dataName) throws IOException{


		String cadena,linea, token;
		StringTokenizer lineas,tokens;

		//		System.out.println("Loading file: "+currentdir+dataName+".test.pred.arff"+valOrTest);
		cadena = Fichero.leeFichero(currentdir+dataName+".test.pred.arff"+valOrTest);
		lineas = new StringTokenizer (cadena,"\n\r");

		probabilities = new double [testSize][this.numberOfClass];
		actualClasses = new double [testSize][this.numberOfClass];

		// it is possible that the order of the classes become different in the output file

		int indexes [] = new int [this.numberOfClass];

		linea = "";
		int c=0;
		while(!linea.equalsIgnoreCase("@DATA")){
			linea = lineas.nextToken();

			if(linea.startsWith("@ATTRIBUTE class-a-")){

				String clase = linea.replace("@ATTRIBUTE class-a-", "");
				clase = clase.replace("{1,0}", "");
				clase = clase.trim();

				indexes[c]= c; 
				//  System.out.println(indexes[c]);

				c++;
			}


			// copyin heaer into predi file:

			if(linea.contains("Original-p-")){

				linea=linea.replace("Original-p-", "");

			}

		}  // go ahead until @data

		//  System.out.println("number of classes: "+c);
		int u=0;

		//	  System.out.println("");
		while (lineas.hasMoreTokens()) {// for each unlabeled instance!
			linea = lineas.nextToken();


			//	  System.out.println("\n"+linea);


			tokens = new StringTokenizer (linea,",");

			// first token is the ACTUAL class. NOT NECESSARY TO BE PROCESSED!
			token = tokens.nextToken();

			// Does not considered actual Labels!
			int clase=0;
			//	actualClasses[u][0]=1; // Fix for the root node.
			while(tokens.hasMoreTokens() && clase < this.numberOfClass){  // for each class!
				token = tokens.nextToken();

				actualClasses[u][indexes[clase]] = Double.parseDouble(token);  // i'll keep this time.. I need them!
				//System.out.println(token);

				clase++;
			}			   
			//ArrayList<Integer> toBeAdded = new ArrayList<Integer>();
			clase=0;

			//	probabilities[u][0]=1;
			while(tokens.hasMoreTokens() && clase < this.numberOfClass){  // for each class!
				token = tokens.nextToken();

				//	System.out.println(clase+","+token);

				probabilities[u][indexes[clase]] = Double.parseDouble(token); 

				if(probabilities[u][indexes[clase]]>1) probabilities[u][indexes[clase]]=1;

				clase++;	

			}			
			// the next one is the reliability computed by celine



			token = tokens.nextToken();
			//	System.out.print(u+",");

			u++;

		}
		System.out.println("");


		return probabilities;

	}

	

	public double getThresholdHMC(){

		String cadena = Fichero.leeFichero(currentdir+"config.out.val");
		StringTokenizer lineas = new StringTokenizer (cadena,"\n\r");

		String performance = "";
		String linea = "";
		while(!linea.equalsIgnoreCase("Testing error")){linea = lineas.nextToken();}  // go ahead until Testing error	  


		//till we arrive to the next part of the config.out file!

		//double maxPrecisionRecall = 0.0;
		//double bestCorte=0;

		double maxFmeasure = 0.0;
		double bestCorteFmeasure=0;
		double corte, prec, rec;

		while(!linea.equalsIgnoreCase("Hierarchical error measures")){

			linea = lineas.nextToken();



			if(linea.contains("T(")){
				int start = linea.indexOf('(');
				int end = linea.indexOf(')');

				corte = Double.parseDouble(linea.substring(start+1, end))/100.0;

				StringTokenizer trozos = new StringTokenizer (linea.substring(end)," ");

				trozos.nextToken();trozos.nextToken();trozos.nextToken();
				String aux = trozos.nextToken();


				prec = Double.parseDouble(aux.substring(0, aux.length()-1));

				trozos.nextToken();
				aux = trozos.nextToken();

				rec = Double.parseDouble(aux.substring(0, aux.length()-1));

				// System.out.println(prec+ "* "+ rec +"= "+prec*rec);
				/*
				if(prec*rec > maxPrecisionRecall){
					maxPrecisionRecall= prec*rec;
					bestCorte = corte;
				}*/

				if(prec+rec > 0){

					if((2.*prec*rec)/(prec+rec)>maxFmeasure){
						maxFmeasure= (2.*prec*rec)/(prec+rec);
						bestCorteFmeasure = corte;
					}
				}
			}


		}  	  

		//System.out.println("Best Prec*Rec = "+maxPrecisionRecall + ". Used Threshold: "+bestCorte);
		System.out.println("Best Fmeasure = "+maxFmeasure + ". Used Threshold: "+bestCorteFmeasure);

		//  procesar y calcular le mjor corte!

		return bestCorteFmeasure;
	}


	/**
	 * Compute a Single Threshold for HMC based on the HierarchicalLoss measure. 
	 * 
	 * Assume: probabilities have been previosly calculated!
	 * @return
	 */
	public double getThresholdHMC_hierarchicalLoss(){


		Integer [] cutPoints = {0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58,60,62,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100};

		int testSize = probabilities.length;
		int bestCutPoint=0;
		double lowestHierarchicalLoss=Double.MAX_VALUE;


		for (Integer cP: cutPoints){

			int pre[][]=new int[testSize][numberOfClass];


			// apply cut point to the probabilities:

			for(int i=0; i<testSize; i++){

				Arrays.fill(pre[i],0); // INitially, no one is selected.

				for(int j=0; j<numberOfClass; j++){

					if(probabilities[i][j]>(cP/100.0)){
						pre[i][j]=1;
					}

				}

			}



			// compute the hierarchical loss with this cutPoint.
			double hloss= this.computeHierarchicalLoss(testSize, pre);

			if(hloss<lowestHierarchicalLoss){

				lowestHierarchicalLoss=hloss;
				bestCutPoint=cP;
			}


		}

		System.out.println("Best Cut point: "+bestCutPoint+", "+lowestHierarchicalLoss);


		return bestCutPoint/100.0;


	}
  	

	public double STO(String measure){


		arbolSTO = new TreeHMC(numberOfClass, typeData, classes);

		arbolSTO.preComputeCoefficients();
		arbolSTO.preComputeListsOfParents();
		

		int testSize = probabilities.length;
		//int bestCutPoint=0;
		double bestCutPoint=0;
		double lowestHierarchicalLoss=Double.MAX_VALUE;
		double highestHierarchicalLoss=Double.MIN_VALUE;


		/**
		 * Exahustive search or approximation.
		 */


		TreeSet cutPoints2 = new TreeSet();
		
		
		if(this.exactSearch){
			for(int i=0; i<testSize; i++){
				for(int j=0; j<numberOfClass; j++){
					cutPoints2.add(probabilities[i][j]);
				}
			}
		}else{
			System.out.println("Approximation");
			for (double cP=0;cP<1; cP+=0.01){
				cutPoints2.add(cP);
			}
			
		}



		System.out.println("CutPoints2 size: "+cutPoints2.size());

		//for (Integer cP: cutPoints){

		//for (int cP=0;cP<100; cP++){
		
		for (Object cP: cutPoints2){

			//System.out.print((Double)cP+"\n");
			int pre[][]=new int[testSize][numberOfClass];


			// apply cut point to the probabilities:

			for(int i=0; i<testSize; i++){

				Arrays.fill(pre[i],0); // INitially, no one is selected.

				for(int j=0; j<numberOfClass; j++){

					//if(probabilities[i][j]>(cP/100.0)){ //
					if(probabilities[i][j]>((Double)cP)){ //
						pre[i][j]=1;
					}

				}

			}


			double medida=0;

			if(measure.equalsIgnoreCase("Norm-Hloss")){
				// compute the hierarchical loss with this cutPoint.
				medida= this.computeNormHierarchicalLoss(testSize, pre);
			}else if(measure.equalsIgnoreCase("f-measure")){
				medida = this.computeHierarchicalFmeasure(testSize, pre);
			}else if(measure.equalsIgnoreCase("uniform-Hloss")){
				medida= this.computeHierarchicalLoss(testSize, pre);
			}else if(measure.equalsIgnoreCase("HMC-loss")){
				medida= this.computeHMCLoss(testSize, pre);
			}else if(measure.equalsIgnoreCase("macroWFmeasure")){
				medida = this.computeMacroAverageFmeasure(testSize, pre, true);
			}else if(measure.equalsIgnoreCase("classDistribution")){
				medida = this.computeClassDistributionDistance(testSize, pre);
			}else if(measure.equalsIgnoreCase("labelCardinalities")){
				medida = this.computeLabelCardinalityDistance(testSize, pre);
			}else if(measure.equalsIgnoreCase("combination")){ // combine   class Distribution and LabelCardinaties.
				medida = 0.0*this.computeLabelCardinalityDistance(testSize, pre);
				medida+= 0.0*this.computeClassDistributionDistance(testSize, pre);
				medida+= 1*(1-this.computeHierarchicalFmeasure(testSize, pre));


			}


			if(measure.equalsIgnoreCase("f-measure") || measure.equalsIgnoreCase("macroWFmeasure")  ){
				if(medida>highestHierarchicalLoss){

					highestHierarchicalLoss=medida;
					//bestCutPoint=cP;
					bestCutPoint=(Double)cP;
				}
			}else{
				if(medida<lowestHierarchicalLoss){

					lowestHierarchicalLoss=medida;

					//this.computeNormHierarchicalLoss(testSize, pre);
					//bestCutPoint=cP;
					bestCutPoint=(Double)cP;
				}
			}




		}

		System.out.println("\nBest Cut point: "+bestCutPoint+", "+lowestHierarchicalLoss + ";"+ highestHierarchicalLoss);

		//System.exit(1);

		//return bestCutPoint/100.0;
		return bestCutPoint;

	}


	public double[] MTO(String measure){

		MTO=new double[numberOfClass];


		if (measure.contains("Norm-Hloss")){
			lossMTO(measure);
		}else if(measure.equalsIgnoreCase("f-measure")){  // micro F-measure
			fMeasureMTO();
		}else if(measure.equalsIgnoreCase("HMC-loss")){
			//lossMTO(measure);
			//MTO=MTO_HMC_loss(); //V1 is not optimal at all. it's just a class-by-class procedure.. with top-down hierarchy constraint check.
			MTO=MTO_HMC_loss_v2(); // v2 is not optimal as well.. but, it is much accurate.. it check the whole path at every otpimisation.
		}else if(measure.equalsIgnoreCase("macroWFmeasure")){
			MTO=MTO_weightedMacroFmeasure();
		}

		return MTO;
	}


	/**
	 * Implementation by Pillai et al.
	 * What happens with the hierarchy constraint...
	 * 
	 * Notation:
	 * s_k(x_i) == probabilities[][]
	 * k = 1,..,N, where N is the number of classes;
	 * i = 1,..,n, where n is the number of samples.
	 * 
	 * The scores are sorted. It means: sk(x(i)) <= sk(x(i+1));
	 */
	public void fMeasureMTO(){
		
		//0: WE DONT NEED TO  sort the scores from min, to max for each instance!	

		//1st: initialize the threshold values. ti <- any value in  [s_k(x(0)), s_k(x(1))], k=1, N

		// Since I'm going to use a discrete search, I will initilize it as 0, that is the first cut-point we are going to use.
		// modifies MTO global variable:


		int testSize = probabilities.length;
		

		// Integer [] cutPoints = {0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58,60,62,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100};


		TreeHMC arbol = new TreeHMC(numberOfClass, typeData, classes);

		//arbol.preComputeListsOfParents();
		//	arbol.preComputeListsOfParentsNum();
		arbol.preComputeListsOfChildrenNum();
	//	arbol.preComputeListsOfParentsNum(); // ascendant will be also needed.
		//Attribute[] a = Attributes.getOutputAttributes();


		boolean update;
		double bestFmeasure= Double.MIN_VALUE;


		double minPerclass[] = new double[numberOfClass];
		Arrays.fill(minPerclass,Double.MAX_VALUE);


		double maxPerclass[] = new double[numberOfClass];
		Arrays.fill(maxPerclass,Double.MIN_VALUE);

		// min per class will be needed to explore.
		for(int i=0; i<testSize; i++){
			for(int j=0; j<numberOfClass; j++){

				if(probabilities[i][j]<minPerclass[j]){
					minPerclass[j]=probabilities[i][j];
				}

				if(probabilities[i][j]>maxPerclass[j]){
					maxPerclass[j]=probabilities[i][j];
				}
			}
		}

		for(int j=0; j<numberOfClass; j++){
			if(minPerclass[j]>=1){ // check extreme case
				minPerclass[j]=0;
			}
		}		

		//double minProb=(double)cutPoints2.toArray()[cutPoints2.size()-1];


		System.out.println("Initial threshold verctor.");
		boolean analyzed[] = new boolean[numberOfClass];
		Arrays.fill(analyzed,false);

		for(int i=0; i<numberOfClass; i++){

			if(!analyzed[i]){
				MTO[i]=minPerclass[i];//+0.0001; // to make it strictly higher than > 

				for(int l=0; l<arbol.ListChildrenNum.get(i).size(); l++){

					int hijo= arbol.ListChildrenNum.get(i).get(l);

					if(MTO[hijo]<MTO[i]){
						MTO[hijo]=MTO[i];  //
						minPerclass[hijo]=MTO[i];
						analyzed[hijo]=true;;
						//System.out.println("enforcing hierarchy!");							
					}
				}
			}
			System.out.print(MTO[i]+", ");
		}
		System.out.println("");


		
		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){

				cutPoints2[c].add(minPerclass[c]-0.000001); // To add -infinite 
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add((probabilities[i][c])); // NOTE: only for the analyzed class!
				}
				
				

			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}

			}

			cutPoint[c]= (Object []) cutPoints2[c].toArray();
		}

				
		do{
			update=false;

			for(int i=0; i<numberOfClass; i++){

				double bestTk=MTO[i];

				// arg max Tk>= tk F(t...Tk...tN)
				// Compute F-measure by varying the cutPoint in one class (i), the rest are fixed!

				double tentativeMTO[] = new double[numberOfClass];
				for(int c=0; c<numberOfClass; c++){
					tentativeMTO[c]=MTO[c];
				}

				//for(int cP : cutPoints){
				int pre[][]=new int[testSize][numberOfClass];

			
				int minIndex=Arrays.binarySearch(cutPoint[i], minPerclass[i]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)
				int maxIndex=Arrays.binarySearch(cutPoint[i], maxPerclass[i]);
				if(maxIndex<0) maxIndex= maxIndex*-1-1;

				
				for(int cP=minIndex; cP<=maxIndex && cP<cutPoint[i].length; cP++){
				

					tentativeMTO[i] = (Double)cutPoint[i][cP];//cP;

					// The childrens must have threholds equal or higher than the parent to respect the hierarchy!

					for(int l=0; l<arbol.ListChildrenNum.get(i).size(); l++){

						int hijo= arbol.ListChildrenNum.get(i).get(l);

						if(MTO[hijo]<(Double)cutPoint[i][cP]){
							tentativeMTO[hijo]=(Double)cutPoint[i][cP];  //
							//	System.out.println("enforcing hierarchy for class "+i);							
						}
					}


					// apply cut point to the probabilities:
					for(int m=0; m<testSize; m++){
						Arrays.fill(pre[m],0); // INitially, no one is selected.

						for(int c=0; c<numberOfClass; c++){

							if(probabilities[m][c]>tentativeMTO[c]){ 
								pre[m][c]=1;

							}
						}

					}


					double medida;//=computeHierarchicalFmeasure(testSize,pre);
					//	System.out.println("before hierarchy constraint: "+medida);


					medida=computeHierarchicalFmeasure(testSize,pre);

					//System.out.println("after hierarchy constraint: "+medida);



					if(medida > bestFmeasure){


						bestFmeasure=medida;
						bestTk=(Double)cutPoint[i][cP];;
						
						if(bestTk==1){ // check extreme case
							bestTk=0;
						}


						MTO[i]=(Double)cutPoint[i][cP];;
										
						update =true;
					}
				}

				// update the MTO variable.
				if(update){
					
				//	System.out.println("Class "+i+"; "+bestTk);
					MTO[i]=bestTk;

					
					for(int l=0; l<arbol.ListChildrenNum.get(i).size(); l++){

						int hijo= arbol.ListChildrenNum.get(i).get(l);

						if(MTO[hijo]<bestTk){
							MTO[hijo]=bestTk;  //
							minPerclass[hijo]=bestTk; // change minimum value to investigate
							//		System.out.println("enforcing hierarchy!");							
						}
					}
				}

			}



		}while(update);



	}

	/**
	 * Implemenation by Pillae et al.
	 * The hierarchy constraint is applied (bottom-up or top-down) in the prediction matrix (not in the thresholds)
	 * 
	 * Notation:
	 * s_k(x_i) == probabilities[][]
	 * k = 1,..,N, where N is the number of classes;
	 * i = 1,..,n, where n is the number of samples.
	 * 
	 * The scores are sorted. It means: sk(x(i)) <= sk(x(i+1));
	 */
	public void fMeasureMTO_v1(){
		//0: WE DONT NEED TO  sort the scores from min, to max for each instance!	
		//1st: initialize the threshold values. ti <- any value in  [s_k(x(0)), s_k(x(1))], k=1, N
		// Since I'm going to use a discrete search, I will initilize it as 0, that is the first cut-point we are going to use.
		// modifies MTO global variable:


		int testSize = probabilities.length;
		//		Integer [] cutPoints = {0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58,60,62,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100};


		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		//arbol.preComputeListsOfParents();
		//arbol.preComputeListsOfParentsNum();
		arbol.preComputeListsOfChildrenNum();
		//Attribute[] a = Attributes.getOutputAttributes();


		boolean update;
		double bestFmeasure= Double.MIN_VALUE;


		double minPerclass[] = new double[numberOfClass];
		Arrays.fill(minPerclass,Double.MAX_VALUE);

		// min per class will be needed to explore.
		for(int i=0; i<testSize; i++){
			for(int j=0; j<numberOfClass; j++){

				if(probabilities[i][j]<minPerclass[j]){
					minPerclass[j]=probabilities[i][j];
				}
			}
		}

		//double minProb=(double)cutPoints2.toArray()[cutPoints2.size()-1];

		for(int i=0; i<numberOfClass; i++){
			MTO[i]=minPerclass[i];
		}



		do{
			update=false;

			for(int i=0; i<numberOfClass; i++){

				double bestTk=MTO[i];

				// arg max Tk>= tk F(t...Tk...tN)
				// Compute F-measure by varying the cutPoint in one class (i), the rest are fixed!

				//for(int cP : cutPoints){
				int pre[][]=new int[testSize][numberOfClass];

				for (double cP=minPerclass[i]; cP<1; cP+=0.01){
					//MTO[i]=cP/100.; // I fix the MTO of class 'i' to this value.


					// apply cut point to the probabilities:
					for(int m=0; m<testSize; m++){
						Arrays.fill(pre[m],0); // INitially, no one is selected.

						for(int c=0; c<numberOfClass; c++){
							if(probabilities[m][c]>MTO[c]){ 
								pre[m][c]=1;

							}
						}

						if(probabilities[m][i]>cP){ 
							pre[m][i]=1;
							/*for(int l=0; l<arbol.ListParentsNum.get(i).size(); l++){

								int padre= arbol.ListParentsNum.get(i).get(l);
								pre[m][padre]=1;							
							}*/
						}else
							pre[m][i]=0;
					}


					double medida;

					// BOTTOM-DOWN

					for(int  c=0; c<numberOfClass; c++){
						//		ArrayList<String> lista =;  // compute parents!

						for(int m=0; m<testSize;m++){


							if(pre[m][c]==1){ 

								for(int l=0; l<arbol.ListParentsNum.get(c).size(); l++){

									int padre= arbol.ListParentsNum.get(c).get(l);

									if(pre[m][padre]!=1){ // not necessary to check.. better to assing inmidiately
										//		System.out.println("enforcing hierarchy, example: "+m+"; class: "+c+"; padre: "+padre);
										pre[m][padre]=1;							
									}
								}

							}

						}

					}
					/*
					// TOP-DOWN

    				for(int  c=0; c<numberOfClass; c++){
    			//		ArrayList<String> lista =;  // compute parents!

    					for(int m=0; m<testSize;m++){


    						if(pre[m][c]==0){ 

    							//System.out.println("Mis hijos son: ");
    							for(int l=0; l<arbol.ListChildrenNum.get(c).size(); l++){


    								int hijo= arbol.ListChildrenNum.get(c).get(l);
    								//System.out.print(hijo+", ");

    								//if(pre[m][hijo]==1){ // not necessary to check.. better to assing inmidiately
    									//System.out.println("enforcing hierarchy, example: "+m+"; class: "+c+"; hijo: "+hijo);
    									pre[m][hijo]=0;							
    								//}
    							}

    						}

    					}

    				}
					 */
					medida=computeHierarchicalFmeasure(testSize,pre);

					//System.out.println("after hierarchy constraint: "+medida);



					if(medida > bestFmeasure){
						System.out.println(medida);

						bestFmeasure=medida;
						bestTk=cP;
						update =true;
					}
				}

				// update the MTO variable.
				if(update){
					MTO[i]=bestTk;
				}

			}


			if(update){
				// System.out.println("Go ahead: "+ bestFmeasure);
				/*
				for(int c=0; c<numberOfClass; c++){
					System.out.print(MTO[c]+", ");
				}

				System.out.println("");
				int pre[][]=new int[testSize][numberOfClass];

				for(int m=0; m<testSize; m++){
					Arrays.fill(pre[m],0); // INitially, no one is selected.

					for(int c=0; c<numberOfClass; c++){

						if(probabilities[m][c]>MTO[c]){ 
							pre[m][c]=1;
						}
					}

				}
				for(int  c=0; c<numberOfClass; c++){
    			//		ArrayList<String> lista =;  // compute parents!

    					for(int m=0; m<testSize;m++){

    						if(pre[m][c]==1){ 

    							for(int l=0; l<arbol.ListParents.get(c).size(); l++){

    								int padre= a[0].getNominalValuesList().indexOf(arbol.ListParents.get(c).get(l));

    								//if(pre[m][padre]!=1){ // not necessary to check.. better to assing inmidiately
    								//	System.out.println("enforcing hierarchy");
    									pre[m][padre]=1;							
    								//}
    							}

    						}

    					}


				}
				System.out.println("Checking: "+computeHierarchicalFmeasure(testSize,pre));
				 */
			}

		}while(update);



	}

	/**
	 * It follows a  TOP-DOWN approach!
	 */

	public void lossMTO(String measure){

		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		double bestCutPoint[]=new double[numberOfClass];
		Arrays.fill(bestCutPoint, 0);

		arbol.preComputeCoefficients();
		arbol.preComputeListsOfParents();

		GenericTreeNode root = arbol.treeClasses.getRoot();

		lossRecursive(root, arbol, measure); // we directly save the threshols accross the tree.

		//System.out.println(arbol.treeClasses.toStringWithDepth());

	}

	/**
	 * Save recursively the threshold in the tree structure!
	 * @param node
	 * @return
	 */

	public void lossRecursive(GenericTreeNode node,TreeHMC arbol, String measure){

		List<GenericTreeNode> lista = node.getChildren();

		for (GenericTreeNode child: lista){

			// optimized 

			Pair<String, Double> dato = (Pair<String, Double>) child.getData();

			double threshold = optimizeLossNODE(child, arbol,measure);

			MTO[arbol.getNumClass(dato.getFirst())] = threshold; // save the thresholds

			Pair<String, Double> NewData = new Pair<String, Double>(dato.first(), threshold);

			child.setData(NewData);

			lossRecursive(child, arbol,measure);

		} 


	}
	/**
	 * It computes the best threshold for a single node, taking into account its path.
	 */

	public double optimizeLossNODE(GenericTreeNode node,TreeHMC arbol, String measure){

		//Integer [] cutPoints = {0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58,60,62,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100};
		int testSize = probabilities.length;

		double threshold=0;

		Pair<String, Double> dato = (Pair<String, Double>) node.getData();
		int clase =  arbol.getNumClass(dato.getFirst());//Prototype.getMLnumber(dato.getFirst());

		double loss=0;
		double bestCutPoint=0;
		double lowestLoss=Double.MAX_VALUE;

		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2 = new TreeSet();

		
		if(this.exactSearch){
			for(int i=0; i<testSize; i++){
				cutPoints2.add(probabilities[i][clase]); // NOTE: only for the analyzed class!
			}
		}else{
			for (double cP=0;cP<1; cP+=0.01){
				cutPoints2.add(cP);
			}
			
		}

		Object [] cutPoint = (Object []) cutPoints2.toArray();
		

		Pair<String, Double> padre = (Pair<String, Double>) node.getParent().getData();

		if(padre.getFirst().equalsIgnoreCase("root")){ // Standard loss
			for (int cP=0; cP<cutPoint.length && lowestLoss>0; cP++){

				double cut = (Double) cutPoint[cP];
				int pre[]=new int[testSize];

				// apply cut point to the probabilities:
				Arrays.fill(pre,0); // INitially, no one is selected.

				for(int i=0; i<testSize; i++){
					if(probabilities[i][clase]>cut){ //(cP/100.0)
						pre[i]=1;
					}
				}
				
				loss=0;

				
				if(measure.equalsIgnoreCase("HMC-loss")){


					int pos=0, neg=0;
					
					for(int j=0; j< testSize  ; j++){ 
						if(actualClasses[j][clase]==1){
							pos++;
						}else{
							neg++;
						}
					}
					double lambda=(1.*neg)/pos;
					double beta = 2.0/(1+lambda);
					double alpha = 2.0 - beta;
					
					
					double FnegativeCost=0;
					double FpositiveCost=0;
					double coef = arbol.coeffients[clase];

					for(int j=0; j< testSize  ; j++){

						if(pre[j]==0 && actualClasses[j][clase]==1){ // Compute false negatives:
							FnegativeCost+=coef;
						}else if (pre[j]==1 && actualClasses[j][clase]==0){ // False Positive;
							FpositiveCost+=coef;
						}

					}

					loss=FnegativeCost*alpha+FpositiveCost*beta;


				}else{  // standard loss!
					
					for(int i=0; i<testSize;i++){
						if(pre[i]!=actualClasses[i][clase]){
							loss+=1;
						}
					}
				
				}
				
				if(loss<lowestLoss){
					
					lowestLoss=loss;
					//bestCutPoint=cP/100.0;
					bestCutPoint=cut;
					
					if(bestCutPoint==1){
						bestCutPoint=0; // Checking extreme case
					}

				}	

					//System.out.println("Class: "+clase+"; Threshold investigated: "+cP + "; lowestLoss: "+lowestLoss);
			}



			threshold=bestCutPoint;

		}else{

			// apply properly the thresholds

			// we have to compute the H-loss according to the PATH!, so first, determine the path.

			ArrayList<GenericTreeNode> lista = arbol.listOfParents(node);


			// i will save the classes to evaluate and the thresholds here.
			int [] classesToEvaluate = new int [lista.size()+1];
			double [] thresholds = new double [lista.size()+1];

			classesToEvaluate[0] = clase;

			for(int l=0; l<lista.size(); l++){
				Pair<String, Double> path = (Pair<String, Double>) lista.get(l).getData();

				classesToEvaluate[l+1] = arbol.getNumClass(path.getFirst());//Prototype.getMLnumber(path.getFirst());
				thresholds[l+1]=path.getSecond();
			}



			// we now only have to check [threshold of your parent, 1]
			//int numberOfCutOffs = ((int)(thresholds[1]*100))/2;
			
			//int initCutOff = ((int)(thresholds[1]*100));
			double initCutOff = thresholds[1];

			// search for starting point.

			int initialIndex=Arrays.binarySearch(cutPoint, initCutOff);
			if(initialIndex<0) initialIndex=initialIndex*-1-1; // not found, but I don't care, just need the position!

			
			if(initialIndex>=cutPoint.length)
				threshold=thresholds[1];
	 
			for(int cP=initialIndex; cP<cutPoint.length && lowestLoss>0; cP++){
				
				//thresholds[0] = cP/100.0; //this is the variable threshold!
				thresholds[0] =(Double)  cutPoint[cP];
						
				// apply cut point to the probabilities:
				int pre[][]=new int[testSize][classesToEvaluate.length];

				for(int i=0; i<testSize; i++){

					Arrays.fill(pre[i],0); // INitially, no one is selected.

					for(int j=0; j<classesToEvaluate.length; j++){

						if(probabilities[i][classesToEvaluate[j]]>thresholds[j]){ // here is the trick!
							pre[i][j]=1;
						}

					}

				}


				double totalHierarchicalLoss=0;

				if(measure.equalsIgnoreCase("Norm-Hloss")){

					for(int j=0; j< testSize  ; j++){ //

						double hierarchicalLoss=0;

						for(int c=0; c<classesToEvaluate.length; c++){

							if(pre[j][c]!=actualClasses[j][classesToEvaluate[c]]){

								// The parents are already computed! c+1 til classesToEvalute.length-1

								//ArrayList<String> lista2 = arbol.listOfParents(classesToEvaluate[c]);

								boolean equalInParents=true;


								for(int l=c+1; l<classesToEvaluate.length-1  && equalInParents; l++){

									if(pre[j][l]!=actualClasses[j][l]){
										equalInParents=false;
									}
								}

								//double coef = arbol.getCoefficient(c);
								double coef = arbol.coeffients[classesToEvaluate[c]];

								if(equalInParents){ // Equal in its parents but different in this one, count mistake!
									hierarchicalLoss+=coef;
								}

							}

						}

						totalHierarchicalLoss+=hierarchicalLoss;

					}

				}else if(measure.equalsIgnoreCase("HMC-loss")){
					
					/*
					 * Alpha+Beta=2
					 * alpha = lambda*Beta
					 * 
					 * lambda= n-/n+
					 */
					
					double lambda[] = new double[classesToEvaluate.length];
					double alpha[] = new double[classesToEvaluate.length];
					double beta[] = new double[classesToEvaluate.length];

					
					for(int c=0; c<classesToEvaluate.length; c++){
						int neg=0,pos=0;
						
						for(int j=0; j< testSize  ; j++){ 
							if(actualClasses[j][classesToEvaluate[c]]==1){
								pos++;
							}else{
								neg++;
							}
						}
						lambda[c]=(1.*neg)/pos;
						beta[c] = 2.0/(1+lambda[c]);
						alpha[c] = 2.0 - beta[c];
						
						//System.out.println(alpha[c]+","+beta[c]);
					}
					
					 //

				
					for(int c=0; c<classesToEvaluate.length; c++){
						double FnegativeCost=0;
						double FpositiveCost=0;
						double coef = arbol.coeffients[classesToEvaluate[c]];

						for(int j=0; j< testSize  ; j++){

							if(pre[j][c]==0 && actualClasses[j][classesToEvaluate[c]]==1){ // Compute false negatives:
								FnegativeCost+=coef;
							}else if (pre[j][c]==1 && actualClasses[j][classesToEvaluate[c]]==0){ // False Positive;
								FpositiveCost+=coef;
							}

						}

						totalHierarchicalLoss+=FnegativeCost*alpha[c]+FpositiveCost*beta[c];
					}
				
				
				}


				totalHierarchicalLoss/=testSize;




			//	System.out.println("Clase: "+clase+"; Padre: "+classesToEvaluate[1]+";Threshold investigated: "+cP + "; hierarchicalLoss: "+totalHierarchicalLoss);

				if(totalHierarchicalLoss<lowestLoss){

					lowestLoss=totalHierarchicalLoss;
					bestCutPoint= (Double) cutPoint[cP];//cP/100.0;
					
					if(clase==27) System.out.println(bestCutPoint+"; "+lowestLoss);

					if(bestCutPoint==1) bestCutPoint=thresholds[1]; // threshold del padre

				}

				// Some checks:
				/*
				if(lowestLoss==0){

					for(int c=0; c<classesToEvaluate.length; c++){

						System.out.println("Class: "+classesToEvaluate[c]);

						for(int i=0; i<testSize;i++){
							System.out.print(probabilities[i][classesToEvaluate[c]]+",");
						}
						System.out.println("");

						for(int i=0; i<testSize;i++){
							System.out.print(pre[i][c]+",");
						}
						System.out.println("");
						for(int i=0; i<testSize;i++){
							System.out.print(actualClasses[i][classesToEvaluate[c]]+",");
						}
						System.out.println("\n*********************");
					}


					System.exit(1);
				}*/

				threshold=bestCutPoint;

			}

			//System.exit(1);
		}

		
		//System.out.println("****");

		return threshold;

	}


	/**
	 * Compute multiple thresholds for classDistribution, independently per class.
	 * Individual processing makes sense for this approach. Require: post-processing? No, because we check hierarchy,.
	 * Assume:
	 * (1) probabilities have been previosly calculated.
	 * (2) CDdistribution is established as well.
	 * @return
	 */
	public double[] MTOclassDistribution(){

		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		int lowestError[]= new int[numberOfClass];

		Arrays.fill(lowestError,Integer.MAX_VALUE);



		for(int  c=0; c<numberOfClass; c++){

			int positives = (int) Math.round(CD[c]*testSize);


			if(positives>0){

				double [] aOrdenar = new double[testSize];
				int [] position = new int [testSize];

				for(int q=0;q<testSize; q++){  
					aOrdenar[q] =  probabilities[q][c];
					position[q] = q;
				}
				Utilidades.quicksort(aOrdenar, position); // ascending order!

				int laste = testSize -1;

				double infBound;
				double supBound;

				if(positives<=laste ){ 
					infBound=probabilities[position[laste-positives]][c];
					supBound=probabilities[position[laste-positives+1]][c];
				}else{

					// it means, all the examples belogn to such a class.
					infBound=supBound=0;

				}

				bestCutPoint[c]=(infBound+supBound)/2.;
				//System.out.println("Inf="+infBound+"; Sup="+supBound);

				if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
					int padre=arbol.ListParentsNum.get(c).get(0);

					//System.out.println("Padre: "+padre);
					if(bestCutPoint[c] < bestCutPoint[padre]){
						//	System.out.println("Enforcing hierarchy -MTO - CD!");
						bestCutPoint[c]=bestCutPoint[padre];
					}

				}
			}else{
				bestCutPoint[c]=1.0; // in case you don't want to classify anyone.
			}
		}



		return bestCutPoint;


	}

	/**
	 * Label cardinalities per class. Compute multiple thresholds for labelCardinalities option, independently per class.
	 * 
	 * Individual processing makes sense for this approach. 
	 * 
	 * Check the cardinality per class in the validation set... establishes the thrshold accordingly.
	 * 
	 * @return
	 */
	public double[] MTOlabelcardinalities(){

		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		double lowestError[]= new double[numberOfClass];

		Arrays.fill(lowestError,Double.MAX_VALUE);


		int pre[]=new int[testSize];


		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add(probabilities[i][c]); // NOTE: only for the analyzed class!
				}
			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}

			}

			cutPoint[c]= (Object []) cutPoints2[c].toArray();
		}
		

		for(int  c=0; c<numberOfClass; c++){

			int minIndex=0;
			
			if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
				int padre=arbol.ListParentsNum.get(c).get(0);

				minIndex=Arrays.binarySearch(cutPoint[c], bestCutPoint[padre]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)

				bestCutPoint[c]=bestCutPoint[padre];
				
			}
			

		//	for(int cP=0; cP<100; cP++){ // check possible thresholds!
			for (int cP=minIndex; cP<cutPoint[c].length; cP++){
			
			
				Arrays.fill(pre,0); // INitially, no one is selected.

				// create prediction.

				for(int i=0; i<testSize; i++){

					if(probabilities[i][c]> (Double)cutPoint[c][cP]){  //(cP/100.0)
						pre[i]=1;
					}
				}

				double newPerformance=computeLabelCardinalityDistanceSingleClass(testSize, pre, c);
				
				
				if(newPerformance<lowestError[c]){

					lowestError[c]=newPerformance;
					bestCutPoint[c]=(Double)cutPoint[c][cP];//cP/100.0;

					if(bestCutPoint[c]==1)
						bestCutPoint[c]=0; // extreme casse
					

				}

			}

			//System.out.println(lowestError[c]+", "+bestCutPoint[c]);
	}



		return bestCutPoint;


	}

	
	/**
	 * 
	 * V2: this version is a bit different in which when we investigate one node
	 * 
	 * 
	 * 
	 * - We still follow the top-down approach. So, let's say we start the optimisation in node B of Figure 1 of the paper.

The problem you found is that there is a strong interaction between the value found for this node and the threshold values that can be
established in subtree (E,F,I,J,K)

To solve this issue, I propose the following:

- Apply a kind of STO approach for the subtree. I mean, we investigate the all the possible thresholds of all these nodes.
Of course, the minimum cutPoint will be established by the current node (Because we're following a top-down approach).

We try to minimise the following HMCloss =  1/3*HMCloss(B)  + 1/6*HMCloss(E) + 1/6*HMCloss(F) + 1/18*HMCloss(I) + 1/18*HMCloss(J) + 1/18*HMCloss(K)

The threshold found after this process is for sure the minimum value that we can put in nodes E, F, I, J and K.

So, then we keep on doing our top-down approach.

- We would go for node E, and we try to optimise this node from this minimum value found before, to the maximum.
- Then, node F... in this case, we apply this idea recursively.. optimise the STO HMCloss = 1/6*HMCloss(F) + 1/18*HMCloss(I) + 1/18*HMCloss(J) + 1/18*HMCloss(K)
from the minimum value to the max of these four nodes.
- and so on. 

	 * @return
	 */
	public double[] MTOlabelcardinalities_v2(){



		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();
		arbol.preComputeListsOfChildrenNum();

		
		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		double lowestError[]= new double[numberOfClass];

		Arrays.fill(lowestError,Double.MAX_VALUE);
	

		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add(probabilities[i][c]); // NOTE: only for the analyzed class!
				}
			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}

			}

		}
		
		System.out.println("****************");
		// add cutPoints of the childrens of each class for Version 2!!
		for(int c=0; c<numberOfClass; c++){
			
			
			if(cutPoints2[c].size()!=1){ // if we only have one value.. that's an extreme case
			
	
				for(int l=0; l<arbol.ListChildrenNum.get(c).size(); l++){
	//				System.out.print(arbol.ListChildren.get(c)+", ");
					
					int hijo= arbol.ListChildrenNum.get(c).get(l);
					cutPoints2[c].addAll(cutPoints2[hijo]);
					
				}
		//		System.out.println("");
			
			}
			
			cutPoint[c]= (Object []) cutPoints2[c].toArray();

		//	System.out.println("cutPoint[c]: "+cutPoint[c].length);

			
		}
		

		for(int  c=0; c<numberOfClass; c++){

			int minIndex=0;
			
			if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
				int padre=arbol.ListParentsNum.get(c).get(0);

				minIndex=Arrays.binarySearch(cutPoint[c], bestCutPoint[padre]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)

				bestCutPoint[c]=bestCutPoint[padre];
			}
			

		//	for(int cP=0; cP<100; cP++){ // check possible thresholds!
			for (int cP=minIndex; cP<cutPoint[c].length; cP++){
			
			
				// apply cut point to the probabilities:
				// I will analyze myself an all my children at the same time!
				
				int nodesToAnalyze=arbol.ListChildrenNum.get(c).size()+1;
				int pre[][]=new int[nodesToAnalyze][testSize];

				
				for(int j=0; j<nodesToAnalyze-1; j++){
					Arrays.fill(pre[j],0); // INitially, no one is selected.

					for(int i=0; i<testSize; i++){
						if(probabilities[i][arbol.ListChildrenNum.get(c).get(j)]> (Double)cutPoint[c][cP]){ 
							pre[j][i]=1;
						}

					}
				}
				
				Arrays.fill(pre[nodesToAnalyze-1],0);
				// the current class
				for(int i=0; i<testSize; i++){
					if(probabilities[i][c]> (Double)cutPoint[c][cP]){ 
						pre[nodesToAnalyze-1][i]=1;
					}
				}
				
				
				double newPerformance=0;
				
				for(int j=0; j<nodesToAnalyze-1; j++){
					newPerformance+=this.computeLabelCardinalityDistanceSingleClass(testSize, pre[j],arbol.ListChildrenNum.get(c).get(j));
				}
				
				newPerformance+=this.computeLabelCardinalityDistanceSingleClass(testSize, pre[nodesToAnalyze-1],c);
				
				if(newPerformance<lowestError[c]){

					lowestError[c]=newPerformance;
					bestCutPoint[c]=(Double)cutPoint[c][cP];//cP/100.0;

					if(bestCutPoint[c]==1)
						bestCutPoint[c]=0; // extreme casse
					

				}

			}

		}



		return bestCutPoint;


	}
	
	/**
	 * Compute multiple thresholds for HMC-loss option, independently per class.
	 * 
	 * Individual processing makes sense for this approach. 
	 * 
	 * @return
	 */
	public double[] MTO_HMC_loss(){



		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();
		arbol.preComputeCoefficients();

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		double lowestError[]= new double[numberOfClass];

		Arrays.fill(lowestError,Double.MAX_VALUE);


		int pre[]=new int[testSize];


		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add(probabilities[i][c]); // NOTE: only for the analyzed class!
				}
			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}

			}

			cutPoint[c]= (Object []) cutPoints2[c].toArray();
		}
		
		
		
	

		for(int  c=0; c<numberOfClass; c++){ // According to the implmenetation followed, that's top-down!


		//	for(int cP=0; cP<100; cP++){ // check possible thresholds!
			
			
			int minIndex=0;
			
			// 1st: check if you have parent... i am going to search from this threshold...
			
			if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
				int padre=arbol.ListParentsNum.get(c).get(0);

				minIndex=Arrays.binarySearch(cutPoint[c], bestCutPoint[padre]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)

				 bestCutPoint[c]=bestCutPoint[padre]; // this is the minimum threshold we'll have.
					/*if(c==27){ System.out.println("Threshold padre: "+bestCutPoint[padre]+"; cutPoint[c].length="+cutPoint[c].length+"; "+minIndex);
					
					for(int p=0; p<cutPoint[c].length;p++){
						System.out.print(cutPoint[c][p]+", ");
					}	
					
					System.out.println("");
					}*/
			}
			
			
			
			for (int cP=minIndex; cP<cutPoint[c].length; cP++){
			
			
				Arrays.fill(pre,0); // INitially, no one is selected.

				// create prediction.

				for(int i=0; i<testSize; i++){

					if(probabilities[i][c]> (Double)cutPoint[c][cP]){  //(cP/100.0)
						pre[i]=1;
					}
				}


				double newPerformance=this.computeHMCLoss_individually(testSize, pre,c, arbol.coeffients[c],false);//computeLabelCardinalityDistanceSingleClass(testSize, pre, c);


				if(newPerformance<lowestError[c]){

					lowestError[c]=newPerformance;
					bestCutPoint[c]=(Double)cutPoint[c][cP];//cP/100.0;

					if(bestCutPoint[c]==1)
						bestCutPoint[c]=0; // extreme casse
					
				}

			}

		}



		return bestCutPoint;


	}
	
	
	/**
	 * Compute multiple thresholds for HMC-loss option, independently per class.
	 * 
	 * Individual processing makes sense for this approach. 
	 * 
	 * @return
	 */
	public double[] MTO_HMC_loss_v2(){
		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();
		arbol.preComputeListsOfChildrenNum();
		//arbol.preComputeListsOfChildren();
		arbol.preComputeCoefficients();

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		double lowestError[]= new double[numberOfClass];

		Arrays.fill(lowestError,Double.MAX_VALUE);

		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add(probabilities[i][c]); // NOTE: only for the analyzed class!
				}
			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}

			}

			//cutPoint[c]= (Object []) cutPoints2[c].toArray();
			//System.out.println("cutPoint[c]: "+cutPoint[c].length);
		}
		
		// add cutPoints of the childrens of each class
		for(int c=0; c<numberOfClass; c++){
			
			
			if(cutPoints2[c].size()!=1){ // if we only have one value.. that's an extreme case
			
	
				for(int l=0; l<arbol.ListChildrenNum.get(c).size(); l++){
	//				System.out.print(arbol.ListChildren.get(c)+", ");
					
					int hijo= arbol.ListChildrenNum.get(c).get(l);
					cutPoints2[c].addAll(cutPoints2[hijo]);
					
				}
		//		System.out.println("");
			
			}
			
			cutPoint[c]= (Object []) cutPoints2[c].toArray();

//			System.out.println("cutPoint[c]: "+cutPoint[c].length);

			
		}
		
	

		for(int  c=0; c<numberOfClass; c++){ // According to the implmenetation followed, that's top-down!


			
			
			int minIndex=0;
			
			// 1st: check if you have parent... i am going to search from this threshold...
			
			if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
				int padre=arbol.ListParentsNum.get(c).get(0);

				minIndex=Arrays.binarySearch(cutPoint[c], bestCutPoint[padre]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)

				 bestCutPoint[c]=bestCutPoint[padre]; // this is the minimum threshold we'll have.

			}
			
						
			for (int cP=minIndex; cP<cutPoint[c].length; cP++){
			
				// apply cut point to the probabilities:
				// I will analyze myself an all my children at the same time!
				
				int nodesToAnalyze=arbol.ListChildrenNum.get(c).size()+1;
				int pre[][]=new int[nodesToAnalyze][testSize];

				
				for(int j=0; j<nodesToAnalyze-1; j++){
					Arrays.fill(pre[j],0); // INitially, no one is selected.

					for(int i=0; i<testSize; i++){
						if(probabilities[i][arbol.ListChildrenNum.get(c).get(j)]> (Double)cutPoint[c][cP]){ 
							pre[j][i]=1;
						}

					}
				}
				
				Arrays.fill(pre[nodesToAnalyze-1],0);
				// the current class
				for(int i=0; i<testSize; i++){
					if(probabilities[i][c]> (Double)cutPoint[c][cP]){ 
						pre[nodesToAnalyze-1][i]=1;
					}
				}

				double newPerformance=0;
				
				for(int j=0; j<nodesToAnalyze-1; j++){
					newPerformance+=this.computeHMCLoss_individually(testSize, pre[j],arbol.ListChildrenNum.get(c).get(j), arbol.coeffients[arbol.ListChildrenNum.get(c).get(j)],false);
				}
				
				newPerformance+=this.computeHMCLoss_individually(testSize, pre[nodesToAnalyze-1],c, arbol.coeffients[c],false);


				if(newPerformance<lowestError[c]){

					lowestError[c]=newPerformance;
					bestCutPoint[c]=(Double)cutPoint[c][cP];//cP/100.0;

					if(bestCutPoint[c]==1)
						bestCutPoint[c]=0; // extreme casse
					

				}

			}

		}

		return bestCutPoint;

	}
	
	
	/**
	 * Compute multiple thresholds for the macroFmeasure. In this case we use coefficent to weight differently the classes!
	 * In this way we alleiviate the problems of the macro F-measure in HMC problems.
	 * 
	 * Individual processing makes sense for this approach. 
	 * 
	 * @return
	 */
	public double[] MTO_weightedMacroFmeasure(){

		TreeHMC arbol = new TreeHMC(numberOfClass, typeData,classes);

		arbol.preComputeListsOfParentsNum();
		arbol.preComputeCoefficients();

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		double highestFmasure[]= new double[numberOfClass];

		Arrays.fill(highestFmasure,Double.MIN_VALUE);

		int pre[]=new int[testSize];


		/**
		 * Exahustive search or approximation: NOTE: only for the analyzed class!
		 */

		TreeSet cutPoints2[] = new TreeSet[this.numberOfClass];

		Object [][] cutPoint = new Object[this.numberOfClass][];

		for(int c=0; c<numberOfClass; c++){
			cutPoints2[c] = new TreeSet();

			if(this.exactSearch){
				for(int i=0; i<testSize; i++){
					cutPoints2[c].add(probabilities[i][c]); // NOTE: only for the analyzed class!
				}
			}else{
				for (double cP=0;cP<1; cP+=0.01){
					cutPoints2[c].add(cP);
				}
			}
			cutPoint[c]= (Object []) cutPoints2[c].toArray();
		}

		for(int  c=0; c<numberOfClass; c++){ // According to the implmenetation followed, that's top-down!

			int minIndex=0;

			// 1st: check if you have parent... i am going to search from this threshold...

			if(arbol.ListParentsNum.get(c).size()>0){ // if I have a parent node.
				int padre=arbol.ListParentsNum.get(c).get(0);

				minIndex=Arrays.binarySearch(cutPoint[c], bestCutPoint[padre]);
				if(minIndex<0) minIndex= minIndex*-1-1; // not found, but I don't care, just need the position! Position = (-(insertion point) - 1)

				bestCutPoint[c]=bestCutPoint[padre]; // this is the minimum threshold we'll have.
			}



			for (int cP=minIndex; cP<cutPoint[c].length; cP++){


				Arrays.fill(pre,0); // INitially, no one is selected.

				// create prediction.

				for(int i=0; i<testSize; i++){

					if(probabilities[i][c]> (Double)cutPoint[c][cP]){  //(cP/100.0)
						pre[i]=1;
					}
				}


				double newPerformance = arbol.coeffients[c]*this.computeMacroAverageFmeasure_individually(testSize, pre,c); // we don't really need to do this.

				if(newPerformance>highestFmasure[c]){

					highestFmasure[c]=newPerformance;
					bestCutPoint[c]=(Double)cutPoint[c][cP];//cP/100.0;

					if(bestCutPoint[c]==1)
						bestCutPoint[c]=0; // extreme casse

				}

			}

		}

		return bestCutPoint;

	}


	/**
	 * Compute a MULTIPLE Threshold for HMC based on the HierarchicalLoss measure. 
	 * 
	 * Assume: probabilities have been previosly calculated!
	 * @return
	 */

	public double[] getMultipleThresholdHMC_hierarchicalLoss(){


		Integer [] cutPoints = {0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58,60,62,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100};

		int testSize = probabilities.length;
		double bestCutPoint[]=new double[numberOfClass];
		int lowestHierarchicalLoss[]= new int[numberOfClass];

		Arrays.fill(lowestHierarchicalLoss,Integer.MAX_VALUE);


		for (Integer cP: cutPoints){

			int pre[]=new int[testSize];


			// apply cut point to the probabilities:

			//	-> for every class, optimize threshold independently. 
			//Optimal threshold is the one that minimizes hierarchical loss, 
			// but for a single class, the hierarchical loss becomes the loss (?)



			for(int j=0; j<numberOfClass; j++){  //
				Arrays.fill(pre,0); // INitially, no one is selected.

				int hloss=0;
				for(int i=0; i<testSize; i++){

					if(probabilities[i][j]>(cP/100.0)){
						pre[i]=1;

						if(actualClasses[i][j]==0){
							hloss++;
						}
					}
				}

				if(hloss<lowestHierarchicalLoss[j]){

					lowestHierarchicalLoss[j]=hloss;
					bestCutPoint[j]=cP/100.0;
				}



			}

		}

		return bestCutPoint;
	}
	

	public double computeLabelCardinalityDistance(int testSize, int pre[][]){

		// one per instance!
		double predictedLC[] = new double[testSize];
		Arrays.fill(predictedLC,0);


		//count how many classes do we have in every validation example
		for(int j=0; j<testSize; j++){
			for(int i=0; i<numberOfClass; i++){

				if(pre[j][i]==1){
					predictedLC[j]+=1;	
				}
	
			}
		}

		// compute distance to the ACTUAL!!
		double acc = 0.0;
		for (int i = 0; i < actualLC.length; i++)
		{
			acc += ((actualLC[i] - predictedLC[i]) * (actualLC[i] - predictedLC[i]));
		}
		//acc=Math.sqrt(acc);

		return acc;
	}

	public double computeLabelCardinalityDistanceSingleClass(int testSize, int pre[], int clase){

	
		for(int j=0; j<testSize; j++){
			if(actualClasses[j][clase]==1){
				actualLC[j]=1;	
			}else
				actualLC[j]=0;
		}
		
		// compute distance to the ACTUAL!! Hamming distance
		double acc = 0.0;
		for (int i = 0; i < actualLC.length; i++)
		{
			if(actualLC[i]!=pre[i]) //predictedLC[i])
				acc +=1; // (actualLC[i] - predictedLC[i]) * (actualLC[i] - predictedLC[i]);
		}

		//System.out.println(acc);

		return acc;
	}


	public double computeClassDistributionDistance(int testSize, int pre[][]){

		
		double predictedCD[] = new double[numberOfClass];
		Arrays.fill(predictedCD,0);


		for(int i=0; i<numberOfClass; i++){

			for(int j=0; j<testSize; j++){

				if(pre[j][i]==1){
					predictedCD[i]+=1;	
				}
			}

			predictedCD[i]/=testSize;
		}

		double acc = 0.0;
		for (int i = 0; i < predictedCD.length; i++)
		{
			acc += ((predictedCD[i] - CD[i]) * (predictedCD[i] - CD[i])) * arbolSTO.coeffients[i]; // Weighted!
		}
		//acc=Math.sqrt(acc);

		return acc;
	}


	public double computeHierarchicalLoss(int testSize, int pre[][]){

		TreeHMC arbol = new TreeHMC(numberOfClass, typeData, classes);


		arbol.preComputeListsOfParents();

		double totalHierarchicalLoss=0;
		for(int j=0; j< testSize  ; j++){ //

			// compute its hierarchical loss.

			int hierarchicalLoss=0;

			for(int c=0; c<numberOfClass; c++){

				if(pre[j][c]!=actualClasses[j][c]){

					// Compute parents... check they keep the ==

					//	ArrayList<String> lista = arbol.listOfParents(c);
					ArrayList<String> lista = arbol.ListParents.get(c);


					boolean equalInParents=true;
					for(int l=0; l<lista.size() && equalInParents; l++){
						//if(lista.get(l).equalsIgnoreCase("root")) System.err.println("ESTo k es");

						int padre= arbol.getNumClass(lista.get(l));//Prototype.getMLnumber(lista.get(l));

						//System.out.println(preST[j][padre]);

						if(pre[j][padre]!=actualClasses[j][padre]){
							equalInParents=false;
						}
						//	System.out.print(lista.get(l)+", ");
					}

					if(equalInParents){ // Equal in its parents but different in this one, count mistake!
						hierarchicalLoss++;
					}


				}

			}
			//	System.out.println("\nhierarchicalLoss: "+hierarchicalLoss);

			totalHierarchicalLoss+=hierarchicalLoss;


		}

		totalHierarchicalLoss/=testSize;
		return totalHierarchicalLoss;

	}

	/** Compute the norm-H-loss.
	 * See: 
	 * Cesa-bianchi, N., Gentile, C., Tironi, A., Zaniboni, L.: Incremental algorithms for hierarchical classification. In: Saul, L., Weiss, Y., Bottou, L. (eds.) Ad- vances in Neural Information Processing Systems 17, pp. 233–240. MIT Press (2005),
	 * @param testSize
	 * @param pre
	 * @return
	 */
	public double computeNormHierarchicalLoss(int testSize, int pre[][]){

	


		double totalHierarchicalLoss=0;
		for(int j=0; j< testSize  ; j++){ //

			double hierarchicalLoss=0;

			/*if(pre[j][0]!=actualClasses[j][0]){ // si eres distinto en el root.
				hierarchicalLoss=1;
			}else{
			 */
			for(int c=0; c<numberOfClass; c++){


				//	System.out.println("Class: "+a[0].getNominalValuesList().get(c)+";"+arbol.coeffients[c]);

				if(pre[j][c]!=actualClasses[j][c]){

					// Compute parents... check they keep the ==

					//ArrayList<String> lista = arbol.listOfParents(c);
					ArrayList<String> lista = arbolSTO.ListParents.get(c);

					boolean equalInParents=true;
					for(int l=0; l<lista.size() && equalInParents; l++){
						//if(lista.get(l).equalsIgnoreCase("root")) System.err.println("ESTo k es");

						int padre= arbolSTO.getNumClass(lista.get(l));

						//System.out.println(preST[j][padre]);

						if(pre[j][padre]!=actualClasses[j][padre]){
							equalInParents=false;
						}
						//	System.out.print(lista.get(l)+", ");
					}


					//double coef = arbol.getCoefficient(c);
					double coef = arbolSTO.coeffients[c];

					//System.out.println("Class: "+a[0].getNominalValuesList().get(c)+";"+coef);

					if(equalInParents){ // Equal in its parents but different in this one, count mistake!
						hierarchicalLoss+=coef;
					}


				}

			}

			totalHierarchicalLoss+=hierarchicalLoss;


		}

		totalHierarchicalLoss/=testSize;
		return totalHierarchicalLoss;

	}


	/** Compute the HMC-loss that alleviate H-loss problems
	 * See: Bi and Kwok. Hierarchical Multilabel classification with Minimum Bayes Risk. 2012- ICDM. 
	 * 
	 * It computes false positives and false negatives and give them a cost. These two componentes are weighted by two parameters alpha and beta.
	 * The hierarchy information is added by setting a different cost depending on the hierarchy as norm-Hloss. 
	 * @param testSize
	 * @param pre
	 * @return
	 */
	public double computeHMCLoss(int testSize, int pre[][]){

		double totalHMCLoss=0;

		
		/*
		 * Alpha+Beta=2
		 * alpha = lambda*Beta
		 * 
		 * lambda= n-/n+
		 */
		
		double lambda[] = new double[this.numberOfClass];
		double alpha[] = new double[this.numberOfClass];
		double beta[] = new double[this.numberOfClass];

		
		for(int c=0; c<numberOfClass; c++){
			int neg=0,pos=0;
			
			for(int j=0; j< testSize  ; j++){ 
				if(actualClasses[j][c]==1){
					pos++;
				}else{
					neg++;
				}
			}
			lambda[c]=(1.*neg)/pos;
			beta[c] = 2.0/(1+lambda[c]);
			alpha[c] = 2.0 - beta[c];
			
		//	System.out.println(alpha[c]+","+beta[c]);
		}
		
		 //

	
		for(int c=0; c<numberOfClass; c++){
			double FnegativeCost=0;
			double FpositiveCost=0;
			double coef = arbolSTO.coeffients[c];

			for(int j=0; j< testSize  ; j++){

				if(pre[j][c]==0 && actualClasses[j][c]==1){ // Compute false negatives:
					FnegativeCost+=coef;
				}else if (pre[j][c]==1 && actualClasses[j][c]==0){ // False Positive;
					FpositiveCost+=coef;
				}

			}

			totalHMCLoss+=FnegativeCost*alpha[c]+FpositiveCost*beta[c];
		}

		totalHMCLoss/=testSize;
		return totalHMCLoss;

	}

	
	/** Compute the HMC-loss that alleviate H-loss problems
	 * See: Bi and Kwok. Hierarchical Multilabel classification with Minimum Bayes Risk. 2012- ICDM. 
	 * 
	 * It computes false positives and false negatives and give them a cost. These two componentes are weighted by two parameters alpha and beta.
	 * The hierarchy information is added by setting a different cost depending on the hierarchy as norm-Hloss. 
	 * 
	 * This version is only for the EVALUATION of a single class.
	 * @param testSize
	 * @param pre
	 * @return
	 */
	public double computeHMCLoss_individually(int testSize, int pre[], int clase, double coef, boolean doNotCountFP){

    	double totalHMCLoss=0;

		/*
		 * Alpha+Beta=2
		 * alpha = lambda*Beta
		 * lambda= n-/n+
		 */

		double lambda,alpha, beta;


		if(doNotCountFP){
			alpha=1;
			beta=0;
		}else{
		
			int neg=0,pos=0;
	
			for(int j=0; j< testSize  ; j++){ 
				if(actualClasses[j][clase]==1){
					pos++;
				}else{
					neg++;
				}
			}
			lambda=(1.*neg)/pos;
			beta = 2.0/(1+lambda);
			alpha = 2.0 - beta;

		}
		
		double FnegativeCost=0;
		double FpositiveCost=0;
		//double coef = arbol.coeffients[clase];

		for(int j=0; j< testSize  ; j++){

			if(pre[j]==0 && actualClasses[j][clase]==1){ // Compute false negatives:
				FnegativeCost+=coef;
			}else if (pre[j]==1 && actualClasses[j][clase]==0){ // False Positive;
				FpositiveCost+=coef;
			}

		}

		totalHMCLoss=FnegativeCost*alpha+FpositiveCost*beta;


		return totalHMCLoss;

	}


	/**
	 * See paper I. Pillai et al. Pattern Recognition 2013.
	 * 
	 * Macro average consists of averaging the correspoiding class-related measure. So, you compute the F-measure per class, and
	 * then we average the results.
	 * 
	 * @param testSize
	 * @param pre
	 * @return
	 */

	public double computeMacroAverageFmeasure(int testSize, int pre[][], boolean weighted){

		
		//double totalHierarchicalFmeasure=0;

		double prec=0, rec=0;
		double fMeasure=0;

		for(int c=0; c<numberOfClass; c++){


			// compute its hierarchical loss.

			int hierarchicalLoss=0;

			int Tp=0,Tn=0,Fp=0,Fn=0;

			for(int j=0; j< testSize  ; j++){ //

				if(pre[j][c]==actualClasses[j][c]){
					if(pre[j][c]==1)
						Tp++;
					else
						Tn++;
				}else{

					if(pre[j][c]==0){ // False negative
						Fn++;
					}else{
						Fp++;
					}


				}

			}

			prec= 1.*Tp/(Tp+Fp);
			rec= 1.*Tp/(Tp+Fn);

			if(prec+rec > 0){
				
				if(weighted)
					fMeasure+= arbolSTO.coeffients[c]*((2.*prec*rec)/(prec+rec));
				else
					fMeasure+=(2.*prec*rec)/(prec+rec);
			}

			//System.out.println("\n fMeasure: "+fMeasure);


		}

// we have to divide between the number of levels!!
		
		
		fMeasure/= arbolSTO.getMaxDepth(); //numberOfClass;
		
		//System.out.println("Max Depth= "+arbol.getMaxDepth());

		return fMeasure;

	}

	/**
	 * See paper I. Pillai et al. Pattern Recognition 2013.
	 * 
	 * Macro average consists of averaging the correspoiding class-related measure. So, you compute the F-measure per class, and
	 * then we average the results.
	 * 
	 * @param testSize
	 * @param pre
	 * @return
	 */

	public double computeMacroAverageFmeasure_individually(int testSize, int pre[], int clase){


		//double totalHierarchicalFmeasure=0;

		double prec=0, rec=0;
		double fMeasure=0;


			// compute its hierarchical loss.

			int hierarchicalLoss=0;

			int Tp=0,Tn=0,Fp=0,Fn=0;

			for(int j=0; j< testSize  ; j++){ //

				if(pre[j]==actualClasses[j][clase]){
					if(pre[j]==1)
						Tp++;
					else
						Tn++;
				}else{

					if(pre[j]==0){ // False negative
						Fn++;
					}else{
						Fp++;
					}


				}

			}

			prec= 1.*Tp/(Tp+Fp);
			rec= 1.*Tp/(Tp+Fn);

			if(prec+rec > 0){
				fMeasure+=(2.*prec*rec)/(prec+rec);
			}



		return fMeasure;

	}

	/**
	 * See paper Cerri 2015. Computational Intelligence.
	 * 
	 * 
	 * 
	 * @param testSize
	 * @param pre
	 * @return
	 */

	public double computeHierarchicalFmeasure(int testSize, int pre[][]){

		//TreeHMC arbol = new TreeHMC(numberOfClass, typeData);

		//Attribute[] a = Attributes.getOutputAttributes();

		//	double totalHierarchicalFmeasure=0;


		int sumTp =0;
		int sumTpFp=0;
		int sumTpFn=0;

		for(int j=0; j< testSize  ; j++){ //

			//int hierarchicalLoss=0;

			int Tp=0,Tn=0,Fp=0,Fn=0;

			for(int c=0; c<numberOfClass; c++){

				if(pre[j][c]==actualClasses[j][c]){
					if(pre[j][c]==1)
						Tp++;
					//	else
					//		Tn++;
				}else{

					if(pre[j][c]==0){ // False negative
						Fn++;
					}else{
						Fp++;
					}


				}



			}

			sumTp+=Tp;
			sumTpFp+=Tp+Fp;
			sumTpFn+=Tp+Fn;



		}

		// hP and hR
		double prec = 1.*sumTp/sumTpFp;
		double rec = 1.*sumTp/sumTpFn;

		double fMeasure=0;

		if(prec+rec > 0){
			fMeasure=(2.*prec*rec)/(prec+rec);
		}


		return fMeasure;

	}

}
