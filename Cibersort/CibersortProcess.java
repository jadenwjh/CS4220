import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CibersortProcess {
    private static ArrayList<Case> cases = new ArrayList<Case>();
    private static HashMap<String,String> ensbHugoMap = new HashMap<String,String>(); // ensb key hugo value, only contains those in LM22
    private static HashMap<String,String> fileUuidMap = new HashMap<String,String>(); // file key uuid value
    private static HashMap<String,String> uuidCaseMap = new HashMap<String,String>(); // uuid key case value
    private static Set<String> targetFiles = new HashSet<String>(); // target file names
    private static Set<String> lm22 = new HashSet<String>();

    public static void main(String[] args) {
        findCasesMissingData();
        init();
        read();
        write();
    }

    private static void init() {
        getEnsbToHugoMap();
        getFileToCaseMap();
        getCaseUuidToCaseIdMap();
        getTargetFiles();
    }

    /**
     * Find the difference between 967 filtered cases of interest and the available cases in dataset.
     * Determine cases that were missing FPKM data.
     */
    private static void findCasesMissingData() {
        Set<String> allCases = new HashSet<String>();
        Set<String> mappedCases = new HashSet<String>();
        int mapped = 0;
        int all = 0;
        try {
            File all_cases = new File("./filtered_cases.txt");
            Scanner allS = new Scanner(all_cases);
            while (allS.hasNextLine()) {
                String line = allS.nextLine();
                if (line.matches("TCGA-..-....")) {
                    allCases.add(line);
                    all++;
                }
            }
            allS.close();
            File avail_cases = new File("./file_case_mapping/available_cases.txt");
            Scanner availS = new Scanner(avail_cases);
            while (availS.hasNextLine()) {
                String in = availS.nextLine();
                if (in.matches("submitter_id TCGA-..-....")) {
                    String id = in.split(" ")[1];
                    mappedCases.add(id);
                    mapped++;
                }
            }
            availS.close();
            allCases.removeAll(mappedCases);
            System.out.println(String.format("%d out of %d cases without gene information:", all - mapped, all));
            for (String missing : allCases) {
                System.out.println(missing);
            }
        } catch (IOException e) {
            System.out.println("Error finding cases with missing data");
            return;
        }
    }

    /**
     * Retrieves HUGO gene symbols of genes included in LM22.
     */
    private static void getLM22Map() {
        try {
            File f = new File("./gene_mapping/LM22.txt");
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String hugo = s.nextLine();
                lm22.add(hugo);
            }
            s.close();
        } catch (IOException e) {
            System.out.println("Error mapping lm22");
            return;
        }
    }

    /**
     * Maps ENSB gene symbols to respective HUGO gene symbols in LM22. 
     */
    private static void getEnsbToHugoMap() {
        getLM22Map();
        try {
            File f = new File("./gene_mapping/mapping.txt");
            Scanner s = new Scanner(f);
            String in = s.nextLine(); //skip table head
            while (s.hasNextLine()) {
                in = s.nextLine();
                String[] inputs = in.split("\\s");
                if (inputs.length == 2) {
                    String hugo = inputs[0];
                    String ensb = inputs[1];
                    if (lm22.contains(hugo)) {
                        ensbHugoMap.put(ensb, hugo);
                    }
                }
            }
            s.close();
        } catch (IOException e) {
            System.out.println("Error mapping genes");
            return;
        }
    }

    /**
     * Maps file names to respective Case UUIDs.
     */
    private static void getFileToCaseMap() {
        try {
            File file = new File("./file_case_mapping/file-case_map.json");
            Scanner s = new Scanner(file);
            StringBuilder sb = new StringBuilder();
            while (s.hasNextLine()) {
                sb.append(s.nextLine().trim());
            }
            s.close();
            String[] entries = sb.toString().split("},");
            Pattern casePattern = Pattern.compile("........-....-....-....-............");
            Pattern filePattern = Pattern.compile("........-....-....-....-.............FPKM.txt.gz");
            for (String entry : entries) {
                String uuid = "";
                String fileID = "";
                Matcher m1 = casePattern.matcher(entry);
                if (m1.find()) {
                    uuid = m1.group();
                }
                Matcher m2 = filePattern.matcher(entry);
                if (m2.find()) {
                    fileID = m2.group();
                }
                if (!uuid.isEmpty() && !fileID.isEmpty()) {
                    fileUuidMap.put(fileID, uuid);
                }
            }
        } catch (IOException e) {
            System.out.println("Error converting file ID json to map");
            return;
        }
    }

    /**
     * Maps Case UUIDs to Case IDs.
     */
    private static void getCaseUuidToCaseIdMap() {
        try {
            File f = new File("./file_case_mapping/uuid-case_map.txt");
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String in = s.nextLine();
                String[] ins = in.split("\t");
                String uuid = ins[0];
                String caseId = ins[1];
                uuidCaseMap.put(uuid, caseId);
            }
            s.close();
        } catch (IOException | NullPointerException exception) {
            System.out.println("Unable to map case uuid to case id");
            return;
        }
    }

    /**
     * Retrieves file names containing tumor samples of each 962 patients.
     */
    private static void getTargetFiles() {
        try {
            File f = new File("./file_case_mapping/tumor_fpkm_files.txt");
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String in = s.nextLine();
                targetFiles.add(in);
            }
        } catch (IOException | NullPointerException exception) {
            System.out.println("Unable to get target files id");
            return;
        }
    }
    
    /**
     * Scans through the entire dataset and maps each entry.
     */
    private static void read() {
        int casesCount = 0;
        try {
            File f = new File("./MANIFEST.txt");
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String in = s.nextLine();
                String[] ins = in.split("\t");
                String dir = ins[1];
                if (!dir.equals("filename") && dir.contains("FPKM.txt.gz")) {
                    String[] ids = dir.split("/");
                    String fileID = ids[1];
                    if (targetFiles.contains(fileID)) {
                        String uuid = fileUuidMap.get(fileID);
                        String caseId = uuidCaseMap.get(uuid);
                        Case caseObj = unzip(dir, caseId);
                        cases.add(caseObj);
                        casesCount++;
                    }
                }
            }
            s.close();
            System.out.println("Number of cases: " + casesCount);
        } catch (IOException | NullPointerException exception) {
            System.out.println("Unable to find files");
            return;
        }
    }

    private static Case unzip(String dir, String caseId) {
        try {
            String f = "./../Isolation of Breast Cancer Related Genes' Expression/datasets/" + dir;
            GZIPInputStream in = new GZIPInputStream(new FileInputStream(f));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            Case caseObj = new Case(caseId);
            br.lines().forEach(line -> {
                String[] ls = line.split("\t");
                String ensb = ls[0].substring(0,15);
                String gene = ensbHugoMap.get(ensb); //only those in lm22 is not null
                if (gene != null) {
                    String v = ls[1];
                    if (v.isEmpty() || v == null) {
                        v = "0.0";
                    }
                    caseObj.genes.put(gene, v);
                }
            });
            br.close();
            return caseObj;
        } catch (IOException exception) {
            System.out.println("Error unzipping files");
            return null;
        }
    }

    /**
     * Writes the extracted data into a text file for CIBERSORT.
     */
    private static void write() {
        String fileName = "./output/output.txt";
        try {
            File dir = new File(fileName);
            FileWriter f = new FileWriter(dir);
            f.write("Samples" + "\t");
            for (Case caseObj : cases) {
                f.write(String.format("%s", caseObj.caseId) + "\t");
            }
            f.write("\n");
            for (String gene : lm22) {
                f.write(gene + "\t");
                for (Case caseObj : cases) {
                    String value = caseObj.genes.get(gene);
                    if (value == null) {
                        value = "0.0";
                    }
                    f.write(value + "\t");
                }
                f.write("\n");
            }
            f.close();
            System.out.println("File written in: " + fileName);
        } catch (IOException e) {
            System.out.println("Error writing file");
            return;
        }
    }
}

class Case {
    public String caseId;
    public HashMap<String,String> genes;

    public Case(String caseId) {
        this.genes = new HashMap<String,String>();
        this.caseId = caseId;
    }
}