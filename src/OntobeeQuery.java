import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import static org.semanticweb.owlapi.search.EntitySearcher.getAnnotationObjects;

public class OntobeeQuery {
	
	public static OWLOntology bao;
	public static OWLOntology obi;
	public static ArrayList<OWLOntology> allOntologies = new ArrayList<OWLOntology>();
	public static HashMap<OWLOntology, OWLReasoner> reasonerMap;
	public static boolean init = false;
	static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	
	public static void main(String[] args){
		parseOntologies();
		Scanner s = new Scanner(System.in);
		String query = s.nextLine();
		while(!query.equals("exit")){
			System.out.println(queryOntology(bao, query));
			System.out.println(queryOntology(obi, query));
			query = s.nextLine();
		}
		s.close();
	}
	
	public static HashSet<String> queryAllOntologies(String query){
		if(!init){
			parseOntologies();
		}
		HashSet<String> results = new HashSet<String>();
		for(OWLOntology onto: allOntologies){
			results.addAll(queryOntology(onto, query));
		}
		return results;
	}
	
	public static void parseOntologies(){
		if(!init){
			init = true;
			reasonerMap = new HashMap<OWLOntology, OWLReasoner>();
			bao = parseOntology(new File("ontologies/bao.owl"));
			obi = parseOntology(new File("ontologies/obi.owl"));
			allOntologies.add(bao);
			allOntologies.add(obi);
		} else {
			System.out.println("Ontologies already initialized...");
		}
	}
	
	public static OWLOntology parseOntology(File file){
		IRI documentIRI = IRI.create(file);
		OWLOntology ontology = null;
		
		try {
			ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
			
			System.out.println("Ontology Loaded...");
	        System.out.println("Document IRI: " + documentIRI);
	        System.out.println("Ontology : " + ontology.getOntologyID());
	        System.out.println("Format      : " + manager.getOntologyFormat(ontology));

	        OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
	        System.out.println("Class       : " + clazz);
	        // Print the hierarchy below thing
	        OWLReasoner reasoner = new StructuralReasoner(ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
	        reasonerMap.put(ontology, reasoner);
	        //printHierarchy(clazz, reasoner, ontology);
	        
		} catch (OWLException e) {
			e.printStackTrace();
		}
		return ontology;
	}
	
	public static HashSet<String> queryOntologyChildren(OWLOntology ontology, String s){
		OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
		OWLReasoner reasoner = reasonerMap.get(ontology);
	    try {
			return getChildren(reasoner, clazz,0, ontology, s);
		} catch (OWLException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	private static HashSet<String> getChildren(@Nonnull OWLReasoner reasoner, @Nonnull OWLClass clazz, int level, OWLOntology ontology, String query) throws OWLException {
	 	HashSet<String> children = new HashSet<String>();
        if (reasoner.isSatisfiable(clazz)) {
            String s = labelFor(clazz, ontology);
            if(s.equals(query)){
            	for (OWLClass heir:  reasoner.getSubClasses(clazz, false).getFlattened()){
            		children.add(labelFor(heir, ontology));
            	}
            }
            
            /* Find the children and recurse */
            for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
                if (!child.equals(clazz)) {
                	children.addAll(getChildren(reasoner, child, level + 1, ontology, query));
                }
            }
        }
        return children;
	}
	
	public static HashSet<String> queryOntology(OWLOntology ontology, String s){
		OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
		OWLReasoner reasoner = reasonerMap.get(ontology);
	    try {
			return getHierarchy(reasoner, clazz,0, ontology, s);
		} catch (OWLException e) {
			e.printStackTrace();
		}
	    return null;
	}

	 private static HashSet<String> getHierarchy(@Nonnull OWLReasoner reasoner, @Nonnull OWLClass clazz, int level, OWLOntology ontology, String query) throws OWLException {
		 	HashSet<String> parents = new HashSet<String>();
	        if (reasoner.isSatisfiable(clazz)) {
	            for (int i = 0; i < level * 4; i++) {
	            	//System.out.print(" ");
	            }
	            String s = labelFor(clazz, ontology);
	            if(s.contains(query)){
	            	//System.out.println(labelFor(clazz, ontology));
	            	for (OWLClass heir: reasoner.getSuperClasses(clazz, false).getFlattened()){
	            		parents.add(labelFor(heir, ontology));
	            	}
	            }
	            
	            /* Find the children and recurse */
	            for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
	                if (!child.equals(clazz)) {
	                    parents.addAll(getHierarchy(reasoner, child, level + 1, ontology, query));
	                }
	            }
	        }
	        return parents;
	    }
	 

	    private static String labelFor(@Nonnull OWLClass clazz, OWLOntology ontology) {
	        /*
	         * Use a visitor to extract label annotations
	         */
	        LabelExtractor le = new LabelExtractor();
	        for (OWLAnnotation anno : getAnnotationObjects(clazz, ontology)) {
	            anno.accept(le);
	        }
	        /* Print out the label if there is one. If not, just use the class URI */
	        if (le.getResult() != null) {
	            return le.getResult();
	        } else {
	            return clazz.getIRI().toString();
	        }
	    }
}
