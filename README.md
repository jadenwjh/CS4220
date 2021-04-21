# CS4220
Source code used for Project.

## Codes used in the project

* [Cibersort](#Cibersort)
* [Cox Analysis](#Cox_Analysis)


## Cibersort

<img src="Cibersort/Cibersort.png" width="600" height="220"/>

* [CibersortProcess.java](https://github.com/jadenwjh/CS4220/blob/main/Cibersort/CibersortProcess.java)
    * Executed to generate the mixture file to be sent to CIBERSORT.
    * Also determines the cases without FPKM data (GDC does not have FPKM records of these patients).

* [filtered_cases.txt](https://github.com/jadenwjh/CS4220/blob/main/Cibersort/filtered_cases.txt)
    * Contains the 967 cases-of-interest after dropping cases or imputation (from 1098 cases).

* [Datasets](https://github.com/jadenwjh/CS4220/tree/main/Cibersort/datasets)
    * Using `filtered_cases.txt`, these datasets were pulled from GDC.
    * Includes a `MANIFEST.txt` file, which contains the filenames of all downloaded files.

* [File to case ID mapping](https://github.com/jadenwjh/CS4220/tree/main/Cibersort/file_case_mapping)
    * `MANIFEST.txt` file does not include the case ID of each dataset.
    * Using a scrapped JSON, these file IDs were mapped to their respective Case IDs.

* [Gene mapping](https://github.com/jadenwjh/CS4220/tree/main/Cibersort/gene_mapping)
    * GDC FPKM files uses the ENSEMBL gene symbols which were incompatible with CIBERSORT's LM22 signature matrix, which required HUGO symbols.
    * Maps the ENSEMBL symbols to HUGO symbols.

* [Output](https://github.com/jadenwjh/CS4220/tree/main/Cibersort/output)
    * Directory to store the output mixture file after executing `CibersortProcess.java`.

