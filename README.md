# LabellingHMC

This repository includes all the software associated to the paper:

I. Triguero, C. Vens. Labelling Strategies for Hierarchical Multi-Label Classification Techniques. Pattern Recognition, in press. doi: 10.1016/j.patcog.2016.02.017 

This software requires a list of datasets for which we have the output files provided by Clus-HMC Software (https://sourceforge.net/projects/clus/).

The output files required from Clus-HMC should be named as:

Validation: 
\<nameData\>.test.pred.arff.val  and  \<nameData\>.out.val

Test:
\<nameData\>.test.pred.arff.test  and  \<nameData\>.out.test

Please modify main.java to indicate the list of datasets you are going to process (Line 45:  String datas[]={"....","...."}.

Using these files, the main program applies all the optimisation strategies proposed in the paper for F-measure, Normalised H-loss and HMC-loss, that are:

- Single threshold selection for each error measure.
- Single threshold selection for the class distribution approach.
- Single threshold selection for the label approach.
- Multiple thresholds seletion for each error measure.
- Multiple thresholds seletion for the class distribution approach.
 
As a result, the program will output a file per dataset, measure and phase (validation or test).

\<nameData\>-\<measure\>-\<phase\>.txt

In each file, we find the results for each dataset in the following form (See Tables 3, 4 in the paper):

Dataset name  |   Error Measure (STS) | Error Measure (MTS) |  Class Distribution (STS) | Class Distribution (MTS) | Label Cardinality (STS)

In addition it will also provide a runtime comparison and a percentage of non labeled examples (see Section 4.2.2 of the paper for details). 



