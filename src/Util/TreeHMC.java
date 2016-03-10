package Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import Util.tree.GenericTree;
import Util.tree.GenericTreeNode;
import Util.Pair;

public class TreeHMC {

	public GenericTree<Pair<String, Double>> treeClasses;

	private int numberOfClass;

	private boolean[] PresentClass;

	public double[] coeffients;

	private ArrayList<String> classes;

	public static ArrayList<ArrayList<String>> ListParents;
	public static ArrayList<ArrayList<Integer>> ListParentsNum;

	public static ArrayList<ArrayList<String>> ListChildren;
	public static ArrayList<ArrayList<Integer>> ListChildrenNum;
	
	private int MaximumDepth=0;

	private double cutOff;

	//  public String[] classes;
	//  public double[] prob;


	TreeHMC(){};

	public TreeHMC(int numberClass, String typeData){
		this.numberOfClass = numberClass;


		//classes = new String[numberClass];

		//  prob = new double[numberClass];

		if(typeData.equals("TREE"))
			this.createTree(); // directly initialized!
		else
			this.createDAGv2();
	};


	public TreeHMC(int numberClass, String typeData, ArrayList<String>classes){
		this.numberOfClass = numberClass;
		this.classes = classes;

		//classes = new String[numberClass];

		//  prob = new double[numberClass];

		if(typeData.equals("TREE"))
			this.createTree(); // directly initialized!
		else
			this.createDAGv2();
	};


	public boolean[] getPresentClass(){
		return PresentClass;
	}


	/**
	 * Provide the list of parents for a given class!
	 * @param clase
	 */
	public ArrayList<String> listOfParents(int classNumber){

		ArrayList<String> lista= new ArrayList<String>();

		// First, search for the class in the hierarchy.

		String target = getNameClass(classNumber);

		//System.out.println("\nTarget: "+target);


		Pair<String, Double> clase = new Pair<String, Double>(target, 0.0);


		GenericTreeNode node= treeClasses.find(clase);

		if(node==null){
			System.out.println("I haven't found classe: "+clase);
		
			System.out.println(treeClasses.toStringWithDepth());
			
			//if(treeClasses.toStringWithDepth().contains(target))
				//System.out.println("Yo toy aki");
		}
		GenericTreeNode daddy = node.getParent();

		// if(daddy==null)  // for root nodes.
		//	  return lista;


		Pair<String, Double> dato = (Pair<String, Double>) daddy.getData();

		//System.out.println("Daddy: "+dato.first());

		if(dato.getFirst().equalsIgnoreCase("root")){
			//lista.add(target);

			return lista;
		}else{
			lista.add(dato.first());

			int numClass=  getNumClass(dato.first()); //Prototype.getMLnumber(dato.first());	    	  
			lista.addAll(listOfParents(numClass));
			return lista;
		}

	}


	/**
	 * Provide the list of parents for a given node!
	 * @param clase
	 */
	public ArrayList<GenericTreeNode> listOfParents(GenericTreeNode node){

		ArrayList<GenericTreeNode> lista= new ArrayList<GenericTreeNode>();

		GenericTreeNode daddy = node.getParent();

		Pair<String, Double> dato = (Pair<String, Double>) daddy.getData();

		//System.out.println("Daddy: "+dato.first());

		if(dato.getFirst().equalsIgnoreCase("root")){
			//lista.add(target);

			return lista;
		}else{
			lista.add(daddy);

			lista.addAll(listOfParents(daddy));
			return lista;
		}

	}


	/**
	 * Provide the list of children for a given class!
	 * @param clase
	 */
	public ArrayList<String> listOfChildren(String target){

		ArrayList<String> lista= new ArrayList<String>();

		Pair<String, Double> clase = new Pair<String, Double>(target, 0.0);

		GenericTreeNode node= treeClasses.find(clase);

		List<GenericTreeNode> lista2 = node.getChildren();


		if(lista2.size()==0){
			return lista;
		}else{

			for (GenericTreeNode aux: lista2){
				Pair<String, Double> dato = (Pair<String, Double>) aux.getData();

				lista.add(dato.first());
				lista.addAll(listOfChildren(dato.first()));

			}

		}
		return lista;

	}


	/**
	 * Get coefficient of the given class, according to:
	 * If node i is a root node, then ci = 1/|rootnodes|, otherwise ci = cj /|child(j)| with j the parent of i.
	 * @param clase
	 */
	public double getCoefficient(int classNumber){

		double coef=0;
		ArrayList<String> lista= new ArrayList<String>();

		// First, search for the class in the hierarchy.
		
		String target = getNameClass(classNumber);

		//  System.out.println("\nTarget: "+target);


		Pair<String, Double> clase = new Pair<String, Double>(target, 0.0);


		GenericTreeNode node= treeClasses.find(clase);

		GenericTreeNode daddy = node.getParent();

		// if(daddy==null)  // for root nodes.
		//	  return lista;


		Pair<String, Double> dato = (Pair<String, Double>) daddy.getData();

		//System.out.println("Daddy: "+dato.first());

		if(dato.getFirst().equalsIgnoreCase("root")){
			//lista.add(target);
			coef = 1./(daddy.getChildren().size()); // this corresponds to the number of 'rootnodes'.
			return coef;
		}else{

			int numChildrenDaddy =daddy.getChildren().size();
			int numClass= getNumClass(dato.first()); //Prototype.getMLnumber(dato.first());

			coef = getCoefficient(numClass)/(numChildrenDaddy);
			return coef;
		}

	}

	public void preComputeCoefficients(){

		this.coeffients = new double[this.numberOfClass];

		for(int i=0; i<this.numberOfClass; i++){
			coeffients[i]=  getCoefficient(i);
			
			//System.out.println(getNameClass(i)+"; "+coeffients[i]);
		}


	}

	public void preComputeListsOfParents(){


		this.ListParents = new ArrayList<ArrayList<String>>();

		for(int i=0; i<this.numberOfClass; i++){

			ListParents.add(this.listOfParents(i));
		}


	}
	


	public void preComputeListsOfParentsNum(){


		this.ListParentsNum = new ArrayList<ArrayList<Integer>>();

		for(int i=0; i<this.numberOfClass; i++){

			ArrayList<String> lista=this.listOfParents(i);
			ArrayList<Integer> listaNum=new ArrayList<Integer>();

			for(int l=0; l<lista.size(); l++){
				Integer num=getNumClass(lista.get(l));//Prototype.getMLnumber(lista.get(l));
				listaNum.add(num);
			}

			ListParentsNum.add(listaNum);
		}


	}

	public void preComputeListsOfChildren(){


		this.ListChildren = new ArrayList<ArrayList<String>>();

		for(int i=0; i<this.numberOfClass; i++){
			String target = getNameClass(i);
			ListChildren.add(this.listOfChildren(target));
		}


	}
	public void preComputeListsOfChildrenNum(){


		this.ListChildrenNum = new ArrayList<ArrayList<Integer>>();

		for(int i=0; i<this.numberOfClass; i++){

			String target = getNameClass(i);

			ArrayList<String> lista=this.listOfChildren(target);
			ArrayList<Integer> listaNum=new ArrayList<Integer>();

			//System.out.println("Class: "+target);

			for(int l=0; l<lista.size(); l++){
				//  System.out.print(lista.get(l)+", ");

				Integer num=  getNumClass(lista.get(l));//Prototype.getMLnumber(lista.get(l));
				listaNum.add(num);
			}

			// System.out.println("");

			ListChildrenNum.add(listaNum);
		}

		//System.exit(1);
	}


	/**
	 * Return list of evaluated classes and fulfill PrseentClass variable. TREE-based datasets!
	 * @param labeled
	 * @return
	 * @throws IOException
	
	public String EvaluatedClasses (PrototypeSet labeled) throws IOException{

		PresentClass  = new boolean[this.numberOfClass];

		Arrays.fill(PresentClass, false);


		GenericTreeNode root = treeClasses.getRoot();

		int examplesPerClass[] = new int [numberOfClass];
		Arrays.fill(examplesPerClass,0);
		String cad = "";

		for (int i =0; i< this.numberOfClass; i++){

			String target = getNameClass(i);
			
			
			Pair<String, Double> clase = new Pair<String, Double>(target, 0.0);

			GenericTreeNode node= treeClasses.find(clase);

			examplesPerClass[i] = labeled.getFromMultipleClasses(i).size();

			// then, check if there is from its children!

			examplesPerClass[i] += ExamplesPerNode(node, labeled);

			//List<GenericTreeNode> lista = node.getChildren();

			//  System.out.println("Examples class "+ i+ "- "+ target + " - "+ lista.size()+": "+examplesPerClass[i]);

			if( examplesPerClass[i] >0){

				PresentClass[i] = true;
				
				cad += getNameClass(i)+"\n";
			}



		}



		return cad;


	}
 */

	/**
	 * Return list of evaluated classes and fulfill PrseentClass variable. TREE-based datasets!
	 * @param labeled
	 * @return
	 * @throws IOException
	 
	public String EvaluatedClassesDAG (PrototypeSet labeled) throws IOException{

		PresentClass  = new boolean[this.numberOfClass];

		Arrays.fill(PresentClass, false);


		GenericTreeNode root = treeClasses.getRoot();

		int examplesPerClass[] = new int [numberOfClass];
		Arrays.fill(examplesPerClass,0);
		String cad = "";

		for (int i =1; i< this.numberOfClass; i++){  // I START FROM 1 BECAUSE I SKIP THE ROOT NODE!

			String target=getNameClass(i);
			

			
			Pair<String, Double> clase = new Pair<String, Double>(target, 0.0);

			GenericTreeNode node= treeClasses.find(clase);

			examplesPerClass[i] = labeled.getFromMultipleClasses(i).size();

			// then, check if there is from its children!

			examplesPerClass[i] += ExamplesPerNode(node, labeled);

			//List<GenericTreeNode> lista = node.getChildren();

			//  System.out.println("Examples class "+ i+ "- "+ target + " - "+ lista.size()+": "+examplesPerClass[i]);

			if( examplesPerClass[i] >0){

				PresentClass[i] = true;
				cad += getNameClass(i)+"\n";
			}



		}



		return cad;


	}

	*/

	private void createTree(){

		Pair<String, Double> rootPair = new Pair<String, Double>("root", 0.0);

		GenericTreeNode root = new GenericTreeNode(rootPair);

		treeClasses = new GenericTree();

		treeClasses.setRoot(root);


		//int cont=0;

		for (int i =0; i< this.numberOfClass; i++){

			//cont++;

			String target=getNameClass(i);

			//  System.out.print(target+",");
			String parts[] = target.split("/");

			if(parts.length>this.MaximumDepth)
				MaximumDepth=parts.length;
			
			String name ="";

			for(int n=0; n<parts.length; n++){

				name+=parts[n];
				Pair<String, Double> dato = new Pair<String, Double>(name, 0.0);
				GenericTreeNode newDato = new GenericTreeNode(dato);


				//this.classes[i]=name;

				if(n==0){
					if(!treeClasses.exists(dato)){
						root.addChild(newDato);  
					}

				}else{

					String trozos[] = name.split("/");
					String padre= "";
					for(int m=0; m<trozos.length-2; m++)
						padre += trozos[m]+"/";
					padre+=trozos[trozos.length-2];

					// System.out.println(name+ ": "+ padre);
					Pair<String, Double> parent = new Pair<String, Double>(padre, 0.0);

					GenericTreeNode daddy= treeClasses.find(parent);

					List forks = (List) daddy.getChildren();

					boolean Included = false;

					for(int m=0; m< forks.size() && !Included; m++){

						if(forks.get(m).equals(newDato)){
							Included=true;
						}
					}

					if(!Included){
						daddy.addChild(newDato);
					}


				}

				name+="/";
			}


			// }

		}

		// System.out.println("NUmber of nodes: "+treeClasses.getNumberOfNodes()+ ", ");
		//  System.out.println(treeClasses.toStringWithDepth());


		//this.treeClasses =

		//treeClasses.printTree(1);

	}


	public int numberOfLabelsUnderRoot(){
		int number =0;
		for(int i=0; i<numberOfClass;i++){
			if(this.listOfParents(i).size()==0){
				number++;
			}
			
		}
		return number;	
	}

	
	
	private void createDAGv2(){
		
		System.out.println("Creating DAG");

		Pair<String, Double> rootPair = new Pair<String, Double>("root", 0.0);

		GenericTreeNode root = new GenericTreeNode(rootPair);

		treeClasses = new GenericTree();

		treeClasses.setRoot(root);
		
		Attribute[] a = Attributes.getOutputAttributes();

		//here I have the structure of the DAG
		
		String structure = Attributes.lineClasses;
		structure = structure.substring(18);
		structure = structure.replace("}", "");
		
		System.out.println(structure);
		
		String hierarchies[]=structure.split(",");
		
		for(int i=1; i<hierarchies.length; i++){  // the first one is always the root!
			String parts[]=hierarchies[i].split("/");
			
			Pair<String, Double> nodo = new Pair<String, Double>(parts[0], 0.0);
			GenericTreeNode daddy= treeClasses.find(nodo);
			
			if(daddy==null){
				GenericTreeNode newDato = new GenericTreeNode(nodo);
				
				Pair<String, Double> hijo = new Pair<String, Double>(parts[1], 0.0);
				GenericTreeNode child = new GenericTreeNode(hijo);
				
				newDato.addChild(child);
				
				root.addChild(newDato);

				
			}else{
				
				Pair<String, Double> hijo = new Pair<String, Double>(parts[1], 0.0);
				GenericTreeNode newDato = new GenericTreeNode(hijo);

				daddy.addChild(newDato);
				
			}

		}
		
		
	
		System.out.println(treeClasses.toStringWithDepth());
		
		//System.exit(1);
		
	}
	
	private void createDAGtree(){

		Pair<String, Double> rootPair = new Pair<String, Double>("root", 0.0);

		GenericTreeNode root = new GenericTreeNode(rootPair);

		treeClasses = new GenericTree();

		treeClasses.setRoot(root);


		//int cont=0;

		for (int i =0; i< this.numberOfClass; i++){

			//cont++;
			String target=getNameClass(i);

			
			System.out.print(target+",");
			String parts[] = target.split("/");

			String name ="";

			for(int n=0; n<parts.length; n++){

				name+=parts[n];
				Pair<String, Double> dato = new Pair<String, Double>(name, 0.0);
				GenericTreeNode newDato = new GenericTreeNode(dato);




				if(n==0){
					if(!treeClasses.exists(dato)){
						root.addChild(newDato);  
					}

				}else{

					String trozos[] = name.split("/");
					String padre= "";
					for(int m=0; m<trozos.length-2; m++)
						padre += trozos[m]+"/";
					padre+=trozos[trozos.length-2];

					// System.out.println(name+ ": "+ padre);
					Pair<String, Double> parent = new Pair<String, Double>(padre, 0.0);

					GenericTreeNode daddy= treeClasses.find(parent);

					List forks = (List) daddy.getChildren();

					boolean Included = false;

					for(int m=0; m< forks.size() && !Included; m++){

						if(forks.get(m).equals(newDato)){
							Included=true;
						}
					}

					if(!Included){
						daddy.addChild(newDato);
					}


				}

				name+="/";
			}


			// }

		}

		// System.out.println("NUmber of nodes: "+treeClasses.getNumberOfNodes()+ ", ");
		//  System.out.println(treeClasses.toStringWithDepth());


		//this.treeClasses =

		//treeClasses.printTree(1);

	}


	/**
	 * modifies the treeClasses variable, including the probabilities of one unlabeled instances.
	 * @param prob
	 */
	public void establishProb(double [] prob){


		for (int i =0; i< this.numberOfClass; i++){
			// if(this.PresentClass[i]){
			
			String target=getNameClass(i);

			Pair<String, Double> node = new Pair<String, Double>(target, 0.0);

			GenericTreeNode toBeModified = treeClasses.find(node);

			if(toBeModified!=null){

				node = new Pair<String, Double>(target, prob[i]);

				toBeModified.setData(node);
			}else{
				System.out.println("Not found: "+target);
			}

			//  }
		}

		//  System.out.println("NUmber of nodes: "+treeClasses.getNumberOfNodes()+ ", ");


		//  System.out.println(treeClasses.toStringWithDepth());
	}

/*

	public int ExamplesPerNode(GenericTreeNode nodo , PrototypeSet labeled){

		int output=0;

		List<GenericTreeNode> lista = nodo.getChildren();



		if(lista.size()==0){ // we have reached one leaf
			// determine number of class

			return output;
		}else{

			for (GenericTreeNode node: lista){
				Pair<String, Double> dato = (Pair<String, Double>) node.getData();

				int clase;

				if(this.classes.size()!=0)
					clase = classes.indexOf(dato.getFirst());
				else
					clase= getNumClass(dato.getFirst());;
				
				//int clase = a[0].getNominalValuesList().indexOf(dato.getFirst());

				output+=labeled.getFromMultipleClasses(clase).size();

				output+= ExamplesPerNode(node, labeled);

			}

		}


		return output;

	}
*/


	public ArrayList<String> obtainMostConfidenceClasses(double cutOff){
		GenericTreeNode root = this.treeClasses.getRoot();

		this.cutOff = cutOff;

		return this.obtainMostConfidenceClasses(root);
	}

	public ArrayList<String> obtainMostConfidenceClasses(GenericTreeNode nodo){

		ArrayList<String> output = new ArrayList<String>();

		List<GenericTreeNode> lista = nodo.getChildren();

		if(lista.size()==0){ // we have reached one leaf
			return output;  
		}else{

			for (GenericTreeNode node: lista){

				Pair<String, Double> dato = (Pair<String, Double>) node.getData();

				if(dato.second()>cutOff){
					output.add(dato.first());
					output.addAll(obtainMostConfidenceClasses(node));
				}

			}
		}

		return output;

	}

	
	
	public String getNameClass(int i){
		
		Attribute[] a = Attributes.getOutputAttributes();

		String target;
		
		if(this.classes.size()!=0)
			target = classes.get(i);
		else{
			
		target=  (String) a[0].getNominalValuesList().get(i);
			
			
		}
		return target;
		
	}

	public int getNumClass(String name){
		Attribute[] a = Attributes.getOutputAttributes();

		int clase;

		if(this.classes.size()!=0)
			clase = classes.indexOf(name);
		else
			clase= a[0].getNominalValuesList().indexOf(name);
	
		return clase;
			
	}
	
	public int getMaxDepth(){
		return this.MaximumDepth;
	}
}
