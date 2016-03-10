//
//  Main.java
//
//  Isaac Triguero
//
//  Copyright (c) 2015 __MyCompanyName__. All rights reserved.
//


import Util.ClusWrapper;


//import java.io.IOException;
import java.util.*;

/**
 * Compute threshold/s for labeling HMC outputs.
 * @ input: a list of datasets to be processed!
 * @ req: files-  <nameData>.test.pred.arff.val; <nameData>.out.val; <nameData>.test.pred.arff.test; <nameData>.out.test
 * @author Isaac Triguero
 */
public class main // extends PrototypeGenerationAlgorithm
{


	private static String typeData= "TREE";
	private static int numberOfClass=0;
	private static double [][] probabilities,actualClasses;
	private double [] CD;


	/**
	 * Main method. 
	 * @param args Console arguments of the method.
	 * @throws Exception 
	 */
	public static void main(String args[]) throws Exception
	{

		String datasets[];
		String path;
		
		if(args.length==0){
			// list of data: This case I put here manually. 
			String datas[]={"imclef07a"}; //"expr_FUN", "seq_FUN", "interpro_ara_FUN", "Enron_corr","ImCLEF07A"  ,"seq_FUN" "simple"
										 // new data:   "reuters", "diatoms", "wipo"   ", "", "reuters", "struc_ara_GO
			datasets=datas;
			
			path="src/InputFiles/";
			
		}else{ // to run on the cluster.
			datasets = args;
			path="";
		}

		/**
		 * Measures to be optimized: (micro) F-measure and  Normalized-Hloss. in all the settings
		 * 3 Settings:
		 * – The evaluation measure that is used in the end to evaluate the predictions.
		 * – The class distributions.
		 * – The label cardinalities.
		 */

		String[] measures = {"f-measure", "Norm-Hloss","HMC-loss"}; //"f-measure", "Norm-Hloss","HMC-loss", "macroWFmeasure"
		String[] settings = {"measure","classDistribution", "labelCardinalities"}; //"measure","classDistribution", "labelCardinalities", "combination"

		double ErrorSTO[][][]=new double[datasets.length][measures.length][2]; // for each dataset, for each measure , validation/Test
		double ErrorMTO[][][]=new double[datasets.length][measures.length][2];;
		double ClassDistrSTO[][][]=new double[datasets.length][measures.length][2];
		double ClassDistrMTO[][][]=new double[datasets.length][measures.length][2];
		double LabelCardSTO[][][]=new double[datasets.length][measures.length][2];
		double LabelCardMTO[][][]=new double[datasets.length][measures.length][2];

		double nonLabeledSTO[][][][] = new double[datasets.length][measures.length][settings.length][2];
		double nonLabeledMTO[][][][] = new double[datasets.length][measures.length][settings.length][2];
		

		double timesSTO[][][] = new double[datasets.length][measures.length][settings.length];
		double timesMTO[][][] = new double[datasets.length][measures.length][settings.length];
		
		boolean AllMeasures=false; // show all measures??
		 

		boolean AlreadyDoneLC=false;
		boolean AlreadyDoneCD=false;


		int numDataset=0;


		for(String data:datasets){

			// Process the data!
			ClusWrapper wrap = new ClusWrapper(typeData);
			wrap.setCurrentDir(System.getProperty("user.dir")+"/"+path);

			wrap.ObtainDataInfoFromOutFile(data);
			
			numberOfClass= wrap.getNumberOfClass();

			int trainSize = wrap.getTrainSize();
			int ValidationSize= wrap.getValSize();
			int testSize =wrap.getTestSize();
			
			// Reading the probabilities matrices:
			
			double [][] probVAL,actualClassesVAL;
			double [][] probTEST,actualClassesTEST;
			double [] cdVAL, cdTEST;
			
			probVAL = wrap.obtainProbabilitiesHMCbase(ValidationSize,".val",data).clone();//
			actualClassesVAL = wrap.getActualClasses().clone();
			cdVAL=wrap.getClassDistribution().clone();
			
			probTEST = wrap.obtainProbabilitiesHMCbase(testSize,".test",data).clone();//
			actualClassesTEST = wrap.getActualClasses().clone();
			//cdTEST=wrap.getClassDistribution().clone();
			
			
			AlreadyDoneLC=false;
			AlreadyDoneCD=false;

			int numMedida=0;
			for(String medida: measures){
				System.out.println("\n\nMeasure: "+medida);

				AllMeasures=false;
				System.out.println("\nProcessing data:"+data);

				double cutOff=1; // One cutOff per measure.
				double multiple_cutOffs[] = new double[numberOfClass]; // one set of CutOffs per measure


				wrap.setSearchMode(true);  // true for OPTIMAL OR false for SUBOPTIMAL STO
				
				long time;
				double timeSTO, timeMTO;
				
				for(String set: settings){ 
					
					probabilities = probVAL;
					actualClasses = actualClassesVAL;
					wrap.setClassDistribution(cdVAL);
					wrap.setProb(probabilities);
					wrap.setActualClasses(actualClasses);
					
					//System.out.println("prob size: "+probabilities.length);

					System.out.println("\n\nSetting: "+set);

					if(set.equalsIgnoreCase("measure")){

						time= System.currentTimeMillis();
						cutOff= wrap.STO(medida);
						
						timeSTO = (double)(System.currentTimeMillis()-time)/1000.0;
						timesSTO[numDataset][numMedida][0] =timeSTO;
						System.out.println("STO Runtime: " + timeSTO);
						
						time= System.currentTimeMillis();
						multiple_cutOffs= wrap.MTO(medida);
						timeMTO= (double)(System.currentTimeMillis()-time)/1000.0;
						System.out.println("MTO Runtime: " + timeMTO);
						timesMTO[numDataset][numMedida][0] =timeMTO;

					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){

						AllMeasures=true; // i want to show all the measure when ClassDistribution is applied.
												
						time= System.currentTimeMillis();
						
						cutOff=wrap.STO(set);
						
						timeSTO = (double)(System.currentTimeMillis()-time)/1000.0;
						System.out.println("STO Runtime: " + timeSTO);
						timesSTO[numDataset][numMedida][1] =timeSTO;

						
						time= System.currentTimeMillis();
						multiple_cutOffs= wrap.MTOclassDistribution();
						timeMTO= (double)(System.currentTimeMillis()-time)/1000.0;
						System.out.println("MTO Runtime: " + timeMTO);
						timesMTO[numDataset][numMedida][1] =timeMTO;



					}else if((set.equalsIgnoreCase("labelCardinalities"))&& !AlreadyDoneLC){

						
						
						AllMeasures=true; // i want to show all the measure when labelCardinalities is applied.
						
						double actualLC[] = new double[ValidationSize];
						Arrays.fill(actualLC,0);

					   //count how many classes do we have in every validation example
						for(int j=0; j<ValidationSize; j++){
							for(int i=0; i<numberOfClass; i++){
								if(actualClasses[j][i]==1){
									actualLC[j]+=1;	
								}
							}
						}
						
						wrap.setLabelCaridinality(actualLC);
						

						
						time= System.currentTimeMillis();
						cutOff=wrap.STO(set);
						timeSTO = (double)(System.currentTimeMillis()-time)/1000.0;
						System.out.println("STO Runtime: " + timeSTO);
						timesSTO[numDataset][numMedida][2] =timeSTO;

		
						
						// 15/9: We are not running MTO label cardinalities anymore
						Arrays.fill(multiple_cutOffs,1);
						
						time= System.currentTimeMillis();
						System.out.println("LC-V2 running!");
					// 	multiple_cutOffs= wrap.MTOlabelcardinalities_v2(); // V2 running!
						timeMTO= (double)(System.currentTimeMillis()-time)/1000.0;
						System.out.println("MTO Runtime: " + timeMTO);
						timesMTO[numDataset][numMedida][2] =timeMTO;
						
						for(int i=0; i<numberOfClass; i++){
							System.out.print(multiple_cutOffs[i]+", ");
						}



					}


				for(int i=0; i<numberOfClass;i++){
					System.out.print(multiple_cutOffs[i]+", ");
				}
				System.out.println("");

				// check in the validation set, the proportion.
				int preST[][]= new int[ValidationSize][numberOfClass];
				int preMT[][]= new int[ValidationSize][numberOfClass];


				//System.out.println("prob size: "+probabilities.length);
				int cont[] = new int[3];
				Arrays.fill(cont,0);

				for (int j=0; j< ValidationSize ; j++){
					Arrays.fill(preST[j], 0);
					Arrays.fill(preMT[j], 0);


					boolean AssignClass_ST=false;
					boolean AssignClass_MT=false;
					for(int c=0; c<numberOfClass; c++){

						//System.out.print(probabilities[j][c]+", ");

						// Single threshold option.
						if(probabilities[j][c] > cutOff){
							preST[j][c]=1;
							AssignClass_ST=true;
						}

						// multi-threshold option:

						if(probabilities[j][c] > multiple_cutOffs[c] && multiple_cutOffs[c]!=-1 ){   //&& multiple_cutOffs[c] >0
							preMT[j][c]=1;
							AssignClass_MT=true;
						}

					}
					//System.out.println("");

					if(!AssignClass_ST){
						cont[0]++;

					}
					if(!AssignClass_MT)
						cont[1]++;


				}



				if(medida.equalsIgnoreCase("f-measure") || AllMeasures){
					double value= wrap.computeHierarchicalFmeasure(ValidationSize, preST);
					System.out.println("\nSTO Validation: Micro Hierarhical Fmeasure "+value +", %no labeled: "+(cont[0]*100.)/ValidationSize);
					
				
					
					double value2= wrap.computeHierarchicalFmeasure(ValidationSize, preMT);
					System.out.println("MTO Validation: Micro Hierarhical Fmeasure "+value2 +", %no labeled: "+(cont[1]*100.)/ValidationSize);
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][0][0]= value;
						ErrorMTO[numDataset][0][0]= value2;
						
						nonLabeledSTO[numDataset][0][0][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][0][0][0]=((cont[1])*100.)/ValidationSize;
					
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][0][0] = value;
						ClassDistrMTO[numDataset][0][0] = value2;
						
						nonLabeledSTO[numDataset][0][1][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][0][1][0]=((cont[1])*100.)/ValidationSize;
					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][0][0] = value;
						LabelCardMTO[numDataset][0][0] = value2;
						
						nonLabeledSTO[numDataset][0][2][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][0][2][0]=((cont[1])*100.)/ValidationSize;
					}
					
				

				}
				
				if(medida.equalsIgnoreCase("Norm-Hloss")|| AllMeasures){
					double value=	wrap.computeNormHierarchicalLoss(ValidationSize, preST);
					System.out.println("STO Validation: norm-Hloss "+value +", %no labeled: "+((cont[0])*100.)/ValidationSize);
					double value2=	wrap.computeNormHierarchicalLoss(ValidationSize, preMT);
					System.out.println("MTO Validation: norm-Hloss "+value2 +", %no labeled: "+((cont[1])*100.)/ValidationSize);
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][1][0]= value;
						ErrorMTO[numDataset][1][0]= value2;
						
						nonLabeledSTO[numDataset][1][0][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][1][0][0]=((cont[1])*100.)/ValidationSize;
						
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][1][0] = value;
						ClassDistrMTO[numDataset][1][0] = value2;
						
						nonLabeledSTO[numDataset][1][1][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][1][1][0]=((cont[1])*100.)/ValidationSize;
					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][1][0] = value;
						LabelCardMTO[numDataset][1][0] = value2;
						
						nonLabeledSTO[numDataset][1][2][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][1][2][0]=((cont[1])*100.)/ValidationSize;
					}
					
					

				}
				
				
				if(medida.equalsIgnoreCase("HMC-loss")|| AllMeasures){
					double value=	wrap.computeHMCLoss(ValidationSize, preST);
					System.out.println("STO Validation: HMC-loss "+value +", %no labeled: "+((cont[0])*100.)/ValidationSize);
					double value2=	wrap.computeHMCLoss(ValidationSize, preMT);
					System.out.println("MTO Validation: HMC-loss "+value2 +", %no labeled: "+((cont[1])*100.)/ValidationSize);
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][2][0]= value;
						ErrorMTO[numDataset][2][0]= value2;
						
						nonLabeledSTO[numDataset][2][0][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][2][0][0]=((cont[1])*100.)/ValidationSize;
						
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][2][0] = value;
						ClassDistrMTO[numDataset][2][0] = value2;
						
						nonLabeledSTO[numDataset][2][1][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][2][1][0]=((cont[1])*100.)/ValidationSize;
					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][2][0] = value;
						LabelCardMTO[numDataset][2][0] = value2;
						
						nonLabeledSTO[numDataset][2][2][0]=((cont[0])*100.)/ValidationSize;
						nonLabeledMTO[numDataset][2][2][0]=((cont[1])*100.)/ValidationSize;
					}
					
					

				}
				
				
				if(medida.equalsIgnoreCase("uniform-Hloss") || AllMeasures){
					double value=	wrap.computeHierarchicalLoss(ValidationSize, preST);
					System.out.println("STO Validation: uniform-Hloss "+value +", %no labeled: "+(cont[0]*100.)/ValidationSize);
				}


				/**
				 * Computing prob. in the TEST data:
				 **/


				probabilities = probTEST; //wrap.obtainProbabilitiesHMCbase(testSize,".test",data);//
				actualClasses = actualClassesTEST; //wrap.getActualClasses();
			//	wrap.setClassDistribution(cdTEST);
				wrap.setProb(probabilities);
				wrap.setActualClasses(actualClasses);
				
				
				
				// matrix of predictions for ST (Single Threshold), MT (multiple thresholds) and SCF (Simulating class frequency);

				preST= new int[testSize][numberOfClass];
				preMT= new int[testSize][numberOfClass];


				cont = new int[3];
				Arrays.fill(cont,0);

				for (int j=0; j< testSize ; j++){
					Arrays.fill(preST[j], 0);
					Arrays.fill(preMT[j], 0);


					boolean AssignClass_ST=false;
					boolean AssignClass_MT=false;
					for(int c=0; c<numberOfClass; c++){

						//System.out.print(probabilities[j][c]+", ");

						// Single threshold option.
						if(probabilities[j][c] > cutOff){
							preST[j][c]=1;
							AssignClass_ST=true;
						}

						// multi-threshold option:

						if(probabilities[j][c] > multiple_cutOffs[c] && multiple_cutOffs[c]!=-1 ){   //&& multiple_cutOffs[c] >0
							preMT[j][c]=1;
							AssignClass_MT=true;
						}

					}
					//System.out.println("");

					if(!AssignClass_ST){
						cont[0]++;
					}
					if(!AssignClass_MT)
						cont[1]++;


				}

				if(medida.equalsIgnoreCase("f-measure")|| AllMeasures){
					double value= wrap.computeHierarchicalFmeasure(testSize, preST);
					System.out.println("STO: Micro Hierarhical Fmeasure "+value +", %no labeled: "+(cont[0]*100.)/testSize);
					double value2= wrap.computeHierarchicalFmeasure(testSize, preMT);
					System.out.println("MTO: Micro Hierarhical Fmeasure "+value2 +", %no labeled: "+(cont[1]*100.)/testSize);
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][0][1]= value;
						ErrorMTO[numDataset][0][1]= value2;
						
						nonLabeledSTO[numDataset][0][0][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][0][0][1]=((cont[1])*100.)/testSize;
						
						System.out.println("Liandola: "+nonLabeledSTO[numDataset][0][0][1]);
						
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][0][1] = value;
						ClassDistrMTO[numDataset][0][1] = value2;
						
						nonLabeledSTO[numDataset][0][1][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][0][1][1]=((cont[1])*100.)/testSize;
					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][0][1] = value;
						LabelCardMTO[numDataset][0][1] = value2;
						
						nonLabeledSTO[numDataset][0][2][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][0][2][1]=((cont[1])*100.)/testSize;
					}
					
	

				}
				if(medida.equalsIgnoreCase("Norm-Hloss")|| AllMeasures){
					double value=	wrap.computeNormHierarchicalLoss(testSize, preST);
					System.out.println("STO: norm-Hloss "+value +", %no labeled: "+(cont[0]*100.)/testSize);
					double value2=	wrap.computeNormHierarchicalLoss(testSize, preMT);
					System.out.println("MTO: norm-Hloss "+value2 +", %no labeled: "+((cont[1])*100.)/testSize);
					
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][1][1]= value;
						ErrorMTO[numDataset][1][1]= value2;
						
						nonLabeledSTO[numDataset][1][0][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][1][0][1]=((cont[1])*100.)/testSize;
						
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][1][1] = value;
						ClassDistrMTO[numDataset][1][1] = value2;
						
						nonLabeledSTO[numDataset][1][1][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][1][1][1]=((cont[1])*100.)/testSize;
						

					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][1][1] = value;
						LabelCardMTO[numDataset][1][1] = value2;
						
						nonLabeledSTO[numDataset][1][2][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][1][2][1]=((cont[1])*100.)/testSize;
					}
	
				}
				
				if(medida.equalsIgnoreCase("HMC-loss")|| AllMeasures){
					double value=	wrap.computeHMCLoss(testSize, preST);
					System.out.println("STO: HMC-loss "+value +", %no labeled: "+(cont[0]*100.)/testSize);
					double value2=	wrap.computeHMCLoss(testSize, preMT);
					System.out.println("MTO: HMC-loss "+value2 +", %no labeled: "+((cont[1])*100.)/testSize);
					
					
					if(set.equalsIgnoreCase("measure")){
						ErrorSTO[numDataset][2][1]= value;
						ErrorMTO[numDataset][2][1]= value2;
						
						nonLabeledSTO[numDataset][2][0][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][2][0][1]=((cont[1])*100.)/testSize;
						
					}else if((set.equalsIgnoreCase("classDistribution")) && !AlreadyDoneCD){
						ClassDistrSTO[numDataset][2][1] = value;
						ClassDistrMTO[numDataset][2][1] = value2;
						AlreadyDoneCD=true;
						
						nonLabeledSTO[numDataset][2][1][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][2][1][1]=((cont[1])*100.)/testSize;
						

					}else if((set.equalsIgnoreCase("labelCardinalities")) && !AlreadyDoneLC){
						LabelCardSTO[numDataset][2][1] = value;
						LabelCardMTO[numDataset][2][1] = value2;
						AlreadyDoneLC= true;
						
						System.out.println("Already done LC");
						nonLabeledSTO[numDataset][2][2][1]=((cont[0])*100.)/testSize;
						nonLabeledMTO[numDataset][2][2][1]=((cont[1])*100.)/testSize;
					}
	
				}
				
				if(medida.equalsIgnoreCase("uniform-Hloss")|| AllMeasures){
					double value=	wrap.computeHierarchicalLoss(testSize, preST);
					System.out.println("STO: uniform-Hloss "+value +", %no labeled: "+(cont[0]*100.)/testSize);
				}

				
				
				} // end setting!
				
				numMedida++;
			}

			numDataset++;



		}
		
		
		// Print the results
		
		String ficheros[]= new String[17]; // 4 ficheros, val/test * 3 measures + runtimes!
		
		
		Arrays.fill(ficheros, "");
		
		for(int i=0; i<datasets.length;i++){
			ficheros[0]+=datasets[i]+"\t";	ficheros[1]+=datasets[i]+"\t";
			ficheros[2]+=datasets[i]+"\t";	ficheros[3]+=datasets[i]+"\t";
			ficheros[4]+=datasets[i]+"\t";  ficheros[5]+=datasets[i]+"\t";
			ficheros[6]+=datasets[i]+"\t";	ficheros[7]+=datasets[i]+"\t";
			ficheros[8]+=datasets[i]+"\t";	ficheros[9]+=datasets[i]+"\t";
			ficheros[10]+=datasets[i]+"\t";	ficheros[11]+=datasets[i]+"\t";
			ficheros[12]+=datasets[i]+"\t";	ficheros[13]+=datasets[i]+"\t";	
			ficheros[14]+=datasets[i]+"\t";	ficheros[15]+=datasets[i]+"\t";	
			ficheros[16]+=datasets[i]+"\t";
			
			// F-measure, val and test.
			ficheros[0]+= ErrorSTO[i][0][0]+"\t"+ErrorMTO[i][0][0]+"\t"+ClassDistrSTO[i][0][0]+"\t"+ClassDistrMTO[i][0][0]+"\t"+LabelCardSTO[i][0][0]+"\t"+LabelCardMTO[i][0][0] +"\n";
			ficheros[1]+= ErrorSTO[i][0][1]+"\t"+ErrorMTO[i][0][1]+"\t"+ClassDistrSTO[i][0][1]+"\t"+ClassDistrMTO[i][0][1]+"\t"+LabelCardSTO[i][0][1]+"\t"+LabelCardMTO[i][0][1]+"\n";
			
			//Norm-loss val and test
			ficheros[2]+= ErrorSTO[i][1][0]+"\t"+ErrorMTO[i][1][0]+"\t"+ClassDistrSTO[i][1][0]+"\t"+ClassDistrMTO[i][1][0]+"\t"+LabelCardSTO[i][1][0]+"\t"+LabelCardMTO[i][1][0]+"\n";
			ficheros[3]+= ErrorSTO[i][1][1]+"\t"+ErrorMTO[i][1][1]+"\t"+ClassDistrSTO[i][1][1]+"\t"+ClassDistrMTO[i][1][1]+"\t"+LabelCardSTO[i][1][1]+"\t"+LabelCardMTO[i][1][1]+"\n";
			
			//HMC-loss val and test
			ficheros[4]+= ErrorSTO[i][2][0]+"\t"+ErrorMTO[i][2][0]+"\t"+ClassDistrSTO[i][2][0]+"\t"+ClassDistrMTO[i][2][0]+"\t"+LabelCardSTO[i][2][0]+"\t"+LabelCardMTO[i][2][0]+"\n";
			ficheros[5]+= ErrorSTO[i][2][1]+"\t"+ErrorMTO[i][2][1]+"\t"+ClassDistrSTO[i][2][1]+"\t"+ClassDistrMTO[i][2][1]+"\t"+LabelCardSTO[i][2][1]+"\t"+LabelCardMTO[i][2][1]+"\n";
		
			
			//Macro-Weighted F-measure val and test
		//	ficheros[6]+= ErrorSTO[i][2][0]+"\t"+ErrorMTO[i][2][0]+"\t"+ClassDistrSTO[i][2][0]+"\t"+ClassDistrMTO[i][2][0]+"\t"+LabelCardSTO[i][2][0]+"\t"+LabelCardMTO[i][2][0]+"\n";
		//	ficheros[7]+= ErrorSTO[i][2][1]+"\t"+ErrorMTO[i][2][1]+"\t"+ClassDistrSTO[i][2][1]+"\t"+ClassDistrMTO[i][2][1]+"\t"+LabelCardSTO[i][2][1]+"\t"+LabelCardMTO[i][2][1]+"\n";
		
			
			
			// F-measure, non labeled.
			ficheros[8]+= nonLabeledSTO[i][0][0][0]+"\t"+ nonLabeledMTO[i][0][0][0]+"\t"+nonLabeledSTO[i][0][1][0]+"\t"+ nonLabeledMTO[i][0][1][0]+"\t"+nonLabeledSTO[i][0][2][0]+"\t"+ nonLabeledMTO[i][0][2][0]+"\n";
			ficheros[9]+= nonLabeledSTO[i][0][0][1]+"\t"+ nonLabeledMTO[i][0][0][1]+"\t"+nonLabeledSTO[i][0][1][1]+"\t"+ nonLabeledMTO[i][0][1][1]+"\t"+nonLabeledSTO[i][0][2][1]+"\t"+ nonLabeledMTO[i][0][2][1]+"\n";

			// Norm-hloss val and test
			ficheros[10]+= nonLabeledSTO[i][1][0][0]+"\t"+ nonLabeledMTO[i][1][0][0]+"\t"+nonLabeledSTO[i][1][1][0]+"\t"+ nonLabeledMTO[i][1][1][0]+"\t"+nonLabeledSTO[i][1][2][0]+"\t"+ nonLabeledMTO[i][1][2][0]+"\n";
			ficheros[11]+= nonLabeledSTO[i][1][0][1]+"\t"+ nonLabeledMTO[i][1][0][1]+"\t"+nonLabeledSTO[i][1][1][1]+"\t"+ nonLabeledMTO[i][1][1][1]+"\t"+nonLabeledSTO[i][1][2][1]+"\t"+ nonLabeledMTO[i][1][2][1]+"\n";

			// HMCloss val and test
			ficheros[12]+= nonLabeledSTO[i][2][0][0]+"\t"+ nonLabeledMTO[i][2][0][0]+"\t"+nonLabeledSTO[i][2][1][0]+"\t"+ nonLabeledMTO[i][2][1][0]+"\t"+nonLabeledSTO[i][2][2][0]+"\t"+ nonLabeledMTO[i][2][2][0]+"\n";
			ficheros[13]+= nonLabeledSTO[i][2][0][1]+"\t"+ nonLabeledMTO[i][2][0][1]+"\t"+nonLabeledSTO[i][2][1][1]+"\t"+ nonLabeledMTO[i][2][1][1]+"\t"+nonLabeledSTO[i][2][2][1]+"\t"+ nonLabeledMTO[i][2][2][1]+"\n";

			//Runtime:
			ficheros[16]+= timesSTO[i][0][0] +"\t"+ timesMTO[i][0][0]+"\t"+ timesSTO[i][1][0] +"\t"+ timesMTO[i][1][0]+"\t"+ timesSTO[i][2][0] +"\t"+ timesMTO[i][2][0]+"\t"+ timesSTO[i][0][1] +"\t"+ timesMTO[i][0][1]+"\t"+ timesSTO[i][0][2] +"\t"+ timesMTO[i][0][2]+"\n";
			
		}
		
		String init="output/";
		if(datasets.length==1) init+=datasets[0]; 
	
		Util.Fichero.escribeFichero(init+"-fMeasure-val.txt", ficheros[0]);
		Util.Fichero.escribeFichero(init+"-fMeasure-test.txt", ficheros[1]);

		Util.Fichero.escribeFichero(init+"-NormLoss-val.txt", ficheros[2]);
		Util.Fichero.escribeFichero(init+"-NormLoss-test.txt", ficheros[3]);
		

		Util.Fichero.escribeFichero(init+"-HMCLoss-val.txt", ficheros[4]);
		Util.Fichero.escribeFichero(init+"-HMCLoss-test.txt", ficheros[5]);
		
		//Util.Fichero.escribeFichero(init+"-macroWFmeasure-val.txt", ficheros[6]);
	//	Util.Fichero.escribeFichero(init+"-macroWFmeasure-test.txt", ficheros[7]);
		
		Util.Fichero.escribeFichero(init+"-nonLabeled-fmeasure-val.txt", ficheros[8]);
		Util.Fichero.escribeFichero(init+"-nonLabeled-fmeasure-test.txt", ficheros[9]);
		
		Util.Fichero.escribeFichero(init+"-nonLabeled-NormHloss-val.txt", ficheros[10]);
		Util.Fichero.escribeFichero(init+"-nonLabeled-NormHloss-test.txt", ficheros[11]);
		
		Util.Fichero.escribeFichero(init+"-nonLabeled-HMCloss-val.txt", ficheros[12]);
		Util.Fichero.escribeFichero(init+"-nonLabeled-HMCloss-test.txt", ficheros[13]);

	//	Util.Fichero.escribeFichero(init+"-nonLabeled-macroW-fmeasure-val.txt", ficheros[14]);
		//Util.Fichero.escribeFichero(init+"-nonLabeled-macroW-fmeasure-test.txt", ficheros[15]);
		
		Util.Fichero.escribeFichero(init+"-runtime.txt", ficheros[16]);


	} // End-Main


}
